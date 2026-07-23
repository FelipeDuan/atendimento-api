# Decisões de desenho

Síntese das escolhas que condicionam o código atual. Não substitui históricos
externos de ADR; descreve o estado implementado neste repositório.

## Isolamento multi-tenant

PostgreSQL RLS com `ENABLE` e `FORCE`, variável `app.tenant_id` por
transação (`set_config` LOCAL). Claim `tenant_id` exclusivo no JWT.

## Conta global e vínculo

E-mail único em `usuario`. Perfil e status por empresa em `usuario_empresa`.
Troca de contexto via reemissão de token.

## Administrador da plataforma

Entidade distinta de `Usuario`. Token sem tenant. Escopo restrito a empresas.

## Webhook público com HMAC

`POST /webhooks/messages` é público. A assinatura `X-Hub-Signature-256`
impede escrita anônima com `phone_number_id` conhecido. Payload no formato
Cloud API para compatibilidade com integração real.

## Entrada autenticada complementar

`POST /mensagens/entrada` replica a regra de negócio do webhook com JWT,
para verificação operacional sem HMAC. Não remove o contrato do webhook.

## Conversa encerrada

Mensagem em conversa encerrada é rejeitada. Ingestão automática abre nova
conversa com referência à anterior; reabertura explícita só por `PATCH`.

## Persistência antes do envio outbound

Mensagens de saída são gravadas com `envioPendente: true`. Cliente Graph API
não está implementado; o registro não depende do envio externo.

## Monólito modular

Package-by-feature, sem camadas hexagonais obrigatórias. Fronteira entre
módulos pelo Service público.

## Demonstração sem frontend

Scalar + cliente HTTP. Documentação narrativa em `docs/`.

## Infraestrutura Redis

O Compose e os testes sobem Redis, e a dependência Spring Data Redis está
no classpath. A aplicação **não** utiliza cache nem repositórios Redis no
domínio atual; o isolamento e a persistência relevantes estão no
PostgreSQL com RLS.
