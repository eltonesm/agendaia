# gestao-de-clientes - Technical Spec

**Feature**: gestao-de-clientes
**Backlog**: IDEA-018, IDEA-019, IDEA-006
**Status**: approved
**Data**: 2026-09-09
**Aprovado por**: Elton Marques em 2026-09-09T22:24:52Z

---

## Executive Summary

Duas telas novas (`/admin/clientes` e `/admin/clientes/{id}`) e um campo
novo em `Appointment` (status de pagamento), todos derivados de dado que
já existe — `Appointment.customerId`/`price`/`status` e
`Customer.name`/`phone`. Nenhum contexto novo, nenhuma tabela nova (só
uma coluna).

A decisão central (DD-2) é **onde** a tela mora: como ela precisa tanto
de `Customer` (nome, telefone) quanto de `Appointment` (histórico,
valores), e `scheduling` **já** depende de `customer.api` (usado por
`DailyScheduleSummaryHandler`/`ProfessionalAgendaHandler` desde a
TODO-008/TODO-110), a tela inteira mora em `scheduling` — reaproveitando
essa dependência já declarada, sem abrir a direção contrária (que
fecharia um ciclo, exatamente a lição da DD-6 da TODO-110). Diferente da
TODO-110, esta feature **não** precisa de um novo pacote `scheduling.api`
nem de `@ControllerAdvice`: a tela é autocontida dentro de `scheduling`
(controller, handler e template), não injeta dado na view de outro
contexto.

---

## Architecture Overview

```
Lista de clientes (US-1):

  GET /admin/clientes?page=N
    └─ CustomerAdminController.listar()  [novo, scheduling.adapter.in.web]
         └─ ListCustomerActivityUseCase.list(page, size)  [novo port]
              └─ CustomerActivityHandler  [novo, scheduling.application]
                   ├─ CustomerDirectory.listForTenant(page, size)  [novo método em customer.api]
                   │    └─ CustomerRepository.findByTenantIdAndAnonymizedAtIsNullOrderByNameAsc(...)  [novo, customer]
                   └─ AppointmentRepository.findActivityByCustomerIds(tenantId, ids)  [novo método, batch]
                        └─ AppointmentJpaRepository (GROUP BY customer_id, status=COMPLETED)  [nova query]

Detalhe do cliente (US-2, US-3):

  GET /admin/clientes/{id}
    └─ CustomerAdminController.detalhe()
         └─ CustomerActivityDetailUseCase.detail(id)  [novo port, mesma classe acima — IDEA-017]
              ├─ CustomerDirectory.find(id)  [já existe]
              └─ AppointmentRepository.findCompletedByTenantIdAndCustomerId(tenantId, id)  [novo método]
                   (totais calculados em memória sobre essa lista — mesmo padrão de DailyScheduleSummaryHandler)

  Template: link wa.me/{telefone} — puro HTML, sem chamada nova (US-3)

Status de pagamento na agenda (US-4):

  admin/agenda.html
    └─ POST /admin/agenda/agendamentos/{id}/pagamento
         └─ AgendaController.marcarPagamento()  [novo método no controller já existente]
              └─ UpdatePaymentStatusUseCase.updatePaymentStatus(id, status)  [novo port]
                   └─ ProfessionalAgendaHandler.updatePaymentStatus()  [novo método, mesma classe — IDEA-017]
                        ├─ Appointment.markPaymentAsPaid()/markPaymentAsPending()/markPaymentAsOnCredit()  [novo, domain]
                        └─ AppointmentRepository.updatePaymentStatus(...)  [novo método, mesma forma de updateStatus]
```

---

## Design Decisions

### DD-1: A tela inteira mora em `scheduling`, não em `customer` nem `organization`

**Selected**: `CustomerAdminController`, o novo handler de aplicação e os
templates novos vivem em `scheduling` (`adapter.in.web`, `application`,
`templates/admin/`). `scheduling` já declara `customer.api` em
`allowedDependencies` (usado desde a TODO-008 para resolver nome/telefone
na agenda) — a tela reaproveita essa mesma dependência, só com um método
novo na interface (`listForTenant`), sem abrir nenhuma aresta nova no
grafo de módulos.

