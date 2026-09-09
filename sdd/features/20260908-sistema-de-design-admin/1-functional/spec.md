# sistema-de-design-admin - Functional Spec

**Feature**: sistema-de-design-admin
**Backlog**: TODO-110
**Status**: approved
**Data**: 2026-09-08
**Aprovado por**: Elton Marques em 2026-09-08T22:43:05Z

---

## Problem Statement

As telas de `/admin/**` usam uma navbar horizontal simples desde a
TODO-001. Ela já comportou oito rotas sem apertar, mas o layout não seguia
nenhum sistema de design consciente — cor, raio e sombra eram só o padrão
do Bootstrap. Numa sessão anterior a esta feature, o dono trouxe
protótipos visuais (Gemini/Tailwind) que foram traduzidos para um sistema
de design em Bootstrap 5, já documentado em `sdd/PATTERNS.md` (paleta,
badge soft, sidebar, card de KPI) e já parcialmente aplicado — a paleta de
cores e o botão de alternar tema já valem para toda página, e
`operador/painel.html` já tem sidebar. Falta estender a mesma estrutura
para as telas de estabelecimento, e corrigir o que ficou no padrão antigo.

Ao mesmo tempo, o painel do dono (`admin/dashboard.html`) é hoje só um
link e uma lista do que falta cadastrar — mesmo com `scheduling` e
`Appointment` existindo desde a TODO-008, nada do dia a real aparece ali.
E não existe, em lugar nenhum do sistema, uma forma de registrar que um
atendimento realmente aconteceu: um agendamento fica `CONFIRMED` para
sempre, mesmo depois do cliente ser atendido — não há como saber, pelos
dados, quantos atendimentos de um dia foram de fato realizados.

---

## Objectives

1. Toda tela de `/admin/**` navega por uma sidebar, no mesmo padrão já
   construído em `operador/painel.html`, em vez da navbar horizontal.
2. Todo badge de status (billing no painel do operador, agendamento na
   agenda do dono) usa o padrão soft/subtle já documentado em
   `sdd/PATTERNS.md`, nunca mais o `text-bg-*` sólido.
3. O painel do dono mostra o dia com números reais: quantos agendamentos
   hoje, quem é o próximo cliente, quantos atendimentos já foram
   concluídos e o faturamento estimado do dia.
4. O dono (ou o profissional) consegue marcar um agendamento como
   concluído com um clique, a partir da agenda — fechando um ciclo que o
   domínio não tinha: hoje só existe `SCHEDULED` → `CONFIRMED` →
   `CANCELLED`/`NO_SHOW`, nunca "aconteceu de verdade".

---

## Scope

### In Scope

- Sidebar de navegação (lista simples, um item por rota) substituindo a
  navbar em: `admin/dashboard`, `admin/agenda` (+ `agenda-novo`,
  `agenda-reagendar`, que herdam o mesmo esqueleto), `admin/bloqueios`,
  `admin/horario-funcionamento`, `admin/jornadas`, `admin/ofertas`,
  `admin/profissionais`, `admin/servicos`.
- Migração de badge sólido (`text-bg-*`) para o padrão soft/subtle
  (`bg-*-subtle` + `text-*-emphasis`) em `operador/painel.html` (billing) e
  `admin/agenda.html` (status do agendamento).
- Quatro cards de KPI no `admin/dashboard.html`, com dado real: **agendamentos
  de hoje**, **próximo cliente**, **atendidos hoje**, **receita estimada do
  dia** — nenhum card com número fixo/decorativo.
- Novo status `AppointmentStatus.COMPLETED` no domínio `scheduling`, e uma
  nova ação "concluir" na agenda do dono (`admin/agenda.html`), ao lado das
  ações já existentes de confirmar/cancelar/reagendar (TODO-007/008).

### Out of Scope

- `admin/conta-suspensa.html` **não** ganha sidebar — é uma tela de beco
  sem saída de propósito (conta bloqueada por falta de pagamento); sidebar
  com itens que não funcionam confundiria mais do que ajudaria.
- Sidebar agrupada em seções expansíveis (Agenda / Catálogo / Equipe /
  Configurações, como no protótipo Gemini) — fica para quando o número de
  itens justificar (mesma nota de gatilho da IDEA-015). Esta feature entrega
  a lista simples, um item por rota.
- Ação de marcar `NO_SHOW` pela tela — o status já existe no domínio
  (TODO-006), mas nenhuma tela hoje o produz, e esta feature não adiciona
  essa ação; só adiciona a de concluir.
- Detalhamento de receita por profissional/serviço — só o total do dia.
- Qualquer mudança na página pública de agendamento (`/b/{slug}/**}`).

---

## User Stories

### US-1: Navegação por sidebar em `/admin/**`

**Como** dono de um estabelecimento usando o painel administrativo,
**quero** navegar pelas telas através de uma sidebar fixa,
**para** ter a mesma experiência de navegação em todas as telas, com
espaço para crescer sem apertar (hoje já são 8 rotas).

