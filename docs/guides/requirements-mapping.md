# Mapeamento requisito → implementação

Referência ao enunciado da API de atendimento multi-tenant e ao que o
código expõe hoje.

## Webhook

| Requisito | Implementação |
|---|---|
| Endpoint `POST /webhooks/messages` | `WebhookController` |
| Identificar o tenant | `phone_number_id` → empresa → `TenantContext` |
| Localizar/criar contato | `ContatoService.localizarOuCriar` |
| Localizar/criar conversa aberta | `ConversaService.garantirConversaAberta` |
| Registrar mensagem | `MensagemService.registrarRecebida` |
| Atualizar última interação | `prepararRegistroDeMensagem` / `registrarInteracao` |

Autenticação do endpoint público por HMAC e formato de payload compatível
com Cloud API são decisões de segurança e interoperabilidade; não alteram
o fluxo de negócio acima.

## Complemento

`POST /mensagens/entrada` executa o mesmo fluxo de negócio com JWT, para
verificação sem assinatura. Não substitui `POST /webhooks/messages`.

## Demais recursos

Empresas, usuários, contatos, conversas, mensagens (CRUD/ciclo de vida) e
auth estão descritos em [api/endpoints.md](../api/endpoints.md) e nos
fluxos em [flows/](../flows/).
