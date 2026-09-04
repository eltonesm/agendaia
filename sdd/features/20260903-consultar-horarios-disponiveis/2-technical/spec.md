# consultar-horarios-disponiveis - Technical Spec

**Feature**: consultar-horarios-disponiveis
**Status**: approved
**Data**: 2026-09-03
**Aprovado por**: Elton Marques em 2026-09-04T01:14:57Z

---

## Executive Summary

Primeira feature de `scheduling` — regime completo de Clean Architecture
(ADR 0002): domínio em Java puro, sem `org.springframework` nem
`jakarta.persistence`. Não há nenhuma tabela nova: esta feature não
persiste nada (`AvailableSlot` é value object, nunca gravado; `Appointment`
fica para a TODO-006). O trabalho é, inteiramente, ler dado de dois outros
contextos via `api` e calcular em memória.

Dois pacotes `api` novos nascem aqui: `organization.api.AvailabilityDirectory`
(expondo `BusinessOperatingHours`, `WorkSchedule` e `TimeOff` já convertidos
para `shared.TimeRange`) e `catalog.api` (primeira vez que `catalog` expõe
contrato próprio — `ServiceOfferingDirectory`, para resolver
profissional/duração/intervalo a partir do id da oferta).

`shared.TimeRange` nasce nesta feature — um value object de intervalo com
`overlaps`/`intersect`/`subtract`, reaproveitado por `WorkSchedule.overlaps()`
(refactor mecânico, comportamento idêntico, coberto pelos testes já
existentes da TODO-004).

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│  scheduling (core domain — regime completo, ADR 0002)               │
│                                                                       │
│  application/port/in/                                                │
│    GetAvailableSlotsUseCase, GetAvailableSlotsQuery                   │
│  application/                                                        │
│    GetAvailableSlotsHandler  ──┐                                     │
│  domain/ (Java puro)            │                                    │
│    AvailableSlot (value object) │                                    │
│    SlotCalculator (BR-2..BR-4)  │                                    │
│    exception/                   │                                    │
│      AvailabilityQueryOutOfRangeException  (BR-5)                    │
│      ServiceOfferingNotFoundException      (BR-7)                    │
└──────────────────────────────────┼───────────────────────────────────┘
                                    │  lê via api (DD-4, DD-3)
                    ┌───────────────┴────────────────┐
                    ▼                                 ▼
