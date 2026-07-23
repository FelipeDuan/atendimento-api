# Modelo de dados

Migrations Flyway em `src/main/resources/db/migration/`.

## Tabelas principais

```mermaid
erDiagram
  EMPRESA ||--o{ USUARIO_EMPRESA : possui
  USUARIO ||--o{ USUARIO_EMPRESA : vincula
  EMPRESA ||--o{ CONTATO : possui
  EMPRESA ||--o{ CONVERSA : possui
  CONTATO ||--o{ CONVERSA : participa
  CONVERSA ||--o{ MENSAGEM : contem
  CONVERSA ||--o| CONVERSA : previous_conversation_id
  ADMINISTRADOR_PLATAFORMA ||--|| ADMINISTRADOR_PLATAFORMA : singleton_seed

  EMPRESA {
    uuid id PK
    string cnpj UK
    string phone_number_id UK
    string status
  }
  USUARIO {
    uuid id PK
    string email UK
    boolean deve_trocar_senha
  }
  USUARIO_EMPRESA {
    uuid usuario_id FK
    uuid empresa_id FK
    string perfil
    string status
  }
  CONTATO {
    uuid id PK
    uuid empresa_id
    string numero_whatsapp
    string status
  }
  CONVERSA {
    uuid id PK
    uuid empresa_id
    uuid contato_id
    string status
    uuid previous_conversation_id
  }
  MENSAGEM {
    uuid id PK
    uuid empresa_id
    uuid conversa_id
    string sentido
    string whatsapp_message_id
  }
```

## Row Level Security

Tabelas de dado de tenant possuem `empresa_id`, com RLS **ENABLE** e
**FORCE**. A policy utiliza `current_setting('app.tenant_id', true)`.

A aplicação define a variável por transação via `set_config(..., true)`
(equivalente a `SET LOCAL`), no aspecto de tenancy, **dentro** da transação
aberta. Índices compostos priorizam `empresa_id`.

Tabelas sem RLS de tenant: `administrador_plataforma` (escopo global).

## Constraints relevantes

| Constraint | Motivo |
|---|---|
| `UNIQUE(empresa_id, numero_whatsapp)` em contato | Número único por tenant |
| Unique parcial em `whatsapp_message_id` | Idempotência de reentrega |
| `UNIQUE(email)` em usuario | Conta global |
| `phone_number_id` único em empresa | Resolução do webhook |

## Papel de aplicação no banco

A aplicação conecta com usuário de runtime distinto do dono Flyway, alinhado
às policies com `FORCE ROW LEVEL SECURITY`.
