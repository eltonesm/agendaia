# agenda-profissional - Technical Spec

**Feature**: agenda-profissional
**Status**: approved
**Data**: 2026-09-07
**Aprovado por**: Elton Marques em 2026-09-07T13:47:51Z

---

## Executive Summary

Primeira tela admin de `scheduling`. Até aqui, `Appointment` só era
criado, confirmado ou cancelado pelo cliente, sem sessão (TODO-006/007).
Esta feature dá ao dono, autenticado em `/admin/**`, os mesmos poderes
— e um a mais que o cliente não tem: agir sobre agendamento cujo
horário já passou (BR-2).

Nenhuma infraestrutura nova. `/admin/agenda/**` cai automaticamente sob
`anyRequest().hasRole("OWNER")` do `SecurityConfig` (nada de rota
liberada por engano); `TenantContextFilter` já resolve o tenant pela
sessão para qualquer rota fora de `/b/**`. A exclusion constraint (ADR
0005) continua sendo a garantia real contra overbooking, agora também
para criação manual e para o `Appointment` novo de um reagendamento.

---

## Architecture Overview

```
GET/POST /admin/agenda/**  (sessão OWNER, TenantContext pela sessão)
        │
        ▼
┌───────────────────────────────────────────────────────────────────┐
│ scheduling.adapter.in.web.AgendaController (NOVO)                  │
│   GET  /admin/agenda                    → lista do dia (query params) │
│   GET  /admin/agenda/novo               → formulário de criar         │
│   POST /admin/agenda/novo               → cria manualmente            │
│   POST /admin/agenda/agendamentos/{id}/confirmar → reusa TODO-007     │
│   POST /admin/agenda/agendamentos/{id}/cancelar  → sem restrição de   │
│                                                     horário (BR-2)    │
│   GET  /admin/agenda/agendamentos/{id}/reagendar → formulário         │
│   POST /admin/agenda/agendamentos/{id}/reagendar → reagenda           │
└───┬─────────────────┬──────────────────┬──────────────┬─────────────┘
    │                  │                  │              │
    ▼                  ▼                  ▼              ▼
ProfessionalAgenda  ConfirmAppointment  organization.api  catalog.api
Handler (NOVO)      UseCase (TODO-007,  .ProfessionalDirectory
 implements          sem mudança)        .listActive() (dropdown)
  ViewAgendaUseCase
  CreateAppointmentManuallyUseCase        catalog.api.ServiceOfferingDirectory
  CancelAppointmentByOwnerUseCase          .listActive() (NOVO, DD-2)
  RescheduleAppointmentUseCase             — dropdown único, sem cascata JS
    │
    ▼
AppointmentFactory (NOVO, package-private, DD-5)
 — monta o retrato e grava, reaproveitado por
   BookAppointmentHandler (cliente, com teto BR-9)
   e ProfessionalAgendaHandler (dono, sem teto, BR-4)
    │
    ▼
Appointment.schedule(...) / .cancelByOwner() (NOVO, DD-6)
```

**Fluxo de reagendar** (DD-9): cria o novo `Appointment` primeiro (se a
exclusion constraint recusar, nada muda); só depois cancela o original
via `cancelByOwner()`. Nunca edita o registro original in-place (ADR
0011).

---

## Design Decisions

### DD-1: Nenhuma mudança em `SecurityConfig`/`TenantContextFilter`

**Contexto**: toda rota `/admin/**` já cai em `anyRequest().hasRole("OWNER")`
por padrão (regra "proteger por omissão", TODO-001); `TenantContextFilter`
já resolve `TenantContext` pela sessão `AuthenticatedUser` para qualquer
rota fora de `/b/**` (ADR 0004, DD-3 da TODO-006).

**Decisão**: `/admin/agenda/**` não precisa de nenhuma entrada nova em
nenhum dos dois — a proteção e a resolução de tenant já existem, de
propósito, desde a primeira feature admin do projeto.

**Trade-offs Accepted**: nenhum.

### DD-2: `catalog.api.ServiceOfferingDirectory` ganha `listActive()`

**Contexto**: criar/reagendar precisa de um dropdown único (DD-10) com
todas as ofertas ativas do tenant, já com nome de profissional e de
serviço resolvidos — diferente de `listActiveByService` (TODO-006, filtra
por um serviço) e de `find` (um id só).

