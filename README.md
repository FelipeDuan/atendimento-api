# Atendimento API

API REST multi-tenant para atendimento via WhatsApp: empresas (tenants),
usuários, contatos, conversas, mensagens e recepção de mensagens inbound.

**Stack:** Java 25 · Spring Boot 4.1 · PostgreSQL 17 (RLS) · Redis · Flyway ·
JWT HS256 · MapStruct · Testcontainers · springdoc + Scalar.

## Destaques

- Isolamento entre tenants no PostgreSQL com RLS (`ENABLE` e `FORCE`)
- Tenant resolvido exclusivamente pelo claim `tenant_id` do JWT
- Recepção de mensagens por webhook público (HMAC) ou por rota autenticada
- Monólito modular package-by-feature, com fronteiras entre módulos
- Soft delete por `status`; listagens paginadas
- Cobertura automatizada de isolamento multi-tenant (Testcontainers)

## Arquitetura

Monólito modular. Módulos principais:

| Módulo | Responsabilidade |
|---|---|
| `empresas` | Tenants e credenciais WhatsApp (`phone_number_id`) |
| `usuarios` / `vinculos` | Conta global + perfil/status por empresa |
| `auth` / `platformadmin` | Login, troca de senha, switch de tenant, admin da plataforma |
| `contatos` | Contatos do tenant (soft delete + reativação) |
| `conversas` / `mensagens` | Ciclo de vida da conversa e mensagens |
| `webhook` | Recepção pública de mensagens (orquestra contatos/mensagens) |
| `shared` | Segurança, tenancy/RLS, erros RFC 9457, OpenAPI |

## Pré-requisitos

- JDK 25
- Docker (PostgreSQL 17 e Redis via Compose)
- Make (opcional; os alvos delegam ao Gradle Wrapper)

## Como executar

```bash
cp .env.example .env
docker compose up -d
make boot
```

Variáveis obrigatórias no `.env` (sem default na aplicação): `JWT_SECRET`
(≥ 32 bytes), `META_APP_SECRET` e `META_VERIFY_TOKEN`. Em produção, forneça-as
apenas por secrets do ambiente. O profile `test` define valores próprios para
a suíte automatizada.

A aplicação sobe em http://localhost:8080. Documentação interativa (profile
`dev`): http://localhost:8080/scalar.

## Verificação

Quem clonar o repositório deve validar a build e o comportamento da API na
ordem abaixo.

### 1. Qualidade e testes automatizados

```bash
make check        # formatação, Checkstyle e suíte de testes
make test-fresh   # limpa artefatos e reexecuta a suíte completa
```

A suíte usa Testcontainers (PostgreSQL). Inclui, entre outros, isolamento
multi-tenant (RLS), autenticação, ciclo de vida de conversas/mensagens e
recepção por webhook. O gate de CI equivale a `make check`.

### 2. Subir a API local

Com Compose e `.env` configurados:

```bash
make boot
curl -sS http://localhost:8080/actuator/health
```

Resposta esperada: status `UP`.

### 3. Fluxo funcional ponta a ponta

Pode ser exercitado no Scalar ou via HTTP. Sequência mínima:

1. `POST /auth/plataforma/login` — credenciais do Platform Admin do `.env`.
2. `POST /empresas` — provisiona tenant e administrador inicial (senha
   temporária; resposta com `exigeTrocarSenha` quando aplicável).
3. `POST /auth/login` com o admin da empresa; se necessário,
   `POST /auth/trocar-senha` e novo login.
4. Autorizar com o `accessToken` (Bearer).
5. `POST /contatos` → `POST /conversas` → `POST /mensagens` (saída).
6. Entrada autenticada: `POST /mensagens/entrada` com número, tipo e conteúdo.
7. Alternativa pública: configurar `phoneNumberId` em `PUT /empresas/{id}` e
   enviar `POST /webhooks/messages` com assinatura HMAC (exemplo abaixo).
8. `PATCH /conversas/{id}` com `ENCERRAR` / `REABRIR`; mensagem em conversa
   encerrada deve retornar **409**.

