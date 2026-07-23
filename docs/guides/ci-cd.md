# CI/CD e ambientes

Pipeline e deploy versionados em [`.github/workflows/`](../../.github/workflows/).
Hospedagem via Dokploy; artefato publicado no GHCR.

## Ambientes

| Ambiente | Branch | Imagem GHCR | URL pública | Público da entrega |
|---|---|---|---|---|
| Local | — | — | `http://localhost:8080` | Sim (caminho principal de verificação) |
| QA | `develop` | `ghcr.io/felipeduan/atendimento-api:develop` | interna | Não — ambiente privado do autor |
| Produção | `main` | `ghcr.io/felipeduan/atendimento-api:main` | `https://api.atendimento.felipeduan.com` | Sim |

O ambiente de **QA** existe para validação interna após merge em `develop`.
A URL de QA **não é publicada** nesta documentação.

### O que o avaliador pode exercitar em produção

- Health: `GET /actuator/health`
- API autenticada e webhook, com as mesmas regras do ambiente local
- Credenciais do administrador da plataforma da instância de produção são
  informadas **fora do repositório** (entrega / mensagem ao avaliador), para
  não versionar senha

**Importante:** no profile `prod`, Scalar e `/v3/api-docs` ficam **desligados**
(`application-prod.yaml`). O roteiro interativo do Scalar é para o ambiente
**local** ([verification-scalar.md](verification-scalar.md)). Em produção,
use `curl` (ou cliente HTTP) contra a URL pública.

**bash** / **fish**

```bash
curl -sS https://api.atendimento.felipeduan.com/actuator/health
```

```fish
curl -sS https://api.atendimento.felipeduan.com/actuator/health
```

## Fluxo

```text
PR / push → ci.yml (testes + lint)
                │
push develop/main → deploy.yml
                      ├─ gate: test + spotless + checkstyle
                      ├─ docker build → push GHCR
                      │     tags: :develop ou :main  +  :sha-<commit>
                      └─ POST application.deploy (Dokploy puxa a tag da branch)
```

Estratégia de deploy: **tag mutável da branch** no Dokploy (Opção A).
QA aponta para `:develop`; produção aponta para `:main`. A tag
`:sha-<commit>` existe no registry para auditoria e rollback manual; o
painél não é atualizado para SHA a cada deploy.

Diferença entre QA e produção no GitHub Actions: job com
`environment: qa` vs `environment: production` (este último pode exigir
aprovação de reviewer) e variável `DOKPLOY_*_APP_ID` distinta. A forma de
publicar a imagem é a mesma.

## Workflows

| Arquivo | Gatilho | Função |
|---|---|---|
| `ci.yml` | PR e push em `develop` / `main` | `./gradlew test`; lint Spotless + Checkstyle |
| `deploy.yml` | Push em `develop` / `main`; `workflow_dispatch` | Gate → build/push GHCR → `application.deploy` |

`workflow_dispatch` aceita `qa` ou `production` como alvo, desde que a imagem
correspondente já exista no GHCR (ou seja gerada no mesmo run a partir da
branch correta).

## Configuração no GitHub

| Nome | Tipo | Escopo |
|---|---|---|
| `DOKPLOY_API_KEY` | Secret | Token da API Dokploy |
| `DOKPLOY_SERVER_URL` | Variable | URL base do painel (ex.: `https://painel.exemplo.com`) |
| `DOKPLOY_QA_APP_ID` | Variable | ID da aplicação QA no Dokploy |
| `DOKPLOY_PROD_APP_ID` | Variable | ID da aplicação de produção no Dokploy |

`GITHUB_TOKEN` (automático) publica no GHCR com `packages: write` no job
`docker`.

Secrets de **runtime** da aplicação (`JWT_SECRET`, `META_*`,
`PLATFORM_ADMIN_*`, Postgres, Redis) ficam **somente no Dokploy**, por
ambiente — nunca no workflow.

### Manutenção com GitHub CLI (fish)

Fish não possui heredoc `<<EOF`. Exemplos:

```fish
gh variable set DOKPLOY_PROD_APP_ID --body "<APP_ID>" -R FelipeDuan/atendimento-api
```

```fish
gh secret set DOKPLOY_API_KEY -R FelipeDuan/atendimento-api
```

```fish
gh variable list -R FelipeDuan/atendimento-api
gh secret list -R FelipeDuan/atendimento-api
```

Environments `qa` e `production`:

```fish
gh api --method PUT repos/FelipeDuan/atendimento-api/environments/qa
gh api --method PUT repos/FelipeDuan/atendimento-api/environments/production
```

Reviewer opcional em `production`:

```fish
set REVIEWER_ID (gh api user -q .id)
printf '{"reviewers":[{"type":"User","id":%s}]}' $REVIEWER_ID | gh api --method PUT repos/FelipeDuan/atendimento-api/environments/production --input -
```

## Configuração no Dokploy (por app)

| Item | QA | Produção |
|---|---|---|
| Source | Docker (GHCR) | Docker (GHCR) |
| Imagem | `.../atendimento-api:develop` | `.../atendimento-api:main` |
| Registry | `ghcr.io` + credencial com `read:packages` | idem |
| Domínio / porta | interno → `8080` | `api.atendimento.felipeduan.com` → `8080` |
| Profile | tipicamente `prod` ou equivalente de runtime | `SPRING_PROFILES_ACTIVE=prod` |
| Postgres / Redis | serviços do ambiente QA | serviços do ambiente production |

A aplicação declara dependência Redis no classpath (autoconfig); o serviço
Redis no Dokploy atende essa conexão. **Não há uso de cache de negócio**
(`@Cacheable`) no código atual — ver [architecture/overview.md](../architecture/overview.md).

## Rollback

1. Painel Dokploy → Deployments → Rollback do deploy anterior  
2. Ou apontar manualmente a imagem para `ghcr.io/felipeduan/atendimento-api:sha-<commit>` e redeployar  

## Relação com a verificação local

| Objetivo | Onde |
|---|---|
| Gate automatizado (mesmo do CI) | `make check` / `make test-fresh` |
| Roteiro funcional completo com Scalar | local + [verification-scalar.md](verification-scalar.md) |
| Confirmar instância publicada | produção + `GET /actuator/health` e chamadas HTTP autenticadas |
