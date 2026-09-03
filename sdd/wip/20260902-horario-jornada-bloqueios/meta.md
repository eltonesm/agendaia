# Feature Metadata

**Feature Name**: horario-jornada-bloqueios
**Feature ID**: feat-20260902-horario-jornada-bloqueios
**Mode**: brownfield (por continuidade — mesma nota da TODO-002/003)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-09-02
**Last Updated**: 2026-09-02
**Current Stage**: technical

> **Sobre o modo brownfield aqui**: `organization` já tem `Business`, `User`
> e `Professional` implementados e testados (TODO-001/002). Esta feature
> acrescenta três agregados novos ao mesmo contexto —
> `BusinessOperatingHours`, `WorkSchedule`, `TimeOff` — sem tocar código
> existente. `/sdd.reverse-eng` não se aplica: os três já estão documentados
> no glossário e no data-model desde a Fase 0, só falta o código.

---

## Framework Version

```yaml
framework:
  version_created: "desconhecida"
  version_current: null
  last_compatibility_check: null
  migration_notes:
    - "Mesma instalação parcial das TODO-001/002/003: ver sdd-kit/PORTABILITY.md para o que funciona de fato nesta instalação fora do Mercado Livre."
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

> Três tabelas esperadas: `business_operating_hours` (entidade de `Business`),
> `work_schedule` (raiz de agregado, por profissional) e `time_off` (entidade
> de `WorkSchedule`, com `professional_id` anulável — nulo vale para o
> estabelecimento inteiro, é assim que feriado e fechamento entram sem
> tabela nova).

---

## Backlog Workflow

```yaml
from_backlog: TODO-004
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> Os dados que alimentam o cálculo de disponibilidade.
> `BusinessOperatingHours`, `WorkSchedule` e `TimeOff`. Feriado é um
> `TimeOff` de dia inteiro. Sem estes dados a disponibilidade não tem de
> onde sair.

**Prioridade**: High · **Complexidade**: Medium
**Contextos afetados**: `organization` (três agregados novos, nenhum código de outro contexto tocado)

---

## Contexto técnico levantado antes da spec

- **`BusinessOperatingHours` — entidade de `Business`, não raiz própria.**
  `day_of_week` + `opens_at` + `closes_at`, hora local. Várias faixas por dia
  são permitidas; dia sem nenhuma linha é dia fechado (glossário, seção
  "Horário de funcionamento"; data-model.md).
- **`WorkSchedule` — raiz de agregado, por profissional.** `professional_id`,
  `day_of_week`, `starts_at`, `ends_at`, hora local. **Almoço recorrente é
  modelado como duas faixas no mesmo dia**, não como `TimeOff` — o vão entre
  as duas faixas É o almoço. Isso já está decidido no glossário; a spec
  funcional não precisa reabrir essa escolha, só refletir nos critérios de
  aceite.
- **`TimeOff` — entidade de `WorkSchedule`, indisponibilidade excepcional e
  datada.** `starts_at`/`ends_at` (timestamptz, não `day_of_week` recorrente
  — é pontual), `reason` opcional. **`professional_id` anulável**: nulo
  significa que vale para o estabelecimento inteiro — é assim que feriado e
  fechamento para reforma entram sem tabela nova (data-model.md, seção
  TimeOff).
- **Nenhum agendamento existe ainda.** `scheduling` está vazio — esta
  feature só declara os dados de disponibilidade, não calcula nada com eles.
  O cálculo de fato (interseção `WorkSchedule` menos `TimeOff` menos
  agendamentos existentes) é `scheduling`, feature futura. **Pergunta em
  aberto para a spec funcional**: até onde vai o critério de aceite aqui —
  só CRUD dos três agregados, ou também alguma validação cruzada (ex.: um
  `WorkSchedule` fora do `BusinessOperatingHours` do dia é um estado
  possível, já que ninguém força a interseção agora)?
- **Sobreposição de faixas dentro do mesmo agregado**: nada no glossário diz
  se duas faixas do mesmo dia no mesmo `WorkSchedule` podem se sobrepor (ex.:
  08:00–12:00 e 10:00–14:00). **Pergunta em aberto para a spec funcional.**
- **Telas novas**: nenhuma tela de admin para isso existe ainda. Precisa de
  no mínimo uma tela por agregado, ou uma combinada — mesma decisão de
  design (DD) que TODO-002 (uma tela) e TODO-003 (duas telas) já
  precisaram tomar. **Pergunta em aberto para a spec técnica.**

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `docs/domain/glossary.md` | Definições normativas de `BusinessOperatingHours`, `WorkSchedule`, `TimeOff` (seção de disponibilidade) |
| `docs/domain/data-model.md` | Schema conceitual dos três agregados, incluindo o padrão de almoço como duas faixas |
| `sdd/PATTERNS.md` | Rigor proporcional (`organization` é regime CRUD); `Repository` mora em `application.port.out`, nunca em `domain` (decisão de 2026-09-02) |
| `sdd/PROJECT.md` | Branching trunk-based: `feature/horario-jornada-bloqueios`, origem e destino `main` |

**ADRs diretamente aplicáveis**:

- [0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — `organization` é subdomínio de suporte, regime CRUD; repositório em `application.port.out` (amendment 2026-09-02)
- [0004](../../../docs/architecture/adr/0004-multi-tenancy-por-discriminador.md) — `tenant_id` vem da sessão, nunca do formulário
- [0009](../../../docs/architecture/adr/0009-uuidv7-como-identificador.md) — identidade gerada na aplicação
- [0011](../../../docs/architecture/adr/0011-ciclo-de-vida-dos-dados.md) — nada é apagado; `TimeOff` e `WorkSchedule` seguem o mesmo ciclo de vida de `Professional`/`Service`

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-09-02
    completed: 2026-09-02
    status: approved
    owner: Elton Marques
    approved_by: Elton Marques
    approved_at: 2026-09-03T01:17:35Z
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
    total_tasks: 0
    completed_tasks: 0
```

---

## Execution Strategy

```yaml
execution_strategy:
  type: null
  chosen_date: null
  estimated_agent_time: null
  estimated_tokens: null
  actual_agent_time: null
  rationale: null
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

Quarta feature do projeto pelo ciclo SDD completo. Duas perguntas abertas
levantadas antes da spec funcional (validação cruzada com
`BusinessOperatingHours`, sobreposição de faixas dentro do mesmo
`WorkSchedule`) e uma decisão de design adiada para a spec técnica (telas
combinadas ou separadas) — ver seção "Contexto técnico" acima.
