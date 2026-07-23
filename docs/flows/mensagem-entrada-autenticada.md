# Recepção autenticada

Endpoint: **`POST /mensagens/entrada`** (tag OpenAPI: Mensagens).

Produz o mesmo efeito de negócio do webhook (contato → conversa aberta →
mensagem de entrada → última interação), com autenticação JWT. O tenant vem
do token, não do corpo.

```mermaid
sequenceDiagram
  participant Op as Operador
  participant API as MensagemService
  participant Ct as ContatoService
  participant Cv as ConversaService
  participant DB as PostgreSQL

  Op->>API: POST /mensagens/entrada + JWT
  API->>Ct: localizarOuCriar numeroWhatsapp
  API->>Cv: garantirConversaAberta
  API->>DB: INSERT mensagem ENTRADA
  API-->>Op: 201 MensagemResponse
```

Uso típico: verificação no Scalar sem calcular assinatura HMAC. Não substitui
`POST /webhooks/messages` como contrato público de integração.
