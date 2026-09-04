# Architecture Haiku

> Escrito em 2026-08-29. Documento de contexto — a página de entrada da
> arquitetura, em uma folha. O detalhe mora nos [ADRs](adr/).

## O que é

Um SaaS de agendamento online para barbearias, salões e profissionais que
atendem por horário. Cada estabelecimento tem sua conta, seus dados e um link
público (`/b/{slug}`) que compartilha com seus clientes, para que agendem
sozinhos sem depender de WhatsApp, caderno ou memória.

## Para quem

O estabelecimento que **hoje não usa nada**. O concorrente real não é o Trinks
nem o Booksy — é o caderno e a conversa de WhatsApp. Isso define tudo: a
promessa é simplicidade, não quantidade de recursos.

## Restrições

| Restrição | Consequência |
|---|---|
| Uma pessoa desenvolvendo | Nada que exija time para manter |
| Uma VPS, custo próximo de zero | Sem Kubernetes, sem broker, sem serviço gerenciado |
| Mensalidade de R$ 50–70 | A infraestrutura por cliente precisa ser marginal |
| Validar com um piloto real antes de escalar | Escopo mínimo, evolução guiada por uso observado |
| Cliente final usa o navegador do celular | Mobile-first, rápido em 3G, sem app |

## Atributos de qualidade, em ordem

Quando dois conflitarem, o de cima ganha.

1. **Corretude da agenda.** Marcar dois clientes no mesmo horário destrói a
   confiança e manda o estabelecimento de volta para o caderno. É a única falha
   verdadeiramente fatal.
2. **Isolamento entre estabelecimentos.** Um tenant enxergar dado de outro é o
   fim do produto como negócio.
3. **Simplicidade de uso.** Se o barbeiro não entende a tela, o produto não
   existe. Nenhuma configuração que ele não saiba responder.
4. **Velocidade de evolução.** Uma pessoa precisa conseguir entregar uma
   funcionalidade por semana.
5. **Custo operacional.** Marginal por cliente.

Desempenho, alta disponibilidade e escala **não** estão nesta lista. Um
estabelecimento tem dezenas de agendamentos por dia, não milhares.

## Decisões que sustentam isso

| Decisão | Serve a |
|---|---|
| Monólito modular, contextos como pacotes, fronteira por ArchUnit ([0001](adr/0001-modular-monolith-com-contextos-como-pacotes.md)) | Velocidade, custo |
| Clean Architecture com rigor proporcional ao subdomínio ([0002](adr/0002-clean-architecture-com-rigor-proporcional.md)) | Velocidade, corretude no núcleo |
| Identidade dentro de Organization, sem IAM separado ([0003](adr/0003-identidade-dentro-de-organization.md)) | Simplicidade |
| Multi-tenancy por discriminador, tenant nunca vindo do cliente ([0004](adr/0004-multi-tenancy-por-discriminador.md)) | Isolamento |
| Exclusion constraint do Postgres contra overbooking ([0005](adr/0005-exclusion-constraint-contra-overbooking.md)) | Corretude da agenda |
| Grade fixa de 10 minutos, estratégia única ([0006](adr/0006-grade-fixa-como-unica-estrategia-de-slot.md)) | Simplicidade de uso |
| Thymeleaf server-side, sem API REST ([0007](adr/0007-thymeleaf-server-side-sem-api-rest.md)) | Velocidade, custo |
| Rota pública com prefixo `/b/` ([0008](adr/0008-rota-publica-com-prefixo.md)) | Corretude de roteamento |
| UUIDv7 gerado na aplicação ([0009](adr/0009-uuidv7-como-identificador.md)) | Corretude, independência da persistência |

## O que deliberadamente não fazemos

Microsserviços · Kubernetes · Kafka · React ou qualquer SPA · aplicativo
mobile · gateway de pagamento · planos Free/Pro/Premium · IA generativa ·
contexto de notificação · outbox.

Cada um tem um gatilho registrado no [backlog](../../sdd/backlog.md) que o
traria de volta. Nenhum entra por antecipação.

> **Nota (TODO-009, 2026-09-04)**: a exclusão de "gateway de pagamento" e
> "planos Free/Pro/Premium" continua valendo integralmente — nenhum dos
> dois foi construído. O que a TODO-009 introduziu foi só o **prazo de
> acesso** por estabelecimento (`BillingAccount.accessValidUntil`, ver
> glossário, "Contexto Billing"): trial de 30 dias e marcação manual de
> pagamento recebido por fora (Pix), sem nenhuma cobrança automática, sem
> preço diferenciado por plano. É a decisão nova que o glossário já
> prometia antes de liberar `Plano`/`Assinatura`/`Pagamento`.

**Preparado, mas desligado:** o endpoint de métricas do Prometheus é exposto
desde o início, mas o servidor Prometheus e o Grafana só sobem quando houver o
que observar — eles disputam memória com o Postgres na mesma VPS. O cache segue
a mesma lógica: a abstração do Spring desde já, com implementação em memória, e
Redis quando a medição pedir. Ver `sdd/PATTERNS.md`.

## O princípio que resolve empate

> A arquitetura mais simples capaz de suportar o domínio atual e evoluir sem
> criar dívida estrutural desnecessária.

Na dúvida entre duas opções, escolha a que é mais barata de desfazer.
