# pagina-publica-agendamento - Technical Spec

**Feature**: pagina-publica-agendamento
**Status**: approved
**Data**: 2026-09-05
**Aprovado por**: Elton Marques em 2026-09-05T14:14:10Z

---

## Executive Summary

Primeira escrita real em `scheduling` (`Appointment`, regime completo do
ADR 0002) e primeiro uso real de `customer` (`Customer`, regime CRUD).
Primeira rota HTTP pública do projeto, sem autenticação: `/b/{slug}`.

O tenant deixa de ter uma única via de resolução (sessão autenticada) —
`TenantContextFilter` ganha uma segunda via, pelo slug da URL, exatamente
como o próprio javadoc de `TenantContext` já previa desde o ADR 0004
("a página pública resolverá pelo slug da URL, quando ela existir").

A garantia real contra overbooking é a exclusion constraint do ADR 0005,
aplicada literalmente — a extensão `btree_gist` já foi habilitada
preventivamente na `V1`.

---

## Architecture Overview

```
Requisição pública
        │
        ▼
┌───────────────────────────────────────────────────────────────────┐
│ platform.tenant.TenantContextFilter (EDITADO, DD-3)                │
│   não autenticado + path "/b/{slug}/**"                            │
│     → organization.api.BusinessDirectory.findBySlug(slug)          │
│     → achou: TenantContext.set(tenantId) + atributo de requisição  │
│       "resolvedBusiness" (BusinessRef) — DD-4, evita 2ª consulta   │
│     → não achou: segue sem tenant (controller decide 404)          │
└───────────────────────────────────────────┬───────────────────────┘
                                             ▼
┌───────────────────────────────────────────────────────────────────┐
│ scheduling.adapter.in.web.PublicBookingController (NOVO)           │
│   GET  /b/{slug}                          → lista serviços          │
│   GET  /b/{slug}/servicos/{serviceId}     → lista profissionais     │
│   GET  /b/{slug}/ofertas/{offeringId}     → lista horários (data=)  │
│   POST /b/{slug}/ofertas/{offeringId}     → confirma (DD-7 rate     │
│                                              limit + honeypot)      │
└───┬─────────────────────┬──────────────────────┬───────────────────┘
    │ lê                  │ lê                    │ escreve
    ▼                     ▼                        ▼
catalog.api          scheduling.application   scheduling.application
 ServiceDirectory      GetAvailableSlots        BookAppointmentUseCase
 ServiceOfferingDirectory  UseCase (TODO-005,      (NOVO, DD-5)
  (ambos EDITADOS,          sem mudança)              │
   DD-1: métodos de                                    │ dentro da mesma
   listagem novos)                                     │ transação, chama
                                                        ▼
                                          customer.api.CustomerDirectory
                                           .findOrCreate(name, phone)
                                           (NOVO contexto, regime CRUD)
                                                        │
                                                        ▼
                                          grava Appointment (ADR 0005:
                                          exclusion constraint decide
                                          de verdade, não a aplicação)
```

**Fluxo de confirmação** (`POST`):

1. `PublicBookingController` recusa antes de qualquer outra coisa se:
   honeypot preenchido (BR-7) → sucesso genérico silencioso; rate limit
   de IP excedido (BR-8) → erro genérico.
2. Revalida `serviceOfferingId` contra o tenant resolvido pelo slug —
   reaproveita `ServiceOfferingDirectory.find` (TODO-005, já existente).
3. Chama `BookAppointmentHandler.handle(BookAppointmentCommand)`.
4. Handler resolve/cria `Customer` via `customer.api.CustomerDirectory`
   (BR-3), conta agendamentos futuros ativos daquele telefone (BR-9),
   grava `Appointment` `SCHEDULED` com retrato de preço/duração (BR-2).
5. `DataIntegrityViolationException` da exclusion constraint é traduzida
   em `SlotUnavailableException` (BR-4/US-6) pelo adapter de persistência.
