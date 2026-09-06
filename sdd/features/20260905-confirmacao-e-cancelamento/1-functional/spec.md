# confirmacao-e-cancelamento - Functional Spec

**Feature**: confirmacao-e-cancelamento
**Backlog**: TODO-007
**Status**: approved
**Data**: 2026-09-05
**Aprovado por**: Elton Marques em 2026-09-06T13:41:18Z

---

## Problem Statement

A TODO-006 deixou o cliente agendar sozinho, mas o ciclo fica pela metade:
a única prova do agendamento é a tela de sucesso, que usa flash attribute
e some ao recarregar a página ou fechar a aba. O cliente não tem como
conferir depois o que marcou, não tem como confirmar presença, não tem
como cancelar se mudar de ideia, e não recebe nada para adicionar ao
próprio calendário. Sem isso, ele volta a ligar ou mandar mensagem para o
estabelecimento — exatamente o problema que o AgendaIA existe para
resolver.

Esta feature entrega um link persistente e seguro, sem login, onde o
cliente pode: ver os detalhes do agendamento a qualquer momento,
confirmar presença, cancelar, baixar um arquivo `.ics` para o calendário,
e falar com o estabelecimento pelo WhatsApp quando o número estiver
cadastrado.

---

## Objectives

1. Entregar, na tela de sucesso da TODO-006, um link persistente (não
   dependente de flash attribute) para ver o agendamento depois.
2. Permitir que o cliente confirme presença (`SCHEDULED` → `CONFIRMED`)
   pelo link, sem login — sinal de que ele viu e pretende comparecer.
3. Permitir que o cliente cancele (`SCHEDULED`/`CONFIRMED` → `CANCELLED`)
   a qualquer momento até o horário chegar, liberando o intervalo para
   outro cliente reservar.
4. Gerar um arquivo `.ics` com os dados do agendamento, para o cliente
   adicionar ao próprio aplicativo de calendário.
5. Oferecer um link `wa.me` pré-preenchido para o cliente falar
   diretamente com o estabelecimento, quando este tiver WhatsApp
   cadastrado.
6. Estender o cadastro (TODO-001) com um campo opcional de WhatsApp do
   estabelecimento — sem essa informação, não há como montar o link
   `wa.me` do objetivo anterior.

---

## Scope

### In Scope

- Link persistente e seguro (revalidado contra o tenant do
  estabelecimento, nunca confiado às cegas) entregue na tela de sucesso
  da TODO-006, reutilizável a qualquer momento até o agendamento
  acontecer.
- Tela "meu agendamento" (`GET` pelo token): mostra estabelecimento,
  serviço, profissional, data, horário e status atual.
- Ação "Confirmar presença" (`POST`): `SCHEDULED` → `CONFIRMED`.
- Ação "Cancelar" (`POST`): `SCHEDULED` ou `CONFIRMED` → `CANCELLED`.
- Geração de arquivo `.ics` para download, com os dados do compromisso.
- Link `wa.me` pré-preenchido com mensagem padrão, apontando para o
  WhatsApp do estabelecimento — só aparece quando esse campo existir.
- Campo opcional de WhatsApp no formulário de cadastro do estabelecimento
  (TODO-001), sem tela de edição posterior.
- Atualização do teto de agendamentos futuros por telefone (BR-9 da
  TODO-006): passa a contar `SCHEDULED` **e** `CONFIRMED`.
- Cenários E2E cobrindo caminho feliz, isolamento entre tenants,
  idempotência das ações e agendamento no passado.

### Out of Scope

- **Reagendar.** É a TODO-008 (agenda do profissional).
- **Cancelamento pelo profissional/dono.** Também TODO-008.
- **Marcar `NO_SHOW`.** Decisão do dono na própria agenda, TODO-008.
- **Envio automático do link por e-mail, SMS ou WhatsApp.** Não existe
  canal transacional no projeto (TODO-108/TODO-109); o cliente só recebe
  o link na própria tela de sucesso, no momento em que agenda.
- **Tela de edição/configurações do estabelecimento.** O WhatsApp do
  negócio só é coletado no cadastro (TODO-001); quem já se cadastrou
  antes desta feature fica sem, até outra feature trazer edição.
