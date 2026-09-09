# gestao-de-clientes (IDEA-018, IDEA-019, IDEA-006)

## O que foi construído

Junta três pedaços do mesmo problema — visão do cliente, contato direto,
controle de pagamento — numa única feature, porque nascem do mesmo dado
(`Appointment` + `Customer`) e se usam juntos na prática: o dono abre o
cliente para ver se é novo, quanto já gastou, se deve algo, e para falar
com ele.

```
/admin/clientes           → lista paginada: nome, telefone, contador de
                             visitas, badge "Novo" (zero atendimentos
                             COMPLETED), link de WhatsApp por cliente
/admin/clientes/{id}      → histórico completo de atendimentos + 3
                             totais (visitas, gasto, em aberto/fiado)
/admin/agenda             → cada linha ganha um controle de status de
                             pagamento (pago/pendente/fiado), ao lado das
                             ações já existentes
```

## O que nasceu/mudou nesta feature

| Item | Onde | Por quê |
|---|---|---|
| `PaymentStatus` (`PAID`/`PENDING`/`ON_CREDIT`) | `scheduling.domain` (novo) | Conceito independente de `AppointmentStatus` — "foi pago?" é pergunta diferente de "o atendimento aconteceu?" |
| `Appointment.markPaymentAsPaid/Pending/OnCredit()` | `scheduling.domain` (editado) | Sem setter genérico — 3 métodos nomeados, imutáveis, sem restrição de transição (BR-2) |
| `CustomerActivityHandler` | `scheduling.application` (novo) | Implementa lista e detalhe de cliente numa classe só (padrão "uma classe, várias portas", IDEA-017) |
| `CustomerAdminController` | `scheduling.adapter.in.web` (novo) | `/admin/clientes` e `/admin/clientes/{id}` |
| `customer.api.listForTenant` | `customer.api` (editado) | Novo método paginado, reaproveitado por `scheduling` (que já depende de `customer.api` desde a TODO-008) |
| `findActivityByCustomerIds`, `findCompletedByTenantIdAndCustomerId` | `scheduling.application.port.out`/adapter (novo) | Agregação em lote (GROUP BY) e histórico por cliente — reaproveitam `appointment_customer_idx` (V8), nenhum índice novo |
| Migration V10 | `db/migration` | Coluna `payment_status`, default `PENDING` |
| `GlobalExceptionHandler.parametroInvalido` | `platform.web` (editado) | Achado no code review — corrige um gap real, não específico desta feature |

## Decisões de design centrais

- **A tela inteira mora em `scheduling`, não em `customer`** (DD-1): embora
  a URL seja sobre "clientes", `scheduling` já depende de `customer.api`
  desde a TODO-008 — colocar a tela no lado "óbvio" (`customer`) exigiria
  abrir a dependência contrária e fecharia um ciclo, a mesma lição da DD-6
  da TODO-110. Promovido ao `PATTERNS.md` como regra geral: "verifique quem
  já depende de quem antes de decidir onde uma tela cruzada mora".
- **Paginação real via agregação SQL (DD-2)**: o dono pediu explicitamente
  paginação por performance (BR-6) — descartada a alternativa de carregar
  tudo em memória (funcionaria, mas é exatamente o que a paginação existe
  para evitar). Duas consultas por página (clientes + atividade em lote),
  nunca uma por cliente.
- **Sem histórico de mudança de status de pagamento (DD-3)**: só o valor
  atual é guardado — nenhuma regra de negócio pede auditoria completa
  agora; cerimônia sem retorno (ADR 0002).
- **Achado real durante o code review**: a nova rota de pagamento foi a
  primeira do projeto a fazer bind direto de enum via `@RequestParam` — um
  valor inválido caía no handler genérico como "defeito" (500) em vez de
  uso incorreto (400). Corrigido com um `@ExceptionHandler` dedicado,
  promovido ao `PATTERNS.md`.
- **Fora de escopo, decidido na spec funcional**: cobrança automática de
  fiado (depende de canal de notificação, TODO-111/IDEA-001, ainda não
  existe); relatório agregado entre clientes (IDEA-020); "duplicar agenda
  do dia anterior" do relatório original — deliberadamente fora, caso de
  uso ainda incerto.

## Testes

- Domínio: `AppointmentTest` — 6 casos novos para as 3 transições de
  pagamento, absorção e default `PENDING`.
- Aplicação: `CustomerActivityHandlerTest` (novo, 4 casos — `isNew`,
  paginação, totais, isolamento de tenant); `ProfessionalAgendaHandlerTest`
  — 3 casos novos (`updatePaymentStatus`).
- Web: `CustomerAdminControllerTest` (novo, 4 casos); `AgendaControllerTest`
  — 4 casos novos (rota pagamento, CSRF, 404, status inválido → 400);
  `GlobalExceptionHandlerTest` — 1 caso novo.
- Integração (Testcontainers): `CustomerActivityIT` (novo) cobre E2E-1 a
  E2E-4 e E2E-6; `AgendaProfissionalIT` ganha E2E-10 (E2E-5 da spec —
  fiado na agenda reflete no total em aberto do cliente).
- 614 testes no projeto inteiro, 0 falhas, 0 erros; cobertura mantida
  acima do piso de 80% (`./mvnw clean verify`).

## Quality gates (Layer 3)

Todos `APPROVED` — ver `verdicts/{code_review,performance,security}.json`.

- **Code review**: achado real (`MethodArgumentTypeMismatchException` não
  tratado) encontrado e corrigido durante a própria revisão, com testes
  novos comprovando o fechamento. `CustomerActivityHandler` confirmado
  morando em `scheduling`, sem import de `customer.application`/`domain`.
- **Performance**: nenhuma consulta N+1; `findActivityByCustomerIds` é uma
  única query agregada em lote; nenhum índice novo necessário. 1
  recomendação não bloqueante sobre ordenação por nome (aceitável no
  volume do piloto, já antecipada na spec técnica).
- **Security**: rotas novas caem no `hasRole("OWNER")` já existente de
  `/admin/**`, CSRF obrigatório, `tenantId` sempre resolvido via
  `TenantContext`/revalidado contra a sessão, isolamento entre tenants
  confirmado nas três rotas. Nenhum dado pessoal novo além do que já era
  exposto em outras telas do mesmo painel.