**Options Considered**:
- Tela dentro de `customer` (`customer.adapter.in.web`), já que a URL é
  sobre "clientes" — descartado: `customer` precisaria depender de
  `scheduling.api` para ler histórico/valores, e `scheduling` já depende
  de `customer.api` (contagem de agendamento futuro, resolução de nome) —
  a direção contrária fecha um ciclo `customer ⇄ scheduling`, o mesmo erro
  que a DD-6 da TODO-110 já pagou o preço de descobrir (`organization ⇄
  scheduling`).
- Tela dentro de `organization` (mesmo raciocínio de `admin/dashboard`) —
  descartado pelo mesmo motivo: `organization` teria que importar
  `scheduling.api`/`customer.api` só para montar uma tela que não é
  fundamentalmente sobre estabelecimento.
- Novo `@ControllerAdvice` compondo a partir de outro contexto (padrão
  `DashboardKpiAdvice`, TODO-110) — descartado: esse padrão resolve
  **injetar dado numa view que já existe em outro contexto**; aqui a tela
  é nova e autocontida, não há view alheia para compor.

**Trade-offs Accepted**: o nome do pacote (`scheduling.adapter.in.web`)
não "parece" sobre cliente à primeira vista — mitigado com nome de
classe explícito (`CustomerAdminController`) e comentário Javadoc citando
esta decisão.

**Rationale**: reaproveita uma aresta de dependência já existente e já
testada, em vez de abrir uma nova ou inventar um mecanismo de composição
que esta feature não precisa.

### DD-2: Paginação por agregação SQL, não por carregar tudo em memória

**Selected**: A lista de clientes é paginada por `customer` (via novo
`CustomerRepository.findByTenantIdAndAnonymizedAtIsNullOrderByNameAsc(tenantId,
Pageable)`, Spring Data). Para a página atual (até `size` clientes), uma
segunda consulta busca a atividade agregada só desses ids
(`AppointmentRepository.findActivityByCustomerIds`, `GROUP BY
customer_id`) — nunca uma consulta que carregue todos os agendamentos do
tenant para agregar em memória.

**Options Considered**:
- Carregar todos os clientes e todos os agendamentos do tenant, agregar
  em memória (mesmo estilo de `DailyScheduleSummaryHandler`) — descartado
  explicitamente: o dono pediu paginação **por causa de performance**
  (BR-6 da spec funcional), e carregar tudo é exatamente o que a paginação
  existe para evitar. Funciona bem para "hoje" (um dia, poucos
  agendamentos) mas não para "todo o histórico do estabelecimento".
- Uma única consulta com `JOIN` entre `customer` e `appointment` — proibido
  por `CLAUDE.md` ("Nunca `JOIN` entre tabelas de contextos diferentes").

**Trade-offs Accepted**: duas consultas por página em vez de uma
(`customer` primeiro, depois `scheduling` para os ids daquela página) —
aceitável, é exatamente o padrão "API entre contextos é grossa, em lote"
do `PATTERNS.md`, e o índice `appointment_customer_idx (tenant_id,
customer_id, status)` (migration V8) já cobre bem o filtro por lote de
`customer_id`.

**Rationale**: é a única opção das três que atende a paginação pedida
sem violar a fronteira entre contextos.

### DD-3: `PaymentStatus` é campo em `Appointment`, sem histórico de mudanças

**Selected**: Novo enum `PaymentStatus { PAID, PENDING, ON_CREDIT }`
(`scheduling.domain`) e um campo novo em `Appointment`, ao lado de
`status` (`AppointmentStatus`). Três métodos de domínio —
`markPaymentAsPaid()`, `markPaymentAsPending()`, `markPaymentAsOnCredit()`
— cada um retornando uma instância nova (ou `this` quando já está naquele
valor, mesma disciplina de comparação `antes`/`depois` de `confirm()`).
Nenhuma tabela de histórico — só o valor atual é guardado.

**Options Considered**:
- Tabela `payment_event` separada, um registro por mudança (auditoria
  completa: quem mudou, quando, de que valor para que valor) — descartado:
  nenhuma regra de negócio (BR da spec funcional) pede histórico; seria
  cerimônia sem retorno agora (ADR 0002, "rigor proporcional").
