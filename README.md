# Atendimento API

API REST multi-tenant para plataforma de atendimento via WhatsApp.

O sistema isola dados por empresa (tenant), expõe autenticação JWT,
operações de contatos, conversas e mensagens, e recebe mensagens inbound
por webhook assinado. A demonstração HTTP usa Scalar; não há frontend
dedicado neste repositório.

| | |
|---|---|
| **Linguagem** | Java 25 |
| **Framework** | Spring Boot 4.1 |
| **Persistência** | PostgreSQL 17 (RLS), Flyway, Spring Data JPA |
| **Segurança** | JWT HS256, Spring Security |
| **Qualidade** | Spotless, Checkstyle, Testcontainers, JUnit |

## Documentação

Objetivo da pasta [docs/](docs/README.md): explicar o desenho da aplicação e
permitir reproduzir a verificação completa (automatizada e no Scalar).

| Recurso | Descrição |
|---|---|
| [docs/](docs/README.md) | Índice: arquitetura, segurança, fluxos, API |
| [Primeiros passos](docs/guides/getting-started.md) | Ambiente, variáveis e boot |
| [Roteiro Scalar](docs/guides/verification-scalar.md) | Como testar tudo, passo a passo (local) |
| [CI/CD e ambientes](docs/guides/ci-cd.md) | GitHub Actions, GHCR, Dokploy, produção |
| [Requisitos → código](docs/guides/requirements-mapping.md) | Contrato esperado vs implementação |
| `/scalar` (profile `dev`) | OpenAPI interativo (só local / `dev`) |
| `/v3/api-docs` | OpenAPI JSON (só local / `dev`) |

## Pré-requisitos

- JDK 25
- Docker Engine com Compose
- Make (os alvos delegam ao Gradle Wrapper)
- `curl` e `openssl` (apenas para o POST assinado do webhook no terminal)

## Início rápido

A partir do diretório `atendimento/`:

**bash**

```bash
cp .env.example .env
docker compose up -d
make boot
```

**fish**

```fish
cp .env.example .env
docker compose up -d
make boot
```

A aplicação escuta em `http://localhost:8080`.

| URL | Função |
|---|---|
| `http://localhost:8080/actuator/health` | Saúde |
| `http://localhost:8080/scalar` | Documentação interativa |
| `http://localhost:8080/v3/api-docs` | Especificação OpenAPI |

### Variáveis obrigatórias

Definidas em `.env` (modelo: `.env.example`). Sem default na aplicação:

| Variável | Função |
|---|---|
| `JWT_SECRET` | Assinatura HS256 (≥ 32 caracteres) |
| `META_APP_SECRET` | HMAC de `POST /webhooks/messages` |
| `META_VERIFY_TOKEN` | Desafio de `GET /webhooks/messages` |
| `PLATFORM_ADMIN_EMAIL` | Seed do administrador da plataforma |
| `PLATFORM_ADMIN_PASSWORD` | Senha do administrador da plataforma |

O `bootRun` carrega o `.env` automaticamente. Em produção, use secrets do
ambiente de execução.

## Ambientes

| Ambiente | Uso na entrega |
|---|---|
| Local (`make boot` + Scalar) | Verificação completa recomendada |
| Produção (`https://api.atendimento.felipeduan.com`) | Instância publicada; health e API via HTTP |
| QA (`develop`) | Interno — URL não publicada |

Em produção, Scalar e OpenAPI ficam desligados. Detalhes de pipeline e
deploy: [docs/guides/ci-cd.md](docs/guides/ci-cd.md).

## Verificação

**bash** / **fish** (comandos idênticos):

```bash
make check
make test-fresh
curl -sS http://localhost:8080/actuator/health
```

```fish
make check
make test-fresh
curl -sS http://localhost:8080/actuator/health
```

- `make check` — formatação, Checkstyle e suíte de testes (gate de CI)
- `make test-fresh` — limpa artefatos e reexecuta a suíte
- Roteiro funcional completo (local): [docs/guides/verification-scalar.md](docs/guides/verification-scalar.md)
- Produção (smoke): `curl -sS https://api.atendimento.felipeduan.com/actuator/health`

## Recepção de mensagens

| Endpoint | Autenticação | Observação |
|---|---|---|
| `POST /webhooks/messages` | HMAC (`X-Hub-Signature-256`) | Contrato público de ingestão |
| `POST /mensagens/entrada` | JWT do tenant | Mesma regra de negócio; seção **Mensagens** no Scalar |

Detalhes: [docs/flows/webhook-inbound.md](docs/flows/webhook-inbound.md) e
[docs/flows/mensagem-entrada-autenticada.md](docs/flows/mensagem-entrada-autenticada.md).

## Arquitetura (resumo)

Monólito modular (package-by-feature). Isolamento por tenant no PostgreSQL
com RLS (`ENABLE` e `FORCE`). O claim `tenant_id` do JWT é a fonte do
tenant nas rotas autenticadas; o webhook resolve a empresa por
`phone_number_id`.

Módulos: `empresas`, `usuarios` / `vinculos`, `auth`, `platformadmin`,
`contatos`, `conversas`, `mensagens`, `webhook`, `shared`.

Documentação expandida: [docs/architecture/overview.md](docs/architecture/overview.md).

## Escopo

**Implementado:** autenticação, empresas, usuários e vínculos, contatos,
conversas, mensagens (saída e entrada autenticada), webhook assinado,
Scalar, pipeline `make check`.

**Não implementado:** cliente de envio outbound (Graph API), SSE, rate
limiting (Bucket4j).