- **Reenvio do link caso o cliente perca.** Sem canal de reenvio (mesma
  razão do item anterior) — se perder, precisa contatar o estabelecimento
  por fora.
- **Prazo mínimo de antecedência para cancelar.** Cancelamento é permitido
  a qualquer momento até o horário do agendamento chegar — sem regra de
  "só até X horas antes" nesta feature.
- **Autenticação/CAPTCHA na tela do link.** A revalidação contra o tenant
  já é a defesa — mesma filosofia de `/b/{slug}` na TODO-006.

---

## User Stories

### US-1: Encontrar o agendamento depois

**Como** cliente que acabou de agendar,
**Eu quero** um link que funcione mesmo depois de fechar a aba ou
recarregar a página,
**Para que** eu não dependa de lembrar os detalhes de cabeça ou perguntar
de novo ao estabelecimento.

**Acceptance Criteria**:
- A tela de sucesso da TODO-006 passa a mostrar esse link, além do
  resumo que já existia.
- O link funciona a qualquer momento depois, não só na primeira
  visita — não depende de sessão nem de flash attribute.
- Abrir o link mostra estabelecimento, serviço, profissional, data,
  horário e status atual do agendamento.

### US-2: Confirmar presença

**Como** cliente que já agendou,
**Eu quero** confirmar pelo link que vou comparecer,
**Para que** o estabelecimento saiba que não fui só eu clicando em
"agendar" sem intenção real de aparecer.

**Acceptance Criteria**:
- Botão "Confirmar presença" visível quando o status é `SCHEDULED`.
- Ao confirmar, o status muda para `CONFIRMED` e a tela reflete a
  mudança.
- Confirmar um agendamento que já está `CONFIRMED` não é erro — a tela
  simplesmente mostra o status atual, sem gravar nada de novo.
- Um agendamento `CANCELLED` não pode ser confirmado.

### US-3: Cancelar o agendamento

**Como** cliente que não vai mais poder comparecer,
**Eu quero** cancelar meu agendamento pelo mesmo link,
**Para que** eu não precise ligar para avisar, e o horário fique livre
para outra pessoa.

**Acceptance Criteria**:
- Botão "Cancelar" visível quando o status é `SCHEDULED` ou `CONFIRMED`.
- Ao cancelar, o status muda para `CANCELLED` e o horário some da
  disponibilidade calculada (TODO-005) e da exclusion constraint (ADR
  0005) para aquele profissional.
- Cancelar é permitido a qualquer momento até o horário do agendamento
  chegar.
- Cancelar um agendamento que já está `CANCELLED` não é erro — mostra o
  status atual, sem gravar nada de novo.

### US-4: Baixar o compromisso para o calendário

**Como** cliente que agendou,
**Eu quero** baixar um arquivo que meu aplicativo de calendário entenda,
**Para que** eu não esqueça o horário.

**Acceptance Criteria**:
- Um link/botão na tela do agendamento gera um arquivo `.ics` com nome
  do serviço, data/hora de início e fim, e nome do estabelecimento.
- O arquivo não contém nome nem telefone do cliente (LGPD).
- Disponível enquanto o agendamento não estiver `CANCELLED`.

### US-5: Falar com o estabelecimento pelo WhatsApp

**Como** cliente com alguma dúvida sobre o agendamento,
**Eu quero** um link que já abra uma conversa de WhatsApp com o
estabelecimento, com uma mensagem inicial pronta,
**Para que** eu não precise digitar o número nem explicar do zero quem
sou.

**Acceptance Criteria**:
- Quando o estabelecimento tem WhatsApp cadastrado, a tela mostra um
  link `wa.me` para esse número, com mensagem pré-preenchida citando
  nome do cliente e data/horário do agendamento.
- Quando o estabelecimento não tem WhatsApp cadastrado, o link
  simplesmente não aparece — a tela não quebra nem mostra erro.

### US-6: Horário cancelado libera vaga para outro cliente

**Como** dono do estabelecimento,
**Eu quero** que um agendamento cancelado libere o horário imediatamente,
**Para que** eu não perca uma venda por um horário "ocupado" que na
verdade está livre.

**Acceptance Criteria**:
- Depois de um cancelamento, o mesmo intervalo aparece como disponível
  em `GetAvailableSlotsHandler` (TODO-005) para aquele profissional.
