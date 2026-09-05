# pagina-publica-agendamento (TODO-006)

## O que foi construído

O link público (`/b/{slug}`) que cada estabelecimento compartilha com os
clientes para que agendem sozinhos, sem login — o motivo de existir do
produto. Primeira **escrita** no contexto `scheduling` (que até aqui só
tinha leitura, via `GetAvailableSlotsHandler` da TODO-005) e o contexto
`customer` nasce nesta feature.

```
GET  /b/{slug}                          → catálogo de serviços
GET  /b/{slug}/servicos/{serviceId}     → profissionais que atendem esse serviço
GET  /b/{slug}/ofertas/{offeringId}     → horários livres (data escolhível)
POST /b/{slug}/ofertas/{offeringId}     → confirma (nome + telefone)
    honeypot preenchido        → redirect indistinguível de sucesso, nada gravado
    rate limit excedido        → erro tratado, nada gravado
    oferta de outro tenant     → 404
    horário já ocupado         → SlotUnavailableException (exclusion constraint)
    teto de 3 agend. futuros   → erro tratado
    OK                         → Customer (get-or-create) + Appointment (SCHEDULED)
GET  /b/{slug}/agendamentos/{id}        → resumo de sucesso (flash attribute)
```

## O que nasceu nesta feature

| Item | Onde | Por quê |
|---|---|---|
| `customer` (contexto novo) | `customer` | Sexto contexto delimitado — `Customer` em regime CRUD (ADR 0002), get-or-create por telefone dentro do tenant |
| `Appointment` / `AppointmentStatus` | `scheduling.domain` | Primeira entidade de domínio puro de `scheduling` — retrato de duração/preço no momento da reserva |
| Exclusion constraint `appointment_no_overlap` | `V8__scheduling_create_appointment.sql` | Barreira real contra overbooking (ADR 0005), traduzida por `AppointmentPersistenceAdapter` |
| `TenantContextFilter` (segunda via) | `platform.tenant` | Resolve tenant pelo slug da URL pública, além da sessão autenticada (ADR 0004) |
| `LayoutAdvice` | `platform.web` | Um único `@ModelAttribute("businessName")` para as duas fontes (sessão OU slug) |
| `BookingRateLimiter` / honeypot | `scheduling.adapter.in.web` | Defesas contra abuso de formulário público sem login (BR-7/BR-8) |

## Decisões de design centrais

- **`tenantId` nunca vem do formulário** (BR-5) — vem sempre de
  `TenantContext`, resolvido pelo slug antes do controller rodar; todo id
  recebido do cliente (`offeringId`) é revalidado contra esse tenant.
- **Overbooking é impedido pelo banco, não pela aplicação** (BR-4/ADR
  0005) — `findOccupiedRanges` em memória é só para a lista de horários
  livres refletir reservas já feitas; quem decide de verdade é a
  exclusion constraint GiST, confirmada sob concorrência real em
  `PaginaPublicaAgendamentoIT.e2e2` (duas reservas simultâneas, Postgres
  via Testcontainers — H2 não implementa exclusion constraint).
- **Lacuna encontrada durante o `TASK-006`**: `GetAvailableSlotsHandler`
  (TODO-005) nunca descontava `Appointment` já criados da disponibilidade
  calculada — corrigida nesta feature (usuário optou por resolver aqui em
  vez de abrir tarefa separada), somando `findOccupiedRanges` aos
  bloqueios de `TimeOff` já existentes.
- **Dois `@ControllerAdvice` concorrentes pelo mesmo `@ModelAttribute`**
  (achado durante `TASK-009`) — fundidos em `LayoutAdvice` único, porque a
  ordem entre advices sem `@Order` explícito não é garantida.
- **Tela de horários perdia a data escolhida ao dar erro** (achado
  durante `TASK-010`) — corrigido com campo hidden `data` no formulário.
- **`ServiceOfferingNotFoundException` vazando como 422** (achado durante
  `TASK-015`, `PaginaPublicaAgendamentoIT.e2e3`) — o catch genérico
  recarregava a tela de horários, que relançava a mesma exceção sem catch
  ao redor; corrigido com catch específico devolvendo 404.

## Testes

- Domínio puro: `AppointmentTest`.
- Aplicação (mocks): `BookAppointmentHandlerTest`, `GetAvailableSlotsHandlerTest`
  (atualizado para a lacuna do DD-10), `ServiceOfferingDirectoryHandlerTest`.
- Camada web: `PublicBookingControllerTest` (9 casos, `@WebMvcTest`).
- Integração (Testcontainers, Postgres real — obrigatório para a
  exclusion constraint): `PaginaPublicaAgendamentoIT`, E2E-1 a E2E-7,
  incluindo o teste de concorrência real com `ExecutorService` +
  `CountDownLatch`.
- 394 testes no projeto inteiro, 0 falhas, 0 erros; 90% de cobertura de
  instrução (`./mvnw clean verify`).
- Validação manual ponta a ponta via curl: cadastro real de
  estabelecimento → catálogo → profissionais → horários → confirmação →
  sucesso; exclusion constraint, honeypot e rate limit confirmados em
  uso real (o rate limit chegou a bloquear tentativas manuais
  sequenciais — prova de que funciona).

## Quality gates (Layer 3)

Ver `verdicts/{code_review,performance,security}.json`.

- **Security**: `APPROVED` — tenantId nunca aceito do cliente, overbooking
  pelo banco, CSRF ativo em `/b/**`, XSS coberto (só `th:text`), honeypot
  e rate limit bloqueando antes de qualquer efeito colateral, teto por
  telefone reforçado no handler, sem segredo hardcoded, `Customer.toString()`
  sem dado pessoal (LGPD).
- **Code review**: `CAN_PROCEED_WITH_WARNINGS` — um achado menor,
  documentado como DEBT-017 em vez de corrigido às pressas: uma tentativa
  de capturar `DataIntegrityViolationException` em `CustomerDirectoryHandler.findOrCreate`
  foi revertida por piorar o comportamento (a mesma transação já abortada
  pelo Postgres faria a nova consulta falhar de novo, com um erro mais
  confuso).
- **Performance**: `CAN_PROCEED_WITH_WARNINGS` — um aviso (mapa do
  `BookingRateLimiter` cresce por IP distinto sem nunca remover chaves
  vazias; teórico para o piloto) e uma recomendação (índice dedicado para
  `findOccupiedRanges` se o volume por profissional crescer).