6. Controller redireciona (PRG) para a tela de sucesso.

---

## Design Decisions

### DD-1: `catalog.api` ganha listagem por serviço, não só busca por id

**Contexto**: `ServiceOfferingDirectory.find(id)` (TODO-005) resolve uma
oferta específica — a página pública precisa do inverso: dado um
`serviceId`, listar as ofertas ativas (com nome do profissional e preço
já resolvidos), para o cliente escolher.

**Opções Consideradas**:
- **A — Novo contrato `PublicCatalogDirectory`** com todas as operações
  públicas de `catalog`: mais um nome para decorar, sem ganho real.
- **B (selecionada) — Estender os contratos existentes**: `catalog.api`
  ganha `ServiceDirectory.listActive()` (lista de serviços) e
  `ServiceOfferingDirectory` ganha `listActiveByService(UUID serviceId)`
  (ofertas daquele serviço, com nome do profissional resolvido).

**Trade-offs Accepted**: `ServiceOfferingDirectory` acumula duas
responsabilidades (buscar uma, listar várias) — aceitável porque ambas
seguem exatamente a mesma regra ("oferta ativa do tenant"), só a
granularidade muda.

**Rationale**: reaproveita o precedente de `ListServiceOfferingsHandler`
(TODO-003), que já cruza `ServiceOfferingRepository` +
`ServiceRepository` + `organization.api.ProfessionalDirectory.listActive()`
**uma vez só** (PATTERNS.md, "API entre contextos é grossa"). O handler
novo (`ServiceOfferingDirectoryHandler`, editado) reaproveita a mesma
técnica, filtrando por `serviceId`.

**Contrato**:
```java
// catalog.api
public interface ServiceDirectory {
    List<PublicServiceRef> listActive();
}
public record PublicServiceRef(UUID id, String name) {}

// catalog.api.ServiceOfferingDirectory ganha:
List<PublicOfferingRef> listActiveByService(UUID serviceId);
public record PublicOfferingRef(
        UUID id, UUID professionalId, String professionalName, int durationMinutes, String priceFormatted) {}
```

### DD-2: `customer` nasce em regime CRUD, não regime completo

**Contexto**: ADR 0002 reserva o regime completo (domínio Java puro +
entidade JPA separada) para `scheduling`, o core domain. `Customer` é
nome + telefone — sem invariante complexa o bastante para justificar
duas classes e um mapper.

**Decisão**: `Customer` é `@Entity` que **é** o modelo (mesmo regime de
`Business`/`Professional`/`Service`), com `findOrCreate` como única
operação de escrita, get-or-create pelo par `(tenantId, phone)`.

**Trade-offs Accepted**: nenhum — é a aplicação direta da regra já
documentada em `PATTERNS.md` ("rigor proporcional ao subdomínio").

### DD-3: Resolução de tenant por slug estende `TenantContextFilter`, não um filtro novo