- Uma nova reserva para o mesmo intervalo é aceita normalmente depois do
  cancelamento — sem exigir nenhuma ação manual do dono.

### US-7: Cadastro aceita o WhatsApp do estabelecimento

**Como** dono se cadastrando no AgendaIA,
**Eu quero** informar meu WhatsApp já no cadastro,
**Para que** meus clientes tenham como me chamar pelo link da tela de
agendamento.

**Acceptance Criteria**:
- Campo de WhatsApp no formulário de cadastro (TODO-001), opcional.
- Cadastro sem esse campo continua funcionando normalmente — nada quebra
  para quem não preenche.
- Formato validado (mesmo padrão de telefone já usado por `Customer`),
  mas o campo pode ficar vazio.

---

## Business Rules

### Core Rules

- **BR-1**: O link de acesso ao agendamento não pode permitir que um
  cliente acesse ou altere agendamento de outro tenant, mesmo tentando
  outro id à mão. Mecanismo exato (id opaco revalidado contra o tenant,
  ou token derivado) é decisão da spec técnica — o requisito de produto é
  o isolamento entre tenants, não uma técnica específica.
- **BR-2**: Transições de status permitidas nesta feature: `SCHEDULED` →
  `CONFIRMED` (confirmar), `SCHEDULED` → `CANCELLED` (cancelar),
  `CONFIRMED` → `CANCELLED` (cancelar depois de confirmado). `CANCELLED`
  é estado final — nenhuma transição sai dele.
- **BR-3 (idempotência)**: Repetir uma ação que não muda o estado atual
  (confirmar um já `CONFIRMED`, cancelar um já `CANCELLED`) não é erro —
  a tela reflete o estado existente, sem gravação nova.
- **BR-4 (sem prazo mínimo)**: Cancelar é permitido a qualquer momento
  até `startsAt` chegar. Depois que o horário passa, a tela vira
  somente leitura — nem confirmar nem cancelar ficam disponíveis.
- **BR-5**: Cancelamento libera o intervalo automaticamente: a exclusion
  constraint (ADR 0005, `WHERE status IN ('SCHEDULED','CONFIRMED')`) e a
  disponibilidade calculada (TODO-005) já ignoram `CANCELLED` sem
  precisar de nenhuma lógica nova além da própria mudança de status.
- **BR-6 (teto por telefone atualizado)**: O teto de 3 agendamentos
  futuros ativos por telefone (BR-9 da TODO-006) passa a contar
  `SCHEDULED` **e** `CONFIRMED` — confirmar presença não abre vaga nova
  no teto, continua ocupando uma das três.
- **BR-7**: `tenantId` nunca vem de nenhum outro dado da requisição —
  vem exclusivamente do que o token permite resolver.
- **BR-8**: WhatsApp do estabelecimento é opcional. Ausente, o link
  `wa.me` não aparece — não é um erro, é a ausência normal do dado.
- **BR-9 (LGPD)**: O arquivo `.ics` contém só dados do compromisso
  (serviço, horário, nome do estabelecimento) — nenhum dado pessoal do
  cliente.

### Validation Invariants

- Token inválido, expirado (se a spec técnica adotar expiração) ou de
  formato incorreto → mesma resposta de "não encontrado", sem distinguir
  o motivo.
- Token válido de um agendamento, usado para tentar agir sobre outro
  (adulteração) → recusado, nenhuma gravação.
- Ação (confirmar/cancelar) enviada para um agendamento cujo `startsAt`
  já passou → recusada, tela permanece somente leitura.
- Ação enviada para um agendamento `CANCELLED` → sem efeito (BR-3),
  nunca reabre o agendamento.

---

## Data Model (conceitual, para a spec técnica detalhar)

**`Appointment`** (já existe, TODO-006) — ganha as transições de status
`CONFIRMED` e `CANCELLED`, ambas já previstas no enum
`AppointmentStatus` mas nunca alcançadas até esta feature.

**`Business`** (já existe, TODO-001) — ganha um campo conceitual de
WhatsApp, opcional.

Nenhuma entidade nova. Mecanismo exato do link (id opaco revalidado por
tenant vs. token derivado) é decisão da spec técnica.

---

## User Experience

