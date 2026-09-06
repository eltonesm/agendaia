# confirmacao-e-cancelamento - Technical Spec

**Feature**: confirmacao-e-cancelamento
**Status**: approved
**Data**: 2026-09-06
**Aprovado por**: Elton Marques em 2026-09-06T13:52:19Z

---

## Executive Summary

Fecha o ciclo do cliente aberto pela TODO-006. O `AppointmentStatus`
criado naquela feature já previa `CONFIRMED` e `CANCELLED` — esta feature
é quem finalmente os alcança, através de dois métodos de domínio novos em
`Appointment` (`confirm`/`cancel`) e um controller novo que governa o
agendamento depois de criado.

A rota `/b/{slug}/agendamentos/{id}`, que a TODO-006 já criava mas usava
só para exibir um flash attribute, é repropositada: passa a fazer uma
consulta real, revalidada por tenant, e vira o único ponto de acesso ao
agendamento — para ver, confirmar presença, cancelar, baixar `.ics` e
abrir o WhatsApp do estabelecimento.

Nenhum mecanismo de token novo: o próprio `id` do `Appointment` (UUIDv7,
ADR 0009) já é opaco o bastante, e a defesa real é a mesma de todo o
projeto — revalidar contra o tenant resolvido pelo slug, nunca confiar no
id vindo do cliente.

---

## Architecture Overview

```
GET/POST /b/{slug}/agendamentos/{id}[/confirmar|/cancelar|/ics]
        │
        ▼
┌───────────────────────────────────────────────────────────────────┐
│ platform.tenant.TenantContextFilter (sem mudança, DD-3 da TODO-006)│
│   resolve tenant pelo slug — mesmo path prefixo "/b/"              │
└───────────────────────────────────────────┬───────────────────────┘
                                             ▼
┌───────────────────────────────────────────────────────────────────┐
│ scheduling.adapter.in.web.AppointmentController (NOVO, DD-5)       │
│   GET  .../agendamentos/{id}            → detalhes + status        │
│   POST .../agendamentos/{id}/confirmar  → SCHEDULED→CONFIRMED      │
│   POST .../agendamentos/{id}/cancelar   → →CANCELLED               │
│   GET  .../agendamentos/{id}/ics        → arquivo .ics             │
└───┬─────────────────────────────────────────────┬─────────────────┘
    │ lê/escreve                                    │ lê (nome p/ exibir)
    ▼                                                ▼
scheduling.application                    organization.api.ProfessionalDirectory (DD-8)
 ManageAppointmentHandler (NOVO, DD-2)     customer.api.CustomerDirectory (DD-8)
  implements AppointmentDetailsUseCase,
             ConfirmAppointmentUseCase,
             CancelAppointmentUseCase
    │
    ▼
Appointment.confirm(agora) / .cancel(agora)   (NOVO, domínio puro, DD-3)
    │
    ▼
AppointmentRepository.updateStatus(...)   (NOVO, DD-4 — UPDATE direto,
                                            não passa por save()/merge)
```

**Fluxo de confirmar/cancelar** (`POST`):

1. `AppointmentController` resolve o `Appointment` via
   `AppointmentRepository.findByTenantIdAndId(tenantId, id)` — tenant
   sempre o resolvido pelo slug, nunca outro (BR-1/BR-7). Não encontrado
   → `AppointmentNotFoundException` → 404 (mesmo tratamento de
   `ServiceOfferingNotFoundException` na TODO-006).
2. Chama `appointment.confirm(Instant.now())` ou `.cancel(Instant.now())`
   — método de domínio puro decide se há transição real (DD-3).
3. Se o status mudou, `AppointmentRepository.updateStatus(...)` grava só
   a coluna `status` (+ `updated_at`) — nunca todo o agregado (DD-4).
4. Redireciona (PRG) para o próprio `GET .../agendamentos/{id}`, que
   sempre reflete o estado atual.

---

## Design Decisions