┌───────────────────────────────────┐   ┌─────────────────────────────┐
│ organization.api (novo)           │   │ catalog.api (novo, DD-3)    │
│  AvailabilityDirectory             │   │  ServiceOfferingDirectory   │
│   .operatingHoursFor(dayOfWeek)    │   │   .find(serviceOfferingId)  │
│   .workScheduleFor(profId, day)    │   │   -> ServiceOfferingRef     │
│   .blocksFor(profId, date)         │   │      (profId, duration,     │
│   -> List<shared.TimeRange>        │   │       buffer)                │
│                                     │   └─────────────────────────────┘
│ implementado por                   │
│ AvailabilityDirectoryHandler        │
│ (organization.application) —        │
│ lê BusinessOperatingHours/          │
│ WorkSchedule/TimeOff via             │
│ repositórios já existentes (TODO-004)│
└───────────────────────────────────┘
```

**Fluxo de uma consulta**:

1. `GetAvailableSlotsHandler` recebe `(serviceOfferingId, date)`.
2. Valida `date` contra o horizonte [hoje, hoje + 30 dias] (BR-5) — fora
   disso, lança `AvailabilityQueryOutOfRangeException`.
3. Busca a oferta via `catalog.api.ServiceOfferingDirectory.find(id)` — não
   encontrada (não existe, ou é de outro tenant) lança
   `ServiceOfferingNotFoundException` (BR-7).
4. Com o `professionalId` da oferta, busca via
   `organization.api.AvailabilityDirectory`: horário de funcionamento do
   dia da semana, jornada do profissional no mesmo dia, bloqueios que
   se sobrepõem à data.
5. Chama `SlotCalculator.calculate(...)` (domínio puro) com as três listas
   de `TimeRange` mais duração e intervalo da oferta.
6. Mapeia o resultado (`List<TimeRange>`, horário local do dia) para
   `List<AvailableSlot>`, combinando com `date` para produzir
   `LocalDateTime`.

---

## Design Decisions

### DD-1: `scheduling` precisa declarar `allowedDependencies` explicitamente

**Contexto**: `package-info.java` de `scheduling` hoje não declara
`allowedDependencies` (só `displayName`). Esta feature introduz a primeira
dependência real: leitura de `organization :: api` e `catalog :: api`.

**Decisão**: declarar
`allowedDependencies = {"organization :: api", "catalog :: api", "shared", "platform"}`
explicitamente — incluindo `shared` e `platform`, apesar de ambos serem
`Type.OPEN`. É exatamente o gotcha já documentado em `PATTERNS.md` desde a
TODO-003: declarar a lista uma vez a transforma em whitelist, e módulo
aberto não entra de graça. Aplicado corretamente desde o primeiro commit
desta feature, para não repetir o erro pela terceira vez.

**Trade-offs Accepted**: nenhum — é a forma correta de declarar, só
importa fazer completo da primeira vez.

### DD-2: `shared.TimeRange` nasce aqui, e `WorkSchedule.overlaps()` passa a delegar a ele

**Contexto**: o cálculo de disponibilidade precisa de interseção e
subtração de intervalos de horário — operação genérica, não específica de
`scheduling`. `WorkSchedule.overlaps()` (TODO-004) já implementa a mesma
fórmula de sobreposição inline (`a.start < b.end && b.start < a.end`),
sem reaproveitamento porque `TimeRange` não existia ainda.

**Opções consideradas**:
- **A — `TimeRange` só em `scheduling.domain`**: mais simples de escrever,
  mas duplica a fórmula que `WorkSchedule` já tem, e não serve para
  `BusinessOperatingHours` nem para o resultado convertido de `TimeOff` em
  `organization` (que precisam da mesma operação para expor via `api`).
- **B (selecionada) — `TimeRange` em `shared`, com `WorkSchedule.overlaps()`
  refatorado para delegar**: um único lugar para a fórmula, reaproveitado
  por `organization` (para montar as listas expostas via `api`) e por
  `scheduling.domain` (para o cálculo em si). `shared` já é importado por
  todo mundo (`TenantId`, `Money`), então não introduz acoplamento novo.

**Trade-offs Accepted**: `WorkSchedule.overlaps()` muda de implementação
(delega a `TimeRange.overlaps()`), mesmo já estando mesclado e testado
desde a TODO-004. É refactor mecânico — mesma fórmula, mesmo
comportamento, coberto pelos testes existentes (`WorkScheduleTest`) sem
precisar de caso novo.

**Rationale**: `TimeRange` é exatamente o tipo que o glossário já antecipa
em `shared` (ver tabela de contextos do `CLAUDE.md`) — esta é a primeira
feature que genuinely precisa dele.

**Contrato de `TimeRange`** (`com.agendaia.shared`):
```java
public record TimeRange(LocalTime start, LocalTime end) {
    public TimeRange { /* valida start != null, end != null, end.isAfter(start) */ }
    public boolean overlaps(TimeRange outro);            // [) meio-aberto, mesma semântica do WorkSchedule
    public Optional<TimeRange> intersect(TimeRange outro);
    public List<TimeRange> subtract(TimeRange bloqueio);  // 0, 1 ou 2 pedaços
}
```

### DD-3: `catalog.api` nasce nesta feature

**Contexto**: até a TODO-004, só `organization` tinha pacote `api`
(`ProfessionalDirectory`, TODO-003). `scheduling` agora precisa resolver,
a partir de um `serviceOfferingId`, o `professionalId`, a duração e o
`bufferMinutes` — dado que só existe em `catalog`.

**Decisão**: criar `catalog.api.ServiceOfferingDirectory` com uma única
operação, granular (não em lote como `ProfessionalDirectory.listActive()`,
porque aqui o consumidor já sabe exatamente qual oferta quer — não está
populando um dropdown):

```java
public interface ServiceOfferingDirectory {
    /** Vazio se o id não existe, ou existe em outro tenant (BR-7). */
    Optional<ServiceOfferingRef> find(UUID serviceOfferingId);
}

public record ServiceOfferingRef(UUID id, UUID professionalId, int durationMinutes, int bufferMinutes) {}
```

Implementado por `ServiceOfferingDirectoryHandler` em
`catalog.application`, tenant lido de `TenantContext.require()` (mesma
convenção de `ProfessionalDirectoryHandler`).

**Trade-offs Accepted**: `catalog` ganha seu primeiro `package-info.java`
com `@NamedInterface("api")` — mecânico, mesmo padrão de `organization.api`.

### DD-4: `organization.api.AvailabilityDirectory` — três operações, tenant sempre implícito

**Decisão**:

```java
public interface AvailabilityDirectory {
    /** Faixas de funcionamento do estabelecimento no dia da semana. */
    List<TimeRange> operatingHoursFor(DayOfWeek dayOfWeek);

