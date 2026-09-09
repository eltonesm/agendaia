# Implementation Summary — sistema-de-design-admin (TODO-110)

## Timeline

- Início: 2026-09-08
- Fim: 2026-09-09 (funcional → técnica → tasks → build → finish em dois
  dias corridos)

## Tasks

- Total: 21 (`stats.done: 21` em `tasks.json`)
- Estratégia de execução: Batched — duas frentes largamente independentes
  (status `COMPLETED`/KPIs do dashboard vs. sidebar/badge soft), depois
  convergindo nos 3 quality gates de Layer 3.
- 1 mudança de escopo dentro do plano: a interview funcional gerou US-4
  (marcar "Concluir" manualmente) a partir de uma pergunta aberta sobre
  como representar "atendidos hoje" — não estava nas 3 opções originais
  oferecidas, veio de pedido explícito do dono.
- 0 tasks removidas fora do plano aprovado; TASK-007 teve a descrição
  reescrita em andamento para refletir a correção de DD-6 (ver commit
  `9b0bcaa`).

## Commits (4, `main..feature/sistema-de-design-admin`)

```
9b0bcaa feat(scheduling,billing): status COMPLETED, acao de concluir e KPIs reais do painel (TODO-110)
ba17199 feat(organization,scheduling,platform): sidebar de navegacao e badges soft no admin e operador (TODO-110)
937faa2 test(scheduling,organization): cobertura para COMPLETED, KPIs do painel e regressoes da sidebar (TODO-110)
<próximo> docs(sdd): arquiva a TODO-110 e resolve o backlog (/sdd.finish)
```

> Diferente das features anteriores (TODO-108, TODO-008), esta não teve
> commit incremental por task durante o `/sdd.build` — o ciclo completo
> (spec → tarefas → 21 tasks implementadas → Layer 3) rodou numa única
> sessão longa, e os commits acima foram feitos agrupados por camada
> (backend, frontend, testes) no `/sdd.finish`, não por task individual.

## Quality

- Testes: 581 no projeto inteiro (43 novos ou editados nesta feature —
  ver detalhamento no README.md, seção Testes), 0 falhas, 0 erros
  (`./mvnw clean verify`).
- Cobertura de instrução: 91% no projeto inteiro (piso: 80%).
- Linter/type errors: 0.
- Layer 3 (code review, performance, security): todos `APPROVED`, zero
  achado crítico, major ou menor pendente ao final (ver `verdicts/`) — um
  achado crítico foi encontrado e corrigido durante o próprio code review
  (ver Gotcha real #3 abaixo).

## Gotcha real #1: ciclo de módulo no Spring Modulith (DD-6, duas tentativas)

A primeira tentativa de expor os KPIs do dashboard fazia `organization`
depender diretamente do novo `scheduling.api`. `ModuleStructureTest`
rejeitou: `scheduling` já depende de `organization.api` (para resolver o
profissional/estabelecimento do agendamento), então a dependência inversa
fechava um ciclo `organization ⇄ scheduling`, e transitivamente envolvia
`catalog`. Corrigido revertendo `DashboardView`/`ViewDashboardHandler` ao
estado original e introduzindo `DashboardKpiAdvice`
(`@ControllerAdvice`) dentro de `scheduling` — o mesmo padrão já usado por
`BillingBannerAdvice` para compor dado de um contexto na view de outro
sem import direto.

## Gotcha real #2: `@ControllerAdvice` global quebrou 94 classes de `@WebMvcTest`

Depois da correção acima, 94 classes de `@WebMvcTest` em todo o projeto —
sem nenhuma relação com dashboard ou KPI — passaram a falhar com
`UnsatisfiedDependencyException`. Causa: `@WebMvcTest` escaneia **todos**
os beans `@ControllerAdvice` do contexto, não só os relacionados ao
controller sob teste; o construtor de `DashboardKpiAdvice` exigia
`DailyScheduleDirectory`, que não existe nessas fatias de contexto.
Corrigido trocando a dependência direta por `ObjectProvider<DailyScheduleDirectory>`
e `.getIfAvailable()` — zero mudança nas 94 classes de teste.

## Gotcha real #3: `cancelByOwner()` não tratava `COMPLETED` como terminal

Encontrado durante o code review desta própria feature (não por teste
pré-existente): `cancelByOwner()` só absorvia (virava no-op) quando o
status já era `CANCELLED`, então um agendamento `COMPLETED` podia ser
reaberto direto para `CANCELLED` — contradizendo a regra de negócio de
que `COMPLETED` é terminal (BR-2). `confirm(Instant)`/`cancel(Instant)`
já eram seguros "de graça" pelo guard de horário (`agora.isAfter(startsAt)`,
e `COMPLETED` só existe quando o horário já passou), mas `cancelByOwner()`
deliberadamente não tem esse guard (regra da TODO-008, dono pode agir
sobre o passado). Corrigido adicionando `COMPLETED` ao guard de absorção;
verificado que `ManageAppointmentHandler` (lado do cliente) não tinha
brecha equivalente.

## Gotcha real #4: regressão silenciosa do banner de carência

Trocar `fragments/layout :: navbar` por `adminSidebar` nas 10 telas de
`/admin/**` removeu, sem querer, o `carenciaBanner`/`whatsappButton` que
vinham embutidos dentro do fragmento `navbar`. Só foi pego porque um
teste **pré-existente e não relacionado** (`OperadorBackofficeIT.e2e2CarenciaMostraAvisoSemBloquear`)
falhou ao rodar a suíte completa — nenhum teste novo desta feature
verificava esse banner. Corrigido extraindo um fragmento `adminBanners`
e adicionando-o às 10 telas. Reforça o motivo de sempre rodar a suíte
inteira, não só os testes da feature, após um refactor estrutural de
template.
