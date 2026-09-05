# Implementation Summary — back-office-operador (TODO-009)

## Timeline

- Início: 2026-09-04
- Fim: 2026-09-05 (funcional → técnica → tasks → build → finish em cerca de 24h)

## Tasks

- Total: 15 (`stats.done: 15` em `tasks.json`)
- Estratégia de execução: Batched (quatro tarefas independentes no nível 0
  — amendment de docs, domínio `billing`, `BusinessDirectory`, login
  isolado do operador — depois cadeia sequencial: service → filtro/advice
  → painel/tela → layout → testes → qualidade)
- 0 tasks adicionadas ou removidas fora do plano aprovado; DD-5 foi
  reescrito durante o `TASK-005` (ver "Changes and Deviations" em `meta.md`)

## Commits (9, `main..feature/back-office-operador`)

```
f683fcb docs(sdd): inicia TODO-009 (back-office-operador)
025570a docs(sdd): aprova spec funcional da TODO-009
ec2fd0b docs(sdd): aprova spec tecnica da TODO-009
2ae9b11 docs(sdd): aprova as 15 tarefas da TODO-009 (estrategia batched)
7a8c797 feat(billing): back-office do operador - dominio, apis e login isolado (TASK-001/002/003/004/005)
2048231 feat(billing): painel do operador e guarda de acesso (TASK-006/007/008)
cf01261 test(billing): cobertura de dominio, servico e camada web (TASK-009/010/011)
a100919 fix(platform): isola sessao de operador e de dono por role, nao so autenticacao (TASK-012)
27a4638 docs(sdd): quality gates da TODO-009 - code review, performance e seguranca (TASK-013/014/015)
```

## Quality

- Testes: 34 novos nesta feature (`BillingAccountTest`,
  `BillingAccountServiceTest`, `OperatorPanelControllerTest`,
  `AccessGuardFilterTest`, `OperadorBackofficeIT`), 0 falhas, 0 erros
  (`./mvnw clean verify`)
- Cobertura de instrução: 90% no projeto inteiro (piso: 80%)
- Linter/type errors: 0
- Layer 3 (code review, performance, security): todos `APPROVED` — uma
  recomendação menor de performance (índice redundante em
  `billing_account.tenant_id`), sem achado crítico ou major em nenhum dos
  três (ver `verdicts/`)

## Gotcha real desta vez: vazamento de sessão entre as duas cadeias de autenticação

`OperatorSecurityConfig` (nova) e `SecurityConfig` (existente) compartilham
o mesmo bean `SecurityContextRepository` — necessário para o cadastro
autenticar a sessão logo após o registro (gotcha já resolvido na
TODO-001). Só que as duas cadeias checavam `anyRequest().authenticated()`,
sem exigir uma role específica: uma sessão de dono autenticada em
`/admin/**` passaria também no gate de `/operador/**` (e veria a lista de
**todos** os estabelecimentos, não só o próprio), e uma sessão de operador
passaria no gate de `/admin/**`.

Foi descoberto só ao escrever `OperadorBackofficeIT.e2e5` (TASK-012) —
exatamente o cenário que o E2E-5 da spec funcional pedia para cobrir.
Corrigido trocando `authenticated()` por `hasRole("OWNER")` em
`SecurityConfig` e `hasRole("OPERATOR")` em `OperatorSecurityConfig`, o que
por sua vez exigiu declarar `roles = "OWNER"` explicitamente nos sete
`@WithMockUser` de testes de camada web que dependiam do comportamento
antigo (`ProfessionalControllerTest`, `WorkScheduleControllerTest`,
`TimeOffControllerTest`, `BusinessOperatingHoursControllerTest`,
`ServiceControllerTest`, `ServiceOfferingControllerTest`, `SecurityRoutesIT`).

**Lição para `PATTERNS.md`**: sempre que uma segunda cadeia de segurança
compartilhar `SecurityContextRepository` com uma existente, `authenticated()`
sozinho não isola — é preciso `hasRole`/`hasAuthority` explícito nos dois
lados, e um teste de integração que efetivamente tente cruzar as sessões
(não basta testar cada cadeia isoladamente).
