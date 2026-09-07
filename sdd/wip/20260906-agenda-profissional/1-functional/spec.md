# agenda-profissional - Functional Spec

**Feature**: agenda-profissional
**Backlog**: TODO-008
**Status**: approved
**Data**: 2026-09-07
**Aprovado por**: Elton Marques em 2026-09-07T13:41:21Z

---

## Problem Statement

O dono continua dependendo de papel, memória ou perguntar ao próprio
sistema por fora para saber o que tem na agenda. Nenhuma tela em
`/admin/**` mostra os agendamentos de um profissional; quando um cliente
liga em vez de usar o link público, o dono não tem como criar esse
agendamento pelo painel; quando precisa cancelar ou remarcar em nome de
alguém, também não tem onde fazer isso.

Esta feature fecha o ciclo do dono: uma tela de agenda por profissional
e por dia, com as três ações que o backlog pediu — criar, cancelar e
reagendar — mais uma quarta que já existe no domínio e cabe naturalmente
aqui: confirmar presença. Sem isso, o produto continua incompleto para
um barbeiro real usar no dia a dia.

---

## Objectives

1. Dono visualiza, dentro de `/admin/**`, a agenda de um profissional
   escolhido, um dia por vez, com seletor de data.
2. Dono cria um agendamento manualmente para um cliente que ligou ou
   apareceu, sem passar pelo funil público (`/b/{slug}`).
3. Dono cancela um agendamento em nome do cliente, a qualquer momento —
   inclusive um agendamento cujo horário já passou (diferente da regra do
   cliente, BR-4 da TODO-007).
4. Dono reagenda um agendamento existente, podendo trocar profissional,
   oferta, data e horário, sem perder o registro do que existia antes.
5. Dono confirma presença de um agendamento pelo painel, reaproveitando o
   mesmo mecanismo que o cliente usa pelo link (TODO-007).

---

## Scope

### In Scope

- Tela de agenda (`GET`) em `/admin/**`: lista os agendamentos de um
  profissional escolhido numa data escolhida (seletor de data, `GET`
  simples — sem JavaScript, mesma filosofia de `ADR 0007`).
- Criar agendamento (`POST`): dono escolhe profissional, oferta, data e
  horário, informa nome e telefone do cliente — get-or-create pelo
  telefone dentro do tenant, mesmo mecanismo de `CustomerDirectory`
  (BR-3 da TODO-006).
- Cancelar agendamento (`POST`): sem restrição de horário passado.
- Reagendar agendamento (`POST`): dono escolhe novo profissional, oferta,
  data e horário para o mesmo cliente; o registro original é preservado
  como `CANCELLED` (ADR 0011, nada é apagado), um novo `Appointment`
  nasce `SCHEDULED`.
- Confirmar presença (`POST`): reaproveita `ConfirmAppointmentUseCase`
  (TODO-007) a partir da tela de agenda.
- Exclusion constraint (ADR 0005) continua sendo a garantia real contra
  overbooking — vale também para criação manual e para o novo
  agendamento nascido de um reagendamento.
- Autenticação: mesma sessão `OWNER` já usada no resto de `/admin/**`
  (`SecurityConfig`) — nenhuma rota nova liberada por engano.

### Out of Scope

- **Login para `Professional`.** Só o dono (`User`, papel `OWNER`)
  acessa a agenda; o profissional não autentica nesta feature.
- **Marcar falta (`NO_SHOW`).** Já previsto no glossário, mas fora do
  que o backlog pediu (criar, cancelar, reagendar) — fica para outra
  feature.
- **Notificação automática ao cliente.** Sem canal de e-mail/SMS/WhatsApp
  transacional no projeto (TODO-108/TODO-109) — o dono avisa por fora,
  como já faz hoje.
- **Teto de 3 agendamentos futuros por telefone (BR-9 da TODO-006).** É
  defesa contra abuso do formulário público sem login; não se aplica a
  uma ação autenticada do próprio dono.
- **Busca de cliente por lista/autocomplete.** O dono digita nome e
  telefone; se o telefone já existe no tenant, reaproveita o `Customer`
  — mesmo comportamento do link público, sem tela de busca dedicada.
