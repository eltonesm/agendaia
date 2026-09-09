# sistema-de-design-admin - Technical Spec

**Feature**: sistema-de-design-admin
**Backlog**: TODO-110
**Status**: approved
**Data**: 2026-09-08
**Aprovado por**: Elton Marques em 2026-09-09T00:52:44Z

---

## Executive Summary

Duas frentes independentes, na mesma feature porque nasceram da mesma
conversa de design:

1. **Retrofit visual**: sidebar substituindo a navbar nas 8 telas de
   `/admin/**`, e badge de status migrado do padrão sólido para o soft já
   documentado em `sdd/PATTERNS.md`. Puro HTML/CSS/fragmento Thymeleaf —
   nenhuma linha de Java nova.
2. **Novo status `COMPLETED`**: fecha o ciclo de vida do agendamento que
   faltava (ninguém registrava "aconteceu de verdade"), com uma ação nova
   na agenda do dono e uma métrica nova. Toca `scheduling.domain`
   (core domain, regime completo — ADR 0002) e cria o **primeiro** pacote
   `scheduling.api`, para o painel (`organization`) poder mostrar os KPIs
   do dia sem `organization` importar `scheduling.application` nem
   `scheduling.domain` diretamente (ADR 0010).

Nenhuma migration de schema: `appointment.status` já é `VARCHAR(20)`
(V8), e `COMPLETED` cabe.

---

## Architecture Overview

```
Frente 1 — sidebar/badge (sem camada nova, só template/fragmento):

  fragments/layout.html
    ├── temaToggle, publicHeader     (já existem)
    ├── carenciaBanner (NOVO, extraído de navbar)
    ├── whatsappButton (NOVO, extraído de navbar)
    ├── navbar (inalterado — ainda usado por error/404, error/500)
    └── adminSidebar(activeItem) (NOVO)
            │
            ├── admin/dashboard.html    ┐
            ├── admin/agenda.html       │  cada um: <div class="app-shell">
            ├── admin/agenda-novo.html  │    <aside th:replace="adminSidebar">
            ├── admin/agenda-reagendar  │    <main>...</main>
            ├── admin/bloqueios.html    │  </div>
            ├── admin/horario-funcionamento.html
            ├── admin/jornadas.html
            ├── admin/ofertas.html
            ├── admin/profissionais.html
            └── admin/servicos.html


Frente 2 — status COMPLETED (cross-context novo):

  admin/agenda.html
    └─ POST /admin/agenda/agendamentos/{id}/concluir
         └─ AgendaController.concluir()
              └─ CompleteAppointmentUseCase.complete(id)   [novo port]
                   └─ ProfessionalAgendaHandler.complete()  [novo método]
                        ├─ Appointment.complete(Instant)    [novo, domain]
                        ├─ AppointmentRepository.updateStatus(...)  [já existe]
                        └─ SchedulingMetrics.appointmentCompleted() [novo contador]

  admin/dashboard.html
    └─ GET /admin/dashboard
         └─ DashboardController (organization)
              └─ ViewDashboardHandler.current()
                   └─ scheduling.api.DailyScheduleDirectory.summaryFor(hoje)  [NOVO — 1a api de scheduling]
                        └─ DailyScheduleSummaryHandler (scheduling.application)
                             └─ AppointmentRepository.findByTenantIdAndDate(...) [novo método]
```

---

## Design Decisions

### DD-1: Sidebar como fragmento compartilhado, com CSS genérica extraída do que já existe em `operador/painel.html`

**Selected**: `fragments/layout.html` ganha um novo `th:fragment="adminSidebar(activeItem)"` — um `<aside>` autocontido com os 8 itens de menu, item ativo destacado por comparação de string (`th:classappend="${activeItem == 'agenda'} ? 'active'"`). As classes CSS (`.app-sidebar`, `.app-main`, `.app-nav .nav-link.active`, `.co-avatar`) sobem do `<style>` hoje só em `operador/painel.html` para o `<style>` compartilhado de `fragments/layout.html`, com nomes genéricos (sem o prefixo `operador-`); `operador/painel.html` é retrofitado para usar as mesmas classes, removendo sua cópia local.

