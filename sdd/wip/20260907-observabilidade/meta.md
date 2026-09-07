# Feature Metadata

**Feature Name**: observabilidade
**Feature ID**: feat-20260907-observabilidade
**Mode**: brownfield (por continuidade — mesma nota das features anteriores)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-09-07
**Last Updated**: 2026-09-07
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
  detected: false   # a confirmar na spec tecnica; nada aqui parece exigir schema novo
  service_name: null
  service_type: null
  branch_name: null
  branch_status: pending
  migration_files: []
```

---

## Backlog Workflow

```yaml
from_backlog: TODO-108
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> Log em JSON com `tenantId` e `requestId` no MDC, saindo em toda linha da
> requisição. Actuator com `/health` e `/prometheus`, ambos protegidos.
> Métricas de negócio junto com as técnicas: agendamentos criados,
> cancelados e falhas por conflito de horário. Proibido logar telefone e
> nome de cliente (LGPD).

**Prioridade**: Medium · **Complexidade**: Medium
**Contextos afetados**: `platform`, `application.yaml`

---

## Contexto técnico levantado antes da spec

- **`tenantId` já vai para o MDC.** `TenantContextFilter` (ADR 0004) já
  faz `MDC.put(MDC_TENANT, tenantId)` e `MDC.remove` no `finally` — a
  parte que falta é **usar** isso num formato estruturado (hoje o log é
  texto plano, formato padrão do Spring Boot) e somar um `requestId`,
  que ainda não existe em lugar nenhum do projeto.
- **`/actuator/prometheus` está configurado, mas não vai funcionar.**
  `application.yaml` já expõe `health,info,prometheus` em
  `management.endpoints.web.exposure.include`, mas o `pom.xml` só tem
  `spring-boot-starter-actuator` — falta `micrometer-registry-prometheus`
  no classpath para o endpoint existir de verdade.
- **Tensão a levantar na entrevista**: o backlog diz "`/health` e
  `/prometheus`, ambos protegidos", mas o `SecurityConfig` atual
  **libera `/actuator/health` de propósito** (comentário no código:
  "Health é público para sonda de contêiner") — decisão tomada depois
  que este item entrou no backlog (TODO-101/102, andaime). Proteger
  `/health` de verdade quebraria a sonda de saúde do container em
  produção (TODO-106, ainda não existe). Precisa decidir explicitamente
  com o usuário se `/health` continua público (prática comum: sondas de
  container não autenticam) ou se `/prometheus` é o único que precisa
  ficar atrás de autenticação.
- **Spring Boot 4.1.1 tem *Structured Logging* nativo** (desde a série
  3.4): `logging.structured.format.console: ecs` (ou `logstash`, `gelf`)
  liga log em JSON sem dependência nova, incluindo o conteúdo do MDC
  automaticamente. Provável candidato a decisão técnica, evitando
  `logstash-logback-encoder` como dependência externa.
- **Nenhuma métrica de negócio existe hoje** — nem contador de
  agendamento criado/cancelado, nem falha por conflito de horário
  (`SlotUnavailableException`, ADR 0005). Precisam ser instrumentadas
  nos handlers de `scheduling.application`
  (`BookAppointmentHandler`, `ManageAppointmentHandler`,
  `ProfessionalAgendaHandler`) via `MeterRegistry` (Micrometer, já
  trazido pelo `spring-boot-starter-actuator`).
- **LGPD**: nenhum log atual imprime telefone ou nome de cliente —
  `Customer.toString()` já foi escrito deliberadamente sem esses campos
  (comentário no código: "dado pessoal não deve acabar em log"). A
  regra do backlog já é respeitada pelo desenho existente; a spec
  técnica só precisa confirmar que a instrumentação nova não introduz
  uma regressão (ex.: logar o corpo inteiro de uma exceção de validação
  que inclua o telefone).

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `docs/architecture/adr/0004-duas-rotas-de-resolucao-de-tenant.md` | `TenantContext`/MDC já estabelecidos — esta feature estende, não recria |
| `docs/architecture/adr/0011-ciclo-de-vida-dos-dados.md` | Nada é apagado — coerente com métrica de negócio ser contador cumulativo, não substituição de estado |
| `CLAUDE.md` | Sem API REST — `/actuator/**` é exceção padrão do Spring Boot, não um endpoint de negócio |

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-09-07
    completed: 2026-09-07
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-07T17:09:39Z

  technical:
    started: 2026-09-07
    completed: 2026-09-07
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-07T17:14:37Z

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

Décima feature do projeto pelo ciclo SDD completo. Diferente das
anteriores, não é uma fatia vertical de produto — é observabilidade
transversal (`platform`), tocando todos os contextos que já existem sem
adicionar nenhum agregado novo. O critério de "pronto" aqui é diferente:
não é uma tela nova, é "dá para saber o que está acontecendo em
produção sem entrar no banco a mão".