- **Calendário semanal ou mensal.** Só visão de um dia por vez, com
  seletor de data — mesma filosofia sem JavaScript das telas públicas.
- **Editar nome/telefone do cliente pela tela de agenda.** Corrigir o
  cadastro do cliente, se precisar, fica para outra feature.

---

## User Stories

### US-1: Ver a agenda de um profissional num dia

**Como** dono do estabelecimento,
**Eu quero** ver a lista de agendamentos de um profissional numa data
escolhida,
**Para que** eu saiba o que tenho marcado sem precisar abrir uma agenda
de papel.

**Acceptance Criteria**:
- Tela mostra profissional escolhido, data escolhida, e a lista de
  agendamentos daquele profissional naquela data — horário, serviço,
  nome e telefone do cliente, e status atual.
- Trocar de profissional ou de data é um `GET` simples, sem preservar
  formulário nenhum de outra ação em andamento.
- Dia sem nenhum agendamento mostra mensagem clara, não uma tela vazia
  sem explicação.

### US-2: Criar agendamento manualmente

**Como** dono do estabelecimento,
**Eu quero** criar um agendamento para um cliente que ligou ou apareceu,
sem ele precisar usar o link público,
**Para que** eu não perca esse cliente só porque ele não usa o link
sozinho.

**Acceptance Criteria**:
- Formulário pede profissional, oferta, data, horário, nome e telefone
  do cliente.
- Telefone já cadastrado no tenant reaproveita o `Customer` existente;
  telefone novo cria um `Customer` novo — mesmo comportamento do link
  público.
- `Appointment` criado nasce `SCHEDULED`, com o retrato de
  duração/preço da oferta escolhida (mesma regra da TODO-006).
- Duas tentativas para o mesmo profissional e o mesmo horário — uma pelo
  painel, outra pelo link público, ou as duas pelo painel — resultam em
  exatamente um agendamento; a outra recebe erro tratado.
- Criar manualmente não é bloqueado pelo teto de 3 agendamentos futuros
  por telefone, mesmo que o cliente já tenha 3 vindos do link público.

### US-3: Cancelar agendamento em nome do cliente

**Como** dono do estabelecimento,
**Eu quero** cancelar um agendamento a qualquer momento, mesmo depois do
horário já ter passado,
**Para que** eu consiga corrigir um erro de digitação ou registrar um
cancelamento que o cliente me avisou por telefone, tarde.

**Acceptance Criteria**:
- Cancelar funciona independentemente de o horário já ter passado —
  diferente da regra do cliente pelo link (BR-4 da TODO-007).
- Agendamento cancelado libera o horário na disponibilidade calculada
  (TODO-005) e na exclusion constraint (ADR 0005), igual ao cancelamento
  pelo cliente.
- Cancelar um agendamento já `CANCELLED` não é erro — idempotente, mesmo
  comportamento de `CancelAppointmentUseCase` (TODO-007).

### US-4: Reagendar um agendamento

**Como** dono do estabelecimento,
**Eu quero** mover um agendamento para outro profissional, data ou
horário,
**Para que** eu resolva um conflito de agenda sem precisar cancelar e
pedir para o cliente agendar tudo de novo sozinho.

**Acceptance Criteria**:
- Dono escolhe novo profissional, nova oferta, nova data e novo horário
  para o mesmo cliente do agendamento original.
- O agendamento original fica com status `CANCELLED` — nunca é
  editado/apagado in-place (ADR 0011).
- Um novo `Appointment` nasce `SCHEDULED`, com o retrato (duração/preço)
  da nova oferta escolhida.
- A mesma garantia de US-2 vale para o novo horário: exclusion constraint
  decide, nunca a aplicação sozinha.
- Reagendar um agendamento para o profissional/horário que ele mesmo já
  ocupava (sem mudança real) ainda assim segue o mesmo fluxo (cancela o
  antigo, cria um novo) — não precisa de um caminho especial de "sem
  mudança".

### US-5: Confirmar presença pelo painel