### Fluxo principal — confirmar

1. Cliente agenda (TODO-006) e vê, na tela de sucesso, o link
   persistente para "ver meu agendamento".
2. A qualquer momento, abre o link. Vê os detalhes e o status atual
   (`SCHEDULED`).
3. Clica em "Confirmar presença". Tela atualiza mostrando `CONFIRMED`.

### Fluxo principal — cancelar

1. Cliente abre o link (a partir da tela de sucesso ou de uma visita
   anterior).
2. Vê os detalhes e clica em "Cancelar".
3. Tela atualiza mostrando `CANCELLED`; nem "Confirmar presença" nem
   "Cancelar" continuam disponíveis.

### Fluxo secundário — .ics e WhatsApp

1. Na mesma tela do agendamento (qualquer status exceto `CANCELLED`),
   cliente clica em "Adicionar ao calendário" → baixa o `.ics`.
2. Se o estabelecimento tiver WhatsApp cadastrado, cliente vê e pode
   clicar no link "Falar no WhatsApp", que abre a conversa com mensagem
   pré-preenchida.

### Edge Cases

- **Token adulterado ou de outro agendamento** — mesma resposta de "não
  encontrado" dada a um token com formato errado; nunca revela que o
  token "quase" funcionou.
- **Cancelar duas vezes (duplo clique, duas abas)** — idempotente (BR-3):
  a segunda tentativa só confirma o estado já `CANCELLED`.
- **Confirmar depois de cancelado** — recusado; `CANCELLED` é estado
  final (BR-2).
- **Agendamento cujo horário já passou** — tela somente leitura, sem
  botões de ação; cliente ainda consegue ver os detalhes e baixar o
  `.ics`.
- **Estabelecimento sem WhatsApp cadastrado** — tela funciona
  normalmente, só sem o link `wa.me`.
- **Cancelamento libera horário que outro cliente reserva em seguida** —
  comportamento esperado (US-6), não é um conflito.

---

## Critical E2E Test Scenarios

> Sem LTP nesta instalação. Cenários viram testes de integração e web
> contra Postgres real, como nas features anteriores.

### E2E-1: Caminho feliz — confirmar presença

**Criticidade**: 🔴 Critical — sem isto a feature não existe.

1. Cliente agenda (fluxo da TODO-006) e recebe o link.
2. Abre o link e confirma presença.

**Resultado esperado**: status muda de `SCHEDULED` para `CONFIRMED`; a
tela reflete a mudança ao recarregar.

### E2E-2: Cancelamento libera o horário

**Criticidade**: 🔴 Critical — protege BR-5/US-3/US-6.

1. Cliente agenda e depois cancela pelo link.
2. Consulta `GetAvailableSlotsHandler` para o mesmo profissional/data.

**Resultado esperado**: status `CANCELLED`; o intervalo cancelado
aparece de novo como disponível; uma nova reserva para o mesmo horário é
aceita.

### E2E-3: Token de um tenant não abre agendamento de outro

**Criticidade**: 🔴 Critical — mesma garantia de isolamento de todas as
features anteriores, agora num link individual por agendamento.

1. Dois agendamentos em dois estabelecimentos (tenants) diferentes.
2. Tenta usar o token de um para acessar/alterar o agendamento do outro
   (id trocado, token adulterado).

**Resultado esperado**: recusado como "não encontrado"; nenhuma leitura
nem gravação cruza o tenant.

### E2E-4: Ações são idempotentes

**Criticidade**: 🟡 Important — protege BR-3.

1. Cliente cancela um agendamento.
2. Repete a mesma ação de cancelar (ou tenta confirmar) sobre o mesmo
   agendamento já `CANCELLED`.

**Resultado esperado**: nenhum erro cru; o status continua `CANCELLED`;
nenhuma gravação nova acontece na segunda tentativa.

### E2E-5: Agendamento no passado é somente leitura

**Criticidade**: 🟡 Important — protege BR-4.

1. Um agendamento cujo `startsAt` já passou (`SCHEDULED` ou
   `CONFIRMED`).
2. Cliente abre o link.

**Resultado esperado**: detalhes visíveis; nenhum botão de "Confirmar
presença" ou "Cancelar" disponível; tentativa direta de `POST` (fora da
UI) é recusada.

