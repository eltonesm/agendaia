# Implementation Summary — agenda-profissional (TODO-008)

## Timeline

- Início: 2026-09-06
- Fim: 2026-09-07 (funcional → técnica → tasks → build → finish em cerca de 24h)

## Tasks

- Total: 14 (`stats.done: 14` em `tasks.json`)
- Estratégia de execução: Batched — nível 0 com 5 tarefas independentes
  (`Appointment.cancelByOwner`, `ServiceOfferingDirectory.listActive`,
  `CustomerDirectory.findByIds`, `AppointmentRepository` nova consulta,
  `AppointmentFactory`), depois cadeia sequencial até os 3 quality gates.
- 0 tasks adicionadas ou removidas fora do plano aprovado.

## Commits (11, `main..feature/agenda-profissional`)

```
476eb95 docs(sdd): inicia TODO-008 (agenda-profissional)
0c6e962 docs(sdd): aprova spec funcional da TODO-008
496292a docs(sdd): aprova spec tecnica da TODO-008
5760433 feat(scheduling,catalog,customer): base da agenda do profissional (TASK-001 a 005)
0d23fbc feat(scheduling): ProfessionalAgendaHandler e portas da agenda (TASK-006)
3eb8868 feat(scheduling): AgendaController e telas do painel (TASK-007/008)
58d1b35 test(scheduling): dominio e aplicacao da agenda do profissional (TASK-009)
8d2798c test(scheduling): AgendaControllerTest (TASK-010)
4ad6958 test(scheduling): AgendaProfissionalIT com E2E-1 a E2E-7 (TASK-011)
dc39f9b chore(scheduling): fecha camada 3 da TODO-008 - code review, performance e security
faa13d5 docs(scheduling): registra novos casos de uso da agenda no glossario normativo
```

## Quality

- Testes: 451 no projeto inteiro (36 novos nesta feature: `AppointmentTest`
  +2, `ProfessionalAgendaHandlerTest` 8, `AgendaControllerTest` 13,
  `AgendaProfissionalIT` 7, mais os ajustes de `ManageAppointmentHandlerTest`
  para o novo construtor de `CustomerRef`), 0 falhas, 0 erros
  (`./mvnw clean verify`).
- Cobertura de instrução: 89% no projeto inteiro (piso: 80%).
- Linter/type errors: 0.
- Layer 3 (code review, performance, security): todos `APPROVED`. Zero
  achado crítico ou major em qualquer um dos três; performance registrou
  1 recomendação não bloqueante (ver `verdicts/performance.json` e
  DEBT-018).

## Gotcha real: a própria implementação criou uma rota que não servia para nada

Ao desenhar `AgendaController`, projetei um redirecionamento intermediário
(`GET /admin/agenda/agendamentos/{id}/confirmado`) para depois de criar
ou reagendar um agendamento — a ideia era resolver `professionalId` a
partir do agendamento recém-criado antes de mandar o dono de volta para
a agenda certa. Só que o handler dessa rota **também** não tinha
`professionalId` disponível (só o `AppointmentDetails`, que não carrega
esse dado) — então a rota extra não comprava nada sobre redirecionar
direto para `/admin/agenda?date=...`, só adicionava um salto de rede e
uma consulta a mais (`AppointmentDetailsUseCase.handle`) sem benefício.

Percebido durante a própria revisão de código (antes de rodar os quality
gates, não depois de um achado formal): removida a rota, simplificados
`criar()` e `reagendarConfirmar()` para redirecionar direto. Os testes
que dependiam do formato antigo da URL (`idDoRedirect`, em
`AgendaProfissionalIT`) foram reescritos para resolver o id do
agendamento consultando o agregado de verdade por horário exato — mais
robusto, porque não depende da forma da URL de redirecionamento.

**Lição**: ao desenhar um redirecionamento "de volta para onde fazia
sentido", vale perguntar explicitamente se o dado necessário para essa
decisão *existe* no ponto em que o redirecionamento acontece — caso
contrário a indireção não resolve o problema que pretendia resolver, só
o esconde atrás de mais uma rota.

## Achado não bloqueante de performance (DEBT-018)

`findByProfessionalAndDay` (nova consulta, usada pela tela `/admin/agenda`)
busca agendamentos de qualquer status, inclusive `CANCELLED` — por isso
não pode usar o único índice que cobre `(tenant_id, professional_id, ...)`
hoje, o GiST parcial de `appointment_no_overlap` (que só indexa
`SCHEDULED`/`CONFIRMED`). Tende a sequential scan. Aceitável no volume
do piloto; registrado como débito técnico para quando o histórico de
agendamentos por estabelecimento justificar um índice novo.