Exemplo de webhook assinado (substitua `SEU_PHONE_ID` e carregue
`META_APP_SECRET` do `.env`):

```bash
BODY=$(cat <<'EOF'
{
  "entry": [{
    "changes": [{
      "value": {
        "metadata": { "phone_number_id": "SEU_PHONE_ID" },
        "contacts": [{
          "wa_id": "5586999990001",
          "profile": { "name": "Cliente" }
        }],
        "messages": [{
          "id": "wamid.DEMO1",
          "from": "5586999990001",
          "type": "text",
          "text": { "body": "Olá" }
        }]
      }
    }]
  }]
}
EOF
)

SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$META_APP_SECRET" -hex | awk '{print $NF}')

curl -sS -X POST localhost:8080/webhooks/messages \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=$SIG" \
  -d "$BODY"
```

Resultados esperados nesse fluxo:

| Situação | Resultado |
|---|---|
| Webhook com assinatura válida | **200**; contato/conversa/mensagem criados |
| Mesmo corpo reenviado | **200**; sem duplicar mensagem |
| Assinatura ausente ou inválida | **401** |
| `GET /webhooks/messages` com `hub.verify_token` correto | **200** e o `hub.challenge` |
| Token de outro tenant em recurso alheio | vazio ou **404** |
| Mensagem em conversa encerrada | **409** |
| Ordenação por campo não permitido | **400** |
| Acesso negado por papel | **403** (RFC 9457) |

### 4. Recepção de mensagens

Dois caminhos com a mesma regra de negócio (tenant → contato → conversa
aberta → mensagem → última interação):

| Endpoint | Autenticação | Uso |
|---|---|---|
| `POST /webhooks/messages` | HMAC (`X-Hub-Signature-256`) | Integração com provedor; payload no formato WhatsApp Cloud API |
| `POST /mensagens/entrada` | JWT do tenant | Registro autenticado com JSON simplificado |

Mensagens de saída são persistidas com `envioPendente: true`. O cliente de
envio outbound ao provedor não faz parte do escopo atual.

## Multi-tenancy e segurança

- O tenant vem do claim `tenant_id` assinado no JWT. Header de tenant não é
  aceito.
- No banco: RLS com `ENABLE` e `FORCE`, policy por
  `current_setting('app.tenant_id', true)`, índice composto com `empresa_id`
  líder.
- Por transação: `set_config(..., true)` via `TenantRlsAspect` (equivalente a
  `SET LOCAL` — evita vazamento pelo pool de conexões).
- Exceção controlada: o webhook resolve o tenant por `phone_number_id` e
  executa o trabalho sob `TenantContext.withTenantId(...)`.
- Conta única global: e-mail único em `usuario`; perfil e status vivem em
  `usuario_empresa` (RLS). Listagens de usuários passam pelo vínculo.
- Autorização: regras estáticas na `SecurityFilterChain` (`hasAuthority`) e
  regras dependentes de dado via `PermissionEvaluator`.

## API

O Scalar (`/scalar`, profile `dev`) expõe o contrato OpenAPI gerado a partir
dos controllers: caminhos, métodos, schemas e parâmetros. Textos narrativos
(`summary` / `description` por operação) ficam neste README, para manter os
controllers enxutos e a documentação em um único lugar.

Listagens retornam `PageResponse` com query params `page`, `size` e `sort`.
Campo de ordenação não permitido → **400**. Soft delete por `status`: GET de
registro inativo → **404**; reativação via `PUT` (usuários) ou `POST` do mesmo
número (contatos). Erros de negócio e **403** em RFC 9457; **401** sem corpo,
com `WWW-Authenticate` (RFC 6750).

### Auth

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `POST` | `/auth/plataforma/login` | público | Login do administrador da plataforma |
| `POST` | `/auth/login` | público | Login do usuário no tenant |
| `POST` | `/auth/trocar-senha` | `TROCAR_SENHA` | Define senha definitiva e emite token operacional |
| `POST` | `/auth/switch-tenant` | autenticado | Reemite JWT para outra empresa vinculada, sem senha |

