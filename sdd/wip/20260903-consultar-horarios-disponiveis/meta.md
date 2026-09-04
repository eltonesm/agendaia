# Feature Metadata

**Feature Name**: consultar-horarios-disponiveis
**Feature ID**: feat-20260903-consultar-horarios-disponiveis
**Mode**: brownfield (por continuidade — mesma nota das features anteriores)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-09-03
**Last Updated**: 2026-09-03
**Current Stage**: functional

> **Sobre o modo brownfield aqui**: `organization` já tem `BusinessOperatingHours`,
> `WorkSchedule` e `TimeOff` implementados e testados (TODO-004) — os dados
> declarados que esta feature vai consumir. `scheduling` está **vazio**: esta é
> a primeira linha de código real desse contexto.

---

## Framework Version

```yaml
framework:
  version_created: "desconhecida"
  version_current: null
  last_compatibility_check: null
  migration_notes:
    - "Mesma instalação parcial das features anteriores: ver sdd-kit/PORTABILITY.md para o que funciona de fato nesta instalação fora do Mercado Livre."
```

---

## Project Type Configuration

```yaml
project_type:
  type: production
  decision_date: 2026-08-30   # herdado da TODO-001, nao redecidido aqui
  inherited_from: TODO-001

  testing:
    unit_tests: full_coverage
    ltp_enabled: false
    coverage_target: "80%"    # piso real, verificado pelo JaCoCo desde a TODO-001
```

---

## User Profile Configuration

```yaml
user_profile:
  type: technical
  source: sdd-kit/framework/user-profile.yaml
  selected_at: 2026-08-30
```

---

## Spec Language

```yaml
spec_language: pt   # herdado de sdd/PROJECT.md -> language.specs
```

---

## Database Migrations

```yaml
migration:
  detected: true
  service_name: "agendaia (compose local; VPS na TODO-106)"
  service_type: postgresql
  branch_name: null
  branch_status: pending
  migration_files: []
```

> Escopo de dado ainda em aberto para a spec técnica: esta feature é
> **só leitura** (consulta de horários livres). O agregado `Appointment`
> (agendamento de verdade, com exclusion constraint — ADR 0005) está
> descrito no glossário como parte de `scheduling`, mas a **escrita** é a
> TODO-006 ("Página pública e agendar"), não esta. Decisão pendente: a
> tabela `appointment` nasce aqui (vazia, só para a consulta já "descontar"
> algo que sempre será zero por enquanto) ou só na TODO-006? Ver "Contexto
> técnico" abaixo.

---

## Backlog Workflow

