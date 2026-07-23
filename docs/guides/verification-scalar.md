# Roteiro de verificação (Scalar)

Guia operacional para exercitar a API de ponta a ponta: autenticação,
provisionamento de tenant, contatos, conversas, mensagens, autorização,
soft delete, ordenação, entrada autenticada e webhook assinado.

Use os valores abaixo; ajuste apenas IDs e tokens retornados nas respostas.
Ao concluir os passos, a suíte manual cobre os mesmos comportamentos
críticos cobertos pelos testes automatizados.

Índice da documentação: [../README.md](../README.md). Comandos de terminal
aparecem em **bash** e **fish** quando a sintaxe difere.

No Scalar, `POST /mensagens/entrada` fica em **Mensagens**;
`GET`/`POST /webhooks/messages` ficam em **Webhook**.

## Antes

1. Na raiz do módulo (`atendimento/`):

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

2. Abra http://localhost:8080/scalar

3. Confirme no `.env` (valores de referência alinhados ao `.env.example`):

| Variável | Valor de referência |
|---|---|
| `PLATFORM_ADMIN_EMAIL` | `admin@plataforma.local` |
| `PLATFORM_ADMIN_PASSWORD` | `trocar` |
| `META_APP_SECRET` | `dev-meta-app-secret-change-me` |
| `META_VERIFY_TOKEN` | `dev-verify-token` |
| `JWT_SECRET` | string com pelo menos 32 caracteres |

No Scalar, **Authorize** = Bearer JWT. Troque o token conforme o papel
(plataforma vs tenant).

Também é possível validar a build sem subir a API:

**bash** / **fish**

```bash
make check
make test-fresh
```

```fish
make check
make test-fresh
```

---

## PASSO 1 — Login Platform Admin

**Endpoint:** `POST /auth/plataforma/login`  
**Authorize:** não precisa

```json
{
  "email": "admin@plataforma.local",
  "senha": "trocar"
}
```

**Esperado:** 200 com `accessToken`  
**Faça:** Authorize → cole esse `accessToken`

---

## PASSO 2 — Criar empresa

**Endpoint:** `POST /empresas`  
**Authorize:** token da plataforma

```json
{
  "nome": "Empresa Demo Scalar",
  "cnpj": "12345678000199",
  "email": "contato@empresademo.local",
  "administradorInicial": {
    "nome": "Admin Demo",
    "email": "admin@empresademo.local",
    "senhaTemporaria": "SenhaTemp123!"
  }
}
```

> CNPJ = exatamente 14 dígitos. Se der 409, troque o CNPJ (ex.: `12345678000188`)
> e o e-mail do administrador, se também já existir.

**Anote:** `id` da empresa (= `EMPRESA_ID`)

---

## PASSO 3 — Login do admin da empresa

**Endpoint:** `POST /auth/login`  
**Authorize:** limpe o token (ou ignore o da plataforma)

```json
{
  "email": "admin@empresademo.local",
  "senha": "SenhaTemp123!"
}
```

**Esperado:** 200, `exigeTrocarSenha: true`, `accessToken`  
**Authorize:** cole esse token

---

## PASSO 4 — Trocar senha

**Endpoint:** `POST /auth/trocar-senha`  
**Authorize:** token do passo 3

```json
{
  "senhaAtual": "SenhaTemp123!",
  "novaSenha": "SenhaDefinitiva1!"
}
```

**Esperado:** 200 com novo `accessToken`  
**Authorize:** substitua pelo novo token (token operacional do admin do tenant)

---

## PASSO 5 — Contato

**Endpoint:** `POST /contatos`

```json
{
  "nome": "Maria Silva",
  "numeroWhatsapp": "5586999990001",
  "email": "maria@email.com",
  "observacoes": "Cliente teste Scalar"
}
```

**Anote:** `id` (= `CONTATO_ID`)

**Depois:**

- `GET /contatos` → 200, Maria na lista
- `GET /contatos/{CONTATO_ID}` → 200
- `PUT /contatos/{CONTATO_ID}`:

```json
{
  "nome": "Maria Silva Atualizada",
  "email": "maria.nova@email.com",
  "observacoes": "Atualizado no Scalar"
}
```

---

## PASSO 6 — Abrir conversa

**Endpoint:** `POST /conversas`

```json
{
  "contatoId": "COLE_AQUI_O_CONTATO_ID"
}
```

**Anote:** `id` (= `CONVERSA_ID`)

**Depois:** `GET /conversas` e `GET /conversas/{CONVERSA_ID}` → 200

---

## PASSO 7 — Enviar mensagem (saída)

**Endpoint:** `POST /mensagens`

```json
{
  "conversaId": "COLE_AQUI_O_CONVERSA_ID",
  "tipo": "TEXTO",
  "conteudo": "Olá, tudo bem?"
}
```

> `tipo`: `TEXTO` \| `IMAGEM` \| `DOCUMENTO`  
> Resposta: `sentido: SAIDA`, `envioPendente: true`

**Depois:** `GET /mensagens?conversaId=CONVERSA_ID` → mensagem na lista  
(use `page`, `size`, `sort` como query params; não envie um objeto `pageable`)

---

## PASSO 8 — Encerrar / 409 / reabrir

**Endpoint:** `PATCH /conversas/{CONVERSA_ID}`

```json
{ "acao": "ENCERRAR" }
```

Depois `POST /mensagens` de novo:

```json
{
  "conversaId": "COLE_AQUI_O_CONVERSA_ID",
  "tipo": "TEXTO",
  "conteudo": "Isso deve dar 409"
}
```

**Esperado:** **409**, título relacionado a conversa encerrada

Reabrir:

```json
{ "acao": "REABRIR" }
```

**Esperado:** 200, `status: ABERTA`

---

## PASSO 9 — Usuários

**Endpoint:** `POST /usuarios` (token admin)

```json
{
  "nome": "Atendente Scalar",
  "email": "atendente@empresademo.local",
  "senha": "SenhaTemp123!",
  "perfil": "ATENDENTE"
}
```

> `perfil`: `ADMINISTRADOR` \| `ATENDENTE`

**Anote:** `id` (= `USUARIO_AT_ID`)

**Login do atendente** (`POST /auth/login`):

```json
{
  "email": "atendente@empresademo.local",
  "senha": "SenhaTemp123!"
}
```

Se `exigeTrocarSenha: true`, execute `POST /auth/trocar-senha` e Authorize com
o novo token.

**Com token do atendente:**

- `GET /usuarios` → **200**
- `POST /usuarios` (qualquer body válido) → **403**, `title: "Acesso negado"`,
  `Content-Type: application/problem+json`

Volte ao **token admin** no Authorize.

---

## PASSO 10 — Soft delete / reativação de usuário

**Endpoint:** `PUT /usuarios/{USUARIO_AT_ID}`

```json
{
  "nome": "Atendente Scalar",
  "perfil": "ATENDENTE",
  "status": "INATIVO"
}
```

- `GET /usuarios/{USUARIO_AT_ID}` → **404**
- Reative com o mesmo PUT e `"status": "ATIVO"` → **200**
- `GET` de novo → **200**

---

## PASSO 11 — Ordenação e 403

Com token da **plataforma** no Authorize:

1. `GET /empresas?sort=campoInexistente,desc`  
   → **400**, `title: "Ordenação inválida"`, detail listando campos aceitos
   (`dataCriacao`, `id`, `nome`)

2. `GET /empresas?sort=nome,asc` → **200**

3. `GET /contatos` (ainda com token da plataforma)  
   → **403**, `title: "Acesso negado"`, `Content-Type: application/problem+json`,
   header `WWW-Authenticate`

Volte ao **token admin** no Authorize.

---

## PASSO 12 — Mensagem de entrada (autenticada)

Alternativa ao webhook para registrar entrada sem HMAC. Tenant vem do JWT.

No Scalar este endpoint está na seção **Mensagens** (`POST /mensagens/entrada`),
não em Webhook. O contrato público de integração continua sendo
`POST /webhooks/messages` (passo 13).