- Um único método genérico `updatePaymentStatus(PaymentStatus novo)` em
  vez de três métodos nomeados — descartado: foge da convenção já
  estabelecida (`PATTERNS.md`, "Sem setter, em lugar nenhum" — nome de
  negócio, não `set`/`update` genérico); um método por ação nomeada deixa
  claro, no ponto de chamada, qual das três coisas está acontecendo.

**Trade-offs Accepted**: se algum dia for preciso saber "quando" um
agendamento virou fiado (não só que está fiado agora), essa informação
não existe — aceitável para o volume e a necessidade atual do piloto;
registrado como possível DEBT futuro, não bloqueia esta feature.

**Rationale**: resolve exatamente o que a spec funcional pede (BR-2, BR-3)
sem inventar infraestrutura de auditoria que ninguém pediu.

### DD-4: Duas portas novas, uma classe só — mesmo padrão já documentado (IDEA-017)

**Selected**: `ListCustomerActivityUseCase` e
`CustomerActivityDetailUseCase` são implementadas por uma única classe
nova, `CustomerActivityHandler` — mesma dependência principal
(`AppointmentRepository` + `CustomerDirectory`), mesma funcionalidade
("atividade do cliente") vista de dois ângulos (lista vs. detalhe).
`UpdatePaymentStatusUseCase`, por sua vez, entra em
`ProfessionalAgendaHandler` (já implementa `ViewAgendaUseCase`,
`CompleteAppointmentUseCase`, etc.) — é uma ação sobre o mesmo agregado
que as outras ações da agenda já mexem.

**Options Considered**: uma classe por porta (quatro classes novas ao
todo) — descartado por ser exatamente o desvio que `PATTERNS.md`/IDEA-017
já documentou como aceitável: portas que compartilham dependência e são a
mesma funcionalidade sob ângulos diferentes não precisam de uma classe
cada.

**Trade-offs Accepted**: nenhum novo — é a terceira ocorrência do mesmo
padrão já aceito nas duas features anteriores.

**Rationale**: consistência com a convenção já promovida ao
`PATTERNS.md`.

### DD-5: Ação de pagamento reaproveita o `AgendaController` existente

**Selected**: Nova rota `POST
/admin/agenda/agendamentos/{id}/pagamento` entra no `AgendaController`
já existente (mesmo PRG dos botões de confirmar/cancelar/concluir),
não um controller dedicado a pagamento.

**Options Considered**: controller novo (`PaymentController` ou
similar) — descartado: fragmentaria uma ação que é conceitualmente parte
da mesma tela (`admin/agenda.html`) em dois controllers, sem nenhum
ganho de coesão.

**Trade-offs Accepted**: nenhum.

**Rationale**: mesma tela, mesmo controller, mesmo padrão PRG.

---

## Existing Data & Migrations

Uma migration nova, `V10__scheduling_add_appointment_payment_status.sql`:

```sql
-- Status de pagamento (IDEA-006/gestao-de-clientes): independente de
-- AppointmentStatus (BR-2 da spec funcional) — pergunta diferente ("foi
-- pago?" vs. "o atendimento aconteceu?"). PENDING é o padrão para todo
-- agendamento novo (BR-3); linhas já existentes recebem o mesmo padrão
-- (não há como reconstruir o status de pagamento real de agendamentos já
-- concluídos antes desta feature — o dono corrige manualmente os que
-- lembrar).
ALTER TABLE appointment
  ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
```

Nenhum índice novo: `appointment_customer_idx (tenant_id, customer_id,
status)`, já criado na V8, cobre bem as duas novas consultas por
`customer_id` (lote e individual) — o filtro extra por
`payment_status` na soma do "total em aberto" (DD-2/US-2) roda sobre as
linhas já filtradas por esse índice, sem precisar aparecer nele.

Nenhuma migration em `customer`: `findByTenantIdAndAnonymizedAtIsNullOrderByNameAsc`
usa o índice implícito do `UNIQUE(tenant_id, phone)` (V7) para o filtro
por `tenant_id`; a ordenação por `name` faz sort em cima do resultado já
filtrado — aceitável no volume do piloto (mesmo raciocínio de
DEBT-018/DEBT-019: sequential scan tende a aparecer só quando o histórico
crescer bastante).

---

## Data Model