Cada uma das 8 telas de `/admin/**` ganha o mesmo esqueleto de 3 linhas de boilerplate (abrir `<div class="app-shell d-flex">`, incluir `adminSidebar`, abrir `<main class="app-main">`) — mecânico, não é duplicação de conteúdo, mesmo espírito de `<body>`/`scripts`/`</body>` já repetidos hoje em toda tela.

`carenciaAte` (banner de carência) e o botão flutuante de WhatsApp, hoje dentro do `th:block fragment="navbar"`, são extraídos para dois fragmentos próprios (`carenciaBanner`, `whatsappButton`) — `navbar` passa a incluí-los por `th:replace`, sem mudar nada visualmente para quem ainda usa `navbar` (error/404, error/500), e o novo shell dos 8 admin os inclui do mesmo jeito.

**Options Considered**:
- Sidebar duplicada inteira em cada uma das 8 telas — descartado: exatamente a duplicação que `PATTERNS.md` já veta ("fragmento nasce na segunda repetição", aqui já é a nona).
- Adotar `thymeleaf-layout-dialect` para decoração de página (`layout:fragment` com conteúdo injetado) — descartado: dependência nova só para isto, contra ADR 0012 ("sem build", minimalismo) e sem ganho real sobre o padrão de fragmentos-como-irmãos já usado em todo o projeto.

**Trade-offs Accepted**: cada template ainda carrega 3 linhas de esqueleto próprias (abrir/fechar `div`/`main`) — não é zero duplicação, é duplicação estrutural aceitável, do mesmo tipo que `<body>` já é.

**Rationale**: seis fragmentos pequenos e independentes (`temaToggle`, `publicHeader`, `carenciaBanner`, `whatsappButton`, `navbar`, `adminSidebar`) compõem por `th:replace`, sem exigir nenhuma feature do Thymeleaf além da já usada em todo o projeto.

### DD-2: Badge soft é troca de classe, não CSS novo

**Selected**: `operador/painel.html` (billing) e `admin/agenda.html`
(status do agendamento) trocam `badge text-bg-{cor}` por
`badge rounded-pill bg-{cor}-subtle text-{cor}-emphasis` — as variáveis já
existem em `fragments/layout.html` desde a sessão anterior a esta feature
(`sdd/PATTERNS.md`, "Sistema de design é token do Bootstrap"). `admin/agenda.html`
ganha o 5º `<span>` para `COMPLETED` (verde, mesma cor de sucesso do billing "Pago").

**Options Considered**: nenhuma — a decisão de paleta e nome de classe já
foi tomada e documentada antes desta feature; aqui só falta aplicar.

**Trade-offs Accepted**: nenhum.

**Rationale**: consistência visual com o que já está documentado.

### DD-3: `AppointmentStatus.COMPLETED` e `Appointment.complete(Instant)` — mesma forma imutável de `confirm`/`cancel`

**Selected**: `Appointment` é imutável (`status` é `final`); toda transição
devolve uma instância nova ou `this` (absorção sem transição). `complete`
segue exatamente essa forma:

```java
public Appointment complete(Instant agora) {
    if (status == AppointmentStatus.COMPLETED
            || status == AppointmentStatus.CANCELLED
            || status == AppointmentStatus.NO_SHOW
            || agora.isBefore(startsAt)) {
        return this;
    }
    return new Appointment(
            id, tenantId, professionalId, serviceOfferingId, customerId,
            AppointmentStatus.COMPLETED, startsAt, endsAt, serviceName, durationMinutes, price);
}
```