**Contexto**: o javadoc de `TenantContext` já previa duas rotas de
resolução desde o ADR 0004 ("a área administrativa resolve pela sessão
autenticada; a página pública resolverá pelo slug da URL"). Falta só
implementar a segunda.

**Opções Consideradas**:
- **A — Filtro novo, dedicado à rota pública**: duas classes fazendo a
  mesma coisa (popular `TenantContext`), com duas ordens (`@Order`) para
  coordenar — mais uma peça para o próximo desenvolvedor descobrir que
  existe.
- **B — Controller resolve o slug e chama `TenantContext.set()` ele
  mesmo**: quebra a garantia estabelecida em todo o projeto até aqui de
  que `TenantContext` só é populado por um filtro, nunca por código de
  aplicação — abre espaço para outro controller esquecer de limpar o
  contexto ou fazer errado.
- **C (selecionada) — Estender `TenantContextFilter`**: uma segunda via
  de resolução no mesmo filtro, condicionada a "não autenticado + path
  bate com `/b/{slug}/**`". Continua havendo um único lugar que decide
  "qual é o tenant desta requisição".

**Decisão**: `TenantContextFilter.resolverDaSessao()` passa a
`resolverDaSessaoOuDoSlug()`: tenta sessão primeiro (comportamento
inalterado); se não autenticado e o path começa com `/b/`, extrai o
segundo segmento como slug e chama
`organization.api.BusinessDirectory.findBySlug(slug)` via
`ObjectProvider` (mesmo padrão de `AccessGuardFilter`/`SecurityConfig`:
filtro é `@Component` global, entra em fatias `@WebMvcTest` de outros
contextos que não sobem `organization` — provider vazio segue sem
tenant, em vez de quebrar o contexto de teste).

**Trade-offs Accepted**: `platform.tenant` (que hoje só depende de
`platform.security`) passa a depender de `organization.api` — primeira
vez que `platform` importa outro contexto. Aceitável: `platform` é
`Type.OPEN` sem `allowedDependencies` declarado (não é whitelist), e o
próprio `billing` já estabeleceu o precedente de um contexto de suporte
lendo `organization.api` sem tenant implícito.

**Slug não encontrado**: `TenantContext` fica vazio, a cadeia segue —
`PublicBookingController` verifica `TenantContext.current().isEmpty()`
no início de cada rota e lança `ResponseStatusException(NOT_FOUND)`
explicitamente (não é o filtro que decide 404; rotear é responsabilidade
do controller).

### DD-4: `BusinessRef` resolvido pelo filtro vira atributo de requisição, reaproveitado por `LayoutAdvice` — uma classe só, não duas

**Contexto**: a página pública precisa mostrar o nome do estabelecimento
em todo template. Uma segunda consulta por `findBySlug` no controller ou
no `@ControllerAdvice` duplicaria a mesma leitura que o filtro acabou de
fazer.

**Decisão**: `TenantContextFilter`, ao resolver pelo slug, guarda
`request.setAttribute("resolvedBusiness", businessRef)`.

**Achado durante a implementação (TASK-007)**: o plano original propunha
um `platform.web.PublicLayoutAdvice` novo, irmão de `LayoutAdvice`. Os
dois seriam `@ControllerAdvice` **globais** (sem `basePackages`/
`assignableTypes`) expondo o **mesmo** `@ModelAttribute("businessName")`
— a ordem de execução entre advices sem `@Order` explícito não é
garantida, então um sobrescreveria o valor do outro de forma
não-determinística em qualquer rota (admin OU pública). Corrigido antes
do commit: `LayoutAdvice` passou a checar as duas fontes na mesma
`@ModelAttribute` — sessão autenticada primeiro, senão o atributo de
requisição — mesma técnica de "duas vias, uma classe" que
`TenantContextFilter` já usa para o próprio `TenantContext`.

**Trade-offs Accepted**: `platform.web.LayoutAdvice` (que antes só
conhecia `AuthenticatedUser`) passa a também importar `organization.api.
BusinessRef` — aceitável pelo mesmo argumento do DD-3 (`platform` é
`Type.OPEN` sem `allowedDependencies` declarado).

### DD-5: `BookAppointmentHandler` resolve o `Customer` e grava o `Appointment` na mesma transação

**Contexto**: `Appointment.customerId` precisa de um `Customer` que pode
não existir ainda. `scheduling` e `customer` são contextos diferentes —
PATTERNS.md diz "não escreva em dois contextos na mesma transação", mas
também estabelece que "referência cruzada é UUID solto, sem chave
estrangeira" e que contexto fala com `api` de outro.

**Opções Consideradas**:
- **A — Duas chamadas de aplicação no controller**: o controller chama
  primeiro `customer.api` para resolver/criar o `Customer`, pega o id, e
  só depois chama `scheduling.application` com o `customerId` pronto.
  Mantém cada handler com uma transação própria e mais curta, mas exige
  que o controller (camada web) orquestre uma sequência que é, na
  prática, uma única operação de negócio ("agendar") — se a segunda
  chamada falhar, a primeira já fez commit e cria um `Customer` órfão
  (inofensivo — `Customer` sem `Appointment` é só um cadastro futuro —
  mas incoerente com a ideia de uma ação atômica).
- **B (selecionada) — `BookAppointmentHandler` chama
  `customer.api.CustomerDirectory.findOrCreate()` dentro do próprio
  método `@Transactional`**: uma única transação cobre as duas escritas
  (mesmo banco físico, ADR 0001 — não é problema técnico, é convenção de
  design). Mesmo precedente já em produção:
  `BillingAccountService.criarConta()` (TODO-009) já cruza
  `organization.api` dentro de uma transação de `billing` para resolver
  dado que falta antes de gravar.

**Decisão**: Opção B. `customer.api.CustomerDirectory.findOrCreate` é
chamado de dentro de `BookAppointmentHandler`, antes de montar o
`Appointment`.

**Trade-offs Accepted**: `scheduling.application` ganha uma dependência
de escrita indireta em `customer` (via `api`, nunca direto na tabela) —
documentado aqui para não ser lido como um desvio silencioso da regra
geral do `PATTERNS.md`. A leitura "leitura é chamada, escrita é evento"
continua valendo para efeitos **colaterais** entre contextos (ex.:
notificar); resolver um id que falta antes de uma gravação local é a
mesma categoria de operação que o get-or-create de billing já usa.

### DD-6: Exclusion constraint aplicada literalmente; tradução no adapter

**Decisão**: o SQL do ADR 0005 é copiado sem modificação (ver
"Existing Data & Migrations"). `AppointmentPersistenceAdapter.save`
captura `DataIntegrityViolationException` e relança
`SlotUnavailableException` (`extends DomainException`, sem `field` —
o erro não é de um campo específico do formulário).

**Trade-offs Accepted**: nenhum — é exatamente o desenho já decidido pelo
ADR, esta feature só implementa.

### DD-7: Honeypot e rate limit vivem em `scheduling.adapter.in.web`, não em `platform`

**Contexto**: são defesas específicas desta feature — nenhuma outra rota
do projeto tem formulário público hoje.

**Decisão**: honeypot é um campo a mais em `PublicBookingRequest`
(string que deve chegar vazia; um humano nunca vê o campo, via CSS). Rate
limit é um bean simples em `scheduling.adapter.in.web`
(`BookingRateLimiter`), um `ConcurrentHashMap<String, Deque<Instant>>`
chaveado por IP, sliding window de 10 minutos, sem dependência externa.

**Trade-offs Accepted**: contador em memória — reinicia ao reiniciar a
aplicação, mesmo trade-off já aceito para sessão (DEBT-013). Aceitável
no piloto (instância única). Não é `platform` porque `PATTERNS.md`
determina que promoção para contexto de suporte acontece "quando ele
acumula a terceira regra de negócio de verdade" — hoje é a primeira.

### DD-8: Fluxo multi-etapa via `GET` com querystring, sem fragmento/JS

**Contexto**: ADR 0007 já decidiu Thymeleaf server-side sem API REST.

**Decisão**: cada etapa (serviço → profissional/oferta → horário) é uma
página `GET` separada, navegação por link/formulário comum. A escolha de
data é um parâmetro de query (`?data=2026-09-10`) na tela de horários,
recarregando a página — sem JavaScript, sem endpoint de fragmento.

**Trade-offs Accepted**: trocar de data recarrega a página inteira (sem
atualização parcial). Aceitável: é a mesma experiência de qualquer link
compartilhado por WhatsApp, e evita introduzir JS neste MVP.

### DD-10: `GetAvailableSlotsHandler` (TODO-005) passa a excluir agendamentos já ativos

**Achado durante a implementação (TASK-005/TASK-006)**: a TODO-005 já
documentava explicitamente que "o cálculo não desconta nenhum agendamento
por enquanto — `Appointment` não existe ainda". Como esta feature é
exatamente quem cria `Appointment`, essa lacuna precisava ser fechada
aqui — sem isso, a listagem de horários livres nunca refletiria uma
reserva já feita, e o cliente só descobriria o conflito ao tentar
confirmar (a exclusion constraint continuaria protegendo contra
overbooking de verdade, mas a experiência de listagem ficaria enganosa
assim que a agenda de um profissional começasse a ter agendamentos reais).

**Decisão**: `AppointmentRepository` ganha `findOccupiedRanges(TenantId,
UUID professionalId, LocalDate) -> List<TimeRange>`, com a mesma técnica
de recorte às bordas do dia que `AvailabilityDirectoryHandler#blocksFor`
já usa para `TimeOff` (organization). `GetAvailableSlotsHandler` mescla
o resultado com os bloqueios de `TimeOff` antes de chamar
`SlotCalculator.calculate` — um agendamento ativo é, do ponto de vista do
cálculo, só mais um intervalo bloqueado; nenhuma mudança na assinatura
de `SlotCalculator` (domínio puro) foi necessária.

**Trade-offs Accepted**: `GetAvailableSlotsHandler` (TODO-005) ganha uma
quarta dependência (`AppointmentRepository`, de `scheduling.application`,
mesmo contexto — não atravessa fronteira nenhuma) e um teste existente
(`GetAvailableSlotsHandlerTest`) precisou de um mock a mais. Aceitável:
é a mesma classe corrigindo a lacuna que a própria spec funcional da
TODO-005 já apontava para esta feature.

### DD-11: `recarregarTelaDeHorarios` precisa da data do formulário, não de "hoje"

**Achado durante o teste manual do TASK-010**: a primeira versão de
`PublicBookingController.confirmar` recarregava a tela de erro sempre
com `LocalDate.now()`, ignorando a data que o cliente efetivamente
escolhia (ex.: uma segunda-feira futura). Como a jornada do profissional
pode não cobrir "hoje", isso fazia a tela de erro mostrar "sem horários
disponíveis nesta data" — escondendo a mensagem de erro real (ex.:
`SlotUnavailableException`), porque o bloco de erro global vive dentro
do `<form>`, e o `<form>` só é renderizado quando a lista de horários não
está vazia.

