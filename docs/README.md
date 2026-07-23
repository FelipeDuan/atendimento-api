# Documentação — Atendimento API

Documentação técnica versionada junto ao código. Objetivo: permitir que um
leitor entenda o desenho da aplicação e consiga reproduzir a verificação
completa (build + Scalar + webhook), com o mesmo rigor usado no
desenvolvimento.

O contrato HTTP interativo continua no Scalar
(`http://localhost:8080/scalar`, profile `dev`). O
[README](../README.md) na raiz do módulo é o ponto de entrada operacional;
este diretório aprofunda arquitetura, segurança, fluxos e o roteiro de
teste.

## Como esta pasta está organizada

```text
docs/
├── README.md                 ← este índice
├── architecture/             ← o que o sistema é e como está particionado
│   ├── overview.md           ← contexto, containers, stack
│   ├── modules.md            ← módulos e fronteiras
│   ├── domain.md             ← entidades e invariantes
│   └── data-model.md         ← tabelas, RLS, constraints
├── security/                 ← tenancy, autenticação e autorização
│   ├── tenancy.md
│   ├── authentication.md
│   └── authorization.md
├── flows/                    ← sequências ponta a ponta (Mermaid)
│   ├── README.md
│   ├── bootstrap-empresa.md
│   ├── login.md
│   ├── webhook-inbound.md
│   ├── mensagem-entrada-autenticada.md
│   └── conversa-ciclo-vida.md
├── api/                      ← catálogo HTTP e convenções
│   └── endpoints.md
├── decisions/                ← síntese das decisões de desenho
│   └── README.md
└── guides/                   ← operação e verificação
    ├── getting-started.md
    ├── verification-scalar.md
    └── requirements-mapping.md
```

| Pasta | Público-alvo | Conteúdo |
|---|---|---|
| `architecture/` | Leitores do código | Forma do monólito, domínio e dados |
| `security/` | Quem audita isolamento | JWT, RLS, papéis |
| `flows/` | Quem implementa ou revisa fluxos | Diagramas de sequência e estados |
| `api/` | Consumidores HTTP | Endpoints e códigos de erro |
| `decisions/` | Revisores de desenho | Porquês estáveis do código atual |
| `guides/` | Quem clona e valida | Boot, roteiro Scalar, mapeamento de requisitos |

Ordem de leitura sugerida (entender → operar → validar):

1. [guides/getting-started.md](guides/getting-started.md) — subir o ambiente
2. [architecture/overview.md](architecture/overview.md) — visão do sistema
3. [security/tenancy.md](security/tenancy.md) — por que o isolamento importa
4. [guides/requirements-mapping.md](guides/requirements-mapping.md) — o que o
   contrato exige vs o que o código faz
5. [flows/webhook-inbound.md](flows/webhook-inbound.md) — ingestão pública
6. [guides/verification-scalar.md](guides/verification-scalar.md) — exercitar
   tudo no Scalar (e o webhook no terminal)

Quem for avaliar a entrega pode ir direto ao item 6 após o `make boot`, e
usar os demais documentos para entender o “porquê” de cada comportamento.

## Convenções nesta documentação

- Comandos de terminal são apresentados em **bash** e **fish** quando a
  sintaxe difere (carregamento de `.env`, por exemplo). Comandos idênticos
  nas duas shells aparecem uma vez, com indicação explícita.
- Diagramas usam Mermaid e refletem o código atual (não funcionalidades
  planejadas e ainda não implementadas).
- O Scalar lista operações pelo path; textos narrativos ficam aqui, não em
  `summary`/`description` dos controllers.

## Escopo documentado

Implementado e verificável:

- Multi-tenancy com PostgreSQL RLS (`ENABLE` + `FORCE`)
- Autenticação JWT (tenant e administrador da plataforma)
- Empresas, usuários/vínculos, contatos, conversas, mensagens
- `POST /webhooks/messages` (HMAC) e `POST /mensagens/entrada` (JWT)
- OpenAPI + Scalar; Testcontainers

Fora do escopo atual do código:

- Cliente Graph API (envio outbound)
- SSE
- Rate limiting (Bucket4j)