**Opções Consideradas**:
- **A — Reaproveitar `catalog.application.port.in.ListServiceOfferingsUseCase`
  diretamente**: já existe exatamente essa listagem para a tela
  `/admin/ofertas` (`ServiceOfferingController`), mas mora em
  `catalog.application.port.in` — **não exposto para outro contexto**
  (regra fundamental do `CLAUDE.md`: contextos só se falam pelo pacote
  `api`). `scheduling` não pode importar isso.
- **B (selecionada) — Novo método em `catalog.api.ServiceOfferingDirectory`**:
  `listActive() -> List<ActiveOfferingRef>`, mesma técnica de
  `listActiveByService` (`professionalDirectory.listActive()` chamado
  uma vez, `serviceRepository.findByTenantIdAndActiveTrueOrderByNameAsc`
  chamado uma vez, os dois viram `Map` para join em memória — zero N+1).

**Contrato**:
```java
// catalog.api
public record ActiveOfferingRef(
        UUID id, UUID professionalId, String professionalName,
        String serviceName, int durationMinutes, String priceFormatted) {}

// ServiceOfferingDirectory ganha:
List<ActiveOfferingRef> listActive();
```

**Trade-offs Accepted**: `ActiveOfferingRef` é quase idêntico a
`PublicOfferingRef` (só ganha `serviceName`) — duplicação deliberada
(rejeitada a ideia de estender `PublicOfferingRef`): um é para a página
pública de um serviço só, o outro para um dropdown administrativo
cruzando todos os serviços; acoplar os dois contextos de uso a um tipo
só criaria uma dependência de UX que não existe de verdade.

### DD-3: `AppointmentRepository` ganha `findByTenantIdAndProfessionalIdAndDate`

**Contexto**: nenhuma consulta hoje lista `Appointment` por profissional
e dia — `findOccupiedRanges` (TODO-006) devolve só `TimeRange`, sem id,
sem cliente, sem status, porque serve só para o cálculo de
disponibilidade.

**Decisão**: método novo, mesma técnica de recorte de dia (zona, início e
fim do dia) de `findOccupiedRanges`, mas devolvendo `List<Appointment>`
completo — **todos os status**, inclusive `CANCELLED` (US-1: o dono vê o
quadro completo do dia, não só o que ainda está de pé).

**Trade-offs Accepted**: nenhum.

### DD-4: `CustomerDirectory` ganha `findByIds` (batch); `CustomerRef` ganha `phone`

**Contexto**: a lista de agenda de um dia mostra nome **e telefone** de
cada cliente (US-1) — diferente da tela pública, que nunca expõe
telefone. Resolver isso agendamento por agendamento seria N+1
(PATTERNS.md, "API entre contextos é grossa").

**Decisão**: `CustomerDirectory.findByIds(Collection<UUID> ids) ->
List<CustomerRef>`, uma consulta só para todos os clientes distintos do
dia. `CustomerRef` ganha o campo `phone` — só usado pela tela admin;
`AppointmentController` (TODO-007) continua sem exibir telefone em
lugar nenhum.

**Trade-offs Accepted**: `CustomerRef` passa a carregar um dado que nem
todo consumidor precisa (a tela do cliente, TODO-007, só usa `name()`)
— aceitável: é o mesmo padrão de `BusinessRef` (TODO-007) ganhar
`whatsapp` sem que todo consumidor precise dele.

### DD-5: Lógica de montar e gravar o `Appointment` extraída para `AppointmentFactory`

**Contexto**: `BookAppointmentHandler` (TODO-006) já monta o retrato
(`serviceName`, `durationMinutes`, `price`) a partir da oferta e grava.
A criação manual pelo dono precisa exatamente da mesma montagem, mas sem
o teto por telefone (BR-9 da TODO-006 não se aplica, BR-4 desta feature);
o reagendamento precisa da mesma montagem, mas sem resolver cliente por
telefone (o `customerId` já é conhecido, vem do agendamento original).

**Opções Consideradas**:
- **A — Duplicar a montagem em cada handler**: três cópias quase
  idênticas de "calcula `endsAt`, monta `Appointment.schedule(...)`,
  grava" — o tipo de duplicação que já rendeu um bug corrigido às
  pressas na TODO-006 (retrato inconsistente).
