# Feature Metadata

**Feature Name**: back-office-operador
**Feature ID**: feat-20260904-back-office-operador
**Mode**: brownfield (por continuidade — mesma nota das features anteriores)
**Project Type**: production
**Platform**: backend
**User Profile**: technical
**Created**: 2026-09-04
**Last Updated**: 2026-09-04
**Current Stage**: functional

> **Sobre o modo brownfield aqui**: o projeto já tem `organization.Business`
> (o estabelecimento/tenant) e autenticação de dono via `platform.security`.
> Esta feature introduz um **papel novo, sem precedente no código**: o
> operador da plataforma (você, dono do AgendaIA) — que não é dono de
> nenhum `Business`, vê todos eles, e precisa de uma sessão fora do modelo
> de tenant existente.

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
  migration_files: ["V6__billing_create_billing_account.sql"]
```

> Escopo de dado esperado, a confirmar na spec técnica: trial/status de
> pagamento por `Business` (campos novos em `organization`, ou um agregado
> novo num contexto próprio — pergunta em aberto abaixo) e uma identidade
> própria para o operador (não é `organization.User`, que é sempre dono de
> um tenant específico).

---

## Backlog Workflow

```yaml
from_backlog: TODO-009
workflow_mode: full
auto_generated:
  functional: false
  technical: false