Quem chama compara `antes.status() != depois.status()` para decidir se
grava (mesmo padrão de `ManageAppointmentHandler.gravarSeMudou` e do
`cancel()` de `ProfessionalAgendaHandler`) — BR-1/BR-2/BR-3 da spec
funcional (só de `SCHEDULED`/`CONFIRMED`, terminal, nunca antes de
`startsAt`) saem de graça da mesma condição.

**Options Considered**:
- Método mutável (`void complete()`, altera `status` em campo não-final)
  — descartado: quebraria a imutabilidade do agregado inteiro, decisão já
  tomada (PATTERNS.md, "Sem setter, em lugar nenhum") para todo o resto da
  classe.
- Exceção de domínio em vez de absorção silenciosa quando a transição não
  é permitida — descartado: `confirm`/`cancel` já absorvem em vez de
  lançar, e o controller nunca oferece o botão fora da janela permitida
  (a UI já impede; o domínio é a segunda linha de defesa, não a primeira
  a falar com o usuário).

**Trade-offs Accepted**: chamar `complete()` numa instância que não pode
transicionar não avisa a chamada — só quem compara `antes`/`depois`
percebe. Aceitável porque é o mesmo contrato de `confirm`/`cancel`, já em
produção.

**Rationale**: zero desvio do padrão existente — o revisor que já conhece
`confirm`/`cancel` lê `complete` sem surpresa.

### DD-4: Correção do guard de `canCancel`/`canReschedule` em `ProfessionalAgendaHandler` — achado ao adicionar `COMPLETED`

Hoje, `paraAgendaEntry` calcula `naoCancelado = status != CANCELLED` e usa
essa variável para `canCancel` **e** `canReschedule`. Isso já é
tecnicamente impreciso para `NO_SHOW` (nenhuma tela ainda produz esse
status, então nunca foi exercitado), e ficaria **errado de verdade** para
`COMPLETED`: um agendamento concluído mostraria botão de cancelar e de
reagendar.

**Selected**: variável renomeada para `estaAberto` (`status != CANCELLED
&& status != NO_SHOW && status != COMPLETED` — dado que só existem 5
valores, equivale a `status == SCHEDULED || status == CONFIRMED`),
usada por `canCancel`, `canReschedule` **e** o novo `canComplete`:

```java
var estaAberto = status != CANCELLED && status != NO_SHOW && status != COMPLETED;
var aindaNoFuturo = agora.isBefore(startsAt);
var canConfirm = estaAberto && aindaNoFuturo && status == SCHEDULED;
var canCancel = estaAberto;
var canReschedule = estaAberto;
var canComplete = estaAberto && !aindaNoFuturo;   // AC-1/AC-4 da spec funcional
```

**Options Considered**: deixar `naoCancelado` como está e só somar
`&& status != COMPLETED` no `canComplete` novo, sem tocar `canCancel`/
`canReschedule` — descartado: deixaria o bug latente (cancelar um
`COMPLETED`) sem corrigir, só evitando piorar.

**Trade-offs Accepted**: nenhum — é estritamente uma correção.

**Rationale**: `PATTERNS.md`, "Todo portão precisa ser visto falhando
pelo menos uma vez" — o teste novo de `canCancel == false` para
`COMPLETED` só existe porque este DD foi escrito percebendo o problema
antes do código, não depois do bug relatado.

> **Achado no code review (TASK-019)**: o mesmo raciocínio tinha um buraco
> um passo adiante. `Appointment.cancelByOwner()` (TODO-008) só absorve se
> já `CANCELLED` — nunca ganhou a exclusão de `COMPLETED`. Diferente de
> `confirm(Instant)`/`cancel(Instant)` (que ficam protegidos de graça pela
> checagem de horário, já que um `COMPLETED` sempre tem `startsAt` no
> passado), `cancelByOwner()` **deliberadamente não tem** restrição de
> horário (BR-2 de TODO-008) — nada o impedia de reabrir um `COMPLETED`
> direto para `CANCELLED`, contradizendo a própria BR-2 desta feature
> ("terminal, sem transição de volta"). Corrigido somando `COMPLETED` ao
> guard de idempotência de `cancelByOwner()`, com teste novo em
> `AppointmentTest` e `ProfessionalAgendaHandlerTest`.

