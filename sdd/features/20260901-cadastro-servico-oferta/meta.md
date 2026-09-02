# Feature Metadata

**Feature Name**: cadastro-servico-oferta
**Feature ID**: feat-20260901-cadastro-servico-oferta
**Mode**: brownfield (por continuidade — mesma nota da TODO-002)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-09-01
**Last Updated**: 2026-09-01
**Current Stage**: implementation

> **Sobre o modo brownfield aqui**: `organization` já tem `Business`, `User` e
> `Professional` implementados e testados. Esta é a primeira feature a criar
> código em `catalog` — hoje só um `package-info.java` com o
> `@ApplicationModule` já anotado, sem classe nenhuma dentro.
> `/sdd.reverse-eng` não se aplica: não há implementação de `catalog` para
> extrair spec de — o pacote está vazio.

---

## Framework Version

```yaml
framework:
  version_created: "desconhecida"
  version_current: null
  last_compatibility_check: null
  migration_notes:
    - "Mesma instalação parcial das TODO-001/002: 8 dos 31 scripts referenciados existem de fato (manage-backlog.sh removido em 2026-08-31 — ver sdd-kit/PORTABILITY.md)."
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

> Duas tabelas esperadas: `service` (conceito, sem preço/duração) e
> `service_offering` (o que se agenda, com `professional_id` como UUID
> solto — outro contexto, sem FK). `service_offering` tem unicidade composta
> em `(tenant_id, service_id, professional_id)`, per data-model.md.

---

## Backlog Workflow

```yaml
from_backlog: TODO-003
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> Introduz `ServiceOffering` — serviço por profissional, com duração, preço e
> intervalo próprios. Primeira referência cruzando contexto por UUID solto,
> sem chave estrangeira.

**Prioridade**: High · **Complexidade**: Medium
**Contextos afetados**: `catalog` (novo código), `organization` (primeiro consumidor de `api`)

---

## Contexto técnico levantado antes da spec

- **`catalog` está vazio.** Só existe `package-info.java`, já anotado com
  `@ApplicationModule(displayName = "Catalog")` e já documentando a decisão
  de `professionalId` como UUID solto — a intenção arquitetural foi
  registrada na Fase 0, antes de qualquer código.
- **`Money` ainda não existe.** `CLAUDE.md` promete `Money` em `shared` desde
  o início, mas nenhuma feature precisou dele até agora — `price` de
  `ServiceOffering` é o primeiro uso real. Nasce nesta feature.