**Decisão**: a data escolhida vira um campo `hidden` no formulário de
confirmação (`horarios.html`), lida via `@RequestParam(name = "data")`
em `confirmar` e propagada para `recarregarTelaDeHorarios` — a mesma
data usada para consultar `GetAvailableSlotsUseCase` originalmente é a
mesma usada para recarregar a tela em caso de erro.

**Trade-offs Accepted**: nenhum — era um bug, não uma escolha de design.

### DD-9: Teto por telefone (BR-9) contado dentro do handler, nova query em `AppointmentRepository`

**Decisão**:
```java
@Query("""
        select count(a) from AppointmentJpaEntity a
        where a.tenantId = :tenantId and a.customerId = :customerId
          and a.status = 'SCHEDULED' and a.startsAt > :agora
        """)
long countFutureActive(UUID tenantId, UUID customerId, Instant agora);
```
`BookAppointmentHandler` chama antes de gravar; 3 ou mais → lança
`PhoneAppointmentLimitExceededException` (`extends DomainException`,
`field = "phone"`).

**Trade-offs Accepted**: nenhum.

---

## Existing Data & Migrations

**`V7__customer_create_customer.sql`**:
```sql
CREATE TABLE customer (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    anonymized_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, phone)
);
```

**`V8__scheduling_create_appointment.sql`** (extensão `btree_gist` já
habilitada na `V1`):
```sql
CREATE TABLE appointment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    professional_id UUID NOT NULL,
    service_offering_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    duration_minutes INT NOT NULL,
    price_cents BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- ADR 0005, literal.
ALTER TABLE appointment ADD CONSTRAINT appointment_no_overlap
  EXCLUDE USING gist (
    tenant_id       WITH =,
    professional_id WITH =,
    tstzrange(starts_at, ends_at, '[)') WITH &&
  ) WHERE (status IN ('SCHEDULED', 'CONFIRMED'));

CREATE INDEX appointment_customer_idx ON appointment (tenant_id, customer_id, status);
```