### DD-5: `COMPLETED` fica fora da cláusula `WHERE` da exclusion constraint (mesmo grupo de `CANCELLED`/`NO_SHOW`)

A `appointment_no_overlap` (ADR 0005, V8) protege só `status IN
('SCHEDULED', 'CONFIRMED')`. `COMPLETED` **não** entra nessa lista — nenhuma
migration necessária, a constraint já existe e já ignora qualquer status
fora dela.

**Options Considered**:
- Incluir `COMPLETED` na lista protegida — descartado: exigiria uma
  migration (`DROP CONSTRAINT` + `ADD CONSTRAINT` com a nova lista), só
  para proteger um horário que, por definição (BR-3: só completa depois de
  `startsAt`), já está no passado ou em andamento no momento em que vira
  `COMPLETED`. O risco teórico (alguém marcar `COMPLETED` um pouco antes
  de `endsAt` e um novo agendamento colidir com a sobra do intervalo) já
  existe hoje contra `CANCELLED` também, e nunca foi tratado — não é
  regressão introduzida por esta feature.

**Trade-offs Accepted**: a janela residual entre "marcado como concluído"
e o `endsAt` original não é protegida pela exclusion constraint contra um
novo agendamento sobreposto — mesma exposição que `CANCELLED` já tem.

**Rationale**: consistência com o par `CANCELLED`/`NO_SHOW` já existente,
sem migration nova.

### DD-6: Primeiro `scheduling.api` — `DailyScheduleDirectory` — consumido por um `@ControllerAdvice` dentro de `scheduling`, não por `organization`

`scheduling` hoje só **consome** `organization.api`/`catalog.api`/
`customer.api` — nunca expôs nada para fora (ADR 0010). O painel do dono
(`admin/dashboard.html`) precisa dos 4 KPIs, todos calculados a partir de
`Appointment` — dado que só `scheduling` possui.

**Tentativa 1 (descartada em build, `ModuleStructureTest` pegou um ciclo)**:
`organization.application.ViewDashboardHandler` ganhando
`DailyScheduleDirectory` como dependência direta, e `DashboardView` ganhando
os 4 campos. Compilava, mas o Spring Modulith recusou:
`scheduling` já depende de `organization.api` (`ProfessionalDirectory`,
`AvailabilityDirectory`) — `organization` depender de volta de
`scheduling.api` fecha `organization ⇄ scheduling` (e por tabela,
`catalog ⇄ organization ⇄ scheduling ⇄ catalog`, já que `catalog` também
depende de `organization.api`). `ModuleStructureTest` falhou com
"Cycle detected" listando as duas direções.

**Selected**: `scheduling.api` continua existindo exatamente como
desenhado (`DailyScheduleDirectory`, `DailyScheduleSummary`,
`NextClientRef`, `@NamedInterface("api")`), implementado por
`scheduling.application.DailyScheduleSummaryHandler`. O que muda é **quem
consome**: em vez de `organization` importar `scheduling.api`, um
`@ControllerAdvice` novo — `scheduling.adapter.in.web.DashboardKpiAdvice`
— soma os 4 atributos ao model de **qualquer** controller, exatamente como
`billing.adapter.in.web.BillingBannerAdvice` já faz para `carenciaAte`/
`operadorWhatsapp` (TODO-009). `organization.adapter.in.web.
DashboardController` continua sem saber que `scheduling` existe — o
Spring resolve o `@ControllerAdvice` global em tempo de execução, não em
tempo de import.

