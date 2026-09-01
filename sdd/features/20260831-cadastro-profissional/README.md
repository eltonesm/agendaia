# Cadastro de profissional

**Backlog**: TODO-002 · **Concluída**: 2026-08-31 · **Contexto**: `organization`

Segundo agregado de `organization`. Sem `Professional` não há sobre o que
`ServiceOffering` (TODO-003) ou `WorkSchedule` (TODO-004) se apoiarem — nem
sobre o que a exclusion constraint de agendamento discriminaria.

## O que foi entregue

| Rota | Quem acessa | O que faz |
|---|---|---|
| `GET/POST /admin/profissionais` | dono autenticado | cadastra e lista os profissionais do estabelecimento, numa tela só |

Uma tabela nova: `professional`, com `tenant_id` referenciando `business`. Ao
contrário de `business.slug` e `app_user.email`, **sem** restrição `UNIQUE`
em `name` — nome é rótulo de exibição, não identificador.

## Decisões tomadas antes da spec

Duas perguntas ficaram em aberto ao final do `/sdd.start` e foram respondidas
pelo usuário antes de escrever a spec funcional:

1. **O cadastro de profissional é um passo à parte**, não automático durante
   o cadastro do estabelecimento (TODO-001).
2. **`organization/api/` continua adiado** até a TODO-003 — esta feature não
   tem consumidor externo para justificar criar o pacote agora.

## Decisões técnicas

- **DD-1 — nenhum caso de uso aceita tenant como parâmetro.**
  `RegisterProfessionalCommand` carrega só o nome; `ListProfessionalsUseCase.list()`
  não recebe argumento nenhum. Os dois handlers leem `TenantContext.require()`
  por dentro. Não é só disciplina — é o compilador impedindo que um chamador
  futuro passe um tenant errado, porque a assinatura não aceita nenhum.
- **DD-2 — uma tela só.** `GET` mostra lista + formulário; `POST` válido
  redireciona (PRG) para a mesma rota; `POST` inválido devolve a mesma tela
  com a lista recarregada — sem isso o dono perderia de vista quem já tinha
  cadastrado ao errar o próximo nome.
- **DD-3 — sem `UNIQUE` em `name`.** Não há corrida a resolver, porque não há
  restrição que uma corrida possa violar.

## Números

| | |
|---|---|
| Tasks | 11 |
| Testes | 243 — 203 unitários, 40 de integração |
| Cobertura | 90,6% de instruções |
| Commits | 9 |

## Um bug de teste que a nova FK revelou

`professional.tenant_id REFERENCES business(id)` quebrou a limpeza de três
testes de integração da TODO-001 (`RegistrationIT`, `SlugUnavailableIT`,
`LoginIT`), que compartilham o mesmo container Testcontainers entre classes:
apagavam `business` sem apagar `professional` primeiro. Toda vez que uma
tabela nova ganhar FK para uma existente, checar a ordem de limpeza das ITs
que tocam a tabela referenciada — antes de rodar `verify`, não depois de ver
o erro.

## Onde está o resto

- [`1-functional/spec.md`](1-functional/spec.md) — 3 user stories, 5 business rules, 3 cenários E2E
- [`2-technical/spec.md`](2-technical/spec.md) — as 3 design decisions, com alternativas
- [`3-tasks/tasks.json`](3-tasks/tasks.json) — as 11 tasks
- [`4-implementation/progress.md`](4-implementation/progress.md) — aprendizados e commits
- [`meta.md`](meta.md) — o estado completo da feature