Sem FK entre `appointment` e `customer`/`service_offering`/`professional`
— UUID solto, mesmo padrão de `service_offering.professional_id`.

---

## Data Model

**`Customer`** (`customer.domain`, entidade = modelo, regime CRUD):
campos `id`, `tenantId`, `name`, `phone` (E.164), `anonymizedAt`
(nulo nesta feature, ver glossário/ADR 0011), `createdAt`, `updatedAt`.
Criação/reaproveitamento só por `findOrCreate` — sem `register()` público
avulso, porque não existe "cadastro de cliente" fora do fluxo de
agendamento nesta feature.

**`Appointment`** (`scheduling.domain`, Java puro, regime completo):

```java
public final class Appointment {
    // id, tenantId, professionalId, serviceOfferingId, customerId,
    // status (AppointmentStatus), startsAt, endsAt (Instant),
    // retrato: serviceName, durationMinutes, price (Money)

    public static Appointment schedule(
            TenantId tenantId, UUID professionalId, UUID serviceOfferingId, UUID customerId,
            String serviceName, int durationMinutes, Money price, Instant startsAt, Instant endsAt) { ... }
    // valida: nenhum campo nulo, endsAt depois de startsAt
}

public enum AppointmentStatus { SCHEDULED, CONFIRMED, CANCELLED, NO_SHOW } // só SCHEDULED alcançável aqui
```