### DD-1: Identidade do link é o próprio `Appointment.id`, revalidado por tenant

**Contexto**: BR-1/BR-7 exigem que o link não permita acessar ou alterar
agendamento de outro tenant. A spec funcional deixou o mecanismo em
aberto de propósito.

**Opções Consideradas**:
- **A (selecionada) — UUID do próprio `Appointment`**: a URL
  `/b/{slug}/agendamentos/{id}` (já criada, cosmética, pela TODO-006)
  passa a fazer uma consulta real, sempre filtrada por
  `(tenantId, id)` — exatamente o mesmo padrão já usado por
  `ServiceOfferingDirectory.find` (TODO-005/006). Um id de outro tenant,
  ou inexistente, dá o mesmo 404 que um id mal formado.
- **B — Token assinado (HMAC) derivado do id**: mais forte contra um
  cenário hipotético de vazamento do banco (quem só tem o id, sem a
  chave secreta da aplicação, não forja o token), mas introduz um
  segredo novo para gerenciar (rotação, variável de ambiente) e lógica
  de verificação para um ganho que, dado o perfil do piloto, não se
  paga: `UUIDv7` já carrega ~74 bits de entropia na parte aleatória
  (ADR 0009 já trata o id como identificador opaco seguro em URL), e
  as rotas públicas da TODO-006 (`serviceOfferingId`, `professionalId`)
  já seguem exatamente essa mesma defesa — id cru revalidado por tenant.

**Trade-offs Accepted**: um invasor com acesso de leitura direto ao
banco (cenário que já implicaria comprometimento muito mais grave)
conseguiria montar links válidos sem precisar de nenhuma chave — mas
esse mesmo invasor já veria telefone, nome e tudo mais em texto claro.
Não é uma superfície de ataque nova que a Opção B fecharia de forma
prática.

**Rationale**: consistência com o padrão já estabelecido no projeto
inteiro (nenhuma outra rota pública usa token assinado) e zero segredo
novo para operar no piloto de um único container.

### DD-2: `ManageAppointmentHandler` implementa três portas numa classe só

**Contexto**: `AppointmentDetailsUseCase` (consulta), `ConfirmAppointmentUseCase`
e `CancelAppointmentUseCase` (glossário, já normativo) compartilham
exatamente a mesma dependência (`AppointmentRepository`) e a mesma
resolução "achar o agendamento deste tenant ou lançar
`AppointmentNotFoundException`".

**Opções Consideradas**:
- **A — Três handlers separados** (`AppointmentDetailsHandler`,
  `ConfirmAppointmentHandler`, `CancelAppointmentHandler`): mantém o
  precedente de "uma classe por porta" (`BookAppointmentHandler`,
  `GetAvailableSlotsHandler`), mas triplica a lógica de resolução por
  tenant+id, que é idêntica nas três.
- **B (selecionada) — Uma classe, três portas**: `ManageAppointmentHandler
  implements AppointmentDetailsUseCase, ConfirmAppointmentUseCase,
  CancelAppointmentUseCase`, com um método privado
  `resolverOuFalhar(UUID id)` reaproveitado pelos três métodos públicos.

**Trade-offs Accepted**: desvio deliberado do precedente "uma classe por
porta" — documentado aqui para não ser lido como inconsistência
acidental. Justificativa: as três operações são, na prática, a mesma
funcionalidade ("gerenciar o agendamento já existente") vista de três
ângulos, ao contrário de `BookAppointmentHandler`/`GetAvailableSlotsHandler`,
que são etapas de fluxos diferentes (reservar vs. consultar
disponibilidade antes de reservar).

### DD-3: `Appointment.confirm`/`cancel` absorvem estado terminal sem lançar exceção

**Contexto**: BR-2 (transições), BR-3 (idempotência) e BR-4 (sem ação
depois que o horário passa) juntos definem um comportamento: qualquer
ação sobre um agendamento `CANCELLED`, ou cujo `startsAt` já passou, não
tem efeito — não é erro, é absorvida.

