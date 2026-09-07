# Feature Metadata

**Feature Name**: agenda-profissional
**Feature ID**: feat-20260906-agenda-profissional
**Mode**: brownfield (por continuidade — mesma nota das features anteriores)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-09-06
**Last Updated**: 2026-09-06
**Current Stage**: implementation

> **Sobre o modo brownfield aqui**: `scheduling` já tem `Appointment`,
> `BookAppointmentHandler` (TODO-006) e `ManageAppointmentHandler`
> (TODO-007) — mas os dois só atendem o **cliente**, pela rota pública
> `/b/{slug}`. Nenhum controller admin (`/admin/**`) existe ainda em
> `scheduling` — esta é a primeira vez que o **dono/profissional** enxerga
> ou altera um `Appointment` pelo próprio painel, em vez do cliente pelo
> link público.

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
  detected: false   # a confirmar na spec tecnica; Appointment/AppointmentStatus ja existem, pode nao precisar de schema novo
  service_name: null
  service_type: null
  branch_name: null
  branch_status: pending
  migration_files: []
```

---

## Backlog Workflow

```yaml
from_backlog: TODO-008
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> Fecha o ciclo do dono. Muitos clientes vão continuar ligando, então o
> agendamento manual é requisito, não conveniência. Depois desta feature
> existe um produto que um barbeiro real consegue usar.

**Prioridade**: Medium · **Complexidade**: High
**Contextos afetados**: `scheduling`

---

## Contexto técnico levantado antes da spec

- **Primeira tela admin de `scheduling`.** Todo controller de
  `scheduling` até aqui é público (`/b/{slug}/**`, TODO-006/TODO-007).
  Esta feature precisa da mesma proteção de sessão que `organization`/
  `catalog` já usam (`hasRole("OWNER")`, `SecurityConfig` — nada de rota
  nova liberada por engano em `permitAll()`).
- **`RescheduleAppointmentUseCase` já está no glossário**
  (`docs/domain/glossary.md:91`, "Move o agendamento. Sujeito às mesmas
  invariantes de uma reserva nova.") mas nenhum código existe ainda.
- **Cancelamento pelo dono é diferente do cancelamento pelo cliente**
  (TODO-007): `ManageAppointmentHandler.cancel(UUID)` de hoje resolve o
  tenant via `TenantContext` (que na rota pública vem do slug). Numa rota
  admin, o tenant vem da sessão autenticada — mesmo mecanismo, caminho de
  resolução diferente. Decisão a levantar na spec técnica: reaproveitar
  `CancelAppointmentUseCase`/`ConfirmAppointmentUseCase` como estão, ou
  precisar de uma variante para o dono (ex.: sem a restrição de "agendamento
  no passado é somente leitura", que faz sentido para o cliente mas talvez
  não para o dono corrigindo um erro de digitação).
- **Criar agendamento manualmente é diferente de `BookAppointmentUseCase`**
  (TODO-006): o dono escolhe profissional/oferta/cliente diretamente, sem
  o funil de catálogo público. Precisa decidir: reaproveitar
  `BookAppointmentHandler` com um `Customer` já conhecido (busca por
  telefone) ou por um fluxo de criação de cliente dedicado ao admin.
- **Reagendar precisa da mesma garantia contra overbooking** (ADR 0005) —
  mover um `Appointment` para outro horário não pode abrir uma exceção na
  exclusion constraint; a spec técnica decide se é "cancelar o antigo +
  criar um novo" ou uma operação atômica de update de horário.
- **Visualização em agenda (calendário/lista) é uma consulta nova** —
  nenhum caso de uso hoje lista `Appointment` por profissional e período;
  `AppointmentRepository` só tem `findByTenantIdAndId`,
  `findOccupiedRanges` (por dia) e `countFutureActive`. Precisa de uma
  consulta nova para "todos os agendamentos do profissional X entre as
  datas Y e Z", com nome e telefone do cliente (diferente da tela pública,
  que nunca expõe isso).

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `docs/domain/glossary.md` | `RescheduleAppointmentUseCase` já nomeado — normativo |
| `docs/architecture/adr/0005-exclusion-constraint-contra-overbooking.md` | Overbooking impedido pelo banco, também vale para reagendamento |
| `docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md` | `scheduling` continua regime completo — domínio puro |
| `sdd/PROJECT.md` | Branching trunk-based: `feature/agenda-profissional`, origem e destino `main` |
| `CLAUDE.md` | `tenantId` nunca vem do cliente; overbooking impedido pelo banco; sem API REST |

**ADRs diretamente aplicáveis**:

- [0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — regime completo de `scheduling`
- [0005](../../../docs/architecture/adr/0005-exclusion-constraint-contra-overbooking.md) — exclusion constraint, também vale para criar/reagendar pelo admin
- [0011](../../../docs/architecture/adr/0011-ciclo-de-vida-dos-dados.md) — nada é apagado (cancelamento é mudança de status)

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-09-06
    completed: 2026-09-07
    status: approved
    owner: Elton Marques
    approved_by: Elton Marques
    approved_at: 2026-09-07T13:41:21Z
    iterations: 0

  technical:
    started: 2026-09-07
    completed: 2026-09-07
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-07T13:47:51Z
    mcpqueried: false
    services_count: 0

  tasks:
    started: 2026-09-07
    completed: 2026-09-07
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-07T00:00:00Z

  implementation:
    started: 2026-09-07
    completed: 2026-09-07
    status: done
```

---

## Execution Strategy

```yaml
execution_strategy:
  type: batched
  chosen_date: 2026-09-07
  estimated_agent_time: null
  estimated_tokens: null
  actual_agent_time: null
  rationale: "Mesma estrategia da TODO-007: 14 tarefas, maioria Camada 1 independente"
```

---

## Metrics

```yaml
metrics:
  timeline: { estimated_days: null, actual_days: null, variance_percent: null }
  effort: { estimated_hours: null, actual_hours: null, variance_percent: null }
  quality: { test_coverage: "89%", tests_total: 451, tests_passing: 451, linter_errors: 0, type_errors: 0 }
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

Nona feature do projeto pelo ciclo SDD completo. Fecha o ciclo do dono
que a TODO-006/TODO-007 abriram para o cliente: até aqui, o dono não tem
nenhuma tela para ver a própria agenda, criar um agendamento manualmente
(cliente que ligou), cancelar em nome do cliente ou reagendar. Sem isto,
o dono continua dependendo de anotar em papel ou perguntar ao sistema por
fora — o problema que o produto existe para resolver, agora do lado de
quem presta o serviço, não de quem agenda.