`AppointmentJpaEntity` (`scheduling.adapter.out.persistence`) espelha os
mesmos campos; `AppointmentMapper` converte nos dois sentidos — mesmo
regime de todo agregado de `scheduling` (ADR 0002).

---

## Cross-Context API Contracts

### `organization.api.BusinessDirectory` (editado)

| Método | Entrada | Saída | Tenant |
|---|---|---|---|
| `findBySlug` (NOVO) | `String slug` | `Optional<BusinessRef>` | Nenhum — é quem resolve o tenant |

### `catalog.api` (editado — DD-1)

| Contrato | Método | Entrada | Saída |
|---|---|---|---|
| `ServiceDirectory` (NOVO) | `listActive` | — | `List<PublicServiceRef>` |
| `ServiceOfferingDirectory` | `listActiveByService` (NOVO) | `UUID serviceId` | `List<PublicOfferingRef>` |

Tenant sempre `TenantContext.require()` interno (convenção geral).

### `customer.api.CustomerDirectory` (NOVO)

```java
public interface CustomerDirectory {
    /** Cria ou reaproveita pelo telefone dentro do tenant (BR-3). */
    UUID findOrCreate(String name, String phone);
}
```

### `scheduling.application.port.in.BookAppointmentUseCase` (NOVO)

```java
public record BookAppointmentCommand(
        UUID serviceOfferingId, Instant startsAt, String customerName, String customerPhone) {}

public record BookedAppointment(UUID id, String serviceName, LocalDateTime startsAt) {}

public interface BookAppointmentUseCase {
    BookedAppointment handle(BookAppointmentCommand command);
}
```

### Web Routes (Thymeleaf server-side, ADR 0007 — sem REST)

| Rota | Método | O que faz |
|---|---|---|
| `/b/{slug}` | GET | Lista serviços ativos |
| `/b/{slug}/servicos/{serviceId}` | GET | Lista ofertas (profissional + preço) daquele serviço |
| `/b/{slug}/ofertas/{offeringId}` | GET | Lista horários livres (`?data=`), formulário de confirmação |
| `/b/{slug}/ofertas/{offeringId}` | POST | Confirma — honeypot, rate limit, grava `Appointment` |
| `/b/{slug}/agendamentos/{id}` | GET | Tela de sucesso (resumo) |