**Como** dono do estabelecimento,
**Eu quero** confirmar presença de um agendamento pela minha própria
tela, sem depender do cliente usar o link,
**Para que** eu registre que sei que o cliente vai aparecer, mesmo que
ele tenha confirmado por telefone em vez de pelo link.

**Acceptance Criteria**:
- Botão de confirmar presença aparece na lista de agendamentos do dia,
  para agendamentos `SCHEDULED`.
- Ação reaproveita exatamente `ConfirmAppointmentUseCase` (TODO-007) —
  mesmas regras de idempotência e de estado terminal.

### US-6: Isolamento entre tenants continua valendo

**Como** dono do estabelecimento,
**Eu quero** que minhas ações na agenda nunca alcancem agendamento de
outro estabelecimento,
**Para que** um erro de digitação de id não vaze nem altere dado de
outro tenant.

**Acceptance Criteria**:
- Toda leitura e escrita desta feature é filtrada pelo tenant da sessão
  autenticada — nunca por um id vindo cru do formulário.
- Profissional, oferta e agendamento informados são sempre revalidados
  contra esse tenant antes de qualquer leitura ou gravação.

---

## Business Rules

### Core Rules

- **BR-1 (ADR 0005)**: A exclusion constraint do banco é a garantia real
  contra overbooking também para criação manual e para o `Appointment`
  novo nascido de um reagendamento — a mesma regra fundamental do
  `CLAUDE.md`, sem exceção para o dono.
- **BR-2**: Cancelar pelo dono não tem restrição de horário passado —
  diferente da regra do cliente (BR-4 da TODO-007), que trava ação sobre
  agendamento cujo horário já passou.
- **BR-3 (ADR 0011)**: Reagendar nunca edita o registro original — ele
  vira `CANCELLED`, e um novo `Appointment` nasce `SCHEDULED`. Nada é
  apagado nem sobrescrito.
- **BR-4**: O teto de 3 agendamentos futuros ativos por telefone (BR-9
  da TODO-006) não se aplica à criação manual pelo dono — é defesa
  contra abuso do formulário público sem login, e uma ação autenticada
  do próprio dono não é o mesmo risco.
- **BR-5**: `tenantId` nunca vem de nenhum campo do formulário — vem
  exclusivamente da sessão `OWNER` autenticada, mesma regra fundamental
  usada em todo o resto de `/admin/**`.
- **BR-6 (BR-3 da TODO-006)**: Cliente é resolvido por telefone
  normalizado dentro do tenant — telefone repetido reaproveita o
  `Customer` existente, nunca duplica.
- **BR-7**: Profissional, oferta e agendamento informados por um campo de
  formulário são sempre revalidados contra o tenant da sessão antes de
  qualquer leitura ou gravação — nunca aceitos "cegamente".

### Validation Invariants

- Profissional/oferta/agendamento de outro tenant, ou inexistente →
  erro, nunca leitura nem gravação.
- Horário que colide com outro `Appointment` ativo do mesmo profissional
  → erro vindo da exclusion constraint, traduzido em mensagem amigável
  (mesma tradução de `SlotUnavailableException`, TODO-006).
- Ação sobre agendamento já `CANCELLED` → idempotente, sem efeito, nunca
  erro (mesma regra de `ManageAppointmentHandler`, TODO-007).

---

## Data Model (conceitual, para a spec técnica detalhar)

Nenhuma entidade nova. Reaproveita `Appointment`/`AppointmentStatus`
(`scheduling`, TODO-006/007), `Customer` (`customer`, TODO-006),
`ServiceOffering`/`Service` (`catalog`) e `Professional`
(`organization`). A spec técnica decide se reagendar é implementado como
"cancelar + criar" (dois `Appointment`) ou uma operação atômica — o
requisito de produto (BR-3) é só que o registro original preserve seu
histórico como `CANCELLED`.

Fontes de leitura (nenhuma mudança de contrato esperada, a confirmar na
spec técnica):
- `organization.api.ProfessionalDirectory` — profissionais do tenant.
- `catalog.api.ServiceOfferingDirectory` — ofertas por profissional.
- Consulta nova em `scheduling` — agendamentos de um profissional numa
  data, com nome/telefone do cliente (diferente da tela pública, que
  nunca expõe esse dado).

