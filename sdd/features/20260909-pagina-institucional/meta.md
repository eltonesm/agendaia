# Feature Metadata

**Feature Name**: pagina-institucional
**Feature ID**: feat-20260909-pagina-institucional
**Mode**: brownfield (por continuidade — mesma nota das features anteriores)
**Project Type**: production
**Platform**: web
**User Profile**: technical
**Created**: 2026-09-09
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

## Contexto trazido pelo dono (2026-09-09)

O dono trouxe um protótipo HTML (feito com Claude chat, paleta Tailwind
coral/navy) e a logo nova da marca "simboraagendar" (ícone coral com
chevron duplo branco, wordmark navy/slate, tagline "Sem enrolação, simbora
marcar."). Pediu para ajustar primeiro a página de cadastro/entrada do
estabelecimento, porque hoje ela não explica preço, proposta de valor nem
como funciona — só pede os dados e cria a conta.

Antes de decidir onde o conteúdo entra, foi feita uma investigação
read-only do código para não inventar nada sobre o negócio:

- `WebConfig.java:24` — hoje `/` redireciona para `/cadastro`, com um
  comentário já existente no código: "quando houver página institucional,
  esta linha muda". O lugar já estava previsto.
- `BillingAccount.TRIAL_DAYS = 30` (e `GRACE_PERIOD_DAYS = 5`) — o
  "R$0 nos primeiros 30 dias" do protótipo bate com o domínio real.
- Não existe hoje nenhum campo de valor/plano no domínio de billing — a
  cobrança é 100% manual (Pix combinado fora do sistema). O dono decidiu
  nesta conversa que o valor mensal pós-trial será **R$49,90**, e a
  landing deve exibir esse valor (não existia antes; é decisão de negócio
  tomada agora, não inferida do código).
- Roteamento público real é por caminho (`/b/{slug}`), não subdomínio — a
  URL fake do protótipo (`salaodamaria.simboraagendar.com.br`) é só
  estética e não deve ser copiada literalmente.
- Não existe hoje nenhuma página institucional/marketing no projeto.
- Paleta atual documentada em `sdd/PATTERNS.md`: `--bs-primary: #4F46E5`
  (indigo), igual em claro/escuro, usada em todo o admin/agenda/cadastro.
  A logo nova usa coral (`#FF6B4A`/`#E85A3A`) e navy (`#1E1B4B`/`#312E81`).

**Decisões já tomadas pelo dono nesta conversa** (via AskUserQuestion):
1. **Rebrand completo**: a cor primária do sistema inteiro muda de indigo
   para a paleta coral/navy da logo nova — não é uma paleta isolada só da
   landing. Afeta `sdd/PATTERNS.md` e `fragments/layout.html` (Bootstrap,
   não Tailwind — o protótipo é só referência visual a traduzir, seguindo
   o padrão já estabelecido nas features anteriores).
2. **Preço pós-trial**: R$49,90/mês, a ser exibido na landing junto com o
   trial de 30 dias grátis.

**Ainda a decidir na spec funcional**: colocação exata do conteúdo — nova
landing pública em `/` (substituindo o redirect atual) vs. incorporar ao
`/cadastro` existente. A investigação técnica não decide isso sozinha;
deve ser levantado como pergunta da entrevista funcional se ainda não
estiver claro pelo protótipo compartilhado.

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `sdd/PATTERNS.md`, seção Frontend | Bootstrap 5, não Tailwind — protótipos externos são traduzidos, nunca adotados como framework. Paleta de cores é seção normativa a ser atualizada por esta feature (rebrand). |
| `CLAUDE.md` | Sem API REST — renderização server-side; `tenantId` nunca vem do cliente |
| `docs/domain/glossary.md` | Se a landing introduzir um conceito novo de domínio (ex.: preço/plano), precisa entrar aqui se normativo |

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
    completed: 2026-09-10
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-10T00:44:16Z

  technical:
    started: 2026-09-10
    completed: 2026-09-10
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-10T00:48:39Z

  tasks:
    started: 2026-09-10
    completed: 2026-09-10
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-10T00:48:39Z

  implementation:
    started: 2026-09-10
    completed: 2026-09-10
    status: completed
```

---

## Execution Strategy

```yaml
execution_strategy:
  type: sequential
  chosen_date: 2026-09-10
  estimated_agent_time: null
  estimated_tokens: null
  actual_agent_time: null
  rationale: "8 tarefas com cadeia de dependencia linear (logo/paleta -> template -> rota -> testes -> qualidade), sem servico de infra novo - sequencial evita overhead de paralelismo numa feature pequena."
```

---

## Metrics

```yaml
metrics:
  timeline: { estimated_days: null, actual_days: null, variance_percent: null }
  effort: { estimated_hours: null, actual_hours: null, variance_percent: null }
  quality: { test_coverage: null, tests_total: 627, tests_passing: 627, linter_errors: 0, type_errors: 0 }
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

Décima terceira feature do projeto pelo ciclo SDD completo. Primeira a
mexer na primeira impressão do produto (hoje `/` só redireciona para um
formulário de cadastro sem contexto nenhum) e primeira a tocar identidade
visual do sistema inteiro (rebrand indigo → coral/navy). Nasce de o dono
ter trazido um protótipo próprio (Claude chat + Tailwind) e a logo nova da
marca "simboraagendar", com o pedido explícito de começar por aqui antes
de mexer em operador, admin ou agenda pública — seguindo o princípio do
dono de fechar cada fatia (back + front + regra de negócio) antes de
avançar para a próxima.