- **`organization/api/` precisa nascer agora, de verdade.** As duas features
  anteriores adiaram a criação desse pacote por falta de consumidor. Esta é
  a primeira vez que outro contexto (`catalog`) precisa enxergar
  `organization` de fora — no mínimo para saber quais profissionais existem
  no tenant, ao montar a tela de cadastro de oferta. **Pergunta em aberto
  para a spec técnica**: o que exatamente `organization.api` expõe nesta
  primeira versão — só uma projeção de leitura (`ProfessionalRef` ou
  similar, id + nome), ou mais que isso? Regra do `PATTERNS.md` ("API entre
  contextos é grossa, nunca conversadeira") pede pensar em lote desde já,
  mesmo com um único consumidor.
- **Risco de isolamento entre tenants ainda não coberto por nenhum teste.**
  `ServiceOffering.professionalId` é UUID solto — nada impede, a nível de
  banco, que um POST forjado referencie o `professionalId` de **outro**
  tenant. A UI só ofereceria profissionais do tenant certo no dropdown, mas
  isso não é garantia (mesmo raciocínio de "validação em memória é
  feedback, não garantia" do `ADR 0005`). **Pergunta em aberto para a spec
  funcional/técnica**: o caso de uso de cadastro de oferta precisa validar,
  via `organization.api`, que o `professionalId` recebido pertence ao tenant
  da sessão? Se sim, isso também vira um caso novo no
  `CrossTenantIsolationIT`, que cresce a cada feature.
- **Restrição composta nova**: `service_offering` é único por
  `(tenant_id, service_id, professional_id)` — a primeira `UNIQUE` de mais
  de duas colunas no projeto. `service.name` provavelmente precisa de
  unicidade por tenant também (dois "Corte de Cabelo" no mesmo
  estabelecimento não faz sentido), mas isso não está no glossário
  explicitamente — **pergunta em aberto para a spec funcional**.

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `docs/domain/glossary.md` | `Service` sem preço/duração; `ServiceOffering` com duração, preço, intervalo próprios; os "três significados de serviço" (seção 3) |
| `docs/domain/data-model.md` | Schema de `ServiceOffering`, unicidade composta |
| `sdd/PATTERNS.md` | "Agregação por UUID" (Database Patterns — sem FK entre contextos); "API entre contextos é grossa" |
| `sdd/PROJECT.md` | Branching trunk-based: `feature/cadastro-servico-oferta`, origem e destino `main` |

**ADRs diretamente aplicáveis**:

- [0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — `catalog` também é subdomínio de suporte, mesmo regime CRUD
- [0004](../../../docs/architecture/adr/0004-multi-tenancy-por-discriminador.md) — `tenant_id` de `Service`/`ServiceOffering` vem da sessão
- [0009](../../../docs/architecture/adr/0009-uuidv7-como-identificador.md) — identidade gerada na aplicação
- [0010](../../../docs/architecture/adr/0010-enforcement-spring-modulith-archunit.md) — primeiro `@NamedInterface("api")` real do projeto (`organization.api`); Spring Modulith passa a ter uma dependência **entre** contextos para verificar de verdade, não só dentro de um

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-09-01
    completed: 2026-09-01
    status: approved
    owner: Elton Marques
    approved_by: Elton Marques
    approved_at: 2026-09-01T23:03:38Z
    iterations: 0

  technical:
    started: 2026-09-01
    completed: 2026-09-01
    status: approved
    owner: Elton Marques
    approved_by: Elton Marques
    approved_at: 2026-09-01T23:09:15Z
    mcpqueried: false
    services_count: 0

  tasks:
    started: 2026-09-01
    completed: 2026-09-01
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-01T23:09:15Z
    strategy_chosen_by: Elton Marques
    generated_tasks_count: 15
    iterations: 0
    final_tasks_count: 15

  implementation:
    started: 2026-09-01
    completed: 2026-09-01
    status: completed
    execution_strategy: batched
    total_tasks: 15
    completed_tasks: 15
```

---

## Execution Strategy

```yaml
execution_strategy:
  type: batched
  chosen_date: 2026-09-01
  estimated_agent_time: null
  estimated_tokens: null
  actual_agent_time: null
  rationale: "Batched por nivel de dependencia. Tres pontos reais de paralelismo (TASK-001/002/003; TASK-004/005; TASK-008/009), mas a cadeia de fundo -- migration -> entidades -> casos de uso -> telas -> testes -> qualidade -- e sequencial."
```

---

## Metrics

```yaml
metrics:
  timeline: { estimated_days: null, actual_days: 1, variance_percent: null }
  effort: { estimated_hours: null, actual_hours: null, variance_percent: null }
  quality: { test_coverage: "90.0% de instrucoes, 72.2% de ramos", tests_total: 282, tests_passing: 282, linter_errors: 0, type_errors: 0 }
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

Terceira feature do projeto pelo ciclo SDD completo, e a primeira a tocar
`catalog`. As quatro perguntas em aberto foram delegadas ao critério do
assistente pelo usuário em 2026-09-01, antes da spec funcional:

1. **`organization.api` expõe uma operação só: `ProfessionalDirectory.listActive()`**,
   sem parâmetro — tenant lido de `TenantContext.require()` por dentro, mesma
   extensão do DD-1 da TODO-002 (nenhum caso de uso aceita tenant como
   argumento), agora atravessando a fronteira entre contextos. Devolve
   `List<ProfessionalRef>` (`id`, `name` — projeção, não a entidade). Uma
   chamada só serve os dois usos que existem hoje: popular o dropdown de
   profissional na tela de cadastro de oferta, e validar que o
   `professionalId` submetido pertence ao tenant (pergunta 2).
2. **Sim, valida.** O handler de cadastro de oferta chama
   `ProfessionalDirectory.listActive()` e confere que o `professionalId`
   recebido está na lista antes de gravar — reaproveita a mesma chamada do
   item 1, sem round-trip extra. Sem essa validação, um POST forjado
   referenciaria o profissional de outro tenant sem que nada no banco
   percebesse (não há FK entre contextos). Consequência: `CrossTenantIsolationIT`
   ganha um caso novo nesta feature.
3. **Sim, `service.name` é único por tenant.** Ao contrário de
   `Professional.name` (rótulo, duplicata aceitável — TODO-002), `Service` é
   entrada de catálogo: dois "Corte de Cabelo" no mesmo estabelecimento
   confundiriam o cliente na tela pública (TODO-006). `UNIQUE(tenant_id, name)`.
4. **`Money` guarda centavos como inteiro** (nunca `double`/`float` — erro de
   arredondamento não é aceitável em preço), com fábrica a partir de
   `BigDecimal` (o que chega do formulário) e um método de formatação para
   exibição ("R$ 30,00"). **Sem soma, subtração ou comparação** nesta
   feature — nenhum critério de aceite precisa disso hoje (agendamento de
   serviço combinado é a IDEA-005, ainda não priorizada). Adicionar essas
   operações agora seria abstração especulativa.