- **B (selecionada) — `AppointmentFactory` package-private**:
  ```java
  // scheduling.application, pacote-privado
  final class AppointmentFactory {
      static Appointment buildAndSave(
              AppointmentRepository repo, TenantId tenantId, UUID professionalId,
              UUID customerId, ServiceOfferingRef oferta, Instant startsAt) {
          var endsAt = startsAt.plus(oferta.durationMinutes(), ChronoUnit.MINUTES);
          var agendamento = Appointment.schedule(
                  tenantId, professionalId, oferta.id(), customerId,
                  oferta.serviceName(), oferta.durationMinutes(), oferta.price(), startsAt, endsAt);
          return repo.save(agendamento);
      }
  }
  ```
  `BookAppointmentHandler` passa a: resolver oferta → checar teto (BR-9)
  → `customerDirectory.findOrCreate` → `AppointmentFactory.buildAndSave`.
  `ProfessionalAgendaHandler.create`: resolver oferta →
  `customerDirectory.findOrCreate` (sem checar teto) →
  `AppointmentFactory.buildAndSave`. `ProfessionalAgendaHandler.reschedule`:
  resolver nova oferta → reaproveita `customerId` do agendamento original
  → `AppointmentFactory.buildAndSave`.

**Trade-offs Accepted**: nenhum — é extração de duplicação real, não
abstração especulativa (três chamadores de verdade, não hipotéticos).

### DD-6: `Appointment.cancelByOwner()` — sem restrição de horário

**Contexto**: `Appointment.cancel(Instant agora)` (TODO-007) absorve sem
efeito uma ação sobre agendamento cujo horário já passou (BR-4 daquela
feature) — regra certa para o cliente, errada para o dono corrigindo um
erro ou registrando um cancelamento tardio (BR-2 desta feature).

**Decisão**: método novo, sem parâmetro de tempo:
```java
public Appointment cancelByOwner() {
    if (status == AppointmentStatus.CANCELLED) {
        return this; // idempotente, mesmo raciocínio de cancel()
    }
    return new Appointment(id, tenantId, ..., AppointmentStatus.CANCELLED, ...);
}
```
Único guard: já `CANCELLED` (estado terminal, idempotência) — nenhuma
checagem de `startsAt`.

**Opções Consideradas**:
- **A — Reaproveitar `cancel(Instant)` passando um instante artificial**
  (ex.: `startsAt.minusSeconds(1)`): funcionaria por acidente, mas
  esconde a intenção real atrás de um valor que não significa nada —
  o próximo desenvolvedor teria que ler a implementação para entender
  por que um instante "de mentira" está ali.
- **B (selecionada)**: método novo, nome diferente, sem parâmetro —
  a assinatura já diz o que a regra é.

**Trade-offs Accepted**: `Appointment` ganha um segundo método de
cancelamento — aceitável, porque as duas regras são genuinamente
diferentes (BR-4 da TODO-007 vs. BR-2 desta feature), não a mesma regra
escrita duas vezes.

### DD-7: `CancelAppointmentByOwnerUseCase` — interface nova, não reaproveita `CancelAppointmentUseCase`

**Contexto**: as duas portas teriam a assinatura idêntica
(`void cancel(UUID)`), mas semânticas diferentes por trás.

**Decisão**: interface nova e distinta. Duas implementações da mesma
interface `CancelAppointmentUseCase` (uma para cada semântica) exigiriam
`@Qualifier` em todo ponto de injeção para o Spring não recusar por
ambiguidade — e o nome do qualifier teria que carregar a mesma
informação que o nome de uma interface separada já carrega de graça.

**Trade-offs Accepted**: duas interfaces quase idênticas na assinatura —
aceitável, o nome de cada uma já documenta a diferença de regra sem
precisar abrir a implementação.

### DD-8: `ProfessionalAgendaHandler` implementa quatro portas numa classe só

**Contexto**: `ViewAgendaUseCase`, `CreateAppointmentManuallyUseCase`,
`CancelAppointmentByOwnerUseCase` e `RescheduleAppointmentUseCase`
compartilham as mesmas dependências (`AppointmentRepository`,
`ServiceOfferingDirectory`, `CustomerDirectory`, `ProfessionalDirectory`)
e a mesma resolução por tenant.

**Decisão**: uma classe só — segunda ocorrência do padrão já estabelecido
em `ManageAppointmentHandler` (DD-2 da TODO-007). Com duas ocorrências,
vale registrar em `PATTERNS.md` durante o build: "quando várias portas
in compartilham dependência e resolução, uma classe só é preferível a
uma por porta".

