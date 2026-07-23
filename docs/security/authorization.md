# Autorização

## Modelo

1. **Regras estáticas** na `SecurityFilterChain` com `hasAuthority` /
   `hasAnyAuthority` (constantes sem prefixo `ROLE_`).
2. **Regras dependentes de dado** via `PermissionEvaluator` (ex.: acesso a
   empresa por id).

Não há anotações de autorização customizadas além do mecanismo Spring
Security padrão (`@PreAuthorize` onde aplicável).

## Matriz resumida

| Recurso | PLATFORM_ADMIN | ADMINISTRADOR | ATENDENTE |
|---|---|---|---|
| `POST/DELETE /empresas` | sim | não | não |
| `GET /empresas` (lista) | sim | não | não |
| `GET/PUT /empresas/{id}` | sim* | sim* | não |
| `POST/PUT /usuarios` | não | sim | não |
| `GET /usuarios` | não | sim | sim |
| Contatos / conversas / mensagens | não | sim | sim |
| Webhook | público + HMAC | — | — |

\* Mediante `PermissionEvaluator` / vínculo adequado.

## Erros

- **401**: não autenticado; corpo vazio, `WWW-Authenticate` (RFC 6750).
- **403**: autenticado sem permissão; Problem Detail (RFC 9457),
  `title` tipicamente `Acesso negado`.