---

## User Experience

### Fluxo principal — ver e agir sobre a agenda

1. Dono autenticado abre a tela de agenda. Escolhe um profissional e uma
   data (ou usa os padrões: primeiro profissional ativo, hoje).
2. Vê a lista de agendamentos daquele profissional naquele dia — horário,
   serviço, cliente, status.
3. Para cada agendamento `SCHEDULED`, pode confirmar presença ou
   cancelar. Para `CONFIRMED`, pode cancelar. Para qualquer status, pode
   reagendar (exceto `CANCELLED`, que é estado final).

### Fluxo secundário — criar manualmente

1. Dono clica em "novo agendamento" (a partir da tela de agenda, já com
   profissional e data pré-selecionados).
2. Escolhe oferta (do profissional escolhido), horário, informa nome e
   telefone do cliente.
3. Confirma. Volta para a tela de agenda, que agora mostra o novo
   agendamento.

### Fluxo secundário — reagendar

1. A partir de um agendamento na lista, dono escolhe "reagendar".
2. Escolhe novo profissional (ou mantém o mesmo), nova oferta, nova data
   e novo horário.
3. Confirma. O agendamento original desaparece da lista do dia
   original (fica `CANCELLED`); o novo aparece na lista do novo dia.

### Edge Cases

- **Profissional sem nenhuma oferta ativa** — tela de criar/reagendar
  mostra mensagem clara, sem lista de ofertas vazia silenciosa.
- **Reagendar para um horário que colide com outro agendamento** — erro
  tratado (mesma exclusion constraint), dono tenta outro horário.
- **Cancelar um agendamento que o cliente também está tentando cancelar
  ao mesmo tempo pelo link público** — idempotente, sem erro para
  nenhum dos dois lados.
- **Criar um agendamento cujo horário já colide com um existente do
  mesmo profissional** — mesmo erro tratado que a tela pública já dá
  (BR-1/`SlotUnavailableException`).

---

## Critical E2E Test Scenarios

> Sem LTP nesta instalação. Cenários viram testes de integração e web
> contra Postgres real, como nas features anteriores.

### E2E-1: Caminho feliz — criar agendamento manualmente

**Criticidade**: 🔴 Critical — sem isto a feature não existe.

1. Dono autenticado cria um agendamento para um profissional, oferta,
   data e horário livres, com nome e telefone de um cliente novo.

**Resultado esperado**: `Appointment` `SCHEDULED` criado com o retrato
correto; `Customer` novo criado com aquele telefone.

### E2E-2: Concorrência entre painel e link público

**Criticidade**: 🔴 Critical — protege BR-1, o coração da garantia contra
overbooking.

1. Uma criação manual pelo painel e uma reserva pelo link público
   disputam o mesmo profissional e o mesmo horário, ao mesmo tempo.

**Resultado esperado**: exatamente um `Appointment` `SCHEDULED`; o outro
lado recebe erro tratado, nunca 500.

### E2E-3: Dono cancela agendamento já passado

**Criticidade**: 🔴 Critical — protege BR-2, a diferença central desta
feature em relação ao cancelamento pelo cliente.

1. Um agendamento cujo `startsAt` já passou.
2. Dono cancela pelo painel.

**Resultado esperado**: status muda para `CANCELLED` sem erro — ao
contrário do que aconteceria se o cliente tentasse pelo link (TODO-007,
BR-4, sem efeito).

### E2E-4: Reagendar preserva o registro original

**Criticidade**: 🔴 Critical — protege BR-3.

1. Um agendamento `SCHEDULED` existente.
2. Dono reagenda para outro profissional, outra oferta, outro horário.

**Resultado esperado**: o `Appointment` original fica `CANCELLED`; um
novo `Appointment` `SCHEDULED` existe com os dados novos; o horário
antigo aparece livre na disponibilidade calculada, o novo aparece
ocupado.