Slug/ids inexistentes ou de outro tenant → `ResponseStatusException(NOT_FOUND)`
(slug) ou erro de validação tratado (ids revalidados contra o tenant,
BR-5).

---

## Security

Primeira rota pública sem sessão do projeto:

- `tenantId` nunca vem do formulário (BR-5) — resolvido só pelo slug,
  via `TenantContextFilter` (DD-3); `serviceOfferingId` recebido é
  revalidado contra esse tenant antes de qualquer leitura/gravação
  (reaproveita `ServiceOfferingDirectory.find`, que já devolve vazio
  para outro tenant, TODO-005).
- CSRF: Spring Security exige token em todo `POST` por padrão, inclusive
  rotas sem sessão — o formulário Thymeleaf (`th:action`) já inclui o
  token automaticamente, sem configuração extra. `SecurityConfig`
  precisa liberar `/b/**` em `permitAll()` (leitura e escrita, já que
  não há login) mantendo CSRF ativo.
- XSS: nome do cliente e nome do estabelecimento renderizados só via
  `th:text`, nunca `th:utext` — mesma convenção do resto do projeto.
- Overbooking impedido pelo banco (BR-4/ADR 0005), não pela aplicação —
  regra fundamental do `CLAUDE.md`.
- Honeypot (BR-7) e rate limit (BR-8) contêm abuso automatizado; teto por
  telefone (BR-9) contém abuso por um número real usado em excesso.
- Nenhum segredo novo — sem chave de API, sem credencial.
- Threat model formal deste fluxo fica para o DEBT-005 (já registrado no
  backlog), não bloqueia esta feature.

---

## Performance

Leitura reaproveita `GetAvailableSlotsHandler` (já otimizado, TODO-005) —
sem consulta nova no caminho de leitura de horários. Escrita é uma
transação por confirmação: no máximo uma consulta a `customer` (get-or-
create), uma contagem de agendamentos futuros (BR-9, índice
`(tenant_id, customer_id, status)`), e um `INSERT` em `appointment` (a
exclusion constraint é validação de índice, não uma segunda consulta
lógica). Sem N+1 em nenhuma das listagens (`listActiveByService` cruza
profissionais **uma vez só**, mesmo padrão de `ListServiceOfferingsHandler`).

---

## Testing Strategy

| Camada | Arquivo | Cobre |
|---|---|---|
| Domínio puro | `AppointmentTest` (scheduling) | `schedule()` valida campos, `AppointmentStatus` |
| Domínio puro | `CustomerTest` (customer) — se houver invariante além de JPA | validação de nome/telefone |
| Aplicação (mocks) | `BookAppointmentHandlerTest` | BR-2 (retrato), BR-3 (get-or-create via mock de `CustomerDirectory`), BR-9 (teto) |
| Aplicação (mocks) | `ServiceOfferingDirectoryHandlerTest` (editado) | `listActiveByService` cruza profissionais uma vez só |
| Web (`@WebMvcTest`) | `PublicBookingControllerTest` | honeypot, rate limit, CSRF, erro de validação vira campo, PRG |
| Integração (Testcontainers) | `AppointmentPersistenceAdapterIT` ou dentro do IT principal | `DataIntegrityViolationException` → `SlotUnavailableException` |
| Integração (Testcontainers) | `PaginaPublicaAgendamentoIT` | E2E-1 a E2E-7 da spec funcional, incluindo **duas threads/requisições concorrentes** para o mesmo horário (E2E-2, exige Postgres real — H2 não implementa a constraint, ADR 0005) |

---

## Implementation Locations

