# gestao-de-clientes - Functional Spec

**Feature**: gestao-de-clientes
**Backlog**: IDEA-018, IDEA-019, IDEA-006
**Status**: approved
**Data**: 2026-09-09
**Aprovado por**: Elton Marques em 2026-09-09T22:18:20Z

---

## Problem Statement

Hoje `Customer` e `Appointment` já se relacionam (`customerId`), mas não
existe nenhuma tela que junte esse dado numa visão por cliente. O dono
depende de memória (ou do caderno que o SimboraAgendar deveria substituir) para
lembrar quem é cliente novo — e por isso merece atenção redobrada contra
no-show —, quem já veio muitas vezes, e quem ficou devendo. Também não há
como contatar um cliente direto pelo sistema: o único link de WhatsApp que
existe hoje vai do cliente para o estabelecimento (`Business.whatsapp`),
nunca o caminho inverso.

Ao mesmo tempo, `Appointment` não guarda se um atendimento foi pago. O
dono controla isso de cabeça ou em papel — "fiado" (deixou para pagar
depois, por confiança) é um conceito real do negócio que o sistema não
reconhece.

Esta feature junta três pedaços do mesmo problema — visão do cliente,
contato direto, controle de pagamento — numa única tela nova,
`/admin/clientes`, porque nascem do mesmo dado (`Appointment` +
`Customer`) e se usam juntos na prática: o dono abre o cliente para ver
se ele é novo, quanto já gastou, se deve algo, e para falar com ele.

---

## Objectives

1. O dono vê, numa lista, todos os clientes do estabelecimento com
   contador de visitas e sinalização de "cliente novo" (risco maior de
   no-show).
2. Ao abrir um cliente, o dono vê o histórico completo de atendimentos
   (data, serviço, valor, status de pagamento) e três totais: visitas,
   valor gasto, valor em aberto (fiado).
3. O dono contata o cliente direto por WhatsApp, sem trocar de tela ou
   app.
4. O dono registra se um atendimento foi pago, ficou pendente, ou foi
   fiado — no mesmo lugar e momento em que já marca "Concluir" na agenda
   (TODO-110).

---

## Scope

### In Scope

- Nova tela `/admin/clientes`: lista paginada de clientes do
  estabelecimento, com contador de visitas e sinalização de "cliente
  novo".
- Nova tela de detalhe `/admin/clientes/{id}`: histórico completo de
  atendimentos (data, serviço, valor, status de pagamento) e os totais
  (visitas, valor gasto, valor em aberto).
- Link de WhatsApp (`wa.me/{telefone do cliente}`) na lista e/ou no
  detalhe do cliente, para o dono contatar o cliente.
- Novo conceito de domínio: status de pagamento por agendamento
  (pago/pendente/fiado), independente do status do agendamento
  (`AppointmentStatus`).
- Ação de marcar o status de pagamento na agenda (`admin/agenda.html`),
  ao lado do botão "Concluir" já existente (TODO-110).
- Novo item de menu "Clientes" na sidebar já existente (TODO-110).

### Out of Scope

- Cobrança automática ou lembrete de fiado (depende de canal de
  notificação — TODO-111/IDEA-001, ainda não existe).
- Edição retroativa de serviço ou valor de uma visita já concluída —
  só o status de pagamento pode ser alterado depois.
- Relatório agregado entre todos os clientes (faturamento, serviço mais
  vendido) — é a IDEA-020, feature separada.
- Avaliação do cliente sobre o atendimento — é a IDEA-016, feature
  separada.
- "Duplicar agenda do dia anterior" — deliberadamente fora, combinado
  com o dono em 2026-09-09 (caso de uso ainda incerto).
- Qualquer mudança na página pública de agendamento (`/b/{slug}/**`).

---

## User Stories

### US-1: Lista de clientes com contador de visitas e sinalização de novo

**Como** dono de um estabelecimento,
**quero** ver todos os meus clientes numa lista, com quantas vezes cada
um já veio,
**para** identificar rapidamente quem é cliente novo (maior risco de
no-show) e quem é cliente frequente.

**Acceptance Criteria**:
- AC-1: A lista mostra nome, telefone e contador de visitas de cada
  cliente do estabelecimento (`tenantId` da sessão).
- AC-2: Cliente sem nenhum atendimento `COMPLETED` aparece sinalizado
  como "novo" (BR-1).
- AC-3: A lista é paginada — não carrega todos os clientes de uma vez
  (BR-6).
- AC-4: Cliente de outro estabelecimento nunca aparece na lista.

### US-2: Histórico e totais do cliente

**Como** dono de um estabelecimento,
**quero** abrir um cliente e ver todo o histórico de atendimentos dele,
**para** decidir sobre desconto, fidelização, ou cobrança de fiado com
informação real, não de memória.

