# Primeiros passos

## Pré-requisitos

- JDK 25
- Docker Engine com Compose
- Make (delega ao `./gradlew`)
- `curl` (saúde e webhook opcional)
- `openssl` (apenas para assinar o POST do webhook no terminal)

## Ambiente

Todos os comandos abaixo partem do diretório `atendimento/` (raiz deste
módulo Maven/Gradle).

### 1. Configurar variáveis

**bash** / **fish**

```bash
cp .env.example .env
```

```fish
cp .env.example .env
```

Edite `.env` se necessário. Obrigatórias (sem default na aplicação):

| Variável | Função |
|---|---|
| `JWT_SECRET` | Assinatura HS256 (≥ 32 caracteres) |
| `META_APP_SECRET` | HMAC do webhook |
| `META_VERIFY_TOKEN` | Desafio GET do webhook |
| `PLATFORM_ADMIN_EMAIL` | E-mail do admin da plataforma |
| `PLATFORM_ADMIN_PASSWORD` | Senha do admin da plataforma |

Valores de referência locais estão em `.env.example`.

### 2. Subir dependências e a API

**bash** / **fish**

```bash
docker compose up -d
make boot
```

```fish
docker compose up -d
make boot
```

O alvo `make boot` executa `./gradlew bootRun` (profile `dev`) e carrega o
`.env` na JVM.

### 3. Conferir saúde

**bash** / **fish**

```bash
curl -sS http://localhost:8080/actuator/health
```

```fish
curl -sS http://localhost:8080/actuator/health
```

Resposta esperada contém `"status":"UP"` (ou equivalente do Actuator).

| URL | Função |
|---|---|
| http://localhost:8080/actuator/health | Saúde |
| http://localhost:8080/scalar | OpenAPI interativo |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |

## Qualidade automatizada

**bash** / **fish**

```bash
make check
make test-fresh
```

```fish
make check
make test-fresh
```

| Alvo | Efeito |
|---|---|
| `make check` | Spotless + Checkstyle + testes (mesmo gate do CI) |
| `make test-fresh` | `clean` + suíte completa sem cache de testes |

A suíte usa Testcontainers (PostgreSQL). Isolamento multi-tenant e fluxos
de autenticação/webhook fazem parte da cobertura.

## Verificação funcional

Siga o roteiro em [verification-scalar.md](verification-scalar.md).