    /** Faixas de jornada do profissional no dia da semana. */
    List<TimeRange> workScheduleFor(UUID professionalId, DayOfWeek dayOfWeek);

    /**
     * Bloqueios (do profissional específico, ou do estabelecimento inteiro)
     * que se sobrepõem à data, já convertidos e recortados para o intervalo
     * [00:00, 24:00) local dessa data.
     */
    List<TimeRange> blocksFor(UUID professionalId, LocalDate date);
}
```

Implementado por `AvailabilityDirectoryHandler` em
`organization.application`, usando os repositórios já existentes da
TODO-004 (`BusinessOperatingHoursRepository`, `WorkScheduleRepository`,
`TimeOffRepository`) mais uma consulta nova em cada um (ver "Existing Data
& Migrations" abaixo — nenhuma é migration, são métodos de repositório).

`blocksFor` é o único ponto do projeto que converte `Instant` (como
`TimeOff` persiste) para `LocalTime`/`TimeRange` (como `scheduling.domain`
consome) — a conversão via `ZoneId.systemDefault()` acontece aqui, dentro
de `organization` (que já depende de Spring), nunca em `scheduling.domain`.
Um `TimeOff` que começa antes da meia-noite da data consultada, ou termina
depois, é recortado para as bordas do dia antes de virar `TimeRange`.

**Trade-offs Accepted**: três chamadas separadas (não uma operação
"tudo de uma vez") — mantém `AvailabilityDirectory` simples e cada método
testável isoladamente; o custo é três idas ao banco por consulta, aceitável
dado o volume (uma consulta de disponibilidade não é um caminho de alta
frequência ainda, sem tela consumindo).

### DD-5: Bloqueio geral (`professionalId` nulo) exige `@Query` JPQL, não derived method

**Contexto**: `TimeOffRepository` precisa encontrar bloqueios onde
`professionalId = :id` **ou** `professionalId is null` (estabelecimento
inteiro), sobrepondo o intervalo `[dayStart, dayEnd)` da data consultada.
Um nome de método derivado para "igual a X ou nulo" combinado com
sobreposição de intervalo não tem uma forma limpa no Spring Data.

**Decisão**: uma única query JPQL nova, totalmente parametrizada (não é
SQL nativo):

```java
@Query("""
        select t from TimeOff t
        where t.tenantId = :tenantId
          and t.active = true
          and t.startsAt < :dayEnd
          and t.endsAt > :dayStart
          and (t.professionalId = :professionalId or t.professionalId is null)
        """)
List<TimeOff> findOverlapping(UUID tenantId, UUID professionalId, Instant dayStart, Instant dayEnd);
```

**Trade-offs Accepted**: primeira exceção à convenção "só derived query"
observada pela revisão de segurança da TODO-004. JPQL parametrizado por
posição nomeada não introduz risco de injeção (equivalente em segurança a
um derived method) — documentado aqui para não ser lido como desvio
silencioso.

### DD-6: `GetAvailableSlotsQuery` recebe `serviceOfferingId` + `date`, não `professionalId` separado

**Contexto**: a spec funcional descreve o resultado como "para um
profissional, uma oferta e uma data" (linguagem do glossário), mas
`ServiceOffering` já pertence a exatamente um profissional (TODO-003) —
pedir os dois como entradas independentes abriria a possibilidade de
inconsistência (e se o `professionalId` informado não bater com o da
oferta?).

**Decisão**: `GetAvailableSlotsQuery(UUID serviceOfferingId, LocalDate date)`.
`professionalId` é derivado da `ServiceOfferingRef` retornada por
`catalog.api`. Menos parâmetro, nenhuma inconsistência possível.

**Trade-offs Accepted**: nenhum — é estritamente uma simplificação.

### DD-7: Horizonte de 30 dias validado com "hoje" via parâmetro, não `Clock` injetado

**Contexto**: BR-5 exige validar a data contra "hoje". O projeto já tem um
padrão estabelecido para "agora" testável: os `register()` das entidades
(TODO-001 a TODO-004) têm uma sobrecarga pública que usa `Instant.now()` e
uma sobrecarga package-private que recebe o instante como parâmetro, usada
pelos testes.

**Decisão**: `GetAvailableSlotsHandler` segue o mesmo padrão — um método
package-private que recebe `LocalDate hoje` explícito (usado pelos
testes), chamado pela sobrecarga pública que usa `LocalDate.now()`.

**Trade-offs Accepted**: não introduz `java.time.Clock` como bean Spring
novo — mantém consistência com o padrão já usado em todo o projeto, em vez
de introduzir uma segunda forma de resolver "agora".

---

## Existing Data & Migrations

**Nenhuma migration nesta feature.** Não há tabela nova: `AvailableSlot`
nunca é persistido (BR-1), e `Appointment` fica para a TODO-006.

**Métodos de repositório novos** (sem mudança de schema):

```java
// BusinessOperatingHoursRepository
List<BusinessOperatingHours> findByTenantIdAndDayOfWeekAndActiveTrue(UUID tenantId, DayOfWeek dayOfWeek);

