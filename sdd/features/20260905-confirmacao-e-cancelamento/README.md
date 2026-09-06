# confirmacao-e-cancelamento (TODO-007)

## O que foi construído

Fecha o ciclo do cliente que a TODO-006 abriu. Até aqui, a única prova de
um agendamento era a tela de sucesso — dependia de flash attribute e
sumia ao recarregar. Esta feature reproposita a mesma URL
(`/b/{slug}/agendamentos/{id}`) para consultar o agregado de verdade, a
qualquer momento, e adiciona: confirmar presença, cancelar, baixar
`.ics` e falar com o estabelecimento pelo WhatsApp.

```
GET  /b/{slug}/agendamentos/{id}            → detalhes + status atual
POST /b/{slug}/agendamentos/{id}/confirmar  → SCHEDULED → CONFIRMED
POST /b/{slug}/agendamentos/{id}/cancelar   → → CANCELLED (libera o horário)
GET  /b/{slug}/agendamentos/{id}/ics        → arquivo .ics, sem dado do cliente
    id de outro tenant                → 404 (nunca 422 genérico)
    ação sobre CANCELLED ou já passou → sem efeito, nunca erro
```

## O que nasceu/mudou nesta feature

| Item | Onde | Por quê |
|---|---|---|
| `Appointment.confirm()`/`cancel()` | `scheduling.domain` | Únicos métodos de transição de status desde a criação (TODO-006) — absorvem estado terminal sem lançar exceção |
| `ManageAppointmentHandler` | `scheduling.application` | Implementa 3 portas numa classe só (desvio deliberado do padrão "uma classe por porta") |
| `AppointmentController` / `IcsWriter` | `scheduling.adapter.in.web` | Controller novo, separado do fluxo de reserva; `.ics` em texto plano, sem biblioteca |
| `Business.whatsapp` | `organization.domain` | Campo opcional, coletado só no cadastro (TODO-001), migration `V9` |
| `ProfessionalDirectory.find` / `CustomerDirectory.find` | `organization.api` / `customer.api` | Resolvem nome por id para exibição, revalidados por tenant |

## Decisões de design centrais

- **Sem token assinado — o próprio UUID do `Appointment` é a identidade
  do link** (DD-1), revalidado por tenant a cada acesso, mesma defesa já
  usada em toda rota pública do projeto. Decisão tomada explicitamente
  contra a formulação inicial da spec funcional ("token assinado"), que
  foi corrigida depois de comparar com o precedente do próprio projeto
  (ADR 0009).
- **`confirm()`/`cancel()` nunca lançam exceção para estado terminal**
  (DD-3) — cancelar um já cancelado, ou agir sobre um agendamento cujo
  horário já passou, é absorvido silenciosamente. Simplifica o
  controller e casa com o que a spec funcional já pedia ("sem efeito,
  nunca erro").
- **Mudança de status grava por `UPDATE` direto, nunca por `save()`**
  (DD-4) — `AppointmentMapper.toEntity` sempre atribuía `createdAt =
  Instant.now()`, o que corromperia esse campo numa atualização.
- **Achado durante o `TASK-011`**: `AppointmentNotFoundException` sem
  catch específico caía no 422 genérico do `GlobalExceptionHandler`, em
  vez do 404 que a própria spec técnica exigia. Corrigido nas 4 rotas de
  `AppointmentController`, confirmado por `AppointmentControllerTest` e
  pelo E2E-3 do teste de integração.
- **Teto por telefone (BR-9 da TODO-006) passa a contar `CONFIRMED`
  também** (DD-6) — confirmar presença não abre vaga nova no limite de 3
  agendamentos futuros ativos.

## Testes

- Domínio puro: `AppointmentTest` (editado) — `confirm()`/`cancel()`:
  transição normal, idempotência, terminal por cancelamento, terminal
  por horário passado.
- Aplicação (mocks): `ManageAppointmentHandlerTest` (as 3 portas),
  `RegisterBusinessHandlerTest` (WhatsApp opcional gravado/rejeitado).
- Camada web: `AppointmentControllerTest` (10 casos — 4 rotas, 404 de
  tenant errado, `.ics` sem dado do cliente, `wa.me` condicional),
  `RegistrationControllerTest` (WhatsApp válido/inválido).
- Integração (Testcontainers, Postgres real): `ConfirmacaoECancelamentoIT`,
  E2E-1 a E2E-7 — confirmar, cancelar libera horário, isolamento entre
  tenants, idempotência, agendamento no passado somente leitura, teto
  contando `CONFIRMED`, `wa.me` condicional.
- 490 testes no projeto inteiro, 0 falhas, 0 erros; 90% de cobertura de
  instrução (`./mvnw clean verify`).

## Quality gates (Layer 3)

Todos `APPROVED` — ver `verdicts/{code_review,performance,security}.json`.

- **Code review**: `scheduling.domain` continua Java puro;
  `ManageAppointmentHandler` documentado como exceção deliberada ao
  padrão "uma porta por classe"; `updateStatus` é o único ponto que
  persiste mudança de status.
- **Performance**: consulta de detalhes faz 3 buscas fixas, sem laço;
  confirmar/cancelar fazem no máximo 1 busca + 1 `UPDATE`, zero escrita
  quando é no-op.
- **Security**: isolamento por tenant confirmado (BR-1/BR-7); CSRF ativo
  (arquivo não tocado nesta feature); `.ics` sem dado pessoal do cliente
  (BR-9); sem segredo novo (DD-1 evitou essa necessidade de propósito).
