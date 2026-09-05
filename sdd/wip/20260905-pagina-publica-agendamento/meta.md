# Feature Metadata

**Feature Name**: pagina-publica-agendamento
**Feature ID**: feat-20260905-pagina-publica-agendamento
**Mode**: brownfield (por continuidade — mesma nota das features anteriores)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-09-05
**Last Updated**: 2026-09-05
**Current Stage**: functional

> **Sobre o modo brownfield aqui**: `scheduling` já tem `SlotCalculator` e
> `GetAvailableSlotsHandler` (TODO-005) — o cálculo de horários disponíveis
> já existe e é só consultado aqui. Esta feature é a primeira **escrita**
> nesse contexto: cria `Appointment`, a exclusion constraint (ADR 0005) e o
> formulário público em `/b/{slug}` que o cliente usa sem estar logado.

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
  migration_files: []   # a confirmar na spec tecnica (tabela appointment + exclusion constraint, ADR 0005)
```

---

## Backlog Workflow

```yaml
from_backlog: TODO-006
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> A escrita. Exclusion constraint (ADR 0005), revalidação de todo id do
> formulário contra o tenant do slug, teste de concorrência com duas
> reservas simultâneas. Inclui as defesas contra abuso: honeypot, rate
> limit e teto por telefone.

**Prioridade**: High · **Complexidade**: High
**Contextos afetados**: `scheduling`, `customer`, `platform`

---

## Contexto técnico levantado antes da spec

- **`Appointment` nasce aqui.** Nenhuma tabela, nenhuma exclusion
  constraint (ADR 0005) existe ainda em `scheduling` — a TODO-005 só
  calculava disponibilidade, não persistia nada.
- **Overbooking é impedido pelo banco, não pela aplicação** (regra
  fundamental do `CLAUDE.md`) — a exclusion constraint com `tstzrange` e
  limites `[)` é a garantia real; a checagem em memória contra
  `SlotCalculator` é só feedback rápido ao cliente antes de tentar
  gravar.
- **`customer` nasce ou ganha o primeiro uso real aqui** — quem agenda
  pelo link público (`/b/{slug}`) é um `Customer`, não um `User` (que é
  sempre dono de um tenant). Precisa decidir na spec técnica: cliente é
  criado/reaproveitado por telefone dentro do mesmo tenant, sem conta,
  sem senha.
- **`tenantId` nunca vem do cliente** (regra fundamental) — o formulário
  público só tem o `slug` na URL; todo id recebido (profissional, oferta
  de serviço, horário) precisa ser revalidado contra o tenant resolvido
  pelo slug antes de gravar.
- **Defesas contra abuso são novas no projeto**: honeypot (campo
  invisível que só bot preenche), rate limit por IP/sessão e teto de
  agendamentos por telefone — nenhum desses mecanismos existe hoje em
  `platform`. Decisão técnica: onde vive cada um, e se algum exige
  biblioteca nova.
- **Teste de concorrência é obrigatório**: duas requisições disputando o
  mesmo horário do mesmo profissional — uma tem que ganhar, a outra tem
  que receber erro de conflito, nunca as duas gravando.

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `docs/architecture/adr/0005-*.md` | Exclusion constraint com `tstzrange` como mecanismo de prevenção de overbooking — normativo, não é decisão desta feature |
| `docs/domain/glossary.md` | Nomes de `Appointment`, `Customer` e seus estados — normativo |
| `sdd/PROJECT.md` | Branching trunk-based: `feature/pagina-publica-agendamento`, origem e destino `main` |
| `CLAUDE.md` | `tenantId` nunca vem do cliente; overbooking impedido pelo banco, não pela aplicação |

**ADRs diretamente aplicáveis**:

- [0005](../../../docs/architecture/adr/0005-exclusion-constraint-contra-overbooking.md) — exclusion constraint contra overbooking
- [0006](../../../docs/architecture/adr/0006-grade-fixa-como-unica-estrategia-de-slot.md) — grade fixa como única estratégia de slot, já usada pela TODO-005
- [0008](../../../docs/architecture/adr/0008-rota-publica-com-prefixo.md) — rota pública com prefixo (`/b/{slug}`), diretamente aplicável ao formulário desta feature
- [0009](../../../docs/architecture/adr/0009-uuidv7-como-identificador.md) — identidade gerada na aplicação
- [0011](../../../docs/architecture/adr/0011-ciclo-de-vida-dos-dados.md) — nada é apagado

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-09-05
    completed: null
    status: pending
    owner: null
    approved_by: null
    approved_at: null
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
```

---

## Execution Strategy

```yaml
execution_strategy:
  type: null   # decidido no /sdd.plan
  chosen_date: null
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

Sexta feature do projeto pelo ciclo SDD completo (sétima, contando a
TODO-009 fora de ordem numérica). Primeira a escrever de verdade em
`scheduling` — todas as anteriores só liam ou calculavam. Primeira
também a expor uma rota pública sem autenticação (`/b/{slug}`), o que
muda a superfície de ataque: CSRF não se aplica a quem nunca logou, mas
abuso automatizado (bot reservando horários) vira o risco central.