**Trade-offs Accepted**: mesmo já aceito na TODO-007 — desvio deliberado
do precedente "uma classe por porta", documentado no Javadoc da classe.

### DD-9: Reagendar cria o novo antes de cancelar o antigo

**Contexto**: se o reagendamento cancelasse o antigo primeiro e a
criação do novo falhasse (exclusion constraint, horário ocupado), o
cliente ficaria sem nenhum agendamento válido.

**Decisão**: `ProfessionalAgendaHandler.reschedule` — dentro da mesma
transação (`@Transactional`): (1) resolve o agendamento original por
tenant+id (BR-7); (2) resolve a nova oferta por tenant+id; (3)
`AppointmentFactory.buildAndSave` com o `customerId` do original; (4) só
se o passo 3 não lançar `SlotUnavailableException`, chama
`antigo.cancelByOwner()` e persiste via `updateStatus` (mesmo mecanismo
de `ManageAppointmentHandler`, TODO-007).

**Trade-offs Accepted**: se a transação inteira for revertida por
qualquer motivo depois do passo 3, o novo `Appointment` também some
(rollback) — comportamento correto, é uma transação só, não duas ações
independentes.

### DD-10: Formulário de criar/reagendar usa um dropdown único, sem cascata

**Contexto**: ADR 0007 já decidiu Thymeleaf server-side sem API REST,
sem JavaScript nas telas públicas. A mesma filosofia vale aqui: um
dropdown "profissional → serviço" dependente exigiria JS (ou um
`GET` extra por troca de profissional).

**Decisão**: um único `<select>` com todas as ofertas ativas do tenant
(`ServiceOfferingDirectory.listActive()`, DD-2), rótulo
"Profissional — Serviço (duração, preço)" — o dono já sabe quem
quer marcar, escolher direto é mais rápido que dois cliques em cascata.

**Trade-offs Accepted**: dropdown cresce linearmente com o número de
ofertas do tenant — aceitável no piloto (poucos profissionais, poucas
ofertas); se crescer muito, filtrar por profissional escolhido antes
vira uma reavaliação futura.

### DD-11: Confirmar presença pelo painel reaproveita `ConfirmAppointmentUseCase` sem mudança

**Contexto**: US-5 pede exatamente o mesmo mecanismo do cliente
(TODO-007) — mesma idempotência, mesma restrição de horário (diferente
do cancelamento desta feature, que é BR-2, sem restrição).

**Decisão**: `AgendaController` injeta `ConfirmAppointmentUseCase`
diretamente — nenhuma classe nova, nenhum método novo em
`ManageAppointmentHandler`.

**Trade-offs Accepted**: nenhum.

---

## Existing Data & Migrations

Nenhuma migration nova. `Appointment`/`AppointmentStatus` (`V8`,
TODO-006) já suportam todos os status que esta feature alcança;
nenhuma tabela nova, nenhuma coluna nova.

---

## Data Model

**`Appointment`** (`scheduling.domain`, editado) — ganha:
```java
public Appointment cancelByOwner() { ... }  // DD-6
```

**`ActiveOfferingRef`** (`catalog.api`, NOVO):
```java
public record ActiveOfferingRef(
        UUID id, UUID professionalId, String professionalName,
        String serviceName, int durationMinutes, String priceFormatted) {}
```

**`CustomerRef`** (`customer.api`, editado):
```java
public record CustomerRef(UUID id, String name, String phone) {}
```

**`AgendaEntry`** (`scheduling.application.port.in`, NOVO — uma linha da
lista de agenda, já com nomes resolvidos):
```java
public record AgendaEntry(
        UUID id, String customerName, String customerPhone, String serviceName,
        Instant startsAt, Instant endsAt, AppointmentStatus status,
        boolean canConfirm, boolean canCancel, boolean canReschedule) {}
```
`canConfirm` = `status == SCHEDULED && agora.isBefore(startsAt)` (mesma
regra de `ManageAppointmentHandler`, TODO-007). `canCancel` e
`canReschedule` = `status != CANCELLED` (BR-2: sem restrição de
horário para o dono).

**`RescheduleAppointmentCommand`** (`scheduling.application.port.in`,
NOVO):
```java
public record RescheduleAppointmentCommand(
        UUID appointmentId, UUID newServiceOfferingId, Instant newStartsAt) {}
```