```

**Contexto herdado do backlog**:

> Hoje não existe nenhuma visão de quem opera o AgendaIA (você) sobre os
> estabelecimentos cadastrados — quantos existem, qual o status de
> pagamento, quem está em trial. Isso é uma "decisão nova" de propósito: o
> glossário bane `Plano`, `Assinatura` e `Pagamento` do MVP justamente até
> que exista essa decisão — esta TODO é ela. Painel do operador (fora de
> `/admin/**`, que é do dono do estabelecimento — o operador não é um
> tenant) lista cada `Business` com nome, slug, data de cadastro, "modelo"
> (campo para plano futuro; só um valor fixo no MVP) e status: trial,
> vencido (aguardando pagamento), bloqueado, pago. Trial automático de 30
> dias corridos a partir do cadastro (retroativo ao piloto existente,
> calculado a partir do `created_at` dele). Ao vencer o trial, entra em
> carência de 5 dias corridos: o admin do dono passa a mostrar um aviso
> fixo com QR code/chave Pix, sem bloquear nada ainda. Se ninguém marcar
> pagamento até o fim da carência (dia 35), o sistema bloqueia `/admin/**`
> sozinho, redirecionando para uma tela "conta suspensa" com link de
> WhatsApp de suporte. O operador pode, a qualquer momento, marcar como
> pago (Pix recebido por fora, sem gateway nesta feature) ou estender o
> prazo manualmente. Inclui também um botão de WhatsApp no admin do dono
> do estabelecimento, para dúvidas e sugestões.

**Prioridade**: High · **Complexidade**: High
**Contextos afetados**: contexto novo (nome a decidir — ex. `billing`), `organization` (`Business` ganha dados de trial/status, ou referência a eles), `platform` (segunda forma de autenticação, do operador, fora do modelo de tenant)

---

## Contexto técnico levantado antes da spec

- **O operador não é um tenant.** Todo o modelo de autenticação existente
  (`platform.security`, `TenantContext`) pressupõe uma sessão de dono de
  `Business` — o próprio `RegistrationIT`/`LoginIT` autenticam
  `organization.User`, sempre com um `tenantId` resolvido. Uma sessão de
  operador vendo *todos* os tenants é um conceito novo, sem `tenant_id`
  nenhum. Precisa de uma segunda forma de autenticação, coexistindo com a
  primeira (`/admin/**` continua sendo do dono; o painel do operador é uma
  rota nova, ex. `/backoffice/**`).
- **Trial automático de 30 dias + carência de 5 dias + bloqueio automático.**
  Isso é lógica de negócio nova que precisa rodar independente de qualquer
  requisição HTTP — alguém (ou algo) precisa notar que o dia 30 ou o dia 35
  chegou, mesmo que ninguém acesse o sistema naquele momento. O projeto não
  tem hoje nenhum mecanismo de job agendado (`@Scheduled` ou equivalente).
  Pergunta para a spec técnica: o cálculo de status é feito sob demanda
  (toda vez que `/admin/**` é acessado, comparando `trialEndsAt`/`hoje`) ou
  precisa de um job que roda uma vez por dia? Calcular sob demanda é mais
  simples e evita infraestrutura nova — mas precisa ser avaliado.
- **Bloqueio de `/admin/**` por status de pagamento** é uma regra
  transversal nova, parecida em espírito com o filtro de tenant
  (`TenantContextFilter`) — mas aqui o gatilho é "o negócio está com
  pagamento em dia", não "existe tenant". Como isso se encaixa na cadeia de
  filtros do Spring Security é decisão técnica.
- **`Plano`/`Assinatura`/`Pagamento` estavam banidos do glossário** (seção
  "Termos que não devem aparecer sem decisão nova") justamente até esta
  feature. A spec técnica precisa produzir o amendment do glossário
  liberando os termos com as definições exatas usadas aqui.
- **Perguntas em aberto herdadas do backlog** (a resolver na spec
  funcional/técnica):
  - Nome exato do "modelo"/plano — provavelmente só um valor fixo tipo
    "Padrão" no MVP, sem tela de configuração de planos.
  - O vencimento do trial/carência notifica o operador proativamente (ex.
    e-mail, WhatsApp) ou ele só descobre olhando o próprio painel? Notificar
    exigiria um canal de saída que o projeto não tem hoje (mesma lacuna já
    registrada na IDEA-001).
  - Como autenticar o operador: um `role` novo sem `tenant_id` dentro do
    mesmo mecanismo de sessão, ou um login inteiramente separado (usuário e
    senha do operador vivendo fora de `organization`)? Login separado é
    conceitualmente mais limpo (o operador nunca teve, e nunca deveria ter,
    um `tenant_id`), mas é mais código.
- **Gateway de pagamento explicitamente fora de escopo.** Pix é cobrado por
  fora do sistema; o operador só registra manualmente que recebeu.
  Stripe/Mercado Pago assinatura fica para quando o volume justificar
  (nota já no backlog).

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `docs/domain/glossary.md` | Lista `Plano`, `Assinatura`, `Pagamento` como termos banidos "sem decisão nova" — esta feature é a decisão. Precisa de amendment liberando os termos com definição exata. |
| `docs/architecture/architecture-haiku.md` | Exclui explicitamente "gateway de pagamento" e "planos Free/Pro/Premium" do escopo original — mesma nota, mesma decisão nova. |
| `sdd/PROJECT.md` | Branching trunk-based: `feature/back-office-operador`, origem e destino `main` |
| `CLAUDE.md` | `tenantId` nunca vem do cliente — a sessão do operador precisa de um mecanismo igualmente rigoroso, mesmo não tendo tenant nenhum |

**ADRs diretamente aplicáveis**:

- [0004](../../../docs/architecture/adr/0004-multi-tenancy-por-discriminador.md) — multi-tenancy por discriminador; a sessão do operador é a primeira exceção deliberada a esse modelo
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
    started: 2026-09-04
    completed: 2026-09-04
    status: approved
    owner: Elton Marques
    approved_by: Elton Marques
    approved_at: 2026-09-04T02:19:20Z
    iterations: 0

  technical:
    started: 2026-09-04
    completed: 2026-09-04
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-04T02:26:20Z
    mcpqueried: false
    services_count: 0

  tasks:
    started: 2026-09-04
    completed: 2026-09-04
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-04T02:30:04Z
    strategy_chosen_by: Elton Marques
    generated_tasks_count: 15
    iterations: 0
    final_tasks_count: 15

  implementation:
    started: null
    completed: null
    status: pending
    execution_strategy: batched
    total_tasks: 15
    completed_tasks: 0
```

---

## Execution Strategy

```yaml
execution_strategy:
  type: batched
  chosen_date: 2026-09-04
  estimated_agent_time: null
  estimated_tokens: null
  actual_agent_time: null
  rationale: "Batched por nivel de dependencia. Quatro tarefas independentes no nivel 0 (amendment de docs, dominio billing, organization.api.BusinessDirectory, login isolado do operador), depois cadeia sequencial: service -> filtro/advice -> painel/tela -> layout -> testes -> qualidade."
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

Sexta feature do projeto pelo ciclo SDD completo, e a primeira a introduzir
um papel de usuário que não é dono de estabelecimento nem cliente público —
o operador da plataforma. Também a primeira a liberar deliberadamente
termos que o glossário mantinha banidos (`Plano`, `Assinatura`,
`Pagamento`), exigindo amendment formal. Várias perguntas em aberto
levantadas antes da spec funcional — ver "Contexto técnico" acima.