- `PaymentStatus` (**novo enum**, `scheduling.domain`): `PAID`,
  `PENDING`, `ON_CREDIT`.
- `Appointment` (`scheduling.domain`): `+ paymentStatus` (campo),
  `+ markPaymentAsPaid()`, `+ markPaymentAsPending()`,
  `+ markPaymentAsOnCredit()`. `schedule(...)` nasce sempre com
  `PaymentStatus.PENDING` (BR-3).
- `AppointmentJpaEntity`/`AppointmentMapper` (`scheduling.adapter.out.persistence`):
  `+ paymentStatus` (coluna `payment_status`, `@Enumerated(EnumType.STRING)`).
- `AppointmentRepository` (`scheduling.application.port.out`):
  - `+ void updatePaymentStatus(TenantId, UUID, PaymentStatus, Instant)`
    — grava só a coluna + `updatedAt`, mesma forma de `updateStatus`
    (nunca via `save`, que sobrescreveria `createdAt`).
  - `+ List<Appointment> findCompletedByTenantIdAndCustomerId(TenantId, UUID)`
    — para o detalhe do cliente (US-2).
  - `+ Map<UUID, CustomerActivity> findActivityByCustomerIds(TenantId, Collection<UUID>)`
    — para a lista (US-1), em lote.
- `CustomerActivity` (**novo record**, `scheduling.application.port.out`):
  `record CustomerActivity(long visitCount, Money totalSpent, Money totalOwed)`
  — resultado da agregação, um por `customerId`.
- `CustomerListEntry` (**novo record**, `scheduling.application.port.in`):
  `record CustomerListEntry(UUID customerId, String name, String phone, long visitCount, boolean isNew)`.
- `CustomerActivityDetail` (**novo record**, `scheduling.application.port.in`):
  `record CustomerActivityDetail(UUID customerId, String name, String phone, long visitCount, Money totalSpent, Money totalOwed, List<VisitEntry> visits)`.
- `VisitEntry` (**novo record**, `scheduling.application.port.in`):
  `record VisitEntry(Instant startsAt, String serviceName, Money price, PaymentStatus paymentStatus)`.
- `PagedResult<T>` (**novo record genérico**, `scheduling.application.port.in`):
  `record PagedResult<T>(List<T> items, int page, int size, long totalElements)`
  — evita vazar `org.springframework.data.domain.Page` (framework) através
  de um `port.in` (mesmo espírito de `domain` nunca conhecer JPA).
- `ListCustomerActivityUseCase` (**novo port**, `scheduling.application.port.in`):
  `PagedResult<CustomerListEntry> list(int page, int size)`.
- `CustomerActivityDetailUseCase` (**novo port**): `Optional<CustomerActivityDetail> detail(UUID customerId)`.
- `UpdatePaymentStatusUseCase` (**novo port**): `void updatePaymentStatus(UUID appointmentId, PaymentStatus status)`.
- `AgendaEntry` (`scheduling.application.port.in`): `+ PaymentStatus paymentStatus`
  — sem novo `canX`, já que qualquer valor pode mudar para qualquer outro
  a qualquer momento (BR-2, sem guard de transição).
- `CustomerDirectory` (`customer.api`): `+ PagedCustomers listForTenant(int page, int size)`.
- `PagedCustomers` (**novo record**, `customer.api`):
  `record PagedCustomers(List<CustomerRef> items, int page, int size, long totalElements)`.
- `CustomerRepository` (`customer.application.port.out`):
  `+ Page<Customer> findByTenantIdAndAnonymizedAtIsNullOrderByNameAsc(UUID, Pageable)`.

**Glossário** (`docs/domain/glossary.md`, normativo): esta feature
introduz três termos de domínio novos — status de pagamento
(`PaymentStatus`), seus três valores (pago/`PAID`, pendente/`PENDING`,
fiado/`ON_CREDIT`) — a incluir como tarefa da própria feature, não depois.

---

## Cross-Context API Contracts

Sem REST (`CLAUDE.md`). Um contrato novo, in-process, em `customer.api`
(a única aresta que muda — `scheduling` continua consumindo
`customer.api`, só com um método a mais):