// WorkScheduleRepository — já existe, reaproveitado sem mudança:
// findByTenantIdAndProfessionalIdAndDayOfWeekAndActiveTrue(UUID, UUID, DayOfWeek)

// TimeOffRepository
@Query(...) // ver DD-5
List<TimeOff> findOverlapping(UUID tenantId, UUID professionalId, Instant dayStart, Instant dayEnd);

// ServiceOfferingRepository
Optional<ServiceOffering> findByTenantIdAndIdAndActiveTrue(UUID tenantId, UUID id);
```

---

## Data Model

**`AvailableSlot`** (`scheduling.domain`, value object, nunca persistido):

```java
public record AvailableSlot(
        UUID professionalId,
        UUID serviceOfferingId,
        LocalDateTime startsAt,
        LocalDateTime endsAt) {}
```

`endsAt` é `startsAt + duration` (sem o `bufferMinutes` — o intervalo é
reservado *depois* do atendimento, não faz parte do horário mostrado ao
cliente; ver glossário).

**`SlotCalculator`** (`scheduling.domain`, classe utilitária, sem estado):

```java
public final class SlotCalculator {
    private static final int GRID_MINUTES = 10; // ADR 0006 — método com nome próprio, não interface

    public static List<TimeRange> calculate(
            List<TimeRange> businessHours,
            List<TimeRange> workSchedule,
            List<TimeRange> blocked,
            int durationMinutes,
            int bufferMinutes) {
        // 1. interseção par a par de businessHours × workSchedule → janelas livres brutas
        // 2. subtrai cada bloqueio de cada janela livre bruta → janelas livres finais
        // 3. para cada janela livre, gera candidatos a cada 10 min a partir do início,
        //    mantém só os que cabem inteiros: [candidato, candidato + duration + buffer) ⊆ janela
        // retorna os TimeRange [candidato, candidato + duration) válidos
    }
}
```

`GetAvailableSlotsHandler` combina cada `TimeRange` retornado com a
`date` da consulta para produzir `AvailableSlot` (`LocalDateTime`).

---

## Cross-Context API Contracts

> Sem REST — nenhuma rota HTTP nesta feature. Os contratos abaixo são
> Java, atravessando a fronteira `api` entre módulos Spring Modulith (ADR
> 0010), não endpoints.

### `organization.api.AvailabilityDirectory`

| Método | Entrada | Saída | Tenant |
|---|---|---|---|
| `operatingHoursFor` | `DayOfWeek` | `List<TimeRange>` | `TenantContext.require()` |
| `workScheduleFor` | `UUID professionalId, DayOfWeek` | `List<TimeRange>` | `TenantContext.require()` |
| `blocksFor` | `UUID professionalId, LocalDate` | `List<TimeRange>` | `TenantContext.require()` |

### `catalog.api.ServiceOfferingDirectory`

| Método | Entrada | Saída | Tenant |
|---|---|---|---|
| `find` | `UUID serviceOfferingId` | `Optional<ServiceOfferingRef>` | `TenantContext.require()` |

### `scheduling.application.port.in.GetAvailableSlotsUseCase`

```java
public record GetAvailableSlotsQuery(UUID serviceOfferingId, LocalDate date) {}

