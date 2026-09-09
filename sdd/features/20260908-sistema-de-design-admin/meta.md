# Feature Metadata

**Feature Name**: sistema-de-design-admin
**Feature ID**: feat-20260908-sistema-de-design-admin
**Mode**: brownfield (por continuidade — mesma nota das features anteriores)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-09-08
**Last Updated**: 2026-09-09
**Current Stage**: completed

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
  detected: false   # a confirmar na spec tecnica; provavel que NAO precise (sem campo novo em Business/Appointment)
  service_name: null
  service_type: null
  branch_name: null
  branch_status: pending
  migration_files: []
```

---

## Backlog Workflow

```yaml
from_backlog: TODO-110
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> Trocar a navbar simples por sidebar de navegação (IDEA-015) em todas as
> telas de `/admin/**`; trocar badge de status sólido (`text-bg-*`) pelo
> padrão soft/subtle (`bg-*-subtle` + `text-*-emphasis`) definido no guia —
> inclusive em `operador/painel.html`, que já tem sidebar mas ainda está no
> padrão antigo de badge; e adicionar ao dashboard os cards de KPI com dado
> real (agendamentos de hoje, próximo cliente, atendidos hoje), agora viável
> porque `scheduling`/`Appointment` existe desde a TODO-008 (bloqueio antigo
> da IDEA-009).

**Prioridade**: Medium · **Complexidade**: Large
**Contextos afetados**: `platform` (fragments/layout.html), `organization`
(telas de `/admin/**`), `scheduling` (KPIs do dashboard dependem de
`Appointment`)

---

## Contexto técnico levantado antes da spec

- **A paleta e o dark mode já existem de verdade**, não é ponto em aberto:
  `fragments/layout.html` já tem os tokens de cor (claro/escuro), o
  fragmento `temaToggle` e o fragmento `publicHeader` — trabalho feito
  numa sessão anterior a esta feature (ainda não commitado em `main`), a
  ser incorporado no primeiro commit desta feature. O que falta é
  **estrutura**: sidebar e KPI.
- **`operador/painel.html` já é o modelo de sidebar a copiar** — largura
  fixa, `border-end`, item ativo com `bg-primary-subtle text-primary`,
  colapsa em telas pequenas com uma barra de topo alternativa. As telas de
  `/admin/**` hoje usam `fragments/layout :: navbar` (barra horizontal
  simples), compartilhada por 10 templates.
- **`operador/painel.html` usa badge sólido (`text-bg-info/success/warning/
  danger`), não o padrão soft novo** (`bg-*-subtle` + `text-*-emphasis`) —
  precisa de ajuste indo junto com esta feature, não só as telas de admin.
- **KPI do dashboard é o item que exige código novo, não só template**: hoje
  não existe nenhuma consulta de "agendamentos de hoje", "próximo cliente"
  ou "atendidos hoje" para o dono do estabelecimento — só existe agenda por
  data/profissional (`AppointmentRepository.findByTenantIdAndProfessionalIdAndDate`,
  da TODO-008) e agenda por dia inteiro do estabelecimento ainda não tem
  handler dedicado. A spec técnica precisa decidir: nova porta de leitura em
  `scheduling.application.port.out`, ou reaproveitar consulta existente
  agregando no dashboard.
- **Sidebar única ou por seção?** `/admin/**` hoje tem 10 telas (agenda ×3,
  bloqueios, horário de funcionamento, jornadas, ofertas, profissionais,
  serviços, dashboard, conta-suspensa) — a spec funcional precisa decidir os
  itens de menu da sidebar (provavelmente: Painel, Agenda, Serviços,
  Profissionais, Configurações — precisa mapear as 10 rotas em grupos, não
  10 itens soltos).
- **`admin/conta-suspensa.html` é tela de bloqueio (billing), sem ação
  disponível** — precisa decidir se ela ganha sidebar (mostrando itens
  desabilitados) ou fica fora do escopo por ser uma tela de "beco sem
  saída" intencional.

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `sdd/PATTERNS.md`, seção Frontend | Sistema de design (paleta, componentes, dark mode) — a espinha desta feature é aplicar o que já está documentado |
| `docs/architecture/adr/0012-...-tema-bootstrap.md` (ver nome real do arquivo) | Bootstrap 5 por CDN, fixado — nenhuma dependência nova, só classe e estrutura |
| `CLAUDE.md` | Sem API REST — tela renderizada no servidor; qualquer dado novo para o KPI é resolvido no controller, nunca calculado no template |

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-09-08
    completed: 2026-09-08
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-08T22:43:05Z

  technical:
    started: 2026-09-08
    completed: 2026-09-09
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-09T00:52:44Z

  tasks:
    started: 2026-09-09
    completed: 2026-09-09
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-09T00:56:37Z

  implementation:
    started: 2026-09-09
    completed: 2026-09-09
    status: completed
```

---

## Execution Strategy

```yaml
execution_strategy:
  type: batched
  chosen_date: 2026-09-09
  estimated_agent_time: null
  estimated_tokens: null
  actual_agent_time: null
  rationale: "21 tarefas, duas frentes largamente independentes (status COMPLETED/KPI vs sidebar/badge) — batched aproveita isso sem a sobrecarga de paralelismo total."
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

Décima primeira feature do projeto pelo ciclo SDD completo. Como a
observabilidade (TODO-108), não é uma fatia vertical de produto novo — é
retrofit visual sobre telas que já existem e já funcionam, guiado por um
documento de design que o dono trouxe (protótipos Gemini/Tailwind,
traduzidos para Bootstrap 5 em `sdd/PATTERNS.md` numa sessão anterior a
esta feature). O critério de "pronto" é: toda tela de `/admin/**` usa
sidebar em vez de navbar, todo badge de status usa o padrão soft novo, e o
dashboard mostra pelo menos um KPI com dado real por trás — nenhum card
decorativo com número fixo.