```java
// scheduling.adapter.in.web
@ControllerAdvice
public class DashboardKpiAdvice {

    private final ObjectProvider<DailyScheduleDirectory> dailySchedule;
    // ...construtor...

    @ModelAttribute
    public void kpisDoPainel(HttpServletRequest request, Model model) {
        if (!"/admin/dashboard".equals(request.getRequestURI())) {
            return;   // sem isso, toda tela de /admin/** pagaria a consulta à toa
        }
        var directory = dailySchedule.getIfAvailable();
        if (directory == null) return;   // ver nota sobre @WebMvcTest abaixo
        var resumo = directory.summaryFor(LocalDate.now());
        model.addAttribute("agendamentosHoje", resumo.totalToday());
        model.addAttribute("atendidosHoje", resumo.completedToday());
        model.addAttribute("receitaEstimada", resumo.estimatedRevenue());
        model.addAttribute("proximoCliente", resumo.nextClient());
    }
}
```

> **Segundo achado, mesmo build**: `@WebMvcTest` carrega **todo**
> `@ControllerAdvice` do classpath — inclusive `DashboardKpiAdvice` —
> mesmo em fatias de teste de contextos que nunca sobem
> `scheduling.application` (`ServiceControllerTest`, `AgendaControllerTest`,
> etc.). Com `DailyScheduleDirectory` injetado direto no construtor, **94
> testes** em 11 classes `@WebMvcTest` de todo o projeto quebraram com
> `UnsatisfiedDependencyException` ao rodar o `verify` completo — nenhum
> deles tem nada a ver com o painel, e nenhum precisaria saber que este
> bean existe. Corrigido trocando a injeção direta por
> `ObjectProvider<DailyScheduleDirectory>` (`getIfAvailable()`, `null` em
> vez de exceção quando o bean não existe no contexto) — zero mudança nos
> 11 arquivos de teste alheios, e o caminho real da aplicação (bean sempre
> presente) não muda de comportamento.

`DashboardView`/`ViewDashboardHandler` voltam à forma original de antes
desta feature (3 campos: `businessName`, `slug`, `publicUrl`) —
`admin/dashboard.html` lê os 4 KPIs como atributos de model no nível
raiz (`${agendamentosHoje}` etc.), não mais `${painel.agendamentosHoje}`.

**Options Considered**:
- `organization` consultar `AppointmentRepository`/`Appointment` direto —
  proibido por construção: `application`/`domain` de outro contexto nunca
  são importáveis (ADR 0010, `ModuleStructureTest`).
- Calcular os 4 números dentro de `DashboardController` a partir de dados
  brutos vindos de `scheduling.api` — descartado: além de exigir a mesma
  dependência cíclica `organization -> scheduling.api`, moveria regra de
  negócio de `scheduling` (o que conta como receita, o que é "atendido")
  para dentro de um controller de `organization`.
- `organization -> scheduling.api` direto (tentativa 1) — descartado:
  ciclo de módulo, `ModuleStructureTest` vermelho.

**Trade-offs Accepted**: `scheduling.api` nasce com uma única operação,
específica do painel — aceitável (API grossa desde o primeiro contrato,
sem generalização especulativa antes de haver um segundo consumidor). O
`@ControllerAdvice` é global (roda em toda requisição), mas o corpo só
consulta o banco quando a URI bate — mesmo custo desprezível de
`BillingBannerAdvice` para as demais telas.

**Rationale**: é o mesmo mecanismo que o projeto já usa para exatamente
este problema (dado de um contexto aparecendo em view de outro sem
acoplamento de import) — `BillingBannerAdvice` já provou o padrão em
produção desde a TODO-009. Reaproveitar em vez de inventar um segundo
jeito de resolver o mesmo problema.

### DD-7: Nova consulta `findByTenantIdAndDate` — mesmo padrão de fuso de `findByTenantIdAndProfessionalIdAndDate`, sem filtro de profissional

