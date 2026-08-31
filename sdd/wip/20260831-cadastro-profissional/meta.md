# Feature Metadata

**Feature Name**: cadastro-profissional
**Feature ID**: feat-20260831-cadastro-profissional
**Mode**: brownfield (por continuidade — ver nota abaixo)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-08-31
**Last Updated**: 2026-08-31
**Current Stage**: implementation

> **Sobre o modo brownfield aqui**: não é o brownfield clássico do kit — não
> há código legado sem spec para fazer engenharia reversa. É brownfield porque
> `organization` já tem `Business` e `User` implementados e testados pela
> TODO-001, e esta feature constrói em cima disso. `/sdd.reverse-eng` não se
> aplica: o que ele produziria já existe, com mais precisão, em
> `sdd/features/20260830-cadastro-estabelecimento-login/` e nos ADRs.

---

## Framework Version

```yaml
framework:
  version_created: "desconhecida"
  version_current: null
  last_compatibility_check: null
  migration_notes:
    - "Mesma instalação parcial da TODO-001: 9 dos 31 scripts referenciados existem de fato."
```

---

## Project Type Configuration

```yaml
project_type:
  type: production
  decision_date: 2026-08-30   # herdado da TODO-001, não redecidido aqui
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

> Nova tabela esperada: `professional`. Convenção da TODO-001 se repete —
> `tenant_id` é FK para `business.id`, nunca coluna solta sem restrição.

---

## Backlog Workflow

```yaml
from_backlog: TODO-002
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> A menor fatia possível já com tenant. Valida o padrão inteiro de ponta a
> ponta e é candidata a virar o exemplo pendente do PATTERNS.md. Todo
> estabelecimento tem ao menos um profissional, mesmo sendo uma pessoa só.

> **Nota**: a referência original à DEBT-004 (exemplo pendente do
> `PATTERNS.md`) está desatualizada — a DEBT-004 já foi resolvida no
> `/sdd.finish` da TODO-001, usando o fluxo de cadastro como exemplo. Corrigido
> no `backlog.md` ao marcar esta task como `in-progress`.

**Prioridade**: High · **Complexidade**: Medium
**Contextos afetados**: `organization`

---

## Contexto técnico levantado antes da spec

Levantamento rápido do que já existe em `organization`, para a spec funcional
não reinventar o que a TODO-001 já decidiu:

- `Business` e `User` já existem, com UUIDv7 gerado na aplicação, construtor
  privado, criação por fábrica estática. `Professional` deveria seguir o
  mesmo padrão (TASK-004 da TODO-001 é o modelo).