### E2E-6: Teto por telefone conta CONFIRMED também

**Criticidade**: 🟡 Important — protege BR-6 (atualização da BR-9 da
TODO-006).

1. Um telefone tem 3 agendamentos futuros: alguns `SCHEDULED`, outros já
   `CONFIRMED`, somando 3.
2. Tenta criar um 4º agendamento (fluxo da TODO-006).

**Resultado esperado**: recusado pelo teto — confirmar presença não abre
vaga nova.

### E2E-7: Link do WhatsApp aparece só quando cadastrado

**Criticidade**: 🟢 Should — protege BR-8/US-5.

1. Um estabelecimento com WhatsApp cadastrado e outro sem.
2. Cliente abre o link do agendamento em cada um.

**Resultado esperado**: o primeiro mostra o link `wa.me`; o segundo não
mostra nada no lugar dele — sem erro, sem espaço em branco quebrado.

### E2E Test Summary

| ID | Cenário | Tipo | Cobre |
|---|---|---|---|
| E2E-1 | Confirmar presença | Caminho feliz | US-1, US-2, BR-2 |
| E2E-2 | Cancelamento libera horário | Caminho feliz | US-3, US-6, BR-5 |
| E2E-3 | Token de outro tenant recusado | Isolamento | BR-1, BR-7 |
| E2E-4 | Ações idempotentes | Erro/exclusão | BR-3 |
| E2E-5 | Agendamento no passado, somente leitura | Erro/exclusão | BR-4 |
| E2E-6 | Teto por telefone conta CONFIRMED | Erro/exclusão | BR-6 |
| E2E-7 | wa.me condicional ao cadastro | Caminho feliz | BR-8, US-5 |

---

## Success Metrics

### Business Metrics

- Primeiro cliente do piloto confirma presença ou cancela pelo link, sem
  ligar para o estabelecimento — marco qualitativo, sem meta numérica
  ainda.

### User Metrics

- Cliente encontra e usa o link em uma visita posterior (não só na hora
  de agendar) — sem meta numérica formal nesta fase.

### Technical Metrics

- Zero vazamento entre tenants pelo token — **target: 0**, verificado
  por E2E-3.
- Zero ação bem-sucedida sobre agendamento `CANCELLED` ou no passado —
  **target: 0**, verificado por E2E-4 e E2E-5.

---

## Non-Functional Requirements

### Performance

Sem exigência numérica formal — volume do piloto. Cada ação (confirmar,
cancelar) é uma única transação de atualização de status, sem consulta
adicional além da resolução do token.

### Security

Segunda rota pública sem autenticação do projeto (depois de `/b/{slug}`
na TODO-006) — mesma filosofia de defesa:
- `tenantId` nunca vem de nenhum dado da requisição além do que o token
  permite resolver (BR-1/BR-7).
- Nenhum agendamento de outro tenant deve ficar acessível ou alterável a
  partir de um id diferente digitado à mão — decisão de mecanismo na spec
  técnica, mas o requisito de produto é o isolamento entre tenants.
- XSS: qualquer dado exibido (nome do estabelecimento, nome do serviço)
  via `th:text`, nunca `th:utext`, mesma convenção do projeto.
- `.ics` não carrega dado pessoal do cliente (BR-9, LGPD).
- Sem CAPTCHA nem rate limit dedicado nesta feature — a revalidação
  contra o tenant já restringe quem consegue agir sobre qual agendamento;
  se abuso real aparecer (ex.: tentativa de enumeração de ids), fica para
  revisão futura.

---

## Assumptions

- O link entregue na tela de sucesso da TODO-006 é a única forma de
  acesso — não existe busca nem listagem pública de agendamentos por
  telefone ou nome.
- Sem canal de reenvio do link nesta fase (Out of Scope) — mesma limitação
  já registrada para e-mail/SMS (TODO-108/TODO-109).
- O WhatsApp do estabelecimento, quando ausente, não bloqueia nenhuma
  outra funcionalidade desta feature — é estritamente opcional.
- Mecanismo exato do link (id opaco revalidado por tenant, ou token
  derivado) fica para a spec técnica decidir — o requisito de produto é
  só isolamento entre tenants, sem prazo de expiração exigido.
