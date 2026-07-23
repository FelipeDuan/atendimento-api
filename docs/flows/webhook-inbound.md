# Recepção via webhook

Endpoint exigido pelo contrato da API: **`POST /webhooks/messages`**.

Também existe `GET /webhooks/messages` para o desafio de verificação
(`hub.mode`, `hub.verify_token`, `hub.challenge`).

## Sequência

```mermaid
sequenceDiagram
  participant Origem as Origem externa
  participant WH as WebhookController
  participant Sig as AssinaturaWebhook
  participant Svc as WebhookService
  participant Emp as EmpresaService
  participant Ct as ContatoService
  participant Msg as MensagemService
  participant DB as PostgreSQL

  Origem->>WH: POST body bruto + X-Hub-Signature-256
  WH->>Sig: validar HMAC-SHA256
  alt assinatura inválida
    Sig-->>Origem: 401
  else ok
    WH->>Svc: processar corpo
    Svc->>Emp: resolver empresa por phone_number_id
    alt desconhecido
      Svc-->>WH: ignora (200)
    else conhecido
      Svc->>Svc: TenantContext.withTenantId
      Svc->>Ct: localizarOuCriar contato
      Svc->>Msg: registrarRecebida
      Msg->>DB: garante conversa aberta + INSERT mensagem
      WH-->>Origem: 200
    end
  end
```

## Comportamento

| Aspecto | Comportamento |
|---|---|
| Autenticação | HMAC do corpo bruto; comparação em tempo constante |
| Tenant | Resolvido por `phone_number_id` da empresa |
| Contato | Localiza ou cria; reativa se inativo |
| Conversa | Garante aberta; se a última está encerrada, cria nova com referência |
| Idempotência | Mesmo `whatsapp_message_id` não duplica mensagem |
| Formato do body | Envelope compatível com WhatsApp Cloud API |

Este é o caminho público de ingestão. Para exercício autenticado sem HMAC,
ver [mensagem-entrada-autenticada.md](mensagem-entrada-autenticada.md).
