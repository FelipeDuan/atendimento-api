# Atendimento API

API REST multi-tenant para atendimento via WhatsApp: empresas (tenants),
usuários, contatos, conversas, mensagens e ingestão de eventos da Meta Cloud
API.

**Stack:** Java 25 · Spring Boot 4.1 · PostgreSQL 17 (RLS) · Redis · Flyway ·
JWT HS256 · MapStruct · Testcontainers · springdoc + Scalar.

## Destaques

- Isolamento entre tenants no PostgreSQL com RLS (`ENABLE` e `FORCE`)
- Tenant resolvido exclusivamente pelo claim `tenant_id` do JWT
- Webhook Meta autenticado por HMAC-SHA256 (`X-Hub-Signature-256`) sobre o
  corpo bruto — secrets sem default (a aplicação não sobe sem eles)
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
| `webhook` | Verificação e ingestão Meta (orquestra contatos/mensagens) |
| `shared` | Segurança, tenancy/RLS, erros RFC 7807, OpenAPI |

Demonstração sem frontend dedicado: Scalar no profile `dev` (`/scalar`) e
`curl`.

## Como executar

```bash
cp .env.example .env   # JWT_SECRET (>= 32 bytes), senhas e secrets Meta
docker compose up -d   # Postgres + Redis
make boot              # profile dev → http://localhost:8080/scalar
make check             # spotless + checkstyle + testes (gate do CI)
```

`META_APP_SECRET` e `META_VERIFY_TOKEN` são obrigatórios (mesmo padrão de
`JWT_SECRET`). Em produção, forneça-os apenas via secrets do ambiente. O
profile `test` define valores próprios para a suíte.

## Demonstração (Scalar)

1. `docker compose up -d && make boot`
2. Abra http://localhost:8080/scalar
3. `POST /auth/plataforma/login` com o Platform Admin configurado no `.env`
4. `POST /empresas` — cria a empresa e o administrador inicial
5. `POST /auth/login` com esse admin; se `exigeTrocarSenha`,
   `POST /auth/trocar-senha` e autentique novamente
6. Em **Authorize**, informe o `accessToken` (Bearer JWT)
7. Exercite Contatos → Conversas → Mensagens
8. Associe um `phoneNumberId` à empresa (`PUT /empresas/{id}`) e envie um
   webhook assinado:

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

Reenvio idêntico → 200 sem duplicar a mensagem. Corpo alterado sem nova
assinatura → **401**.

Com token de outro tenant, recursos alheios retornam vazio ou **404**.
Mensagem em conversa encerrada → **409**.

## Multi-tenancy e segurança

- O tenant vem do claim `tenant_id` assinado no JWT (ADR-0002). Header de
  tenant não é aceito.
- No banco: RLS com `ENABLE` e `FORCE`, policy por
  `current_setting('app.tenant_id', true)`, índice composto com `empresa_id`
  líder (ADR-0001).
- Por transação: `set_config(..., true)` via `TenantRlsAspect` (equivalente a
  `SET LOCAL` — evita vazamento pelo pool de conexões).
- Exceção controlada: o webhook resolve o tenant por `phone_number_id` e
  executa o trabalho sob `TenantContext.withTenantId(...)`.
- Conta única global (ADR-0010): e-mail único em `usuario`; perfil e status
  vivem em `usuario_empresa` (RLS). Listagens de usuários passam pelo vínculo.
- Autorização: regras estáticas na `SecurityFilterChain` (`hasAuthority`) e
  regras dependentes de dado via `PermissionEvaluator` (ADR-0014).

## API

| Recurso | Métodos | Papéis |
|---|---|---|
| Auth | `POST /auth/login`, `/auth/switch-tenant`, `/auth/trocar-senha`, `/auth/plataforma/login` | público / autenticado |
| Empresas | `POST`, `GET`, `GET/{id}`, `PUT/{id}`, `DELETE/{id}` (+ inativas) | `PLATFORM_ADMIN` / admin da empresa |
| Usuários | `POST`, `GET`, `GET/{id}`, `PUT/{id}` | escrita: `ADMINISTRADOR`; leitura: admin ou atendente |
| Contatos | `POST`, `GET`, `GET/{id}`, `PUT/{id}`, `DELETE/{id}` | `ADMINISTRADOR` \| `ATENDENTE` |
| Conversas | `POST`, `GET`, `GET/{id}`, `PATCH/{id}` | `ADMINISTRADOR` \| `ATENDENTE` |
| Mensagens | `POST`, `GET?conversaId=`, `GET/{id}` | `ADMINISTRADOR` \| `ATENDENTE` |
| Webhook | `GET` / `POST /webhooks/messages` | público + HMAC |

Listagens usam `PageResponse`. Soft delete por `status` — GET de registro
inativo retorna **404**.

## Trade-offs conscientes

- E-mail único **globalmente**; perfil e status por vínculo (ADR-0010). Troca
  de contexto via `/auth/switch-tenant`.
- Registro excluído (soft delete) responde **404** na leitura (ADR-0017).
- Contato inativo reaparece se o mesmo número voltar via `POST /contatos` ou
  webhook — reativação com os dados novos (filtro explícito; sem
  `@SQLRestriction` na entidade, para permitir escrita na reativação).
- Mídia (imagem/documento) é registrada como placeholder textual; não há
  download de binário da Meta neste escopo (ADR-0009).
- Mensagens de saída persistem com `envioPendente: true` até existir envio
  real pela Graph API.

## Escopo atual e próximos passos

**Implementado:** autenticação, empresas, usuários/vínculos, contatos,
conversas, mensagens, webhook assinado, documentação interativa (Scalar),
pipeline de qualidade (`make check`).

**Não implementado ainda:** cliente Graph API para envio outbound, SSE em
tempo real, rate limiting (Bucket4j), coleção Postman e tipagem enum na
coluna JPA de vínculo (já tipada na fronteira HTTP).
