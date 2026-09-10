# Feature Metadata

**Feature Name**: redesenho-cadastro
**Feature ID**: feat-20260910-redesenho-cadastro
**Mode**: brownfield (por continuidade — mesma nota das features anteriores)
**Project Type**: production
**Platform**: web
**User Profile**: technical
**Created**: 2026-09-10
**Last Updated**: 2026-09-10
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

## Contexto trazido pelo dono (2026-09-10)

O dono trouxe um segundo protótipo (Claude chat, mesma paleta Tailwind
coral/navy) para a tela de `/cadastro`, e pediu para ela seguir esse
desenho: layout em duas colunas (painel de marca à esquerda, formulário
à direita), com um preview ao vivo do link e do nome do estabelecimento
dentro de um mockup de "link público", e o botão de mostrar/ocultar
senha com o ícone exato que ele especificou (já corrigido nesta mesma
sessão, ver commit `1fd6455`, aplicado a todos os campos de senha do
sistema).

Investigação feita antes de especificar:

- `RegistrationController` (`organization.adapter.in.web`) já expõe
  `GET /cadastro` e `POST /cadastro` — **nenhuma mudança de campo, rota
  ou lógica de negócio**: mesmo `RegistrationRequest` (businessName,
  slug, email, password, whatsapp opcional), mesma validação, mesmo
  fluxo (erro de negócio vira erro no campo, autenticação automática
  pós-cadastro).
- `static/js/slug.js` **já faz** a derivação ao vivo do slug a partir do
  nome (normaliza acento, minúsculas, hífen) e atualiza uma prévia —
  exatamente o que o JS do protótipo reimplementa do zero. A tela nova
  reaproveita `slug.js` como está, sem duplicar a lógica; só acrescenta
  os elementos visuais que también reagem ao mesmo evento `input`
  (nome do painel de marca, mockup de link).
- Diferença deliberada do protótipo: a URL do mockup usa
  `simboraagendar.com.br/b/{slug}` (roteamento real, confirmado e já
  registrado como decisão de projeto — ver memória
  `roteamento-por-caminho-nao-subdominio`), nunca subdomínio.
- Ícone de olho do campo de senha: já é o path exato do protótipo em
  todo o sistema (fix aplicado antes desta feature começar) — esta
  feature só usa o componente já pronto, não precisa mexer nele de
  novo.

**Conclusão**: é uma feature 100% de apresentação — nenhum contexto
delimitado (`organization`, `shared`, `platform`) tem domínio, use case
ou persistência tocados. Só o template `auth/cadastro.html` e,
possivelmente, um pequeno ajuste de JS para popular o painel de marca
(nome do negócio e prévia de link), reaproveitando os elementos que
`slug.js` já atualiza.

---

## Documentos normativos que governam esta feature

| Documento | O que impõe |
|---|---|
| `sdd/PATTERNS.md`, seção Frontend | Bootstrap 5, não Tailwind; "Ícone é SVG inline"; token de cor coral/navy já vigente (rebrand da pagina-institucional) |
| `CLAUDE.md` | Sem API REST — renderização server-side; nenhuma mudança de regra de negócio nesta feature |
| `docs/domain/glossary.md` | Não se aplica — nenhum conceito de domínio novo |

---

## Team

**Owner**: Elton Marques <eltonesm@gmail.com>
**Team Members**: —

---

## Stage History

```yaml
stages:
  functional:
    started: 2026-09-10
    completed: 2026-09-10
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-10T09:11:32Z

  technical:
    started: 2026-09-10
    completed: 2026-09-10
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-10T17:20:15Z

  tasks:
    started: 2026-09-10
    completed: 2026-09-10
    status: approved
    approved_by: Elton Marques
    approved_at: 2026-09-10T17:20:15Z

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
  rationale: "8 tarefas com cadeia de dependencia linear (CSS -> template -> JS -> teste -> docs -> qualidade), feature pequena e puramente de apresentacao - sequencial evita overhead de paralelismo."
```

---

## Metrics

```yaml
metrics:
  timeline: { estimated_days: null, actual_days: null, variance_percent: null }
  effort: { estimated_hours: null, actual_hours: null, variance_percent: null }
  quality: { test_coverage: null, tests_total: 628, tests_passing: 628, linter_errors: 0, type_errors: 0 }
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

Décima quarta feature do projeto pelo ciclo SDD completo. Segunda a
tocar a "porta de entrada" do produto (depois da `pagina-institucional`)
— agora o próprio formulário de cadastro, que a feature anterior
deixou deliberadamente intocado ("continua exatamente como está hoje").
Puramente visual: reaproveita `slug.js` e o ícone de olho já corrigido,
sem nenhuma mudança de campo, rota ou regra de negócio.