**Selected**: `AppointmentRepository` ganha
`List<Appointment> findByTenantIdAndDate(TenantId tenantId, LocalDate date)`,
implementada com o **mesmo** `ZoneId.systemDefault()` (não
`Business.timezone`) que `findByTenantIdAndProfessionalIdAndDate` e
`findOccupiedRanges` já usam — reaproveita o cálculo de `dayStart`/`dayEnd`
já existente no adapter, só sem o filtro `professionalId` na query JPA
(`findByTenantAndDay` nova, ao lado de `findByProfessionalAndDay`).

`DailyScheduleSummaryHandler` chama essa consulta **uma vez** e computa os
4 números em memória (contagem, soma de `price`, seleção do próximo por
`startsAt` mínimo entre os ainda no futuro) — nunca 4 queries agregadas
separadas, e nunca em laço.

**Options Considered**:
- Usar `Business.timezone` para calcular "hoje" (o que a spec funcional
  presumia como *assumption*) — descartado ao encontrar o código real: o
  precedente já em produção (TODO-008) usa `ZoneId.systemDefault()`, e
  introduzir `Business.timezone` só para os KPIs criaria dois jeitos
  diferentes de responder "que dia é hoje" dentro do mesmo contexto. Fica
  registrado como possível `DEBT` separado (usar o fuso do estabelecimento
  em vez do fuso do servidor em toda consulta por data), não algo a
  resolver aqui.
- 4 consultas agregadas (`COUNT`, `SUM`, etc.) em vez de uma carga +
  cálculo em memória — descartado: volume diário de um piloto é pequeno
  (o mesmo raciocínio que já vale para a agenda por profissional, que
  carrega o dia inteiro), e uma consulta só é mais simples que manter
  4 queries JPQL sincronizadas com a mesma janela de tempo.

**Trade-offs Accepted**: "hoje" no painel é o fuso do servidor, não do
estabelecimento — mesma limitação que já existe (silenciosamente) desde a
TODO-008, agora só ficando visível também no painel.

**Rationale**: consistência com o código real já em produção pesa mais que
a suposição da spec funcional — corrigida aqui.

---

## Existing Data & Migrations

Nenhuma migration. `appointment.status VARCHAR(20)` já comporta
`'COMPLETED'` (8 caracteres). A exclusion constraint `appointment_no_overlap`
não muda (DD-5).

---

## Data Model

- `AppointmentStatus` (enum, `scheduling.domain`): `+ COMPLETED`.
- `Appointment` (`scheduling.domain`): `+ complete(Instant agora)`.
- `AgendaEntry` (`scheduling.application.port.in`): `+ boolean canComplete`.
- `AppointmentRepository` (`scheduling.application.port.out`):
  `+ List<Appointment> findByTenantIdAndDate(TenantId, LocalDate)`.
- `CompleteAppointmentUseCase` (novo, `scheduling.application.port.in`):
  `void complete(UUID appointmentId)`.
- `scheduling.api` (**novo pacote**): `DailyScheduleDirectory`,
  `DailyScheduleSummary`, `NextClientRef`.
- `DashboardKpiAdvice` (novo, `scheduling.adapter.in.web`): `@ControllerAdvice`
  que soma `agendamentosHoje`/`atendidosHoje`/`receitaEstimada`/
  `proximoCliente` ao model quando a URI é `/admin/dashboard` (DD-6,
  corrigido em build — ver DD-6 para a tentativa descartada).
- `DashboardView` (`organization.application.port.in`): **sem mudança** —
  continua com os 3 campos originais (`businessName`, `slug`, `publicUrl`).

---

## Cross-Context API Contracts

Sem REST (CLAUDE.md). O único contrato novo é in-process:

```java
package com.agendaia.scheduling.api;

public interface DailyScheduleDirectory {
    /** Tenant vem de TenantContext.require() — nunca argumento (ADR 0004). */
    DailyScheduleSummary summaryFor(LocalDate date);
}
```