### E2E-5: Isolamento entre tenants

**Criticidade**: 🔴 Critical — mesma garantia de todas as features
anteriores.

1. Dono do tenant A tenta ver, cancelar ou reagendar um agendamento do
   tenant B (id manipulado).

**Resultado esperado**: erro/404 — nenhuma leitura nem gravação cruza o
tenant.

### E2E-6: Criação manual não é bloqueada pelo teto por telefone

**Criticidade**: 🟡 Important — protege BR-4.

1. Um telefone já tem 3 agendamentos futuros ativos (criados pelo link
   público, TODO-006).
2. Dono cria um 4º agendamento manualmente para esse mesmo telefone.

**Resultado esperado**: aceito — o teto não se aplica à criação
autenticada do dono.

### E2E-7: Confirmar presença pelo painel

**Criticidade**: 🟢 Should — protege US-5.

1. Um agendamento `SCHEDULED`.
2. Dono confirma presença pela tela de agenda.

**Resultado esperado**: status muda para `CONFIRMED`, mesmo mecanismo de
`ConfirmAppointmentUseCase` (TODO-007).

### E2E Test Summary

| ID | Cenário | Tipo | Cobre |
|---|---|---|---|
| E2E-1 | Criar agendamento manualmente | Caminho feliz | US-2, BR-1 |
| E2E-2 | Concorrência painel × link público | Concorrência | BR-1 |
| E2E-3 | Cancelar agendamento passado | Caminho feliz | US-3, BR-2 |
| E2E-4 | Reagendar preserva histórico | Caminho feliz | US-4, BR-3 |
| E2E-5 | Isolamento entre tenants | Isolamento | BR-5, BR-7 |
| E2E-6 | Teto por telefone não bloqueia dono | Erro/exclusão | BR-4 |
| E2E-7 | Confirmar presença pelo painel | Caminho feliz | US-5 |

---

## Success Metrics

### Business Metrics

- Primeiro agendamento criado pelo painel (não pelo link público) —
  marco qualitativo do piloto, sem meta numérica ainda.

### User Metrics

- Dono consegue ver, criar, cancelar e reagendar sem precisar perguntar
  "como faço isso" — sem meta numérica formal nesta fase.

### Technical Metrics

- Zero overbooking sob concorrência entre painel e link público —
  **target: 0**, verificado por E2E-2.
- Zero vazamento entre tenants — **target: 0**, verificado por E2E-5.

---

## Non-Functional Requirements

### Performance

Sem exigência numérica formal — volume do piloto. Consulta da agenda de
um dia é limitada a um profissional e uma data — nunca uma listagem sem
filtro.

### Security

Primeira tela admin de `scheduling` — mesma proteção de sessão `OWNER`
já usada em `organization`/`catalog`:
- `tenantId` nunca vem do formulário (BR-5) — vem da sessão autenticada,
  regra fundamental do `CLAUDE.md`.
- Profissional/oferta/agendamento revalidados contra o tenant da sessão
  (BR-7) antes de qualquer leitura ou gravação.
- Overbooking impedido pelo banco (BR-1/ADR 0005), não pela aplicação.
- Nome e telefone do cliente aparecem nesta tela (diferente da pública)
  porque quem vê é o próprio dono, autenticado — não é exposição
  indevida.

---

## Assumptions

- O teto de 3 agendamentos futuros por telefone (BR-9 da TODO-006) não
  se aplica à criação manual pelo dono (BR-4) — decisão registrada aqui
  para não ser lida como omissão.
- Confirmar presença pelo painel (US-5) entrou nesta feature por ser uma
  extensão barata de um mecanismo já existente (TODO-007), mesmo não
  estando no título literal do backlog ("criar, cancelar, reagendar").
- Reagendar sempre passa por "o original vira `CANCELLED`, um novo
  nasce `SCHEDULED`" — a spec técnica decide a implementação exata
  (duas gravações vs. uma operação), mas o requisito de produto (BR-3)
  é esse.
- Sem tela de edição de cadastro do cliente (nome/telefone) a partir da
  agenda — fica para outra feature, se necessário.