- **`organization/api/` ainda não existe.** A spec técnica arquivada da
  TODO-001 registra explicitamente: *"organization/api/ não é criado nesta
  feature... nasce na TODO-002, junto com `@NamedInterface("api")`"*
  ([spec arquivada](../../features/20260830-cadastro-estabelecimento-login/2-technical/spec.md#L571)).
  Esta feature é a primeira vez que outro contexto poderia precisar enxergar
  `organization` de fora — mas até onde este levantamento vai, nada em
  `catalog` ou `scheduling` existe ainda para consumir essa API. Pergunta em
  aberto para a spec técnica: **a API nasce aqui mesmo sem consumidor, ou
  continua adiada até o primeiro consumidor real (TODO-003)?**
- **Invariante documentada, ainda não implementada**: o glossário normativo
  afirma que *"todo estabelecimento tem ao menos um `Professional`... o dono
  se cadastra como profissional"* ([glossary.md:145](../../../docs/domain/glossary.md#L145)).
  O cadastro da TODO-001 cria `Business` + `User`, mas **não** cria
  `Professional`. Pergunta em aberto para a spec funcional: o dono vira
  profissional automaticamente no cadastro de estabelecimento (retroativo para
  quem já se cadastrou?), ou esta feature introduz um passo explícito de
  "cadastre-se como profissional" depois do login?
- `Professional` pode ou não ter um `User` associado (glossário) — sugere que
  cadastrar um segundo profissional (não-dono) é caso válido desde já, mesmo
  que login para profissionais não-dono fique fora de escopo.

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `docs/domain/glossary.md` | `Professional` é raiz de agregado; invariante do profissional único obrigatório |
| `docs/domain/data-model.md` | `Professional` — raiz de agregado, seção própria |
| `sdd/PATTERNS.md` | Exemplo de ponta a ponta (seção nova, da TODO-001) — mesmo padrão de camadas |
| `sdd/PROJECT.md` | Branching trunk-based: `feature/cadastro-profissional`, origem e destino `main` |

**ADRs diretamente aplicáveis**:

- [0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — `organization` continua em regime CRUD, entidade JPA é o modelo
- [0003](../../../docs/architecture/adr/0003-identidade-dentro-de-organization.md) — sem contexto IAM; `Professional` mora em `organization`
- [0004](../../../docs/architecture/adr/0004-multi-tenancy-por-discriminador.md) — `tenant_id` de `Professional` vem da sessão, nunca do formulário
- [0009](../../../docs/architecture/adr/0009-uuidv7-como-identificador.md) — identidade gerada na aplicação
- [0010](../../../docs/architecture/adr/0010-enforcement-spring-modulith-archunit.md) — se `organization/api` nascer aqui, é o primeiro `@NamedInterface` do projeto

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-08-31
    completed: 2026-08-31
    status: approved
    owner: Elton Marques
    approved_by: Elton Marques
    approved_at: 2026-08-31T23:30:01Z
    iterations: 0

  technical:
    started: 2026-08-31
    completed: 2026-08-31
    status: approved
    owner: Elton Marques
    approved_by: Elton Marques
    approved_at: 2026-08-31T23:38:36Z
    mcpqueried: false
    services_count: 0

  tasks:
    started: 2026-08-31
    completed: 2026-08-31
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-08-31T23:38:36Z
    strategy_chosen_by: Elton Marques
    generated_tasks_count: 11
    iterations: 0
    final_tasks_count: 11

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
  type: batched
  chosen_date: 2026-08-31
  estimated_agent_time: null
  estimated_tokens: null
  actual_agent_time: null
  rationale: "Revisao por lote de nivel de dependencia. A cadeia e praticamente sequencial (migration -> entidade -> casos de uso -> tela -> testes -> qualidade), com um unico ponto de paralelismo real: TASK-003 e TASK-004 nao dependem uma da outra."
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

Segunda feature do projeto pelo ciclo SDD completo.

Antes de `/sdd.start`, a branch `feature/cadastro-estabelecimento-login` foi
mesclada em `main` via fast-forward (18 commits, nunca tinha sido mesclada) —
corrigindo uma violação da própria regra de "branch curta" do `PROJECT.md`.
`feature/cadastro-profissional` nasce corretamente a partir de `main`
atualizada.

**As duas perguntas em aberto foram respondidas pelo usuário em 2026-08-31,
antes da spec funcional:**

1. **O dono vira `Professional` num passo à parte**, não automaticamente no
   cadastro de estabelecimento. Consequência direta para a spec funcional:
   existe uma janela entre "cadastrei o estabelecimento" e "tenho um
   profissional" — o painel da TODO-001 precisa continuar orientando esse
   próximo passo (ele já diz "cadastrar seus profissionais e serviços"), e
   esta feature é quem cumpre essa promessa. Precisa decidir na spec: o dono é
   *obrigado* a se cadastrar como profissional, ou pode cadastrar só outra
   pessoa? O glossário sugere que o estabelecimento sem nenhum profissional é
   estado inválido de longo prazo, mas não que o *dono especificamente* seja
   o profissional.
2. **`organization/api/` continua adiado até a TODO-003** — não nasce nesta
   feature. Consequência: nenhuma classe aqui pode anticipar
   `@NamedInterface("api")` nem criar o pacote vazio; `Professional` e seu
   repositório ficam em `organization.domain`, mesmo padrão da TODO-001.
