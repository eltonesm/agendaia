# Implementation Summary — confirmacao-e-cancelamento (TODO-007)

## Timeline

- Início: 2026-09-05
- Fim: 2026-09-06 (funcional → técnica → tasks → build → finish em cerca de 24h)

## Tasks

- Total: 15 (`stats.done: 15` em `tasks.json`)
- Estratégia de execução: Batched — nível 0 com 4 tarefas independentes
  (`Appointment.confirm/cancel`, WhatsApp de `organization`, `find(UUID)`
  dos directories, `AppointmentRepository`), depois cadeia sequencial até
  os 3 quality gates.
- 0 tasks adicionadas ou removidas fora do plano aprovado.

## Commits (14, `main..feature/confirmacao-e-cancelamento`)

```
c834e86 docs(sdd): inicia TODO-007 (confirmacao-e-cancelamento)
fdb01e0 docs(sdd): aprova spec funcional da TODO-007
4618341 fix(sdd): remove premissa de token assinado da spec funcional da TODO-007
37f1f87 docs(sdd): aprova spec tecnica da TODO-007
29f472d docs(sdd): aprova as 15 tarefas da TODO-007 (estrategia batched)
80c86d4 feat(scheduling,organization,customer): base da confirmacao e cancelamento (TASK-001/002/004/005)
0bae703 feat(organization,scheduling): cadastro com WhatsApp e ManageAppointmentHandler (TASK-003/006)
f4cd0c7 feat(scheduling): AppointmentController e tela do agendamento (TASK-007/008)
285ded7 test(scheduling): dominio e aplicacao da confirmacao e cancelamento (TASK-009)
f9bded3 test(organization): cadastro com WhatsApp opcional (TASK-010)
477f069 fix(scheduling): AppointmentController devolve 404 para tenant errado (TASK-011)
af42c7c test(scheduling): ConfirmacaoECancelamentoIT com E2E-1 a E2E-7 (TASK-012)
86c5d79 chore(scheduling): fecha camada 3 da TODO-007 - code review, performance e security
aca27e1 docs(scheduling): registra ConfirmAppointmentUseCase no glossario normativo
```

## Quality

- Testes: 490 no projeto inteiro (61 novos nesta feature: `AppointmentTest`
  +6, `ManageAppointmentHandlerTest` 8, `RegisterBusinessHandlerTest` +2,
  `RegistrationControllerTest` +2, `AppointmentControllerTest` 10,
  `ConfirmacaoECancelamentoIT` 7), 0 falhas, 0 erros
  (`./mvnw clean verify`).
- Cobertura de instrução: 90% no projeto inteiro (piso: 80%).
- Linter/type errors: 0.
- Layer 3 (code review, performance, security): todos `APPROVED`, zero
  achado crítico, major ou menor em qualquer um dos três (ver
  `verdicts/`).

## Gotcha real desta vez: minha própria spec funcional presumiu a técnica antes da hora

BR-1 da primeira versão da spec funcional dizia "token assinado, não um
identificador cru" — uma decisão de mecanismo que eu escrevi durante a
entrevista, sem que o usuário tivesse pedido especificamente. Na spec
técnica, ao desenhar o DD-1, percebi que o próprio ADR 0009 já trata
UUIDv7 como identificador opaco seguro em URL, e que toda rota pública
do projeto (`serviceOfferingId`, `professionalId` na TODO-006) já usa
exatamente essa defesa — id cru revalidado contra o tenant, sem
assinatura nenhuma. Levei a tensão ao usuário antes de decidir por conta
própria; ele escolheu a opção simples (id do `Appointment`, sem token
novo), e eu voltei à spec funcional já aprovada para corrigir a
linguagem de BR-1 e de mais seis trechos que repetiam a mesma
presunção — antes de escrever a spec técnica, não depois.

**Lição**: numa entrevista de spec funcional, é fácil por engano já
escrever a solução técnica na frase da regra de negócio ("token
assinado" é mecanismo, não requisito). O requisito de produto por trás
era só "isolamento entre tenants" — vale reler as próprias BRs com essa
pergunta antes de fechar a spec funcional, não só depois que a spec
técnica expõe a contradição.

## Gotcha real #2: achado durante o TASK-011 (não durante o design)

`AppointmentNotFoundException` (nova, sem tratamento específico) caía no
`@ExceptionHandler(DomainException.class)` genérico do
`GlobalExceptionHandler`, que devolve 422 — mas a própria spec técnica
(DD-1) já exigia 404 para id de outro tenant, mesmo tratamento de
`ServiceOfferingNotFoundException` na TODO-006. Só apareceu ao escrever
`AppointmentControllerTest` e ver o teste de 404 falhar com 422 no lugar.
Corrigido com um catch específico (`detalhesOuFalhar`/`executarOuFalhar`)
nas 4 rotas de `AppointmentController`, antes de qualquer commit dessa
tarefa.

**Lição para `PATTERNS.md`**: sempre que uma feature nova introduzir uma
exceção de domínio que precisa de um HTTP status diferente do genérico
(`DomainException` → 422), o teste de camada web que verifica esse
status deve ser escrito **antes** de considerar a tarefa pronta — o
compilador não pega isso, só o teste de comportamento.
