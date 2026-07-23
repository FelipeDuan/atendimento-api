# Catálogo de endpoints

O Scalar (`/scalar`) gera o contrato a partir dos controllers (caminhos,
schemas, parâmetros). Descrições narrativas ficam na documentação em
`docs/`, não em `summary`/`description` das operações OpenAPI.

Convenções:

- Listagens: `PageResponse` com `page`, `size`, `sort`
- Soft delete: GET de inativo → **404**
- Erros de negócio / **403**: RFC 9457
- **401**: sem corpo, header `WWW-Authenticate`

## Auth

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `POST` | `/auth/plataforma/login` | público | Login da plataforma |
| `POST` | `/auth/login` | público | Login do tenant |
| `POST` | `/auth/trocar-senha` | `TROCAR_SENHA` | Senha definitiva |
| `POST` | `/auth/switch-tenant` | autenticado | Troca de empresa |

## Empresas

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `POST` | `/empresas` | `PLATFORM_ADMIN` | Cria tenant + admin inicial |
| `GET` | `/empresas` | `PLATFORM_ADMIN` | Lista ativas |
| `GET` | `/empresas/inativas` | `PLATFORM_ADMIN` | Lista inativas |
| `GET` | `/empresas/{id}` | admin / plataforma | Detalhe |
| `PUT` | `/empresas/{id}` | admin / plataforma | Atualiza (incl. `phoneNumberId`) |
| `DELETE` | `/empresas/{id}` | `PLATFORM_ADMIN` | Inativa |

## Usuários

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `POST` | `/usuarios` | `ADMINISTRADOR` | Cria vínculo / conta |
| `GET` | `/usuarios` | admin ou atendente | Lista |
| `GET` | `/usuarios/{id}` | admin ou atendente | Detalhe ativo |
| `PUT` | `/usuarios/{id}` | `ADMINISTRADOR` | Atualiza / reativa |

## Contatos

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `POST` | `/contatos` | admin ou atendente | Cria / reativa |
| `GET` | `/contatos` | admin ou atendente | Lista ativos |
| `GET` | `/contatos/{id}` | admin ou atendente | Detalhe |
| `PUT` | `/contatos/{id}` | admin ou atendente | Atualiza |
| `DELETE` | `/contatos/{id}` | admin ou atendente | Inativa |

## Conversas

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `POST` | `/conversas` | admin ou atendente | Abre |
| `GET` | `/conversas` | admin ou atendente | Lista |
| `GET` | `/conversas/{id}` | admin ou atendente | Detalhe |
| `PATCH` | `/conversas/{id}` | admin ou atendente | `ENCERRAR` \| `REABRIR` |

## Mensagens

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `GET` | `/mensagens` | admin ou atendente | Lista por `conversaId` |
| `GET` | `/mensagens/{id}` | admin ou atendente | Detalhe |
| `POST` | `/mensagens` | admin ou atendente | Saída (`envioPendente`) |
| `POST` | `/mensagens/entrada` | admin ou atendente | Entrada (JWT) |

No Scalar, `POST /mensagens/entrada` aparece sob **Mensagens**, não sob
Webhook.

## Webhook

| Método | Caminho | Autorização | Função |
|---|---|---|---|
| `GET` | `/webhooks/messages` | público | Verificação `hub.*` |
| `POST` | `/webhooks/messages` | HMAC | Ingestão pública |

Contrato público de recepção inbound. Detalhes em
[flows/webhook-inbound.md](../flows/webhook-inbound.md).