**Acceptance Criteria**:
- AC-1: O detalhe mostra cada atendimento `COMPLETED` do cliente: data,
  serviço, valor, e status de pagamento (pago/pendente/fiado).
- AC-2: O detalhe mostra três totais: total de visitas (BR-7), valor
  total gasto (BR-8), valor total em aberto/fiado (BR-9).
- AC-3: Agendamentos que não são `COMPLETED` (cancelado, falta, ainda
  agendado) não entram no histórico de atendimentos nem nos totais —
  são agendamentos que não aconteceram (ou ainda não aconteceram).
- AC-4: Abrir um cliente de outro tenant (id de outro estabelecimento)
  devolve 404, nunca o histórico.

### US-3: Contato direto por WhatsApp

**Como** dono de um estabelecimento,
**quero** um link que abra o WhatsApp já com o número do cliente,
**para** confirmar presença, avisar atraso, ou cobrar fiado sem trocar de
tela ou copiar o telefone manualmente.

**Acceptance Criteria**:
- AC-1: A lista e/ou o detalhe do cliente mostram um link/botão de
  WhatsApp que abre `wa.me/{telefone do cliente}`.
- AC-2: O link usa o telefone já cadastrado do cliente
  (`Customer.phone`) — nenhum campo novo de contato.

### US-4: Marcar status de pagamento na agenda

**Como** dono (ou profissional) atendendo pelo painel,
**quero** marcar se o atendimento foi pago, ficou pendente, ou foi
fiado, no mesmo momento em que marco "Concluir",
**para** não ter que lembrar disso depois nem anotar em outro lugar.

**Acceptance Criteria**:
- AC-1: Na lista da agenda (`admin/agenda.html`), cada agendamento
  mostra o status de pagamento atual e permite trocá-lo entre
  pago/pendente/fiado.
- AC-2: Um agendamento novo nasce com status de pagamento `PENDENTE`
  (BR-3) — nenhum agendamento fica sem status de pagamento definido.
- AC-3: O status de pagamento pode ser alterado a qualquer momento,
  em qualquer direção (ex.: `FIADO` → `PAGO` quando o cliente volta e
  quita) — não é uma máquina de estados com transições proibidas, ao
  contrário do `AppointmentStatus` (BR-2).
- AC-4: Mudar o status de pagamento não afeta o `AppointmentStatus` do
  agendamento (são dois conceitos independentes — BR-2) nem exige que o
  agendamento já esteja `COMPLETED`.
- AC-5: `tenantId` do agendamento segue a mesma regra de sempre: nunca
  vem do cliente, é revalidado contra a sessão.

---

## Business Rules

### Core Rules

- BR-1: "Cliente novo" = zero agendamentos com status `COMPLETED`. Um
  agendamento cancelado ou uma falta não tiram o rótulo de "novo" — o
  cliente nunca foi de fato atendido.
- BR-2: Status de pagamento (`PAGO`/`PENDENTE`/`FIADO`) é um conceito
  **independente** de `AppointmentStatus` — respondem perguntas
  diferentes ("foi pago?" vs. "o atendimento aconteceu?"). Não é uma
  máquina de estados com transição proibida: pode mudar em qualquer
  direção, a qualquer momento.
- BR-3: Todo agendamento novo nasce com status de pagamento `PENDENTE`.
- BR-4: `tenantId` de qualquer consulta desta feature (lista de
  clientes, detalhe, marcação de pagamento) vem sempre da sessão
  autenticada — nunca de parâmetro ou formulário.
- BR-5: Contador de visitas (US-1) e histórico (US-2) contam **apenas**
  agendamentos `COMPLETED` — o mesmo critério de BR-1.
- BR-6: A lista de clientes é paginada, para não degradar com o
  crescimento da base de clientes (decisão explícita do dono,
  2026-09-09 — prioriza performance desde a primeira versão).
- BR-7: Total de visitas = contagem de agendamentos `COMPLETED` do
  cliente.
- BR-8: Valor total gasto = soma do preço (`price`) de todos os
  agendamentos `COMPLETED` do cliente — representa o valor de serviços
  já realizados, independente do status de pagamento de cada um.
- BR-9: Valor total em aberto = soma do preço dos agendamentos do
  cliente com status de pagamento `FIADO`.

### Validation Invariants

- Status de pagamento vive em `Appointment` (mesmo agregado do
  `AppointmentStatus`) — não é uma tabela nova, é um campo novo. A spec
  técnica decide o nome exato do enum/coluna e a migration necessária.
- Nenhum novo dado pessoal de cliente é coletado — `Customer.phone` e
  `Customer.name` já existem desde a TODO-006.

---

## Data Model (conceitual, para a spec técnica detalhar)