Chamado uma vez por carregamento de `/admin/dashboard`, nunca em laço.

---

## Security

- Nenhuma rota nova exposta sem autenticação: `/admin/agenda/agendamentos/{id}/concluir`
  cai no mesmo `anyRequest().hasRole("OWNER")` de todas as rotas
  `/admin/**` (`SecurityConfig`, sem mudança).
- CSRF: o formulário de "Concluir" segue exatamente o mesmo padrão HTML
  (`method="post"`, token automático do Thymeleaf+Spring Security) dos
  formulários de confirmar/cancelar já existentes na mesma tela — nenhuma
  configuração nova.
- `tenantId` do agendamento a concluir é sempre resolvido via
  `AppointmentRepository.findByTenantIdAndId(TenantContext.require(), id)`
  — id de outro tenant devolve `AppointmentNotFoundException` → 404, nunca
  vaza existência nem permite alteração (mesma garantia de
  `confirmar`/`cancelar`).
- Nenhum segredo novo, nenhuma dependência nova, nenhuma superfície pública
  nova (o dashboard já era autenticado).

---

## Performance

- `findByTenantIdAndDate`: uma consulta indexada por `tenant_id` (a coluna
  já é a primeira do índice em todo o schema — PATTERNS.md), executada uma
  vez por carregamento do painel.
- Os 4 KPIs são computados em memória sobre o resultado dessa única
  consulta — nenhuma consulta adicional em laço.
- `CustomerDirectory`/`ProfessionalDirectory` **não** são chamados para os
  KPIs: "próximo cliente" usa o nome que já vem gravado no agregado? —
  não, `Appointment` não guarda nome do cliente, só `customerId`. Ver nota
  abaixo.

> **Nota de implementação**: `NextClientRef.customerName` precisa do nome
> do cliente, que não está em `Appointment`. `DailyScheduleSummaryHandler`
> resolve isso com **uma** chamada a `CustomerDirectory.find(customerId)`
> (só para o próximo cliente, nunca para os outros do dia) — mesma
> disciplina de "resolver em lote, nunca por item da lista" já usada em
> `ProfessionalAgendaHandler.handle` (que resolve todos os clientes do dia
> de uma vez porque precisa de todos; aqui só um é exibido, então uma
> chamada single-id basta).

---

## Testing Strategy

**Unit (Java puro, `scheduling.domain`)**:
- `AppointmentTest`: `complete()` a partir de `SCHEDULED`/`CONFIRMED` após
  `startsAt` transiciona; antes de `startsAt` absorve; a partir de
  `CANCELLED`/`NO_SHOW`/já `COMPLETED` absorve (idempotência).

**Unit (Mockito, `scheduling.application`)**:
- `ProfessionalAgendaHandlerTest`: `complete()` grava e incrementa a
  métrica só quando há transição real; `canComplete` correto para cada
  combinação de status × horário (incluindo o caso corrigido pela DD-4:
  `COMPLETED` não mostra `canCancel`/`canReschedule`).
- `DailyScheduleSummaryHandlerTest` (novo): contagem, soma de receita
  (exclui `CANCELLED`/`NO_SHOW`) e seleção do próximo cliente, com
  `AppointmentRepository` e `CustomerDirectory` mockados.
- `SchedulingMetricsTest`: `appointmentCompleted()` incrementa o contador
  certo.
- `DashboardKpiAdviceTest` (novo): na URI `/admin/dashboard`, soma os 4
  atributos ao model; fora dela, nunca chama `DailyScheduleDirectory` —
  prova em teste da correção da DD-6 (sem consulta à toa nas outras telas).

**Camada web (`@WebMvcTest`)**:
- `AgendaControllerTest`: nova rota `concluir` — sucesso redireciona
  (PRG), id de outro tenant devolve 404, sem CSRF é recusado (mesmo trio
  de testes já existente para `cancelar`).