**Acceptance Criteria**:
- AC-1: Toda tela de `/admin/**` (exceto `conta-suspensa`) mostra a
  sidebar com os 8 itens de menu, cada um levando à sua rota.
- AC-2: O item da tela atual aparece destacado (mesmo padrão visual de
  `operador/painel.html`: fundo e texto na cor de marca).
- AC-3: Em tela pequena, a sidebar colapsa e um topo enxuto (nome do
  estabelecimento + botão de sair) assume o lugar — mesmo comportamento
  responsivo já existente em `operador/painel.html`.
- AC-4: O botão de alternar tema e o botão de sair continuam acessíveis em
  toda tela, sem duplicar (hoje vêm da navbar; passam a vir da sidebar).

### US-2: Badge de status no padrão soft

**Como** qualquer pessoa olhando uma lista de status (billing ou
agendamento),
**quero** ver o badge no mesmo estilo visual suave em toda tela,
**para** ter uma leitura visual consistente com o resto do sistema.

**Acceptance Criteria**:
- AC-1: Todo badge de status de billing em `operador/painel.html` usa
  `bg-{cor}-subtle` + `text-{cor}-emphasis`, nunca `text-bg-{cor}`.
- AC-2: Todo badge de status de agendamento em `admin/agenda.html` segue o
  mesmo padrão, incluindo o novo status `COMPLETED` (US-4).
- AC-3: Nenhuma tela usa cor hexadecimal direta para badge — só as classes
  do sistema de design (`sdd/PATTERNS.md`).

### US-3: Painel com KPIs reais do dia

**Como** dono de um estabelecimento,
**quero** ver, ao entrar no painel, um resumo numérico do meu dia,
**para** saber rapidamente como o dia está indo sem abrir a agenda
inteira.

**Acceptance Criteria**:
- AC-1: O card "Agendamentos de hoje" mostra a contagem de agendamentos do
  estabelecimento com `startsAt` no dia de hoje (fuso do estabelecimento),
  em qualquer status exceto `CANCELLED`/`NO_SHOW`.
- AC-2: O card "Próximo cliente" mostra nome do cliente e horário do
  próximo agendamento (`SCHEDULED`/`CONFIRMED`) com `startsAt` ainda não
  passado hoje; mostra estado vazio quando não há mais nenhum.
- AC-3: O card "Atendidos hoje" mostra a contagem de agendamentos de hoje
  com status `COMPLETED`.
- AC-4: O card "Receita estimada" soma o preço (`price`) dos agendamentos
  de hoje em `SCHEDULED`, `CONFIRMED` ou `COMPLETED` (exclui cancelado e
  falta).
- AC-5: Nenhum dos quatro cards aparece com número fixo no HTML — todos
  vêm de uma consulta real, resolvida no controller/handler.

### US-4: Marcar agendamento como concluído

**Como** dono (ou profissional) atendendo pelo painel,
**quero** marcar um agendamento como concluído depois de atender o
cliente,
**para** que o sistema saiba a diferença entre "confirmado" e "realmente
aconteceu", e o KPI de atendidos reflita a realidade.

**Acceptance Criteria**:
- AC-1: Na lista da agenda (`admin/agenda.html`), todo agendamento
  `SCHEDULED` ou `CONFIRMED` cujo horário de início já chegou mostra um
  botão "Concluir", ao lado dos já existentes de confirmar/cancelar.
- AC-2: Clicar em "Concluir" muda o status para `COMPLETED` e a lista
  reflete o novo badge (US-2) imediatamente (PRG, mesmo padrão de
  confirmar/cancelar).
- AC-3: Um agendamento `CANCELLED`, `NO_SHOW` ou já `COMPLETED` não mostra
  o botão "Concluir" — a transição só é permitida a partir de `SCHEDULED`
  ou `CONFIRMED`.
- AC-4: Tentar concluir um agendamento cujo horário de início ainda não
  chegou não é oferecido na tela (mesmo raciocínio do AC-1) — não dá para
  concluir algo que ainda não começou.
- AC-5: Depois de concluído, não existe ação para reverter (mesmo espírito
  de `CANCELLED`: transição terminal, sem "desfazer").
- AC-6: A conta de origem (tenant/profissional) do agendamento é sempre a
  já resolvida pela sessão — mesma revalidação de tenant que já existe em
  `confirmar`/`cancelar`.

---

## Business Rules

### Core Rules

- BR-1: `COMPLETED` só é alcançável a partir de `SCHEDULED` ou `CONFIRMED`.
  Nunca a partir de `CANCELLED` ou `NO_SHOW`.
- BR-2: `COMPLETED` é terminal — sem transição de volta para nenhum outro
  status.
- BR-3: Não é possível marcar `COMPLETED` antes de `startsAt` (o
  atendimento precisa ter começado).
- BR-4: "Agendamentos de hoje" e "Receita estimada" tratam `COMPLETED`
  como um agendamento que aconteceu — entram na contagem/soma do dia, do
  mesmo jeito que `SCHEDULED`/`CONFIRMED`.