```yaml
from_backlog: TODO-005
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> O cálculo do core, ainda sem escrita. Interseção de horário da empresa com
> jornada, menos bloqueios e agendamentos, filtrada por quem comporta duração
> mais intervalo. Grade fixa de 10 min (ADR 0006). É a feature mais
> importante do projeto e a mais barata de errar cedo.

**Prioridade**: High · **Complexidade**: High
**Contextos afetados**: `scheduling` (primeiro código real do contexto), leitura de `organization.api` (jornada/bloqueio) e `catalog.api` (duração/intervalo da oferta)

---

## Contexto técnico levantado antes da spec

- **Primeira feature em regime de rigor completo (ADR 0002).** Diferente de
  `organization`/`catalog`/`customer` (regime CRUD: entidade JPA é o modelo),
  `scheduling` é o **core domain**: domínio em Java puro (sem
  `org.springframework`, sem `jakarta.persistence` — regra já travada por
  ArchUnit), entidade JPA separada, mapeamento explícito, portas de
  entrada/saída completas. Esta é a primeira vez que esse regime é exercido
  de verdade neste projeto — as quatro features anteriores foram todas
  regime CRUD.
- **"Disponibilidade" tem dois significados, e o glossário proíbe o nome
  ambíguo.** Declarada (`WorkSchedule` + `TimeOff`, já existe em
  `organization`) vs. Calculada (`AvailableSlot`, resultado de cálculo,
  nunca persistido — mora aqui). Nenhuma classe pode se chamar só
  "Disponibilidade"/"Availability" de forma ambígua; o glossário já reserva
  `AvailableSlot` (value object) como o nome do horário candidato. Em
  aberto para a spec técnica: existe uma classe/DTO `Availability` (a lista
  de `AvailableSlot` para profissional+oferta+data) ou o caso de uso
  simplesmente retorna `List<AvailableSlot>` direto?
- **Grade fixa de 10 minutos, já decidida (ADR 0006).** Só `FIXED_GRID`
  existe; `booking_strategy` é uma coluna de banco com um único valor legal,
  não uma interface. A geração de starts candidatos é um método privado
  com nome próprio dentro do calculador — não abstrair prematuramente.
- **O pipeline de cálculo é conhecido desde a arquitetura**: horário da
  empresa (`BusinessOperatingHours`, via `organization.api`) ∩ jornada do
  profissional (`WorkSchedule`, via `organization.api`) − bloqueios
  (`TimeOff`, via `organization.api`) − agendamentos existentes (`Appointment`,
  local a `scheduling`, se já existir) → janelas livres → gerar starts na
  grade de 10 min → filtrar quem comporta duração + `bufferMinutes` da
  oferta (via `catalog.api`). É a primeira vez que um caso de uso deste
  projeto lê de **dois** contextos diferentes via `api` ao mesmo tempo.
- **`Appointment` existe ou não nesta feature?** Glossário descreve
  `Appointment` como parte de `scheduling`, mas TODO-006 ("A escrita.
  Exclusion constraint ADR 0005...") é quem parece introduzir a tabela e a
  constraint de verdade. Se `Appointment` não existir ainda, o termo
  "menos agendamentos existentes" do cálculo é trivialmente vazio agora
  (não há como agendar ainda) — o que é uma escolha razoável (a tabela e a
  constraint nascem só quando há escrita real para proteger), mas precisa
  ser uma decisão explícita da spec técnica, não uma omissão.
- **Nenhuma tela nova.** Esta feature é o cálculo interno; quem vai
  *mostrar* o horário disponível ao cliente é a TODO-006 (página pública).
  Critério de aceite aqui provavelmente se apoia em teste automatizado
  (unitário de domínio + talvez um endpoint interno para inspeção manual
  durante o desenvolvimento), não em uma tela de admin nova — mas a forma
  exata de expor/testar isso é pergunta para a spec funcional.
- **`slotInterval`** já é mencionado no glossário como "Configuração do
  tenant... Padrão: 10 minutos" — não existe campo isso ainda em nenhuma
  tabela. Se o estabelecimento pode configurar (mesmo que só o intervalo
  entre clientes, per ADR 0006 — "o que o estabelecimento configura é o
  intervalo entre clientes, que ele entende"), é uma pergunta em aberto se
  isso é uma tela nova ou uma constante por enquanto (piloto único, n=1).

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `docs/domain/glossary.md` | Definições normativas de `Appointment`, `AvailableSlot`, `Availability`, `slotInterval` (seção Scheduling); armadilha #2 sobre o nome "disponibilidade" |
| `sdd/PATTERNS.md` | Regime de rigor completo para `scheduling` (ADR 0002); regra ArchUnit `scheduling.domain` sem Spring/JPA |
| `sdd/PROJECT.md` | Branching trunk-based: `feature/consultar-horarios-disponiveis`, origem e destino `main` |

**ADRs diretamente aplicáveis**:

- [0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — `scheduling` é core domain, regime completo (domínio puro + entidade JPA + mapper + portas)
- [0006](../../../docs/architecture/adr/0006-grade-fixa-como-unica-estrategia-de-slot.md) — grade fixa de 10 min, única estratégia no MVP
- [0004](../../../docs/architecture/adr/0004-multi-tenancy-por-discriminador.md) — `tenant_id` vem da sessão, nunca do formulário
- [0009](../../../docs/architecture/adr/0009-uuidv7-como-identificador.md) — identidade gerada na aplicação
- [0005](../../../docs/architecture/adr/0005-exclusion-constraint-contra-overbooking.md) — exclusion constraint contra overbooking (relevante para decidir se `Appointment` nasce aqui ou na TODO-006)

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-09-03
    completed: 2026-09-03
    status: approved
    owner: Elton Marques
    approved_by: Elton Marques
    approved_at: 2026-09-04T01:08:24Z
    iterations: 0

  technical:
    started: null
    completed: null
    status: pending

  tasks:
    started: null
    completed: null
    status: pending

  implementation:
    started: null
    completed: null
    status: pending
    execution_strategy: null
    total_tasks: null
    completed_tasks: 0
```

---

## Metrics

```yaml
metrics:
  timeline: { estimated_days: null, actual_days: null, variance_percent: null }
  effort: { estimated_hours: null, actual_hours: null, variance_percent: null }
  quality: { test_coverage: null, tests_total: null, tests_passing: null, linter_errors: 0, type_errors: 0 }
  velocity: { avg_hours_per_task: null, estimation_accuracy: null }
```

---

## Changes and Deviations

```yaml
changes:
  tasks_added: []
  tasks_removed: []
  tasks_modified: []
  spec_changes:
    functional: []
    technical: []
  risks_materialized: []
```

---

## Validation Overrides

```yaml
overrides:
  functional: { forced: false, reason: null, date: null }
  technical: { forced: false, reason: null, date: null }
  tasks: { forced: false, reason: null, date: null }
  complete: { forced: false, reason: null, date: null }
```

---

## Notes

Quinta feature do projeto pelo ciclo SDD completo, e a primeira a tocar
`scheduling` — regime de rigor completo (ADR 0002), diferente de todas as
anteriores (regime CRUD). Duas perguntas em aberto levantadas antes da spec
funcional: se `Appointment` nasce nesta feature (vazio) ou só na TODO-006
(quando há escrita de verdade para proteger), e se existe uma classe
`Availability` ou o caso de uso retorna `List<AvailableSlot>` direto — ver
"Contexto técnico" acima.