**Decisão**: dois métodos novos em `Appointment` (domínio puro,
ADR 0002), cada um recebendo `Instant agora` explícito (mesmo padrão de
testabilidade de `GetAvailableSlotsHandler.handle(query, hoje)`):

```java
public Appointment confirm(Instant agora) {
    if (status == AppointmentStatus.CANCELLED) return this;      // terminal, sem efeito (BR-3)
    if (agora.isAfter(startsAt)) return this;                    // já passou, sem efeito (BR-4)
    if (status == AppointmentStatus.CONFIRMED) return this;      // idempotente (BR-3)
    return new Appointment(id, tenantId, ..., AppointmentStatus.CONFIRMED, ...);
}

public Appointment cancel(Instant agora) {
    if (status == AppointmentStatus.CANCELLED) return this;      // idempotente (BR-3)
    if (agora.isAfter(startsAt)) return this;                    // já passou, sem efeito (BR-4)
    return new Appointment(id, tenantId, ..., AppointmentStatus.CANCELLED, ...);
}
```

Retorna uma nova instância (mesmo estilo imutável de `Appointment` desde
a TODO-006) só quando há transição real; caso contrário devolve `this` —
o chamador compara `antes.status() != depois.status()` para saber se
precisa persistir.

**Opções Consideradas**:
- **A — Lançar exceção para ação inválida** (`AppointmentTerminatedException`,
  `AppointmentPastDueException`): mais explícito, mas exige o controller
  distinguir "erro de verdade" (id de outro tenant) de "sem efeito,
  não é bem um erro" — duas categorias que a spec funcional já trata como
  a mesma coisa ("sem efeito", Validation Invariants).
- **B (selecionada) — Absorver, devolver `this`**: sem exceção nova,
  sem branch adicional no controller — o `GET` seguinte sempre mostra o
  estado real, que é exatamente o que a spec funcional pede.

**Trade-offs Accepted**: quem chama `confirm`/`cancel` não recebe sinal
explícito de "sua ação não fez nada" além de comparar o status antes/depois
— aceitável porque o único consumidor (o controller) já vai recarregar a
tela com o estado atual de qualquer forma (PRG).

**`NO_SHOW` não é alcançável por nenhum caminho desta feature** — os dois
métodos não precisam de um terceiro branch para esse status; fica para a
TODO-008 decidir o que confirmar/cancelar significa a partir dele, se é
que significa algo.

### DD-4: Mudança de status grava por `UPDATE` direto, não por `save()`

**Contexto**: `AppointmentRepository.save()` (TODO-006) sempre passou por
`saveAndFlush` de um agregado inteiro reconstruído por
`AppointmentMapper.toEntity`, que atribui `createdAt = Instant.now()`
incondicionalmente — correto para o único caso de uso que existia até
aqui (`INSERT`), mas errado para uma atualização: reutilizar `save()`
para persistir `confirm`/`cancel` sobrescreveria `created_at` com o
instante da ação, corrompendo um dado que a `V8` já grava corretamente
na criação.

