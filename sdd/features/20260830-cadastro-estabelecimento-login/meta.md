# Feature Metadata

**Feature Name**: cadastro-estabelecimento-login
**Feature ID**: feat-20260830-cadastro-estabelecimento-login
**Mode**: greenfield
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-08-30
**Last Updated**: 2026-08-30
**Current Stage**: implementation

> **Sobre o modo greenfield**: o repositório tem código, mas nenhum código de
> negócio — apenas o andaime da Fase 0 (build, Flyway, Testcontainers, ArchUnit,
> Spring Modulith e a estrutura de pacotes vazia). A heurística do kit marcaria
> brownfield porque `sdd/features/` existe, mas ela está vazia.
>
> `/sdd.reverse-eng` **não se aplica**: não há implementação de onde extrair
> spec, e a documentação existente (12 ADRs, glossário normativo, modelo de
> dados e `PATTERNS.md`) é mais precisa do que engenharia reversa produziria.

---

## Framework Version

```yaml
framework:
  version_created: "desconhecida"   # sdd-kit/framework/VERSION ausente nesta instalação
  version_current: null
  last_compatibility_check: null
  migration_notes:
    - "Instalação parcial: os comandos referenciam 31 scripts, o framework traz 9."
    - "Templates lite/spec.md e PATTERNS.md ausentes; functional-spec, technical-spec e meta presentes."
```

---

## Project Type Configuration

```yaml
project_type:
  type: production
  decision_date: 2026-08-30

  testing:
    unit_tests: full_coverage
    ltp_enabled: false        # E2E interno do ML; o agente sdd-large-test-writer foi removido
    coverage_target: "80%"    # piso do build, não meta — ver PATTERNS.md
```

> O rótulo `production` é sobre **rigor de engenharia**, não sobre estágio do
> produto. O produto é um MVP; o piso de cobertura, o ArchUnit, o Spring Modulith
> e o Postgres real nos testes já estavam decididos antes desta feature.

---

## User Profile Configuration

```yaml
user_profile:
  type: technical
  source: selected
  selected_at: 2026-08-30
```

---

## Spec Language

```yaml
spec_language: pt   # herdado de sdd/PROJECT.md -> language.specs
```

---

## LTP Configuration

```yaml
ltp:
  enabled: false
  decision_date: 2026-08-30
  decision_reason: "Framework de E2E interno do ML, indisponível aqui. O agente sdd-large-test-writer foi removido na instalação do kit."
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

> Esta feature quase certamente terá migration: cria as primeiras tabelas de
> negócio (`business`, `business_slug_history`, `users`). A V1 existente só
> habilita `btree_gist`.

---

## Backlog Workflow

```yaml
from_backlog: TODO-001
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> Estabelece o tenant — sem ele nada mais tem onde morar. Exercita
> `TenantContext`, Spring Security e a primeira migration de verdade. Inclui a
> escolha do slug, com lista de palavras reservadas.

**Prioridade**: High · **Complexidade**: High
**Contextos afetados**: `organization`, `platform`

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `docs/domain/glossary.md` | `Business` é o tenant; identificadores em inglês, UI e URLs em português |
| `sdd/PATTERNS.md` | 43 padrões, incluindo interface entre camadas, sem setter, e frontend na mesma feature |
| `sdd/PROJECT.md` | Clean Architecture, specs em português, branching trunk-based |

**ADRs diretamente aplicáveis**:

- [0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — `organization` é subdomínio de suporte: a entidade JPA **é** o modelo, sem classe espelho nem mapper
- [0003](../../../docs/architecture/adr/0003-identidade-dentro-de-organization.md) — sem contexto IAM; `Business` e `User` na mesma transação de cadastro
- [0004](../../../docs/architecture/adr/0004-multi-tenancy-por-discriminador.md) — `TenantContext` com duas rotas de resolução
- [0008](../../../docs/architecture/adr/0008-rota-publica-com-prefixo.md) — `/b/{slug}`, e a lista de palavras reservadas do slug
- [0009](../../../docs/architecture/adr/0009-uuidv7-como-identificador.md) — identidade gerada na aplicação
- [0011](../../../docs/architecture/adr/0011-ciclo-de-vida-dos-dados.md) — desativação lógica e histórico de slug
- [0012](../../../docs/architecture/adr/0012-bootstrap-sem-build-com-tema-no-publico.md) — Bootstrap 5 por CDN; esta feature é toda admin

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-08-30
    completed: 2026-08-30
    status: approved
    owner: Elton Marques
    approved_by: Elton Marques
    approved_at: 2026-08-30T19:19:19Z
    iterations: 0

  technical:
    started: 2026-08-30
    completed: 2026-08-30
    status: approved
    owner: Elton Marques
    approved_by: Elton Marques
    approved_at: 2026-08-30T19:25:27Z
    mcpqueried: false
    services_count: 0

  tasks:
    started: 2026-08-30
    completed: 2026-08-30
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-08-30T19:30:48Z
    strategy_chosen_by: Elton Marques
    generated_tasks_count: 16
    iterations: 0
    final_tasks_count: 16

  implementation:
    started: 2026-08-30
    completed: 2026-08-30
    status: completed
    execution_strategy: batched
    total_tasks: 17
    completed_tasks: 17
```

> As três revisões da camada 3 (TASK-014, 015, 016) foram feitas **em linha**,
> não pelos Skills `sdd-code-reviewer` e `sdd-performance-expert`: a instrução
> operacional em vigor na sessão proibia delegar a subagente sem pedido
> explícito. O conteúdo dos portões foi cumprido e está evidenciado em
> `4-implementation/artifacts/revisoes.md`; o mecanismo previsto pelo kit, não.
> Registrado aqui para que o `/sdd.finish` não conclua que os Skills rodaram.

---

## Execution Strategy

```yaml
execution_strategy:
  type: batched
  chosen_date: 2026-08-30
  estimated_agent_time: null
  estimated_tokens: null
  actual_agent_time: null
  rationale: "Revisao por lote de nivel de dependencia. Paralela renderia pouco: a corrente de seguranca 005-008 e estritamente sequencial e domina o caminho critico."
```

---

## Metrics

```yaml
metrics:
  timeline:
    estimated_days: null
    actual_days: null
    variance_percent: null

  effort:
    estimated_hours: null
    actual_hours: null
    variance_percent: null

  quality:
    test_coverage: "91.1% de instrucoes, 68.0% de ramos"
    tests_total: 214           # 181 unitarios + 33 de integracao
    tests_passing: 214
    linter_errors: 0
    type_errors: 0

  velocity:
    avg_hours_per_task: null
    estimation_accuracy: null
```

---

## Changes and Deviations

```yaml
changes:
  tasks_added:
    - id: TASK-017
      title: "Sugestao de variacao livre quando o link esta em uso"
      date: 2026-08-30
      origin: "Divergencia encontrada ao escrever o teste do E2E-3"
      reason: >-
        A spec funcional pedia a sugestao em dois pontos — a tabela de
        tratamento de erro e o resultado esperado do E2E-3 — e nem a spec
        tecnica nem as 16 tasks cobriam. Criada durante o build para a
        implementacao nao ficar sem rastro. O caminho previsto pelo kit seria
        /sdd.fix; a task foi acrescentada a mao e a spec tecnica, atualizada
        junto, para que as quatro camadas voltassem a concordar.
  tasks_removed: []
  tasks_modified: []
  spec_changes:
    functional: []
    technical:
      - section: "DD-4.1 — A sugestao de variacao quando o link esta tomado"
        date: 2026-08-30
        type: addition
        reason: >-
          Cobre o que a spec funcional ja exigia e a tecnica omitia. Registra
          onde cada parte mora (mecanica de string no dominio, busca pela
          primeira livre no caso de uso), o limite de nove tentativas e por que
          palavra reservada nao ganha sugestao.
  risks_materialized:
    - risk: "Corrida no cadastro do mesmo slug"
      status: "mitigado como previsto"
      note: >-
        Nao materializou em producao, mas o caminho de traducao da violacao de
        UNIQUE em erro de campo esta implementado e agora tambem emite sugestao.
    - risk: "Defeito de ordem de filtro nao previsto no Risk Assessment"
      status: "materializou"
      note: >-
        O TenantContextFilter declarava @Order(Integer.MIN_VALUE + 100), que o
        colocava ANTES da cadeia do Spring Security — o SecurityContextHolder
        ainda estava vazio e o tenant nunca era populado. /admin/dashboard
        devolvia 500 para qualquer sessao valida, inclusive no redirecionamento
        logo apos o cadastro. Encontrado pelo LoginIT; travado pelo
        CrossTenantIsolationIT, cujo AC-3 foi conferido reintroduzindo o defeito
        de proposito.
```

---

## Validation Overrides

```yaml
overrides:
  functional:
    forced: false
    reason: null
    date: null
  technical:
    forced: false
    reason: null
    date: null
  tasks:
    forced: false
    reason: null
    date: null
  complete:
    forced: false
    reason: null
    date: null
```

---

## Notes

Primeira feature do projeto a passar pelo ciclo SDD. Todo o andaime da Fase 0
entrou como commit direto, por não ter regra de negócio nem critério de aceite
de produto.

Etapas do `/sdd.start` puladas por serem internas do Mercado Livre e não
existirem nesta instalação: verificação de pré-requisitos do `fury` CLI, Zero
Trust, arquivo `.fury`, criação de app via Puma MCP, descoberta de projeto via
TeamsMCP e clone com template. Este repositório já existe, é público no GitHub e
tem seu próprio andaime.
