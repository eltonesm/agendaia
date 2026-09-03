# Implementation Summary — horario-jornada-bloqueios (TODO-004)

## Timeline

- Início: 2026-09-02
- Fim: 2026-09-02 (funcional → técnica → tasks → build → finish no mesmo dia)

## Tasks

- Total: 17 (`stats.done: 17` em `tasks.json`)
- Estratégia de execução: Batched (paralelismo em TASK-001/002 e
  TASK-003/004/005; cadeia de fundo sequencial daí em diante)
- 0 tasks adicionadas, removidas ou modificadas fora do plano aprovado

## Commits (14, `main..feature/horario-jornada-bloqueios`)

```
c79d84c docs(sdd): inicia TODO-004 (horario-jornada-bloqueios)
69732ae docs(sdd): aprova spec funcional da TODO-004
ddad6a8 docs(sdd): aprova spec tecnica da TODO-004
9064024 docs(sdd): aprova as 17 tarefas da TODO-004 (estrategia batched)
4e33e2c feat(organization): migration V5 e ProfessionalRepository.existsByIdAndTenantId (TASK-001/002)
a0f0ab3 feat(organization): entidades BusinessOperatingHours, WorkSchedule, TimeOff (TASK-003/004/005)
1a07918 feat(organization): casos de uso dos tres agregados novos (TASK-006/007/008)
02c1050 feat(organization): tela e controller de /admin/horario-funcionamento (TASK-009)
bdfa0ae feat(organization): tela e controller de /admin/jornadas (TASK-010)
0ccc24e feat(organization): tela e controller de /admin/bloqueios (TASK-011)
b2854fc feat(organization): painel linka para as tres novas telas (TASK-012)
61baa7a test(organization): E2E-1 e E2E-2 com Postgres real (TASK-013)
c4548c6 test(platform): estende isolamento entre tenants para os 3 agregados de TODO-004 (TASK-014)
582baba fix(organization): Objects::nonNull no filtro de ids (TASK-015/016/017)
```

## Quality

- Testes: 305 no projeto inteiro, 0 falhas, 0 erros (`./mvnw clean verify`)
- Cobertura de instrução: 89,3% (piso: 80%)
- Linter/type errors: 0
- Layer 3 (code review, performance, security): todos `APPROVED`,
  1 achado menor corrigido (ver `verdicts/`)

## Gotcha recorrente (2ª ocorrência, já documentado em PATTERNS.md)

FK nova de `work_schedule`/`time_off` → `professional` e
`business_operating_hours` → `business` quebrou a ordem de limpeza de 5
ITs pré-existentes que compartilham o mesmo container Testcontainers
durante `./mvnw verify` (mesmo padrão da TODO-002, que já havia deixado a
lição escrita em `PATTERNS.md` e foi pulada de novo aqui). Corrigido
adicionando os três repositórios novos + `deleteAllInBatch()` ordenado em
cada IT afetada.
