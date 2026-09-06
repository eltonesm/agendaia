# Feature Metadata

**Feature Name**: confirmacao-e-cancelamento
**Feature ID**: feat-20260905-confirmacao-e-cancelamento
**Mode**: brownfield (por continuidade — mesma nota das features anteriores)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-09-05
**Last Updated**: 2026-09-05
**Current Stage**: functional

> **Sobre o modo brownfield aqui**: `scheduling` já tem `Appointment`,
> `BookAppointmentHandler` e a rota pública `/b/{slug}` inteira (TODO-006).
> Esta feature fecha o ciclo do cliente: hoje ele agenda e não recebe nada
> além da tela de sucesso — sem link para conferir depois, sem forma de
> cancelar, sem lembrete. `CancelAppointmentUseCase` já está previsto no
> glossário (`docs/domain/glossary.md`) mas ainda não existe código.

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
  migration_files: []   # a confirmar na spec tecnica (provavel coluna de token em appointment, ou tabela nova)
```

---

## Backlog Workflow

```yaml
from_backlog: TODO-007
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> Fecha o ciclo do cliente: token assinado para ver e cancelar, arquivo
> `.ics` e link `wa.me` pré-preenchido. Sem isto o cliente agenda e não
> recebe nada, e volta a perguntar por WhatsApp — que é o problema que o
> produto existe para resolver.

**Prioridade**: Medium · **Complexidade**: Medium
**Contextos afetados**: `scheduling`, `platform`

---

## Contexto técnico levantado antes da spec

- **`CancelAppointmentUseCase` já está no glossário** (`docs/domain/glossary.md:89`,
  "Libera o horário") mas nenhum código existe ainda — é o primeiro caso de
  uso de escrita em `scheduling` além de `BookAppointmentUseCase`.
- **Cliente não tem login** (ADR 0008, rota pública `/b/{slug}`) — a tela
  de "ver e cancelar" não pode exigir sessão. Precisa de identidade por
  token (assinado, não adivinhável), decisão a levantar na spec técnica:
  HMAC sobre `appointmentId`, JWT sem estado, ou coluna de token
  persistida no próprio `Appointment`.
- **`tenantId` nunca vem do cliente** (regra fundamental) — o token
  precisa carregar ou permitir derivar o tenant sem confiar em nenhum
  outro dado da requisição.
- **Cancelamento tem que respeitar o mesmo isolamento por tenant** que já
  existe em `BookAppointmentHandler` — cancelar não pode expor nem alterar
  agendamento de outro estabelecimento, mesmo com token válido de outro
  tenant.
- **Arquivo `.ics`** é conteúdo gerado, não persistido — decisão técnica
  sobre biblioteca (ou geração manual do formato, que é texto simples) e
  timezone (`ends_at`/`starts_at` já são `Instant`, então UTC na origem).
- **Link `wa.me` pré-preenchido** é só uma URL montada com o texto da
  mensagem — não é integração com WhatsApp Business API, é o link público
  `https://wa.me/<numero>?text=<mensagem>` que abre o app do cliente.
- **Momento de entrega do link**: hoje a tela de sucesso
  (`PublicBookingController.sucesso`) já existe — o link de
  ver/cancelar/`.ics`/`wa.me` provavelmente aparece ali, mas a spec
  funcional precisa decidir se também é reenviado por algum canal (e-mail
  fica fora de escopo — TODO-109/TODO-108 já apontam que não há e-mail
  transacional no projeto ainda).

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `docs/domain/glossary.md` | `CancelAppointmentUseCase` já nomeado — normativo |
| `docs/architecture/adr/0008-rota-publica-com-prefixo.md` | Rota pública sem autenticação, mesma família de `/b/{slug}` |
| `docs/architecture/adr/0009-uuidv7-como-identificador.md` | Identidade gerada na aplicação |
| `sdd/PROJECT.md` | Branching trunk-based: `feature/confirmacao-e-cancelamento`, origem e destino `main` |
| `CLAUDE.md` | `tenantId` nunca vem do cliente; sem API REST (fragmento HTML é exceção, não padrão) |

**ADRs diretamente aplicáveis**:

- [0008](../../../docs/architecture/adr/0008-rota-publica-com-prefixo.md) — rota pública com prefixo, mesma família de `/b/{slug}`
- [0009](../../../docs/architecture/adr/0009-uuidv7-como-identificador.md) — identidade gerada na aplicação
- [0011](../../../docs/architecture/adr/0011-ciclo-de-vida-dos-dados.md) — nada é apagado (cancelamento muda status, não deleta linha)

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
    status: in_progress
    owner: Elton Marques
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

Oitava feature do projeto pelo ciclo SDD completo. Fecha o ciclo do
cliente que a TODO-006 abriu: agendar sozinho não basta se ele não recebe
nada depois — hoje a única prova do agendamento é a tela de sucesso, que
some ao recarregar (flash attribute). Sem confirmação persistente, o
cliente volta a perguntar por WhatsApp, que é exatamente o problema que o
produto existe para resolver.