### Empresas

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `POST` | `/empresas` | `PLATFORM_ADMIN` | Cria tenant e administrador inicial |
| `GET` | `/empresas` | `PLATFORM_ADMIN` | Lista empresas ativas |
| `GET` | `/empresas/inativas` | `PLATFORM_ADMIN` | Lista empresas inativas |
| `GET` | `/empresas/{id}` | admin da empresa ou plataforma | Detalhe |
| `PUT` | `/empresas/{id}` | admin da empresa ou plataforma | Atualiza dados (incl. `phoneNumberId`) |
| `DELETE` | `/empresas/{id}` | `PLATFORM_ADMIN` | Inativa (soft delete) |

### Usuários

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `POST` | `/usuarios` | `ADMINISTRADOR` | Cria vínculo (e conta, se e-mail novo) |
| `GET` | `/usuarios` | admin ou atendente | Lista vínculos do tenant |
| `GET` | `/usuarios/{id}` | admin ou atendente | Detalhe do vínculo ativo |
| `PUT` | `/usuarios/{id}` | `ADMINISTRADOR` | Atualiza perfil/status (reativação com `ATIVO`) |

### Contatos

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `POST` | `/contatos` | admin ou atendente | Cria ou reativa contato |
| `GET` | `/contatos` | admin ou atendente | Lista ativos |
| `GET` | `/contatos/{id}` | admin ou atendente | Detalhe |
| `PUT` | `/contatos/{id}` | admin ou atendente | Atualiza dados |
| `DELETE` | `/contatos/{id}` | admin ou atendente | Inativa (soft delete) |

### Conversas

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `POST` | `/conversas` | admin ou atendente | Abre conversa para um contato |
| `GET` | `/conversas` | admin ou atendente | Lista (`status` opcional: `ABERTA` \| `ENCERRADA`) |
| `GET` | `/conversas/{id}` | admin ou atendente | Detalhe |
| `PATCH` | `/conversas/{id}` | admin ou atendente | Corpo `{"acao":"ENCERRAR"}` ou `{"acao":"REABRIR"}` |

### Mensagens

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `GET` | `/mensagens` | admin ou atendente | Lista por `conversaId` (obrigatório) |
| `GET` | `/mensagens/{id}` | admin ou atendente | Detalhe |
| `POST` | `/mensagens` | admin ou atendente | Registra saída (`envioPendente: true`) |
| `POST` | `/mensagens/entrada` | admin ou atendente | Registra entrada no tenant do JWT |

### Webhook

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `GET` | `/webhooks/messages` | público | Verificação (`hub.mode`, `hub.verify_token`, `hub.challenge`) |
| `POST` | `/webhooks/messages` | HMAC | Recebe mensagem; resolve tenant por `phone_number_id` |

## Trade-offs conscientes

- E-mail único **globalmente**; perfil e status por vínculo. Troca de contexto
  via `/auth/switch-tenant`.
- Registro excluído (soft delete) responde **404** na leitura.
- Contato inativo reaparece se o mesmo número voltar via `POST /contatos` ou
  webhook — reativação com os dados novos (filtro explícito; sem
  `@SQLRestriction` na entidade, para permitir escrita na reativação).
- Mídia (imagem/documento) é registrada como placeholder textual; não há
  download de binário do provedor neste escopo.
- Mensagens de saída persistem com `envioPendente: true` até existir envio
  outbound ao provedor.

## Escopo atual e próximos passos

**Implementado:** autenticação, empresas, usuários/vínculos, contatos,
conversas, mensagens (saída e entrada autenticada), webhook assinado,
documentação interativa, pipeline de qualidade (`make check`).

**Não implementado ainda:** cliente de envio outbound (Graph API), SSE em
tempo real, rate limiting (Bucket4j), coleção Postman e tipagem enum na
coluna JPA de vínculo (já tipada na fronteira HTTP).