---

## Cross-Context API Contracts

### `catalog.api.ServiceOfferingDirectory` (editado — DD-2)

| Método | Entrada | Saída | Tenant |
|---|---|---|---|
| `listActive` (NOVO) | — | `List<ActiveOfferingRef>` | `TenantContext.require()` interno |

### `customer.api.CustomerDirectory` (editado — DD-4)

| Método | Entrada | Saída | Tenant |
|---|---|---|---|
| `findByIds` (NOVO) | `Collection<UUID>` | `List<CustomerRef>` | `TenantContext.require()` interno |

### `scheduling.application.port.in` (NOVO — DD-8)

```java
public interface ViewAgendaUseCase {
    List<AgendaEntry> handle(UUID professionalId, LocalDate date);
}

public interface CreateAppointmentManuallyUseCase {
    BookedAppointment create(BookAppointmentCommand command); // reaproveita o record da TODO-006
}

public interface CancelAppointmentByOwnerUseCase {
    void cancel(UUID appointmentId);
}

public interface RescheduleAppointmentUseCase { // ja normativo no glossario
    BookedAppointment reschedule(RescheduleAppointmentCommand command);
}
```

### `scheduling.application.port.out.AppointmentRepository` (editado — DD-3)

```java
List<Appointment> findByTenantIdAndProfessionalIdAndDate(TenantId tenantId, UUID professionalId, LocalDate date); // NOVO
```

### Web Routes (Thymeleaf server-side, ADR 0007 — sem API REST)

| Rota | Método | O que faz |
|---|---|---|
| `/admin/agenda` | GET | Lista do dia (`?profissionalId=&data=`, padrão: primeiro profissional ativo, hoje) |
| `/admin/agenda/novo` | GET | Formulário de criar agendamento manual |
| `/admin/agenda/novo` | POST | Cria (`CreateAppointmentManuallyUseCase`) |
| `/admin/agenda/agendamentos/{id}/confirmar` | POST | Confirma presença (`ConfirmAppointmentUseCase`, sem mudança) |
| `/admin/agenda/agendamentos/{id}/cancelar` | POST | Cancela sem restrição de horário (`CancelAppointmentByOwnerUseCase`) |
| `/admin/agenda/agendamentos/{id}/reagendar` | GET | Formulário de reagendar |
| `/admin/agenda/agendamentos/{id}/reagendar` | POST | Reagenda (`RescheduleAppointmentUseCase`) |

Todas sob `anyRequest().hasRole("OWNER")` (DD-1, sem mudança em
`SecurityConfig`). Id de outro tenant ou inexistente → mesmo tratamento
de 404 já usado em `AppointmentController` (TODO-007).

---

## Security

Primeira tela admin de `scheduling` — mesma proteção que
`organization`/`catalog` já usam:

- `tenantId` nunca vem do formulário (BR-5) — vem da sessão `OWNER`
  autenticada, via `TenantContext`, sem mudança em `TenantContextFilter`.
- Profissional/oferta/agendamento revalidados contra esse tenant (BR-7)
  antes de qualquer leitura ou gravação — mesmo padrão de
  `ServiceOfferingDirectory.find`/`ProfessionalDirectory.find`.
- Overbooking impedido pelo banco (BR-1/ADR 0005), também para criação
  manual e reagendamento — nenhuma exceção para o dono.
- CSRF ativo em todos os formulários novos (padrão do projeto, nenhuma
  rota `/admin/**` desliga CSRF).
- XSS: nome/telefone do cliente, nome do profissional/serviço exibidos
  só via `th:text`.
- Nenhum segredo novo.

---

## Performance

Visão de agenda: uma consulta de `Appointment` por profissional+dia
(DD-3) + uma consulta de profissionais ativos (para o dropdown) + uma
consulta de clientes em lote (DD-4, nunca um por agendamento) — fixo,
sem crescer com o número de agendamentos além de uma única consulta
maior. Criar/reagendar: mesma ordem de grandeza de `BookAppointmentHandler`
(uma consulta de oferta + um get-or-create de cliente + um `INSERT`),
mais — no caso de reagendar — um `UPDATE` de status (mesmo `updateStatus`
de dois campos da TODO-007, não um `save()` completo).

---

## Testing Strategy