```java
package com.agendaia.customer.api;

public interface CustomerDirectory {
    // ... métodos já existentes (findOrCreate, find, findByIds) ...

    /**
     * Clientes do tenant, paginados, ordenados por nome. Exclui cliente
     * anonimizado (anonymizedAt != null) — campo já existe na entidade,
     * ainda sem nenhuma feature que o preencha, mas o filtro já entra
     * agora para não esquecer quando existir.
     */
    PagedCustomers listForTenant(int page, int size);
}

public record PagedCustomers(List<CustomerRef> items, int page, int size, long totalElements) {}
```

Chamado uma vez por carregamento de `/admin/clientes` (uma página por
vez, nunca em laço).

---

## Security

- Nenhuma rota nova exposta sem autenticação: `/admin/clientes`,
  `/admin/clientes/{id}` e `/admin/agenda/agendamentos/{id}/pagamento`
  caem no mesmo `anyRequest().hasRole("OWNER")` de `/admin/**`
  (`SecurityConfig`, sem mudança).
- CSRF: o formulário de status de pagamento segue o mesmo padrão HTML
  (`method="post"`, token automático) dos já existentes na mesma tela.
- `tenantId` de toda consulta desta feature vem de
  `TenantContext.require()` — nunca de parâmetro. `CustomerAdminController.detalhe(id)`
  resolve o cliente via `CustomerDirectory.find(id)` (já revalida tenant)
  e o histórico via `AppointmentRepository.findCompletedByTenantIdAndCustomerId(tenantId, id)`
  — id de outro tenant devolve lista vazia/`Optional.empty()`, o
  controller traduz para 404 (mesmo padrão de `AgendaController.detalhesOuFalhar`).
- Nenhum dado pessoal novo é coletado — `Customer.name`/`phone` já
  existem desde a TODO-006; o link de WhatsApp usa o telefone já
  cadastrado, sem enviar nada para fora do navegador do dono (é um link
  `wa.me`, não uma chamada de API).
- Nenhum segredo novo, nenhuma dependência nova.

---

## Performance

- Lista de clientes: **duas** consultas por página — uma para os
  clientes da página (`customer`, indexada por `tenant_id`), uma para a
  atividade agregada só desses ids (`scheduling`, indexada por
  `appointment_customer_idx`). Nenhuma consulta por linha da tabela
  (DD-2).
- Detalhe do cliente: **duas** consultas — uma para o cliente
  (`CustomerDirectory.find`), uma para o histórico completo dele
  (`findCompletedByTenantIdAndCustomerId`). Os três totais (visitas,
  gasto, em aberto) são somados em memória sobre essa lista — mesmo
  padrão de `DailyScheduleSummaryHandler` (TODO-110): um número pequeno
  de visitas por cliente, sem custo relevante.
- Marcar status de pagamento: `UPDATE` de uma linha só
  (`updatePaymentStatus`, mesma forma de `updateStatus`) — O(1), sem
  leitura extra.
- Nenhuma consulta desta feature carrega "todos os agendamentos do
  tenant" nem "todos os clientes do tenant" de uma vez — exatamente o que
  BR-6 (paginação) pede.

---

## Testing Strategy

**Unit (Java puro, `scheduling.domain`)**:
- `AppointmentTest`: `markPaymentAsPaid()`/`markPaymentAsPending()`/`markPaymentAsOnCredit()`
  transicionam de qualquer valor para qualquer outro; absorvem (retornam
  `this`) quando já estão no valor pedido; `schedule(...)` sempre nasce
  `PENDING`.

**Unit (Mockito, `scheduling.application`)**:
- `CustomerActivityHandlerTest` (novo): `list()` combina
  `CustomerDirectory.listForTenant` com
  `AppointmentRepository.findActivityByCustomerIds`, marcando `isNew=true`
  para cliente sem entrada no mapa de atividade (zero `COMPLETED`);
  `detail()` calcula os três totais corretamente a partir da lista de
  agendamentos `COMPLETED` (visitas, gasto, em aberto), e devolve
  `Optional.empty()` para cliente de outro tenant.
- `ProfessionalAgendaHandlerTest`: `updatePaymentStatus()` grava só
  quando há mudança real (mesma disciplina de `complete()`/`cancel()`).

**Camada web (`@WebMvcTest`)**:
- `CustomerAdminControllerTest` (novo): lista pagina corretamente
  (parâmetros `page`/`size`); detalhe de cliente de outro tenant devolve
  404; sinalização "novo" aparece coerente com o mock.
