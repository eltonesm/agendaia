# agenda-profissional (TODO-008)

## O que foi construído

Fecha o ciclo do dono que a TODO-006/TODO-007 abriram para o cliente. Até
aqui, o dono não tinha nenhuma tela para ver a própria agenda, criar um
agendamento manualmente (cliente que ligou), cancelar em nome do cliente
ou reagendar. Esta feature entrega as quatro coisas pelo painel
administrativo (`/admin/agenda/**`, sessão OWNER):

```
GET  /admin/agenda                              → agenda de um profissional, num dia
GET  /admin/agenda/novo                         → formulário de criação manual
POST /admin/agenda/novo                         → cria (sem teto de 3 por telefone)
POST /admin/agenda/agendamentos/{id}/confirmar  → reusa ConfirmAppointmentUseCase (TODO-007)
POST /admin/agenda/agendamentos/{id}/cancelar   → cancela, sem restrição de horário
GET  /admin/agenda/agendamentos/{id}/reagendar  → formulário de reagendamento
POST /admin/agenda/agendamentos/{id}/reagendar  → cria o novo, cancela o antigo
```

## O que nasceu/mudou nesta feature

| Item | Onde | Por quê |
|---|---|---|
| `Appointment.cancelByOwner()` | `scheduling.domain` | Cancelamento sem restrição de horário (BR-2) — diferente de `cancel(Instant)` da TODO-007 |
| `AppointmentFactory` | `scheduling.application` (pacote-privado) | Monta e grava o retrato do `Appointment` — extraído de `BookAppointmentHandler` só depois de existir o 3º chamador real (reagendar) |
| `ProfessionalAgendaHandler` | `scheduling.application` | Implementa 4 portas numa classe só — 2ª ocorrência do desvio deliberado "uma porta por classe" (1ª foi `ManageAppointmentHandler`, TODO-007) |
| `AgendaController` | `scheduling.adapter.in.web` | Primeiro controller admin de `scheduling` — nenhuma rota nova em `SecurityConfig`, `/admin/**` já exigia OWNER por omissão |
| `ServiceOfferingDirectory.listActive()` / `ActiveOfferingRef` | `catalog.api` | Dropdown único "Serviço — Profissional", sem cascata de JavaScript (mesmo padrão de `/admin/ofertas`) |
| `CustomerDirectory.findByIds()` / `CustomerRef.phone` | `customer.api` | Resolve nome e telefone de vários clientes numa consulta só, para a lista da agenda |
| `AppointmentRepository.findByTenantIdAndProfessionalIdAndDate` | `scheduling.application.port.out` | Única consulta nova de schema — lista todos os status do dia, inclusive `CANCELLED` (ADR 0011) |

## Decisões de design centrais

- **Reagendar cria o novo agendamento *antes* de cancelar o antigo**
  (DD-9) — se o novo horário colidir (`SlotUnavailableException`), o
  cliente nunca fica sem nenhum agendamento válido; o antigo permanece
  intacto. Confirmado por teste dedicado em `ProfessionalAgendaHandlerTest`
  e pelo E2E-2 de `AgendaProfissionalIT` (concorrência real via
  Testcontainers).
- **Reagendar pode trocar de profissional** — decisão explícita do dono
  do produto durante a entrevista funcional (opção não recomendada,
  escolhida deliberadamente): a nova oferta escolhida no formulário pode
  pertencer a outro profissional.
- **Criação/cancelamento manual não herdam as restrições do fluxo
  público**: sem teto de 3 agendamentos futuros por telefone (BR-4) e
  sem restrição de horário para cancelar (BR-2) — o dono corrige em
  nome do cliente, inclusive depois do atendimento já ter passado.
- **`ConfirmAppointmentUseCase` (TODO-007) reaproveitado sem nenhuma
  mudança** (DD-11) — confirmar presença continua com a mesma regra
  para cliente e dono (não confirma agendamento cujo horário já passou).
- **Autocorreção durante a própria revisão de código**: a rota
  `GET /admin/agenda/agendamentos/{id}/confirmado` (redirecionamento
  intermediário depois de criar/reagendar) não resolvia `professionalId`
  de qualquer forma — só adicionava um salto de rede sem benefício.
  Removida antes do quality gate; `criar()`/`reagendarConfirmar()`
  redirecionam direto para `/admin/agenda?date=...`.

## Testes

- Domínio puro: `AppointmentTest` (+2) — `cancelByOwner()`: transição
  ignorando horário passado, idempotência.
- Aplicação (mocks): `ProfessionalAgendaHandlerTest` (8 casos) — as 4
  portas: sem teto por telefone, sem restrição de horário no cancelar,
  ordem cria-antes-de-cancelar no reagendar (incluindo o caminho de
  falha), `professionalId` de outro tenant devolve lista vazia.
- Camada web: `AgendaControllerTest` (13 casos) — as 7 rotas, sessão
  OWNER exigida, CSRF ativo, 404 para id de outro tenant.
- Integração (Testcontainers, Postgres real): `AgendaProfissionalIT`,
  E2E-1 a E2E-7 — criar manualmente, concorrência painel × link público
  (exclusion constraint real, ADR 0005), cancelar no passado, reagendar
  preserva histórico, isolamento entre tenants, teto não bloqueia o
  dono, confirmar pelo painel.
- 451 testes no projeto inteiro, 0 falhas, 0 erros; 89% de cobertura de
  instrução (`./mvnw clean verify`).

## Quality gates (Layer 3)

Todos `APPROVED` — ver `verdicts/{code_review,performance,security}.json`.

- **Code review**: `scheduling.domain` continua Java puro;
  `ProfessionalAgendaHandler` documentado como 2ª exceção deliberada ao
  padrão "uma porta por classe" (candidato a promover para
  `PATTERNS.md`, registrado como IDEA no backlog); nenhum import cruzado
  fora de `catalog.api`/`customer.api`/`organization.api`.
- **Performance**: nenhuma consulta em laço; uma única ressalva não
  bloqueante — `findByProfessionalAndDay` não usa o índice GiST parcial
  de `appointment_no_overlap` (que só cobre `SCHEDULED`/`CONFIRMED`);
  aceitável no volume do piloto, registrado como DEBT.
- **Security**: isolamento por tenant confirmado (id de outro tenant
  nunca vaza nem altera dado); `/admin/agenda/**` exige OWNER por
  omissão; CSRF ativo; XSS coberto por `th:text`; sem segredo novo.