| Camada | Arquivo | Cobre |
|---|---|---|
| Domínio puro | `AppointmentTest` (editado) | `cancelByOwner()`: transição normal, idempotência, **sem** restrição de horário |
| Aplicação (mocks) | `ProfessionalAgendaHandlerTest` (NOVO) | as 4 portas: view (canConfirm/canCancel/canReschedule), create sem teto, cancelByOwner sem restrição de horário, reschedule (ordem cria-depois-cancela, revalidação por tenant) |
| Aplicação (mocks) | `BookAppointmentHandlerTest` (editado) | continua verde após extrair `AppointmentFactory` — mesmo comportamento observável |
| Web (`@WebMvcTest`) | `AgendaControllerTest` (NOVO) | as 7 rotas, 404 de tenant errado, formulário de criar/reagendar com dropdown único |
| Integração (Testcontainers) | `AgendaProfissionalIT` (NOVO) | E2E-1 a E2E-7 da spec funcional, incluindo a concorrência painel×link público (E2E-2, exige Postgres real) |

---

## Implementation Locations

```
src/main/java/com/agendaia/scheduling/
├── domain/Appointment.java                                [EDITADO — cancelByOwner(), DD-6]
├── application/
│   ├── port/in/
│   │   ├── ViewAgendaUseCase.java                          [NOVO]
│   │   ├── AgendaEntry.java                                [NOVO]
│   │   ├── CreateAppointmentManuallyUseCase.java           [NOVO]
│   │   ├── CancelAppointmentByOwnerUseCase.java            [NOVO]
│   │   ├── RescheduleAppointmentUseCase.java               [NOVO]
│   │   └── RescheduleAppointmentCommand.java               [NOVO]
│   ├── port/out/AppointmentRepository.java                 [EDITADO — findByTenantIdAndProfessionalIdAndDate, DD-3]
│   ├── AppointmentFactory.java                             [NOVO — pacote-privado, DD-5]
│   ├── BookAppointmentHandler.java                         [EDITADO — delega para AppointmentFactory]
│   └── ProfessionalAgendaHandler.java                      [NOVO — DD-8]
└── adapter/
    ├── out/persistence/
    │   ├── AppointmentJpaRepository.java                   [EDITADO]
    │   └── AppointmentPersistenceAdapter.java               [EDITADO]
    └── in/web/AgendaController.java                         [NOVO]

src/main/java/com/agendaia/catalog/
├── api/
│   ├── ServiceOfferingDirectory.java                       [EDITADO — listActive(), DD-2]
│   └── ActiveOfferingRef.java                               [NOVO]
├── application/ServiceOfferingDirectoryHandler.java          [EDITADO]
└── application/port/out/ServiceOfferingRepository.java       [EDITADO — findByTenantIdAndActiveTrueOrderByCreatedAtAsc]

src/main/java/com/agendaia/customer/
├── api/CustomerDirectory.java                                [EDITADO — findByIds, DD-4]
├── api/CustomerRef.java                                      [EDITADO — + phone]
├── application/CustomerDirectoryHandler.java                  [EDITADO]
└── application/port/out/CustomerRepository.java                [EDITADO — findByTenantIdAndIdIn]

src/main/resources/templates/admin/
├── agenda.html                                                [NOVO]
├── agenda-novo.html                                           [NOVO]
└── agenda-reagendar.html                                      [NOVO]

src/test/java/com/agendaia/
├── scheduling/domain/AppointmentTest.java                      [EDITADO]
├── scheduling/application/ProfessionalAgendaHandlerTest.java   [NOVO]
├── scheduling/application/BookAppointmentHandlerTest.java      [EDITADO — so revalida apos extracao]
├── scheduling/adapter/in/web/AgendaControllerTest.java         [NOVO]
└── scheduling/AgendaProfissionalIT.java                        [NOVO]
```

---

## References

- [ADR 0005](../../../docs/architecture/adr/0005-exclusion-constraint-contra-overbooking.md) — vale também para criação manual e reagendamento
- [ADR 0011](../../../docs/architecture/adr/0011-ciclo-de-vida-dos-dados.md) — reagendar preserva o registro original (DD-9)
- [ADR 0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — regime completo de `scheduling`
- `docs/domain/glossary.md` — `RescheduleAppointmentUseCase` já normativo
- `sdd/PATTERNS.md` — API entre contextos é grossa; uma classe por porta é preferência, não regra absoluta (2ª exceção documentada nesta feature)