- BR-5: `tenantId` do agendamento a concluir segue a mesma regra de todo o
  sistema: nunca vem do cliente, é revalidado contra a sessão (mesma regra
  de `confirm`/`cancel` em `ManageAppointmentHandler`).

### Validation Invariants

- `AppointmentStatus` passa a ter 5 valores: `SCHEDULED`, `CONFIRMED`,
  `CANCELLED`, `NO_SHOW`, `COMPLETED`. Nenhum dos quatro já existentes
  muda de significado.
- A coluna `appointment.status` é `VARCHAR(20)` — `COMPLETED` cabe sem
  precisar de migration de schema (só o enum Java muda). A spec técnica
  decide se `COMPLETED` entra ou não na cláusula `WHERE` da exclusion
  constraint (`appointment_no_overlap`, ADR 0005) que hoje só protege
  `SCHEDULED`/`CONFIRMED`.

---

## Data Model (conceitual, para a spec técnica detalhar)

- `AppointmentStatus` (enum, `scheduling.domain`): adiciona `COMPLETED`.
- `Appointment` (agregado): novo método de negócio `complete()`, mesmo
  padrão de `cancelByOwner()` já existente — sem setter público.
- `AgendaEntry` (view record, `scheduling.application.port.in`): novo
  campo `boolean canComplete`, calculado pelo handler (nunca pelo
  template), mesmo padrão de `canConfirm`/`canCancel`/`canReschedule` já
  existentes.
- Nenhuma tabela nova, nenhuma coluna nova.

---

## User Experience

- **Sidebar** (desktop): largura fixa (~16rem), logo/nome do
  estabelecimento no topo, 8 itens de menu abaixo, botão de alternar tema
  e "Sair" no rodapé — mesmo esqueleto de `operador/painel.html`.
- **Sidebar** (mobile): colapsa; topo enxuto com nome do estabelecimento,
  alternar tema e "Sair".
- **Dashboard**: linha de 4 cards de KPI no topo (mesmo componente visual
  documentado em `sdd/PATTERNS.md`), seguida do que já existe hoje (link
  de agendamento, próximo passo).
- **Agenda**: cada linha ganha um terceiro botão, "Concluir" (verde),
  visível só quando `canComplete` é verdadeiro — ao lado dos botões já
  existentes de confirmar (quando aplicável) e cancelar.

---

## Critical E2E Test Scenarios

### E2E-1: Sidebar substitui a navbar em toda tela de `/admin/**`

Dono autenticado visita cada uma das 8 rotas e vê a sidebar com o item da
tela atual destacado; `conta-suspensa` continua sem sidebar.

### E2E-2: Badge de billing no padrão soft

Operador vê o painel de estabelecimentos; o badge de status usa classe
`bg-*-subtle`/`text-*-emphasis`, não `text-bg-*`.

### E2E-3: Dashboard mostra KPIs reais

Com agendamentos de hoje semeados no banco (alguns `SCHEDULED`, um
`COMPLETED`, um `CANCELLED`), o painel mostra a contagem, o próximo
cliente e o total corretos — o cancelado não entra na contagem nem na
receita.

### E2E-4: Concluir um agendamento

Dono confirma um agendamento, marca como concluído; o status vira
`COMPLETED`, o badge muda, e o card "Atendidos hoje" do painel reflete o
novo total.

### E2E-5: Não é possível concluir fora da janela permitida

Um agendamento `CANCELLED` não mostra o botão "Concluir". Um agendamento
`SCHEDULED` cujo horário ainda não chegou também não mostra o botão.

---

## Success Metrics

- O dono consegue responder "como está meu dia?" (quantos, quem é o
  próximo, quantos já atendi, quanto faturei) olhando só o painel, sem
  abrir a agenda inteira nem o banco.
- Nenhuma tela de `/admin/**` usa mais a navbar antiga nem badge sólido.

---

## Non-Functional Requirements

- Os 4 KPIs do painel são resolvidos com consultas agregadas — nenhuma em
  laço, nenhuma carregando o agregado inteiro só para contar/somar (ver
  `sdd/PATTERNS.md`, "Consulta de leitura não carrega agregado").
- Nenhuma dependência nova: sidebar, badge e KPI usam Bootstrap 5 já
  presente (ADR 0012) e o sistema de tokens já em `fragments/layout.html`.

---

## Assumptions

- "Hoje" é calculado no fuso do estabelecimento (`Business.timezone`, já
  existente), não UTC nem fuso do servidor.
- Nesta fase do MVP, dono e profissional são frequentemente a mesma
  pessoa — a ação de concluir fica disponível para quem já tem acesso à
  agenda (`/admin/agenda`), sem papel novo de permissão.
- O protótipo de referência (Gemini) trazia os quatro KPIs e o padrão de
  botões ✓/✗ por linha da agenda; o ✗ já existe (cancelar) — esta feature
  entrega o ✓ (concluir) que faltava.