- Novo enum de pagamento em `scheduling.domain` (nome técnico exato a
  definir): três valores, conceito pago/pendente/fiado (BR-2, BR-3).
  Campo novo em `Appointment`, com método(s) de domínio para mudar de
  valor — sem setter público (mesmo padrão de `confirm()`/`cancel()`),
  mas sem restrição de transição entre os três valores (BR-2).
- Nova projeção/consulta agregada por cliente (nome técnico a definir):
  `customerId`, contador de visitas, valor gasto, valor em aberto, "é
  novo?" — precisa decidir, na spec técnica, se mora em `scheduling.api`
  (mesmo padrão de `DailyScheduleDirectory`, TODO-110) ou se é uma nova
  porta em `customer.api`, já que cruza os dois contextos
  (`Appointment.customerId`/`price`/status × `Customer.name`/`phone`).
- Nenhuma tabela nova — só coluna nova em `appointment` (status de
  pagamento) e consultas agregadas sobre dado já existente.

---

## User Experience

- **Sidebar**: novo item "Clientes", mesmo padrão visual dos 8 já
  existentes (TODO-110).
- **Lista de clientes**: nome, telefone, contador de visitas, badge
  "Novo" quando aplicável, botão/link de WhatsApp por linha, paginação
  no rodapé.
- **Detalhe do cliente**: cabeçalho com nome/telefone/WhatsApp e os três
  totais (visitas, gasto, em aberto), seguido da lista de histórico
  (data, serviço, valor, status de pagamento de cada atendimento).
- **Agenda** (`admin/agenda.html`): cada linha ganha um controle de
  status de pagamento (ex.: um seletor ou três botões
  pago/pendente/fiado), ao lado dos já existentes de
  confirmar/cancelar/concluir.

---

## Critical E2E Test Scenarios

### E2E-1: Lista de clientes pagina corretamente

Com mais clientes do que cabem numa página, a segunda página mostra os
próximos clientes, sem repetir nem pular nenhum.

### E2E-2: Cliente sem histórico aparece sinalizado como novo

Um cliente que nunca teve um agendamento `COMPLETED` aparece marcado
como "novo" na lista; depois de um atendimento concluído, deixa de
aparecer assim.

### E2E-3: Detalhe do cliente mostra histórico e totais corretos

Com atendimentos `COMPLETED` (alguns `PAGO`, um `FIADO`) e um
`CANCELLED` semeados, o detalhe do cliente mostra só os `COMPLETED` no
histórico, e os três totais (visitas, gasto, em aberto) batem com a
soma esperada — o cancelado não entra em nenhuma conta.

### E2E-4: Link de WhatsApp usa o telefone certo

O link de WhatsApp do cliente aponta para o telefone cadastrado dele,
não o do estabelecimento nem o de outro cliente.

### E2E-5: Marcar status de pagamento na agenda reflete no histórico

O dono marca um agendamento como `FIADO` na agenda; ao abrir o cliente,
o valor aparece no total em aberto e o atendimento aparece com o status
correto no histórico.

### E2E-6: Isolamento entre tenants

Um dono autenticado no estabelecimento A não vê, em nenhuma tela desta
feature (lista, detalhe, ou tentativa de acesso direto por id), nenhum
cliente ou agendamento do estabelecimento B.

---

## Success Metrics

- O dono decide sobre desconto, fidelização ou cobrança de fiado
  olhando o perfil do cliente, sem depender de memória ou anotação em
  papel.
- Todo cliente cadastrado aparece na lista com contagem de visitas
  correta e sinalização de novo coerente com o histórico real.

---

## Non-Functional Requirements

- A lista de clientes e a consulta agregada por cliente são resolvidas
  com paginação e consulta indexada por `tenant_id` — nenhuma consulta
  carrega todos os clientes/agendamentos do estabelecimento de uma vez
  (BR-6, ver `sdd/PATTERNS.md`, "Consulta de leitura não carrega
  agregado").
- Nenhuma dependência nova: WhatsApp continua sendo link `wa.me`
  (mesmo mecanismo já usado para `Business.whatsapp`), sem API paga.

---

## Assumptions

- Ordenação padrão da lista de clientes: por nome (alfabética) — a
  decidir/ajustar na spec técnica se o dono preferir outra (ex.: última
  visita mais recente primeiro).
- Tamanho de página e mecanismo de paginação (offset ou keyset) ficam
  para a spec técnica decidir, dado o volume ainda pequeno do piloto.
- Dono e profissional continuam sendo a mesma pessoa nesta fase (mesma
  suposição da TODO-110) — marcar status de pagamento fica disponível
  para quem já acessa `/admin/agenda`.
- O link de WhatsApp usa o mesmo mecanismo (`wa.me`) já usado para o
  contato cliente→estabelecimento — não é uma integração nova, só uma
  segunda aplicação do mesmo padrão.