**Integração (Postgres real, Testcontainers)**:
- `AgendaProfissionalIT` (já existe, TODO-008) ganha os cenários E2E-8 e
  E2E-9 da spec funcional: concluir de verdade muda o status no banco; não
  concluir antes do horário nem depois de cancelado.
- Novo `DashboardIT`: verifica que a sidebar aparece no painel (E2E-1) e
  que o dashboard mostra os KPIs reais batendo com o banco semeado
  (E2E-3), lendo os atributos de model no nível raiz (não mais
  `painel.*`, por causa da correção da DD-6).

**Sem teste de markup/CSS** (PATTERNS.md, "Teste de tela verifica
contrato, não markup"): os testes de sidebar/badge verificam presença de
texto/atributo relevante (ex.: nome do item de menu, `data-bs-theme`), não
a árvore de classes CSS inteira.

---

## Implementation Locations

| O quê | Arquivo |
|---|---|
| Fragmento da sidebar | `templates/fragments/layout.html` (`adminSidebar`, `carenciaBanner`, `whatsappButton`) |
| CSS genérica movida | `templates/fragments/layout.html` (`<style>`); removida de `templates/operador/painel.html` |
| 8 telas migradas | `templates/admin/{dashboard,agenda,agenda-novo,agenda-reagendar,bloqueios,horario-funcionamento,jornadas,ofertas,profissionais,servicos}.html` |
| Badge soft | `templates/operador/painel.html`, `templates/admin/agenda.html` |
| Enum | `scheduling/domain/AppointmentStatus.java` |
| Agregado | `scheduling/domain/Appointment.java` (`complete`) |
| View record | `scheduling/application/port/in/AgendaEntry.java` (`canComplete`) |
| Porta de entrada nova | `scheduling/application/port/in/CompleteAppointmentUseCase.java` |
| Handler | `scheduling/application/ProfessionalAgendaHandler.java` (`complete`, guard corrigido) |
| Métrica | `scheduling/application/SchedulingMetrics.java` (`appointmentCompleted`) |
| Porta de saída | `scheduling/application/port/out/AppointmentRepository.java` (`findByTenantIdAndDate`) |
| Adapter de persistência | `scheduling/adapter/out/persistence/AppointmentPersistenceAdapter.java`, `AppointmentJpaRepository.java` |
| **Novo pacote `api`** | `scheduling/api/package-info.java`, `DailyScheduleDirectory.java`, `DailyScheduleSummary.java`, `NextClientRef.java` |
| Implementação da api | `scheduling/application/DailyScheduleSummaryHandler.java` |
| Controller | `scheduling/adapter/in/web/AgendaController.java` (rota `concluir`) |
| KPIs do painel (sem tocar organization) | `scheduling/adapter/in/web/DashboardKpiAdvice.java` (novo, DD-6 corrigido) |
| Painel (organization) | `organization/application/ViewDashboardHandler.java`, `organization/application/port/in/DashboardView.java` — **sem mudança**, revertidos à forma original após o achado do ciclo |
| Template do painel | `templates/admin/dashboard.html` (KPIs lidos como `${agendamentosHoje}` etc., não `${painel.agendamentosHoje}`) |

---

## References

- ADR 0002 (rigor proporcional) — `scheduling` continua regime completo.
- ADR 0005 (exclusion constraint) — DD-5.
- ADR 0010 (fronteira entre contextos, Spring Modulith) — DD-6.
- `sdd/PATTERNS.md`, seção Frontend, "Sistema de design é token do
  Bootstrap" — origem da paleta/badge usada nesta feature.
- `sdd/features/20260906-agenda-profissional/` — `ProfessionalAgendaHandler`,
  `AgendaController`, `AgendaEntry` já existentes, estendidos aqui.
- `sdd/features/20260907-observabilidade/` — `SchedulingMetrics`, mesmo
  padrão de contador Micrometer.