```
src/main/java/com/agendaia/customer/
├── package-info.java                                    [EDITADO — allowedDependencies: shared, platform]
├── domain/Customer.java                                 [NOVO]
├── application/port/out/CustomerRepository.java         [NOVO]
├── application/CustomerDirectoryHandler.java             [NOVO]
└── api/
    └── CustomerDirectory.java                            [NOVO]

src/main/java/com/agendaia/organization/
├── api/BusinessDirectory.java                            [EDITADO — findBySlug]
└── application/BusinessDirectoryHandler.java             [EDITADO]

src/main/java/com/agendaia/catalog/
├── api/
│   ├── ServiceDirectory.java                             [NOVO]
│   ├── PublicServiceRef.java                             [NOVO]
│   ├── ServiceOfferingDirectory.java                     [EDITADO — listActiveByService]
│   └── PublicOfferingRef.java                            [NOVO]
└── application/
    ├── ServiceDirectoryHandler.java                       [NOVO]
    └── ServiceOfferingDirectoryHandler.java                [EDITADO]

src/main/java/com/agendaia/scheduling/
├── package-info.java                                     [EDITADO — allowedDependencies ganha customer :: api]
├── domain/
│   ├── Appointment.java                                  [NOVO]
│   ├── AppointmentStatus.java                            [NOVO]
│   └── exception/
│       ├── SlotUnavailableException.java                 [NOVO]
│       └── PhoneAppointmentLimitExceededException.java   [NOVO]
├── application/
│   ├── port/in/BookAppointmentUseCase.java               [NOVO]
│   ├── port/in/BookAppointmentCommand.java               [NOVO]
│   ├── port/in/BookedAppointment.java                    [NOVO]
│   ├── port/out/AppointmentRepository.java               [NOVO]
│   └── BookAppointmentHandler.java                       [NOVO]
└── adapter/
    ├── out/persistence/
    │   ├── AppointmentJpaEntity.java                      [NOVO]
    │   ├── AppointmentMapper.java                         [NOVO]
    │   └── AppointmentPersistenceAdapter.java              [NOVO]
    └── in/web/
        ├── PublicBookingController.java                    [NOVO]
        ├── BookingRateLimiter.java                          [NOVO]
        └── request/PublicBookingRequest.java                [NOVO]

src/main/java/com/agendaia/platform/
├── tenant/TenantContextFilter.java                        [EDITADO — DD-3]
├── web/LayoutAdvice.java                                  [EDITADO — DD-4, checa sessão OU slug]
└── security/SecurityConfig.java                            [EDITADO — permitAll /b/**]

src/main/resources/db/migration/
├── V7__customer_create_customer.sql                       [NOVO]
└── V8__scheduling_create_appointment.sql                  [NOVO]

src/main/resources/templates/public/
├── catalogo.html                                          [NOVO]
├── profissionais.html                                     [NOVO]
├── horarios.html                                          [NOVO]
└── sucesso.html                                           [NOVO]

src/test/java/com/agendaia/
├── scheduling/domain/AppointmentTest.java                 [NOVO]
├── scheduling/application/BookAppointmentHandlerTest.java [NOVO]
├── catalog/application/ServiceOfferingDirectoryHandlerTest.java [EDITADO]
├── scheduling/adapter/in/web/PublicBookingControllerTest.java [NOVO]
└── scheduling/PaginaPublicaAgendamentoIT.java              [NOVO]
```

---

## References

- [ADR 0005](../../../docs/architecture/adr/0005-exclusion-constraint-contra-overbooking.md) — exclusion constraint, SQL literal
- [ADR 0004](../../../docs/architecture/adr/0004-multi-tenancy-por-discriminador.md) — duas vias de resolução de tenant
- [ADR 0007](../../../docs/architecture/adr/0007-thymeleaf-server-side-sem-api-rest.md) — sem API REST
- [ADR 0008](../../../docs/architecture/adr/0008-rota-publica-com-prefixo.md) — prefixo `/b/`
- [ADR 0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — regime completo de `scheduling`
- `docs/domain/glossary.md` — `Appointment`, `Customer`, armadilha #1 ("cliente" ambíguo)
- `sdd/PATTERNS.md` — rigor proporcional; API grossa entre contextos; gotcha de `allowedDependencies`
