# Feature Metadata

**Feature Name**: gestao-de-clientes
**Feature ID**: feat-20260909-gestao-de-clientes
**Mode**: brownfield (por continuidade — mesma nota das features anteriores)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-09-09
**Last Updated**: 2026-09-09
**Current Stage**: functional

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
  detected: true   # provavel: status de pagamento no agendamento (IDEA-006) precisa de coluna nova
  service_name: null
  service_type: null
  branch_name: null
  branch_status: pending
  migration_files: []
```

---

## Backlog Workflow

```yaml
from_backlog: "IDEA-018, IDEA-019, IDEA-006"
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog** (três ideias combinadas numa feature só, por
formarem uma tela coerente — decisão tomada em conversa com o dono em
2026-09-09, a partir da análise de um relatório de features):

> **IDEA-018** — Tela `/admin/clientes` (não existe hoje) com contador de
> visitas por cliente e sinalização de "cliente novo" (sem histórico, risco
> maior de no-show). Ao abrir o cliente: histórico completo (data, serviço,
> valor pago em cada visita) e totais (total de visitas, total gasto).
>
> **IDEA-019** — Link `wa.me/{telefone do cliente}` na mesma tela (ou na
> agenda), para o dono contatar o cliente direto — hoje só existe o caminho
> inverso (cliente → estabelecimento).
>
> **IDEA-006** — Status de pagamento por agendamento: `PAGO`/`PENDENTE`/
> `FIADO`, enum separado de `AppointmentStatus` (pergunta diferente: "foi
> pago?" vs. "o atendimento aconteceu?"). Alimenta o "total em aberto" no
> perfil do cliente (soma de `FIADO`).

**Prioridade**: Medium · **Complexidade**: Medium

**Contextos afetados**: `customer` (histórico, contagem — dado já existe via
`Appointment.customerId`), `scheduling` (novo campo/status de pagamento no
`Appointment`, e a consulta agregada por cliente), `organization` (nova tela
em `/admin/**`, entra na sidebar já existente desde a TODO-110)

---

## Contexto técnico levantado antes da spec

- **`Customer` e `Appointment` já se relacionam** por `customerId` — não é
  preciso desenhar relação nova, só consulta agregada (contagem, soma de
  valor, "cliente novo" = zero agendamentos anteriores).
- **Não existe hoje nenhum controller/template `admin/clientes`** —
  `CustomerRepository` só expõe `findByTenantIdAndPhone` (get-or-create),
  `findByTenantIdAndId` e `findByTenantIdAndIdIn` (usado só para resolver
  nome na agenda). A spec técnica precisa decidir a forma da nova consulta
  agregada (uma projeção por cliente: nome, telefone, total de visitas,
  valor total, data da última visita, "é novo?").
- **Status de pagamento é campo novo em `Appointment`** (`scheduling.domain`,
  Java puro — nada de anotação JPA ali) — precisa de migration Flyway
  (`your-migration-tool init`, nunca SQL manual) e de mapeamento no adapter
  de persistência. Default para agendamento novo: a decidir na spec (talvez
  `PENDENTE` até o dono marcar).
- **Onde a ação de marcar pago/fiado aparece**: provavelmente ao lado do
  botão "Concluir" que a TODO-110 acabou de criar em `admin/agenda.html` —
  a decidir na spec funcional (mesma tela, ou só na tela de detalhe do
  cliente?).
- **Sidebar já existe** (TODO-110) — a nova tela de clientes só precisa de
  mais um item no menu (`adminSidebar`), sem trabalho de estrutura.
- **WhatsApp por cliente é renderização pura** — `Customer.phone` já existe,
  sem campo novo, só o link `wa.me` no template.

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `sdd/PATTERNS.md`, seção Frontend | Sistema de design (sidebar, badge soft, card de KPI) — a tela de clientes segue os mesmos padrões já traduzidos do protótipo |
| `CLAUDE.md` | Sem API REST — tela renderizada no servidor; `tenantId` nunca vem do cliente, sempre da sessão (`/admin/**`) |
| `docs/domain/glossary.md` | Nome de campo/classe novo (status de pagamento, projeção de cliente) precisa entrar aqui se normativo |

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-09-09
    completed: null
    status: pending

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
  quality: { test_coverage: null, tests_total: null, tests_passing: null, linter_errors: null, type_errors: null }
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

Décima segunda feature do projeto pelo ciclo SDD completo. Diferente das
últimas duas (TODO-110 retrofit visual, TODO-108 transversal), esta é uma
fatia de produto nova de verdade: tela que não existe, dado que ninguém
consultava antes (histórico agregado por cliente) e um conceito de domínio
novo (status de pagamento). Nasce da análise de um relatório de features
trazido pelo dono em 2026-09-09, combinando três ideias do backlog
(IDEA-018, IDEA-019, IDEA-006) que fazem mais sentido como uma tela só do
que como três entregas separadas. O item "duplicar agenda do dia anterior"
do mesmo relatório foi deliberadamente deixado de fora — caso de uso ainda
incerto.