public interface GetAvailableSlotsUseCase {
    List<AvailableSlot> handle(GetAvailableSlotsQuery query);
}
```

---

## Security

Nenhuma superfície nova de autenticação — não há controller, não há form,
não há CSRF a considerar. Isolamento entre tenants (BR-7) é garantido em
dois pontos: `AvailabilityDirectory`/`ServiceOfferingDirectory` sempre
filtram por `TenantContext.require()` internamente (nunca recebem tenant
como parâmetro, DD-1 geral do projeto); e `ServiceOfferingDirectory.find`
retorna vazio (não lança) para id de outro tenant — o handler é quem
decide lançar `ServiceOfferingNotFoundException` a partir do `Optional`
vazio, mesma mensagem para "não existe" e "existe em outro tenant" (mesmo
raciocínio de `ProfessionalNotFoundException`, TODO-004).

---

## Performance

Três leituras por consulta (horário de funcionamento do dia, jornada do
profissional no dia, bloqueios que se sobrepõem à data), todas já
filtradas por tenant e por dia/profissional nos índices existentes da
TODO-004 (`work_schedule_professional_day_idx`) ou por tabela pequena
(`business_operating_hours` tem poucas linhas por tenant). Sem N+1: nenhum
loop chama repositório — as três chamadas são fixas por consulta,
independente de quantas faixas cada lista tem. `SlotCalculator` opera em
memória sobre listas pequenas (poucas faixas por dia, nunca centenas).

---

## Testing Strategy

| Camada | Arquivo | Cobre |
|---|---|---|
| Domínio puro | `TimeRangeTest` (shared) | overlaps, intersect, subtract — casos de borda: faixas encostadas, bloqueio no meio, bloqueio cobrindo a faixa inteira |
| Domínio puro | `SlotCalculatorTest` | BR-2, BR-3, BR-4 — interseção, grade de 10 min, filtro por duração+buffer, múltiplas janelas no dia |
| Domínio puro | `WorkScheduleTest` (TODO-004, sem caso novo) | confirma que o refactor de DD-2 não muda comportamento |
| Aplicação (mocks) | `GetAvailableSlotsHandlerTest` | orquestração: horizonte (BR-5), oferta não encontrada (BR-7), combinação de `TimeRange` + `date` em `AvailableSlot` |
| Integração | `AvailabilityDirectoryHandlerTest` ou IT com Testcontainers | leitura real de `BusinessOperatingHours`/`WorkSchedule`/`TimeOff`, conversão `Instant`→`TimeRange` recortada ao dia |
| Integração | `ConsultarDisponibilidadeIT` (Testcontainers) | E2E-1 a E2E-4 da spec funcional, ponta a ponta contra Postgres real, sem camada web |

---

## Implementation Locations

```
src/main/java/com/agendaia/shared/
└── TimeRange.java                                          [NOVO]

src/main/java/com/agendaia/organization/
├── domain/WorkSchedule.java                                [EDITADO — overlaps() delega a TimeRange]
├── api/
│   ├── AvailabilityDirectory.java                          [NOVO]
│   └── package-info.java                                   [EDITADO — javadoc]
├── application/
│   └── AvailabilityDirectoryHandler.java                   [NOVO]
└── application/port/out/
    ├── BusinessOperatingHoursRepository.java                [EDITADO — novo método]
    └── TimeOffRepository.java                               [EDITADO — @Query novo]

src/main/java/com/agendaia/catalog/
├── package-info.java                                        [EDITADO — allowedDependencies sem mudança, javadoc]
├── api/
│   ├── ServiceOfferingDirectory.java                        [NOVO]
│   ├── ServiceOfferingRef.java                              [NOVO]
│   └── package-info.java                                    [NOVO]
├── application/
│   └── ServiceOfferingDirectoryHandler.java                 [NOVO]
└── application/port/out/ServiceOfferingRepository.java      [EDITADO — novo método]

src/main/java/com/agendaia/scheduling/
├── package-info.java                                        [EDITADO — allowedDependencies novo, DD-1]
├── domain/
│   ├── AvailableSlot.java                                   [NOVO]
│   ├── SlotCalculator.java                                  [NOVO]
│   └── exception/
│       ├── AvailabilityQueryOutOfRangeException.java        [NOVO]
│       └── ServiceOfferingNotFoundException.java            [NOVO]
└── application/
    ├── port/in/GetAvailableSlotsUseCase.java                [NOVO]
    ├── port/in/GetAvailableSlotsQuery.java                  [NOVO]
    └── GetAvailableSlotsHandler.java                        [NOVO]

src/test/java/com/agendaia/
├── shared/TimeRangeTest.java                                [NOVO]
├── scheduling/domain/SlotCalculatorTest.java                [NOVO]
├── scheduling/application/GetAvailableSlotsHandlerTest.java [NOVO]
├── organization/application/AvailabilityDirectoryHandlerTest.java [NOVO]
└── scheduling/ConsultarDisponibilidadeIT.java               [NOVO]
```

---

## References

- [ADR 0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — regime completo de `scheduling`
- [ADR 0006](../../../docs/architecture/adr/0006-grade-fixa-como-unica-estrategia-de-slot.md) — grade fixa de 10 min
- [ADR 0004](../../../docs/architecture/adr/0004-multi-tenancy-por-discriminador.md) — tenant nunca por parâmetro
- `docs/domain/glossary.md` — `AvailableSlot`, `Availability`, `slotInterval`, armadilha #2 (nome "disponibilidade")
- `sdd/PATTERNS.md` — regime de rigor completo; gotcha de `allowedDependencies` como whitelist