**Endpoint:** `POST /mensagens/entrada`  
**Authorize:** token admin (ou atendente)

```json
{
  "numeroWhatsapp": "5586999990002",
  "nome": "Cliente Entrada",
  "tipo": "TEXTO",
  "conteudo": "Oi pelo Scalar",
  "whatsappMessageId": "sim-scalar-001"
}
```

**Esperado:** **201**, `sentido: ENTRADA`, `envioPendente: false`  
**Anote:** `conversaId` e `id` da mensagem

**Idempotência:** reenvie o mesmo body → **201** com o mesmo `id` (não duplica).

**Depois:** `GET /mensagens?conversaId=...` → mensagem de entrada na lista.

Se a última conversa do contato estiver encerrada, o sistema abre uma nova
(não reabre a encerrada).

---

## PASSO 13 — Webhook (GET no Scalar + POST no terminal)

### Verificação (Scalar)

- `GET /webhooks/messages`
- Query:
  - `hub.mode` = `subscribe`
  - `hub.verify_token` = valor de `META_VERIFY_TOKEN` no `.env`
  - `hub.challenge` = `abc123`
- **Esperado:** 200 e body `abc123`

### Vincular canal à empresa (Scalar)

- `PUT /empresas/{EMPRESA_ID}` (token admin)

```json
{
  "nome": "Empresa Demo Scalar",
  "email": "contato@empresademo.local",
  "phoneNumberId": "phone-demo-scalar-001"
}
```

### Recepção assinada (terminal)

O Scalar não assina o corpo bruto. Execute a partir de `atendimento/`, com a
API no ar (`make boot`). O `phone_number_id` do JSON deve ser o mesmo do
`PUT` anterior.

**bash**

```bash
set -a
source .env
set +a

BODY='{"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"phone-demo-scalar-001"},"contacts":[{"wa_id":"5586999990001","profile":{"name":"Cliente Webhook"}}],"messages":[{"id":"wamid.DEMO1","from":"5586999990001","type":"text","text":{"body":"Olá webhook"}}]}}]}]}'

SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$META_APP_SECRET" -hex | awk '{print $NF}')

curl -sS -i -X POST http://localhost:8080/webhooks/messages \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=$SIG" \
  -d "$BODY"
```

**fish**

```fish
for line in (cat .env | string match -r -v '^\s*(#|$)')
    set -l parts (string split -m 1 = -- $line)
    set -gx $parts[1] (string trim --chars='"' -- $parts[2])
end

set BODY '{"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"phone-demo-scalar-001"},"contacts":[{"wa_id":"5586999990001","profile":{"name":"Cliente Webhook"}}],"messages":[{"id":"wamid.DEMO1","from":"5586999990001","type":"text","text":{"body":"Olá webhook"}}]}}]}]}'

set SIG (printf '%s' $BODY | openssl dgst -sha256 -hmac $META_APP_SECRET -hex | awk '{print $NF}')

curl -sS -i -X POST http://localhost:8080/webhooks/messages \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=$SIG" \
  -d "$BODY"
```

**Esperado:** **200**

No Scalar, com token admin:

- `GET /contatos` — contato do `wa_id` presente
- `GET /conversas` / `GET /mensagens?conversaId=...` — mensagem de entrada

Reenvio idêntico → **200** sem duplicar a mensagem.  
Assinatura inválida (`X-Hub-Signature-256: sha256=00`) → **401**.

---

## Resumo de resultados esperados

| Situação | Resultado |
|---|---|
| Fluxo auth + CRUD operacional | 2xx conforme o passo |
| Mensagem em conversa encerrada | **409** |
| Atendente em `POST /usuarios` | **403** ProblemDetail |
| Platform Admin em `/contatos` | **403** ProblemDetail |
| `sort` inválido | **400** Ordenação inválida |
| `POST /mensagens/entrada` | **201** ENTRADA |
| Webhook assinado | **200**; reenvio idempotente |
| Webhook sem assinatura válida | **401** |
| GET verificação com token correto | **200** + challenge |
