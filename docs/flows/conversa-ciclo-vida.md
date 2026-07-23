# Ciclo de vida da conversa

```mermaid
stateDiagram-v2
  [*] --> ABERTA: POST /conversas ou entrada
  ABERTA --> ENCERRADA: PATCH acao ENCERRAR
  ENCERRADA --> ABERTA: PATCH acao REABRIR
  ENCERRADA --> ABERTA_NOVA: webhook ou POST /mensagens/entrada
  ABERTA_NOVA --> ENCERRADA: PATCH ENCERRAR
```

| Evento | Resultado |
|---|---|
| `POST /mensagens` com conversa aberta | 201, sentido SAIDA, `envioPendente: true` |
| `POST /mensagens` com conversa encerrada | **409** |
| Entrada com última conversa encerrada | Nova conversa (`previous_conversation_id`) |
| `PATCH` ENCERRAR duas vezes | **409** estado inválido |

Mensagens são listadas com `GET /mensagens?conversaId=...`, paginadas.