- `AgendaControllerTest`: nova rota `pagamento` — sucesso redireciona
  (PRG), id de outro tenant devolve 404, sem CSRF é recusado (mesmo trio
  de testes já existente para `concluir`).

**Integração (Postgres real, Testcontainers)**:
- Novo `CustomerActivityIT`: cobre E2E-1 a E2E-4 da spec funcional —
  paginação real com mais clientes do que uma página, cliente sem
  `COMPLETED` sinalizado como novo, histórico e totais batendo com dados
  semeados (incluindo um `ON_CREDIT` no total em aberto), link de
  WhatsApp com o telefone certo.
- `AgendaProfissionalIT` (já existe) ganha o cenário E2E-5: marcar
  `ON_CREDIT` na agenda reflete no total em aberto do cliente.
- Novo teste de isolamento entre tenants (E2E-6), mesmo padrão de
  `AgendaProfissionalIT.e2e5IsolamentoEntreTenants` (TODO-008).

---

## Implementation Locations

| O quê | Arquivo |
|---|---|
| Migration | `db/migration/V10__scheduling_add_appointment_payment_status.sql` |
| Enum | `scheduling/domain/PaymentStatus.java` (novo) |
| Agregado | `scheduling/domain/Appointment.java` (`+ paymentStatus`, 3 métodos novos) |
| Entidade JPA | `scheduling/adapter/out/persistence/AppointmentJpaEntity.java`, `AppointmentMapper.java` |
| Porta de saída | `scheduling/application/port/out/AppointmentRepository.java` (`updatePaymentStatus`, `findCompletedByTenantIdAndCustomerId`, `findActivityByCustomerIds`), `CustomerActivity.java` (novo) |
| Adapter de persistência | `scheduling/adapter/out/persistence/AppointmentPersistenceAdapter.java`, `AppointmentJpaRepository.java` (nova query `GROUP BY`) |
| Portas de entrada novas | `scheduling/application/port/in/ListCustomerActivityUseCase.java`, `CustomerActivityDetailUseCase.java`, `UpdatePaymentStatusUseCase.java`, `CustomerListEntry.java`, `CustomerActivityDetail.java`, `VisitEntry.java`, `PagedResult.java` |
| View record editado | `scheduling/application/port/in/AgendaEntry.java` (`+ paymentStatus`) |
| Handler novo | `scheduling/application/CustomerActivityHandler.java` |
| Handler editado | `scheduling/application/ProfessionalAgendaHandler.java` (`updatePaymentStatus`) |
| Controller novo | `scheduling/adapter/in/web/CustomerAdminController.java` |
| Controller editado | `scheduling/adapter/in/web/AgendaController.java` (rota `pagamento`) |
| Templates novos | `templates/admin/clientes.html`, `templates/admin/cliente-detalhe.html` |
| Template editado | `templates/admin/agenda.html` (controle de status de pagamento por linha) |
| Sidebar | `templates/fragments/layout.html` (`adminSidebar` ganha o item "Clientes") |
| `customer.api` | `customer/api/CustomerDirectory.java` (`+ listForTenant`), `PagedCustomers.java` (novo) |
| `customer` implementação | `customer/application/CustomerDirectoryHandler.java`, `CustomerRepository.java` (`+ findByTenantIdAndAnonymizedAtIsNullOrderByNameAsc`) |
| Glossário | `docs/domain/glossary.md` (`PaymentStatus`, `PAID`, `PENDING`, `ON_CREDIT`) |

---

## References

- ADR 0002 (rigor proporcional) — DD-3 (sem tabela de histórico de
  pagamento).
- ADR 0010 (fronteira entre contextos, Spring Modulith) — DD-1, mesma
  lição da DD-6 de `sistema-de-design-admin` (TODO-110).
- `sdd/PATTERNS.md`, "API entre contextos é grossa, nunca conversadeira" —
  DD-2.
- `sdd/PATTERNS.md`, "uma classe, várias portas" (promovido via IDEA-017,
  TODO-008) — DD-4.
- `sdd/backlog.md`, IDEA-018/IDEA-019/IDEA-006 — origem funcional desta
  feature.