**Opções Consideradas**:
- **A — Fazer `AppointmentMapper` carregar `createdAt`/`updatedAt`
  originais**: exigiria que o domínio puro `Appointment` passasse a
  carregar metadado de persistência que ele deliberadamente não tem
  (comentário explícito no mapper: "`createdAt`/`updatedAt` são metadado
  de persistência, não do domínio").
- **B (selecionada) — Método de repositório dedicado**:
  `AppointmentRepository.updateStatus(TenantId, UUID id, AppointmentStatus,
  Instant agora)`, implementado como `@Modifying @Query` JPQL que só
  toca `status` e `updated_at`:
  ```java
  @Modifying
  @Query("""
          update AppointmentJpaEntity a
             set a.status = :status, a.updatedAt = :agora
           where a.tenantId = :tenantId and a.id = :id
          """)
  int updateStatus(@Param("tenantId") UUID tenantId, @Param("id") UUID id,
                    @Param("status") AppointmentStatus status, @Param("agora") Instant agora);
  ```

**Trade-offs Accepted**: nenhum — é estritamente mais correto que a
alternativa, e mais barato (um `UPDATE` de duas colunas, sem o `SELECT`
que `merge()` faria por trás de `save()`).

### DD-5: Rota do agendamento sai de `PublicBookingController` para `AppointmentController` novo

**Contexto**: `PublicBookingController` (TODO-006) já tinha
`GET /b/{slug}/agendamentos/{id}` — mas só para mostrar um flash
attribute que "some" ao recarregar. Essa mesma URL agora precisa de
consulta real, dois `POST` novos e um `GET` de `.ics`.

**Decisão**: extrai `AppointmentController` (`scheduling.adapter.in.web`,
`@RequestMapping("/b/{slug}/agendamentos/{id}")`), e remove o método
`sucesso` de `PublicBookingController`. `confirmar` (POST de reserva,
TODO-006) passa a redirecionar para a mesma URL, sem `RedirectAttributes`
— os flash attributes de `serviceName`/`startsAt` ficam obsoletos, porque
a página de destino agora consulta o agregado de verdade.

**Opções Consideradas**:
- **A — Manter tudo em `PublicBookingController`**: um controller só
  cresceria para cobrir dois fluxos distintos (reservar vs. gerenciar o
  que já existe), com dependências que não se sobrepõem
  (`BookAppointmentUseCase` vs. `ManageAppointmentHandler`).
- **B (selecionada) — Controller novo**: cada controller injeta só o
  que o próprio fluxo precisa.

**Trade-offs Accepted**: nenhum — é separação de responsabilidade sem
custo, já que os dois controllers continuam sob o mesmo
`@RequestMapping("/b/{slug}")` conceitual (prefixo comum, classes
diferentes).

### DD-6: Teto por telefone (BR-9 da TODO-006) passa a contar `SCHEDULED` e `CONFIRMED`

**Contexto**: BR-6 desta feature exige que confirmar presença não abra
vaga nova no teto de 3 agendamentos futuros ativos por telefone. A
consulta atual (`countByTenantIdAndCustomerIdAndStatusAndStartsAtAfter`)
está fixa em `AppointmentStatus.SCHEDULED`.

**Decisão**: troca para `countByTenantIdAndCustomerIdAndStatusInAndStartsAtAfter`
(Spring Data deriva `In` a partir de um `Collection<AppointmentStatus>`),
chamada com `List.of(SCHEDULED, CONFIRMED)` no adapter — sem `@Query`
manual, é um método derivado como o anterior.

**Trade-offs Accepted**: nenhum — é a mesma consulta, só com um filtro a
mais; o índice `appointment_customer_idx (tenant_id, customer_id,
status)` (`V8`) continua servindo, mesmo com dois valores no `IN`.

### DD-7: WhatsApp do estabelecimento é campo opcional só no cadastro

**Contexto**: BR-8 exige o WhatsApp do estabelecimento para montar o
link `wa.me`. `Business` (TODO-001) não tem esse campo; não existe tela
de edição pós-cadastro (decisão já tomada na spec funcional).

**Decisão**: `business.whatsapp` (nullable), migration `V9`.
`Business.register(name, slug, whatsapp)` ganha um terceiro parâmetro
opcional; `Business.normalizeWhatsapp(String)` (mesmo formato de
`Customer.normalizePhone`, mas aceita `null`/branco como "não informado",
devolvendo `null`). `RegistrationRequest` ganha `whatsapp` com
`@Pattern` permitindo string vazia OU o formato de telefone.
`BusinessRef` ganha o campo `whatsapp` (nullable) para chegar até
`scheduling` via `organization.api`.

**Opções Consideradas**:
- **A — Value object `PhoneNumber` compartilhado** entre `customer` e
  `organization`: um nome a mais para decorar, para uma validação de
  3 linhas já duplicada sem problema entre `Customer` e outros agregados
  do projeto (nenhum precedente de VO compartilhado entre contextos
  hoje).
- **B (selecionada) — Duplicar a validação em `Business`**: mesma regra
  de "rigor proporcional" já aplicada em todo o projeto — os dois
  contextos continuam sem depender um do outro para uma regra tão
  pequena.

**Trade-offs Accepted**: nenhum — é a mesma filosofia já em produção.

### DD-8: Nome de profissional e cliente resolvidos por um `find(UUID)` novo em cada directory

**Contexto**: a tela do agendamento mostra nome do profissional
(`Appointment` só guarda `professionalId`) e usa o nome do cliente na
mensagem do `wa.me` (`Appointment` só guarda `customerId`, nome vive em
`Customer`).

**Decisão**: `organization.api.ProfessionalDirectory` ganha
`find(UUID id) -> Optional<ProfessionalRef>` (hoje só tinha
`listActive()`); `customer.api.CustomerDirectory` ganha
`find(UUID id) -> Optional<CustomerRef>` (`CustomerRef(UUID id, String
name)` — só o necessário para exibir, sem telefone). Ambos usam
`TenantContext.require()` internamente, mesma convenção geral; ambos
chamados **uma vez cada** por visualização (nunca em laço).

**Opções Consideradas**:
- **A — Reaproveitar `listActive()` e filtrar em memória**: funciona,
  mas carrega todos os profissionais ativos do tenant só para achar um —
  desproporcional para uma consulta de um registro só.
- **B (selecionada) — `find(UUID)` dedicado**: consulta direta por id,
  mesma forma de `ServiceOfferingDirectory.find`.

**Trade-offs Accepted**: nenhum.

**Se o profissional ou o cliente não existir mais** (não há exclusão
física no projeto, ADR 0011 — "nada é apagado" — então este caso é só
teórico hoje): `AppointmentDetailsUseCase` lança `AppointmentNotFoundException`
também nesse caso, tratando como se o agendamento inteiro não pudesse
ser exibido corretamente, em vez de mostrar uma tela com nome em branco.

### DD-9: `.ics` é texto plano montado à mão, sem biblioteca nova

**Contexto**: BR-9 exige um arquivo `.ics` sem dado pessoal do cliente.

**Decisão**: `IcsWriter` (`scheduling.adapter.in.web`, classe utilitária
sem estado) monta o corpo `VCALENDAR`/`VEVENT` diretamente como
`String` — o formato é texto estruturado simples (poucas linhas fixas
mais interpolação), sem necessidade de biblioteca externa para este
caso de uso (um evento único, sem recorrência, sem fuso horário
múltiplo). `AppointmentController` expõe `GET .../ics` com
`Content-Type: text/calendar` e `Content-Disposition: attachment;
filename="agendamento.ics"`.

**Opções Consideradas**:
- **A — Biblioteca ical4j (ou similar)**: resolve casos que este projeto
  não tem (recorrência, fusos múltiplos, timezones VTIMEZONE) — peso de
  dependência sem uso real.
- **B (selecionada) — Texto plano**: ~15 linhas fixas, sem estado,
  testável como qualquer método puro.

**Trade-offs Accepted**: se o produto um dia precisar de recorrência ou
múltiplos fusos no `.ics`, a biblioteca entra então — não antes
(princípio de não superengenharia do `sdd-kit`).

### DD-10: Link `wa.me` montado no controller, nunca no template

**Contexto**: BR-8 exige mensagem pré-preenchida com nome do cliente e
data/horário — precisa de URL-encoding, que não é responsabilidade de
template (PATTERNS.md: "controller decide, template exibe").

**Decisão**: `AppointmentController` monta a `String` completa
(`https://wa.me/{whatsapp}?text={mensagem codificada}`) via
`URLEncoder.encode(mensagem, UTF_8)` e coloca pronta no model
(`whatsappLink`, nulo se `Business.whatsapp` for nulo). O template só
faz `th:if="${whatsappLink != null}"` + `th:href`.

**Trade-offs Accepted**: nenhum.

---

## Existing Data & Migrations

**`V9__organization_add_business_whatsapp.sql`** (nova):
```sql
-- WhatsApp do estabelecimento (TODO-007, confirmacao-e-cancelamento).
-- Opcional: coletado so no cadastro, sem tela de edicao ainda.
ALTER TABLE business ADD COLUMN whatsapp VARCHAR(20);
```

Nenhuma migration nova em `appointment` — `V8` (TODO-006) já previa
`CONFIRMED`/`CANCELLED` tanto no `WHERE` da exclusion constraint quanto
no índice `appointment_customer_idx`. A liberação do horário ao cancelar
(BR-5) e a permissão de confirmar sem novo conflito (DD-3) já funcionam
com o schema existente — confirmado por leitura literal do SQL da `V8`
antes de escrever esta spec.

---

## Data Model

**`Appointment`** (`scheduling.domain`, editado) — ganha:
```java
public Appointment confirm(Instant agora) { ... }  // DD-3
public Appointment cancel(Instant agora) { ... }   // DD-3
```
Nenhum campo novo — só os dois métodos de transição.

**`Business`** (`organization.domain`, editado) — ganha:
```java
@Column(length = 20)
private String whatsapp;  // nullable

public static Business register(String name, String slug, String whatsapp) { ... }
public static String normalizeWhatsapp(String whatsapp) { ... } // null/branco -> null
public String whatsapp() { return whatsapp; } // nullable
```

**`AppointmentDetails`** (`scheduling.application.port.in`, NOVO — DTO de
saída de `AppointmentDetailsUseCase`):
```java
public record AppointmentDetails(
        UUID id, String professionalName, String customerName, String serviceName,
        Instant startsAt, Instant endsAt, AppointmentStatus status,
        boolean canConfirm, boolean canCancel) {}
```
`canConfirm`/`canCancel` calculados pelo handler (`status` atual +
`agora.isBefore(startsAt)`), nunca pelo controller nem pelo template.

**`CustomerRef`** (`customer.api`, NOVO):
```java
public record CustomerRef(UUID id, String name) {}
```

---

## Cross-Context API Contracts

### `organization.api.ProfessionalDirectory` (editado — DD-8)

| Método | Entrada | Saída | Tenant |
|---|---|---|---|
| `find` (NOVO) | `UUID id` | `Optional<ProfessionalRef>` | `TenantContext.require()` interno |

### `organization.api.BusinessDirectory` / `BusinessRef` (editado — DD-7)

`BusinessRef` ganha o campo `whatsapp` (nullable). Nenhum método novo —
`findBySlug` (TODO-006) já devolve o `BusinessRef` inteiro.

### `customer.api.CustomerDirectory` (editado — DD-8)

| Método | Entrada | Saída | Tenant |
|---|---|---|---|
| `find` (NOVO) | `UUID id` | `Optional<CustomerRef>` | `TenantContext.require()` interno |

### `scheduling.application.port.in` (NOVO — DD-2)

```java
public interface AppointmentDetailsUseCase {
    AppointmentDetails handle(UUID appointmentId); // lança AppointmentNotFoundException
}

public interface ConfirmAppointmentUseCase {
    void confirm(UUID appointmentId);
}

public interface CancelAppointmentUseCase {
    void cancel(UUID appointmentId);
}
```

### `scheduling.application.port.out.AppointmentRepository` (editado)

```java
Optional<Appointment> findByTenantIdAndId(TenantId tenantId, UUID id);  // NOVO — DD-1
void updateStatus(TenantId tenantId, UUID id, AppointmentStatus status, Instant agora); // NOVO — DD-4
// countFutureActive: assinatura igual, implementação passa a considerar SCHEDULED e CONFIRMED (DD-6)
```

### Web Routes (Thymeleaf server-side, ADR 0007 — sem API REST)

| Rota | Método | O que faz |
|---|---|---|
| `/b/{slug}/agendamentos/{id}` | GET | Detalhes + status atual (EDITADO — antes só flash) |
| `/b/{slug}/agendamentos/{id}/confirmar` | POST | `SCHEDULED` → `CONFIRMED` (NOVO) |
| `/b/{slug}/agendamentos/{id}/cancelar` | POST | → `CANCELLED` (NOVO) |
| `/b/{slug}/agendamentos/{id}/ics` | GET | Download do arquivo `.ics` (NOVO) |

Id inexistente ou de outro tenant → `ResponseStatusException(NOT_FOUND)`,
mesmo tratamento de `ServiceOfferingNotFoundException` na TODO-006.

---

## Security

Mesma rota pública sem sessão da TODO-006, agora com efeito colateral
(mudar status) além de leitura:

- `tenantId` nunca vem do id do agendamento — vem só do slug, via
  `TenantContext` já resolvido pelo `TenantContextFilter` (DD-1/BR-1/BR-7).
  `AppointmentRepository.findByTenantIdAndId` filtra pelos dois sempre
  juntos; um id de outro tenant dá o mesmo 404 de um id inexistente.
- CSRF: os dois `POST` novos (`/confirmar`, `/cancelar`) caem sob
  `/b/**`, já `permitAll()` com CSRF ativo (TODO-006) — formulário
  Thymeleaf inclui o token via `th:action`, sem configuração extra.
- XSS: nome do profissional, nome do cliente e nome do estabelecimento
  exibidos só via `th:text`, nunca `th:utext`.
- `.ics` não carrega nome nem telefone do cliente (BR-9, LGPD) — só
  serviço, horário e nome do estabelecimento.
- Sem CAPTCHA nem rate limit dedicado (decisão da spec funcional) — a
  revalidação por tenant já é a defesa; abuso de enumeração de ids fica
  para revisão futura se o volume real justificar.
- Nenhum segredo novo — sem chave de API, sem credencial (DD-1 evita
  exatamente essa necessidade).

---

## Performance

Consulta de detalhes: uma busca de `Appointment` por `(tenant, id)` +
uma busca de profissional + uma busca de cliente — três consultas fixas,
sem laço, mesmo em volume alto. Confirmar/cancelar: uma busca (mesma de
cima, sem as duas laterais) + um `UPDATE` de duas colunas — sem `SELECT`
adicional que `merge()` faria (DD-4). Nenhuma consulta nova no caminho
de listagem de horários (`findOccupiedRanges`, TODO-006, já cobria
`CONFIRMED`).

---

## Testing Strategy

| Camada | Arquivo | Cobre |
|---|---|---|
| Domínio puro | `AppointmentTest` (editado) | `confirm`/`cancel`: transição normal, idempotência, terminal por cancelamento, terminal por horário passado |
| Domínio puro | `BusinessTest` (editado, se existir) | `normalizeWhatsapp`: nulo/branco vira `null`, formato inválido rejeita, formato válido passa |
| Aplicação (mocks) | `ManageAppointmentHandlerTest` (NOVO) | as três portas, `AppointmentNotFoundException` para tenant errado, `canConfirm`/`canCancel` calculados certo |
| Aplicação (mocks) | `BookAppointmentHandlerTest` (editado) | BR-6: teto conta `SCHEDULED` e `CONFIRMED` |
| Web (`@WebMvcTest`) | `AppointmentControllerTest` (NOVO) | GET/POST das 4 rotas, 404 de tenant errado, `.ics` sem dado do cliente, `whatsappLink` nulo quando `Business.whatsapp` é nulo |
| Web (`@WebMvcTest`) | `RegistrationControllerTest` (editado, se existir) | cadastro aceita `whatsapp` opcional |
| Integração (Testcontainers) | `ConfirmacaoECancelamentoIT` (NOVO) | E2E-1 a E2E-7 da spec funcional, incluindo a liberação real do horário (E2E-2) via `GetAvailableSlotsHandler` |

---

## Implementation Locations

```
src/main/java/com/agendaia/scheduling/
├── domain/Appointment.java                               [EDITADO — confirm/cancel, DD-3]
├── application/
│   ├── port/in/
│   │   ├── AppointmentDetailsUseCase.java                [NOVO]
│   │   ├── ConfirmAppointmentUseCase.java                 [NOVO]
│   │   ├── CancelAppointmentUseCase.java                  [NOVO]
│   │   └── AppointmentDetails.java                        [NOVO]
│   ├── port/out/AppointmentRepository.java                [EDITADO — findByTenantIdAndId, updateStatus, DD-1/DD-4]
│   ├── ManageAppointmentHandler.java                      [NOVO — DD-2]
│   └── BookAppointmentHandler.java                        [EDITADO — countFutureActive com 2 status, DD-6]
└── adapter/
    ├── out/persistence/
    │   ├── AppointmentJpaRepository.java                  [EDITADO — findByTenantIdAndId, updateStatus, countByStatusIn]
    │   └── AppointmentPersistenceAdapter.java              [EDITADO]
    └── in/web/
        ├── AppointmentController.java                      [NOVO — DD-5]
        ├── IcsWriter.java                                   [NOVO — DD-9]
        └── PublicBookingController.java                     [EDITADO — remove sucesso(), redirect sem flash]

src/main/java/com/agendaia/organization/
├── api/
│   ├── ProfessionalDirectory.java                          [EDITADO — find(UUID), DD-8]
│   └── BusinessRef.java                                     [EDITADO — campo whatsapp, DD-7]
├── application/ProfessionalDirectoryHandler.java             [EDITADO]
├── domain/Business.java                                      [EDITADO — whatsapp, normalizeWhatsapp, DD-7]
├── application/command/RegisterBusinessCommand.java           [EDITADO — campo whatsapp]
├── application/RegisterBusinessHandler.java                   [EDITADO]
└── adapter/in/web/request/RegistrationRequest.java            [EDITADO — campo whatsapp opcional]

src/main/java/com/agendaia/customer/
├── api/
│   ├── CustomerDirectory.java                                [EDITADO — find(UUID), DD-8]
│   └── CustomerRef.java                                       [NOVO]
└── application/CustomerDirectoryHandler.java                  [EDITADO]

src/main/resources/db/migration/
└── V9__organization_add_business_whatsapp.sql                 [NOVO]

src/main/resources/templates/
├── public/agendamento.html                                    [NOVO — substitui sucesso.html]
└── auth/cadastro.html                                          [EDITADO — campo whatsapp opcional]

src/test/java/com/agendaia/
├── scheduling/domain/AppointmentTest.java                      [EDITADO]
├── scheduling/application/ManageAppointmentHandlerTest.java    [NOVO]
├── scheduling/application/BookAppointmentHandlerTest.java      [EDITADO]
├── scheduling/adapter/in/web/AppointmentControllerTest.java    [NOVO]
└── scheduling/ConfirmacaoECancelamentoIT.java                  [NOVO]
```

---

## References

- [ADR 0009](../../../docs/architecture/adr/0009-uuidv7-como-identificador.md) — id opaco, base do DD-1
- [ADR 0005](../../../docs/architecture/adr/0005-exclusion-constraint-contra-overbooking.md) — `WHERE status IN (...)` já cobre `CONFIRMED`/liberação por `CANCELLED`
- [ADR 0011](../../../docs/architecture/adr/0011-ciclo-de-vida-dos-dados.md) — nada é apagado; cancelamento é mudança de status
- [ADR 0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — regime completo de `scheduling`, regime CRUD de `organization`/`customer`
- `docs/domain/glossary.md` — `CancelAppointmentUseCase` já normativo; `ConfirmAppointmentUseCase` é termo novo desta feature, a incluir no glossário durante o build
- `sdd/PATTERNS.md` — rigor proporcional; controller decide, template exibe
