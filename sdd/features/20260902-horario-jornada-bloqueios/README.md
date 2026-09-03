# horario-jornada-bloqueios (TODO-004)

## O que foi construído

Três agregados novos em `organization`, todos em regime CRUD (registrar +
listar, sem editar/desativar por tela — ver Out of Scope da spec funcional):

| Agregado | Papel | Tela |
|---|---|---|
| `BusinessOperatingHours` | Horário em que o estabelecimento pode abrir | `/admin/horario-funcionamento` |
| `WorkSchedule` | Jornada semanal recorrente de um profissional | `/admin/jornadas` |
| `TimeOff` | Indisponibilidade excepcional e datada (profissional específico ou estabelecimento inteiro) | `/admin/bloqueios` |

Esta feature só **declara** o dado. Não calcula disponibilidade — isso é
`scheduling` (TODO-005, ainda vazio).

## Decisões de design centrais

- **BR-3 / DD-2 — sobreposição de `WorkSchedule`**: duas faixas do mesmo
  profissional, no mesmo dia, não podem se sobrepor. Intervalo meio-aberto
  `[)`: faixas encostadas (fim = início) não contam como sobreposição — é o
  mecanismo que representa o intervalo de almoço sem campo próprio.
  Verificado em memória contra as faixas do mesmo profissional+dia (índice
  composto `(professional_id, day_of_week)`), sem exclusion constraint
  `tstzrange` — Postgres não tem range nativo sobre `time`, e criar um tipo
  customizado foi julgado desproporcional para uma tela administrativa de
  baixa concorrência (ver ADR 0005 e a nota em `meta.md`/spec técnica).
- **BR-4 — sem validação cruzada** entre `WorkSchedule` e
  `BusinessOperatingHours`: são dados declarados independentes.
- **BR-8 / DD-3 — isolamento por FK normal**: diferente da TODO-003 (onde
  `Professional` é de outro contexto e a validação passa por
  `organization.api`), aqui os três agregados são do **mesmo contexto**
  (`organization`), então `professional_id`/`tenant_id` usam chave
  estrangeira normal — reforçada por `existsByIdAndTenantId` na aplicação,
  já que a FK sozinha não sabe de tenant.
- **`TimeOff.professionalId` opcional**: nulo vale para o estabelecimento
  inteiro (feriado, fechamento) — sem tabela nova para esse caso.

## Testes

- Domínio puro: `BusinessOperatingHoursTest`, `WorkScheduleTest` (inclui
  casos de borda de `overlaps()`), `TimeOffTest`.
- Aplicação (mocks): 6 Handlers × Register/List.
- Web isolada (`@WebMvcTest`): 3 Controllers, incluindo CSRF e validação de
  campo.
- Integração (Testcontainers): `HorarioJornadaBloqueioRegistrationIT`
  (E2E-1, E2E-2) e extensão do `CrossTenantIsolationIT` (E2E-3).
- 305 testes no projeto inteiro, 0 falhas, 89,3% de cobertura de instrução
  (`./mvnw clean verify`).

## Quality gates (Layer 3)

Todos `APPROVED` — ver `verdicts/{code_review,performance,security}.json`.

- **Code review**: 1 achado menor (`filter(id -> id != null)` em vez de
  `Objects::nonNull` em `ListTimeOffHandler`) — corrigido.
- **Performance**: sem N+1 (resolução de nome de profissional sempre em
  lote via `findAllById`); índice composto casa com a consulta de
  sobreposição. Uma recomendação não bloqueante sobre ordem de colunas do
  índice, registrada para caso o volume cresça muito.
- **Security**: validação de `professionalId` sempre antes do insert,
  mensagem de erro idêntica para "não existe" vs "existe em outro tenant",
  CSRF ligado nos três formulários, sem SQL injection (Spring Data
  derived queries), sem XSS (sem `th:utext`), sem segredo hardcoded.

## Refatoração incidental desta sessão

Antes de iniciar a TODO-004, as interfaces de `Repository` de
`organization` e `catalog` foram movidas de `<contexto>.domain` para
`<contexto>.application.port.out`, alinhando o projeto à prática moderna de
Clean Architecture/hexagonal (repositório é porta de saída, não parte do
modelo) — ver amendment de 2026-09-02 na ADR 0002 e a nova seção em
`PATTERNS.md`. TODO-004 já nasceu seguindo o padrão novo.
