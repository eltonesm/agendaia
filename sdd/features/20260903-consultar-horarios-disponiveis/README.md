# consultar-horarios-disponiveis (TODO-005)

## O que foi construído

Primeira feature a implementar código real em `scheduling` — o core domain
do AgendaIA, com regime completo de Clean Architecture (ADR 0002): domínio
Java puro, sem `org.springframework` nem `jakarta.persistence`.

A feature **só calcula** — não persiste nada, não tem tela, não tem
controller. Dado um profissional (derivado da oferta), uma oferta de
serviço e uma data, calcula a lista de horários realmente disponíveis:

```
BusinessOperatingHours ∩ WorkSchedule − TimeOff
    → janelas livres
    → candidatos na grade fixa de 10 min (ADR 0006)
    → filtrados por duração + bufferMinutes da oferta
    → AvailableSlot[]
```

## O que nasceu nesta feature

| Item | Onde | Por quê |
|---|---|---|
| `shared.TimeRange` | `shared` | Value object de intervalo (overlaps/intersect/subtract), reaproveitado por `WorkSchedule.overlaps()` (TODO-004), que antes duplicava a fórmula inline |
| `catalog.api` | `catalog` | Primeiro contrato público de `catalog` — `ServiceOfferingDirectory.find(id)` resolve profissional/duração/intervalo |
| `organization.api.AvailabilityDirectory` | `organization` | Segundo contrato de `organization` — expõe horário/jornada/bloqueio já convertidos para `TimeRange` |
| `scheduling.domain.SlotCalculator` | `scheduling` | O pipeline de cálculo (BR-2/BR-3/BR-4), Java puro, testável em milissegundos |
| `scheduling.application.GetAvailableSlotsHandler` | `scheduling` | Orquestra tudo: valida horizonte de 30 dias, resolve oferta, chama as duas portas `api`, delega ao calculador |

## Decisões de design centrais

- **Sem `Appointment` nesta feature** — a tabela e a exclusion constraint
  (ADR 0005) nascem só na TODO-006, quando há escrita de verdade para
  proteger. Aqui, "menos agendamentos existentes" é implicitamente vazio.
- **`GetAvailableSlotsQuery` só tem `serviceOfferingId` + `date`** (DD-6) —
  `professionalId` é sempre derivado da oferta resolvida via `catalog.api`,
  nunca aceito como entrada separada, para não abrir espaço para
  inconsistência entre os dois.
- **Horizonte de 30 dias validado com "hoje" explícito** (DD-7) — mesmo
  padrão dos `register()` das entidades: overload público usa
  `LocalDate.now()`, overload package-private recebe "hoje" para os
  testes.
- **Conversão de fuso isolada num único lugar** (DD-4) —
  `AvailabilityDirectoryHandler.blocksFor` é o único ponto do projeto que
  converte `Instant` (como `TimeOff` persiste) para `LocalTime` (como
  `scheduling.domain` consome), recortando o bloqueio para as bordas do
  dia consultado.
- **`allowedDependencies` de `scheduling` declarado corretamente desde o
  primeiro commit** (DD-1) — mesmo gotcha de whitelist que já havia
  pegado a TODO-003 duas vezes, desta vez incluindo `organization :: api`,
  `catalog :: api`, `shared` e `platform` de uma vez.

## Testes

- Domínio puro: `TimeRangeTest` (11 casos), `SlotCalculatorTest` (9 casos,
  cobrindo múltiplas janelas, bloqueio no meio, buffer, oferta maior que a
  janela).
- Aplicação (mocks): `GetAvailableSlotsHandlerTest` (7 casos — horizonte,
  oferta não encontrada, derivação de `professionalId`).
- Integração (Testcontainers): `AvailabilityDirectoryHandlerTest` (5
  casos, foco na conversão Instant→TimeRange recortada ao dia) e
  `ConsultarDisponibilidadeIT` (4 casos, E2E-1 a E2E-4 ponta a ponta,
  chamando o caso de uso direto — sem camada web, já que não existe
  controller nesta feature).
- 337 testes no projeto inteiro, 0 falhas, 90,4% de cobertura de
  instrução (`./mvnw clean verify`).

## Quality gates (Layer 3)

Todos `APPROVED`, sem nenhum achado — ver
`verdicts/{code_review,performance,security}.json`.

- **Code review**: DD-1 a DD-7 confirmadas fielmente no código.
- **Performance**: sem N+1; `workScheduleFor` reaproveita o índice
  `work_schedule_professional_day_idx` já criado na TODO-004;
  `SlotCalculator` opera em memória sobre listas pequenas.
- **Security**: isolamento por `TenantContext` nos dois lados da fronteira
  `api`; mensagem idêntica para oferta inexistente vs. de outro tenant;
  query JPQL nova (`TimeOffRepository.findOverlapping`) totalmente
  parametrizada; nenhuma rota HTTP nova, então CSRF/XSS não se aplicam
  ainda (ficam para a TODO-006).
