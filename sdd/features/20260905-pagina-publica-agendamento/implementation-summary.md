# Implementation Summary — pagina-publica-agendamento (TODO-006)

## Timeline

- Início: 2026-09-05
- Fim: 2026-09-05 (funcional → técnica → tasks → build → finish no mesmo dia)

## Tasks

- Total: 18 (`stats.done: 18` em `tasks.json`)
- Estratégia de execução: Batched — nível 0 com 5 tarefas independentes
  (`BusinessDirectory.findBySlug`, domínio `customer`, domínio
  `Appointment`, migrations, `TenantContextFilter`), depois cadeia
  sequencial até a camada de testes e os 3 quality gates.
- 0 tasks adicionadas ou removidas fora do plano aprovado. Uma decisão de
  escopo tomada durante o `TASK-006`: a lacuna de `GetAvailableSlotsHandler`
  não descontar `Appointment` existentes foi corrigida **dentro** desta
  feature (opção escolhida pelo usuário), em vez de virar tarefa/feature
  separada.

## Commits (12, `main..feature/pagina-publica-agendamento`)

```
4470852 docs(sdd): inicia TODO-006 (pagina-publica-agendamento)
0f438cd docs(sdd): aprova spec funcional da TODO-006
75d2e35 docs(sdd): aprova spec tecnica da TODO-006
2d18a6e docs(sdd): aprova as 18 tarefas da TODO-006 (estrategia batched)
2680c9e feat(customer,catalog,scheduling): base da pagina publica de agendamento (TASK-001/002/003/004/008)
29ee43f feat(scheduling,platform): persistencia de Appointment e resolucao de tenant por slug (TASK-005/007)
48827cf feat(scheduling): BookAppointmentHandler grava o agendamento (TASK-006)
450ba81 feat(scheduling): PublicBookingController fecha a pagina publica de agendamento (TASK-009/010)
3af3fd7 test(scheduling,catalog): dominio e aplicacao da pagina publica (TASK-011/012/013)
093b9b9 test(scheduling): PublicBookingControllerTest com @WebMvcTest (TASK-014)
023828a test(scheduling): PaginaPublicaAgendamentoIT com E2E-1 a E2E-7 (TASK-015)
46d9ad1 chore(scheduling): fecha camada 3 da TODO-006 - code review, performance e security
```

## Quality

- Testes: 394 no projeto inteiro, 0 falhas, 0 erros (`./mvnw clean verify`).
- Cobertura de instrução: 90% (piso: 80%).
- Linter/type errors: 0.
- Layer 3 (code review, performance, security): security `APPROVED`;
  code review e performance `CAN_PROCEED_WITH_WARNINGS` (1 achado menor
  cada, nenhum crítico/major) — ver `verdicts/`.

## Gotcha real desta vez: "corrigir" um achado de review pode piorar o bug

Durante o code review (`TASK-016`), identifiquei que
`CustomerDirectoryHandler.findOrCreate` não tratava
`DataIntegrityViolationException` de `UNIQUE(tenant_id, phone)` — duas
requisições com o mesmo telefone nunca visto antes, no mesmo instante,
fariam a segunda estourar 500 genérico. A correção óbvia (capturar e
reconsultar por telefone) foi implementada, testada mentalmente e
**revertida**: `findOrCreate` roda na mesma transação de
`BookAppointmentHandler.handle` (propagação `REQUIRED`), e o Postgres
aborta a transação inteira na violação de constraint — qualquer novo
comando na mesma conexão, incluindo a reconsulta "de segurança", falha de
novo com `current transaction is aborted`, um erro mais confuso que o
original.

Corrigir direito exigiria uma transação `REQUIRES_NEW` separada só para o
retry (self-injection ou bean colaborador, porque `REQUIRES_NEW` não
funciona em auto-invocação dentro da mesma classe) — desproporcional para
um caso raro (mesmo telefone, mesmo instante, primeira vez) num piloto de
um único estabelecimento. Documentado no Javadoc da classe e registrado
como **DEBT-017** em vez de deixado implícito ou "corrigido" com uma
armadilha.

**Lição para `PATTERNS.md`**: nem todo achado de code review deve virar
correção imediata — quando a correção óbvia depende de comportamento
transacional (retry após `DataIntegrityViolationException` dentro da
mesma transação que a causou), vale a pena testar a hipótese antes de
aplicar, porque o Postgres aborta a transação inteira, não só a
instrução que violou a constraint.
