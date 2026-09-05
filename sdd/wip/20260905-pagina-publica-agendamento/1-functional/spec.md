# pagina-publica-agendamento - Functional Spec

**Feature**: pagina-publica-agendamento
**Backlog**: TODO-006
**Status**: approved
**Data**: 2026-09-05
**Aprovado por**: Elton Marques em 2026-09-05T14:05:23Z

---

## Problem Statement

Depois da TODO-005, `scheduling` sabe **calcular** quais horários estão
realmente livres para um profissional, uma oferta e uma data — mas nada no
projeto ainda deixa um cliente **reservar** um desses horários. O
estabelecimento continua dependendo de WhatsApp/telefone para marcar um
atendimento, que é exatamente o problema que o AgendaIA existe para
resolver.

Esta feature entrega a primeira escrita real de `scheduling`: a página
pública `/b/{slug}` onde o cliente escolhe serviço, profissional, data e
horário, informa nome e telefone, e confirma — sem login, sem conta. O
resultado é um `Appointment` gravado com garantia física contra
overbooking (ADR 0005), mesmo sob duas requisições concorrentes disputando
o mesmo horário.

---

## Objectives

1. Permitir que um visitante sem conta agende um atendimento em
   `/b/{slug}`, escolhendo serviço → profissional → data → horário, a
   partir da disponibilidade já calculada pela TODO-005.
2. Criar (ou reaproveitar) um `Customer` pelo telefone informado, dentro do
   tenant do slug — telefone é a chave natural (glossário).
3. Gravar o `Appointment` com um **retrato** de duração e preço da
   `ServiceOffering` no momento da reserva — mudanças futuras no catálogo
   não afetam agendamentos já feitos.
4. Garantir, com a exclusion constraint do ADR 0005, que duas reservas
   simultâneas para o mesmo profissional e o mesmo intervalo nunca
   coexistam — uma vence, a outra recebe erro tratado, nunca um 500 cru.
5. Conter abuso automatizado do formulário público: honeypot, limite de
   tentativas por IP e teto de agendamentos futuros ativos por telefone.

---

## Scope

### In Scope

- Página pública `/b/{slug}` (GET) mostrando os serviços do
  estabelecimento.
- Fluxo de escolha: serviço → profissional que oferece aquele serviço →
  data (dentro do horizonte de 30 dias já usado pela TODO-005) → horário
  livre (reaproveitando `GetAvailableSlotsHandler`).
- Formulário final: nome e telefone do cliente, com honeypot.
- `POST` que grava o `Appointment`, criando ou reaproveitando o `Customer`
  pelo telefone dentro do tenant.
- Exclusion constraint no banco (ADR 0005) como garantia real contra
  overbooking; validação em memória só como feedback rápido antes de
  tentar gravar.
- Revalidação de todo id recebido do formulário (`serviceOfferingId`,
  `professionalId`) contra o tenant resolvido pelo slug — nenhum vem
  "confiável" do cliente.
- Tela de sucesso com o resumo do agendamento.
- Defesas contra abuso: honeypot, rate limit por IP, teto de agendamentos
  futuros ativos por telefone.
- Teste de concorrência: duas requisições disputando o mesmo horário do
  mesmo profissional.

### Out of Scope

- **Cancelamento e reagendamento pelo cliente.** O link de cancelamento é
  a TODO-007 — esta feature só cria.
- **Cancelamento e reagendamento pelo profissional/dono.** É a TODO-008
  (agenda do profissional).
- **Confirmação por e-mail, WhatsApp ou `.ics`.** TODO-007. A tela de
  sucesso desta feature é só a própria página — nenhum canal externo é
  acionado ainda.
- **Status `CONFIRMED` e `NO_SHOW`.** Todo `Appointment` criado aqui nasce
  `SCHEDULED`; os demais estados do glossário entram com as features que
  os usam.
- **"Qualquer profissional disponível."** O cliente sempre escolhe um
  profissional específico — não existe alocação automática nesta feature.
- **Múltiplos serviços na mesma reserva.** Um `Appointment` é sempre um
  serviço com um profissional, mesma granularidade de `ServiceOffering`.
- **Campo de observação/nota do cliente.** Só nome e telefone são
  coletados.
- **Threat model formal do fluxo público** (DEBT-005) — fica documentado
  à parte, não bloqueia esta feature.
- **Tema visual/branding customizado da página pública** (IDEA já
  registrada) — usa o Bootstrap padrão, sem CSS específico do
  estabelecimento.

---

## User Stories

### US-1: Ver os serviços do estabelecimento

**Como** visitante que recebeu o link `/b/{slug}`,
**Eu quero** ver a lista de serviços que o estabelecimento oferece,
**Para que** eu escolha o que eu quero agendar sem precisar perguntar por
WhatsApp.

**Acceptance Criteria**:
- `GET /b/{slug}` mostra nome do estabelecimento e a lista de serviços
  ativos, com preço e duração de pelo menos uma oferta.
- Slug inexistente devolve 404, não um erro genérico.
- Nenhum dado de outro tenant aparece na página.

### US-2: Escolher profissional para o serviço escolhido

**Como** visitante,
**Eu quero**, depois de escolher um serviço, ver só os profissionais que
oferecem aquele serviço,
**Para que** eu não perca tempo escolhendo alguém que não faz o que eu
quero.

**Acceptance Criteria**:
- A lista de profissionais mostrada depende do serviço escolhido —
  reflete as `ServiceOffering` ativas daquele serviço.
- Um serviço sem nenhum profissional ativo não trava a página: mostra
  mensagem clara de indisponibilidade.

### US-3: Ver horários realmente livres e escolher um

**Como** visitante,
**Eu quero** ver os horários que a TODO-005 calcula como livres para o
profissional e o serviço escolhidos, numa data que eu escolho,
**Para que** eu só tente marcar um horário que de fato existe.

**Acceptance Criteria**:
- A lista de horários vem de `GetAvailableSlotsHandler` (TODO-005), sem
  duplicar a lógica de cálculo.
- Data fora do horizonte de 30 dias não é selecionável.
- Lista vazia mostra mensagem clara ("sem horários disponíveis nesta
  data"), não uma página quebrada.

### US-4: Confirmar o agendamento com nome e telefone

**Como** visitante,
**Eu quero** informar meu nome e telefone e confirmar o horário escolhido,
**Para que** meu atendimento fique marcado sem eu precisar ligar ou
mandar mensagem.

**Acceptance Criteria**:
- Formulário exige nome e telefone (validado em formato, ex.: DDD +
  número).
- Ao confirmar, um `Appointment` com status `SCHEDULED` é gravado,
  vinculado a um `Customer` (novo ou reaproveitado pelo telefone).
- O `Customer` é resolvido pelo telefone **dentro do tenant do slug** —
  o mesmo telefone em dois estabelecimentos diferentes são dois
  `Customer` distintos.
- Duração e preço gravados no `Appointment` são o retrato da
  `ServiceOffering` no momento da confirmação.

### US-5: Ver a confirmação do agendamento

**Como** visitante que acabou de agendar,
**Eu quero** ver um resumo do que eu marquei,
**Para que** eu tenha certeza de que funcionou e saiba o que esperar.

**Acceptance Criteria**:
- Tela de sucesso mostra: nome do estabelecimento, serviço, profissional,
  data e horário.
- Não há link de cancelamento nem qualquer envio externo (e-mail,
  WhatsApp, `.ics`) nesta tela — isso é a TODO-007.

### US-6: Duas pessoas não conseguem marcar o mesmo horário

**Como** dono do estabelecimento,
**Eu quero** que o sistema nunca aceite dois agendamentos para o mesmo
profissional no mesmo intervalo de tempo, mesmo sob requisições
simultâneas,
**Para que** eu nunca precise ligar para um cliente desmarcando um
atendimento que "por engano" foi aceito duas vezes.

**Acceptance Criteria**:
- Duas requisições concorrentes para o mesmo profissional e o mesmo
  horário resultam em exatamente uma reserva bem-sucedida.
- A requisição perdedora recebe uma mensagem de erro tratada ("esse
  horário acabou de ser reservado"), nunca um erro 500 cru.
- A garantia vale mesmo se a validação em memória (contra
  `GetAvailableSlotsHandler`) disser "livre" nas duas requisições ao
  mesmo tempo — quem decide de verdade é a exclusion constraint do banco
  (ADR 0005).

### US-7: Abuso automatizado do formulário é contido

**Como** dono do estabelecimento,
**Eu quero** que um bot não consiga lotar minha agenda com agendamentos
falsos,
**Para que** clientes de verdade encontrem horário disponível.

**Acceptance Criteria**:
- Um campo honeypot invisível ao usuário humano, se preenchido, faz a
  submissão ser recusada sem revelar ao remetente que foi identificado
  como bot.
- Mais de 5 tentativas de agendamento do mesmo IP em 10 minutos são
  recusadas com mensagem genérica de "tente novamente mais tarde".
- Um telefone com 3 agendamentos futuros ativos (`SCHEDULED`) no mesmo
  estabelecimento não consegue criar um 4º até que um dos três já tenha
  passado ou seja cancelado.

---

## Business Rules

### Core Rules

- **BR-1**: `Appointment` nasce sempre com status `SCHEDULED`. Nenhum
  outro status é atingível nesta feature.
- **BR-2**: `Appointment` guarda um retrato (`serviceName`,
  `durationMinutes`, `price`) da `ServiceOffering` no momento da
  confirmação — mudanças futuras no catálogo não alteram agendamentos já
  criados.
- **BR-3**: `Customer` é resolvido por telefone normalizado (E.164)
  **dentro do tenant** — mesmo telefone em tenants diferentes nunca
  colide; mesmo telefone no mesmo tenant sempre reaproveita o `Customer`
  existente, nunca duplica.
- **BR-4 (ADR 0005)**: A garantia real contra overbooking é a exclusion
  constraint `EXCLUDE USING gist` sobre `(tenant_id, professional_id,
  tstzrange(starts_at, ends_at, '['))`, restrita a status ocupantes
  (`SCHEDULED`). A checagem em memória contra `GetAvailableSlotsHandler`
  é só feedback rápido — nunca a garantia.
- **BR-5**: `tenantId` nunca vem do formulário. `serviceOfferingId` e
  `professionalId` recebidos são revalidados contra o tenant resolvido
  pelo `slug` da URL antes de qualquer gravação — um id de outro tenant é
  recusado como se não existisse.
- **BR-6**: Data e horário do agendamento respeitam o mesmo horizonte de
  30 dias da TODO-005 — não é uma regra nova, é a mesma validação
  reaproveitada.
- **BR-7 (honeypot)**: Um campo de formulário invisível ao humano (via
  CSS, não `type="hidden"`) que, se vier preenchido no `POST`, identifica
  a submissão como automatizada. A resposta ao remetente é indistinguível
  de sucesso ou erro genérico — nunca revela que o honeypot foi
  disparado.
- **BR-8 (rate limit)**: No máximo 5 tentativas de `POST` de agendamento
  por IP a cada 10 minutos. A 6ª tentativa na janela é recusada com
  mensagem genérica, sem revelar o limite exato.
- **BR-9 (teto por telefone)**: No máximo 3 `Appointment` com status
  `SCHEDULED` e `startsAt` no futuro, por telefone, dentro do mesmo
  tenant. A 4ª tentativa é recusada com mensagem explicando o motivo (ao
  contrário do rate limit, aqui a causa pode ser explícita — não ajuda
  bot nenhum saber "você já tem 3 agendamentos").

### Validation Invariants

- `serviceOfferingId`/`professionalId` de outro tenant, ou inexistentes →
  erro, nunca gravação.
- Horário fora da disponibilidade calculada pela TODO-005 → erro de
  validação em memória (feedback rápido).
- Horário que colide com outro `Appointment` `SCHEDULED` do mesmo
  profissional, mesmo que a validação em memória tenha aprovado → erro
  vindo da exclusion constraint, traduzido para mensagem amigável.
- Honeypot preenchido → submissão recusada silenciosamente.
- Mais de 5 tentativas/IP em 10 min → 429 ou mensagem equivalente.
- Mais de 3 agendamentos futuros ativos do mesmo telefone → erro
  explícito de campo.

---

## Data Model (conceitual, para a spec técnica detalhar)

**`Appointment`** — raiz de agregado, nasce em `scheduling`. Campos
conceituais: `tenantId`, `professionalId`, `serviceOfferingId`,
`customerId`, `status` (`SCHEDULED` nesta feature), `startsAt`, `endsAt`,
retrato (`serviceName`, `durationMinutes`, `price` no momento da reserva).

**`Customer`** — raiz de agregado, nasce ou ganha o primeiro uso real em
`customer`. Campos conceituais: `tenantId`, `name`, `phone` (E.164, chave
natural dentro do tenant).

Fontes de leitura (nenhuma mudança de contrato):
- `GetAvailableSlotsHandler` (`scheduling`, TODO-005) — disponibilidade.
- `catalog.api` — serviços e ofertas ativos do tenant, para a lista de
  escolha.
- `organization.api`/resolução de slug — tenant a partir de `/b/{slug}`.

---

## User Experience

### Fluxo principal

1. Visitante abre `/b/{slug}`. Vê nome do estabelecimento e lista de
   serviços.
2. Escolhe um serviço. Vê a lista de profissionais que oferecem aquele
   serviço.
3. Escolhe um profissional. Escolhe uma data dentro do horizonte de 30
   dias.
4. Vê a lista de horários livres (TODO-005) para aquele profissional,
   serviço e data. Escolhe um horário.
5. Informa nome e telefone. Confirma.
6. Vê a tela de sucesso com o resumo do agendamento.

### Edge Cases

- **Serviço sem nenhum profissional ativo** — mensagem clara na etapa de
  escolha de profissional, sem travar a página.
- **Data sem nenhum horário livre** — mensagem clara ("sem horários
  disponíveis nesta data"), cliente pode escolher outra data.
- **Duas abas do mesmo visitante tentando o mesmo horário** — mesma
  garantia de US-6: uma vence, a outra recebe erro tratado.
- **Telefone já usado por outro nome no mesmo tenant** — reaproveita o
  `Customer` existente pelo telefone; o nome mais recente informado
  atualiza o cadastro (telefone é a chave, não o nome).
- **Horário escolhido "expira" entre a listagem e a confirmação** (outro
  cliente reservou primeiro) — mesma garantia de US-6, mensagem de erro
  tratada, cliente volta para escolher outro horário.
- **Bot preenchendo o formulário automaticamente** — honeypot (BR-7) e
  rate limit (BR-8) contêm sem depender de CAPTCHA.

---

## Critical E2E Test Scenarios

> Sem LTP nesta instalação. Os cenários viram testes de integração e web
> contra Postgres real, como nas features anteriores.

### E2E-1: Caminho feliz — agendamento criado com sucesso

**Criticidade**: 🔴 Critical — sem este caminho a feature não existe.

1. Estabelecimento com um serviço, uma oferta ativa, um profissional com
   disponibilidade calculada pela TODO-005.
2. Visitante percorre o fluxo completo e confirma com nome e telefone
   novos.

**Resultado esperado**: `Appointment` gravado com status `SCHEDULED`,
retrato correto de duração/preço; `Customer` novo criado com aquele
telefone; tela de sucesso mostra o resumo.

### E2E-2: Duas reservas simultâneas — só uma vence

**Criticidade**: 🔴 Critical — protege BR-4/US-6, o coração da feature.

1. Duas requisições concorrentes de confirmação para o mesmo
   profissional e o mesmo horário.

**Resultado esperado**: exatamente um `Appointment` `SCHEDULED` gravado;
a outra requisição recebe erro tratado, não 500.

### E2E-3: Id de outro tenant é recusado

**Criticidade**: 🔴 Critical — mesma garantia de isolamento de todas as
features anteriores, agora numa rota pública.

1. Requisição de confirmação para `/b/{slug-do-tenant-A}` usando
   `professionalId` ou `serviceOfferingId` que pertence ao tenant B.

**Resultado esperado**: erro de validação — nenhum `Appointment` gravado.

### E2E-4: Honeypot preenchido é recusado silenciosamente

**Criticidade**: 🟡 Important — protege BR-7/US-7.

1. `POST` de confirmação com o campo honeypot preenchido.

**Resultado esperado**: nenhum `Appointment` gravado; resposta não
revela que o honeypot foi a causa.

### E2E-5: Rate limit por IP

**Criticidade**: 🟡 Important — protege BR-8/US-7.

1. 6 tentativas de confirmação do mesmo IP em menos de 10 minutos.

**Resultado esperado**: as 5 primeiras seguem o fluxo normal (podem
falhar por outro motivo); a 6ª é recusada por rate limit antes de
qualquer outra validação.

### E2E-6: Teto de agendamentos futuros por telefone

**Criticidade**: 🟡 Important — protege BR-9/US-7.

1. Um telefone já tem 3 `Appointment` `SCHEDULED` futuros no mesmo
   tenant.
2. Tenta confirmar um 4º.

**Resultado esperado**: recusado com mensagem explicando o teto; os 3
existentes continuam intactos.

### E2E-7: Telefone repetido reaproveita o Customer

**Criticidade**: 🟢 Should — protege BR-3.

1. Um telefone já tem um `Customer` e um `Appointment` anteriores no
   tenant.
2. Confirma um novo agendamento com o mesmo telefone.

**Resultado esperado**: o `Appointment` novo referencia o **mesmo**
`Customer` — nenhum `Customer` duplicado é criado.

### E2E Test Summary

| ID | Cenário | Tipo | Cobre |
|---|---|---|---|
| E2E-1 | Caminho feliz | Caminho feliz | US-1 a US-5, BR-1, BR-2 |
| E2E-2 | Concorrência no mesmo horário | Concorrência | US-6, BR-4 |
| E2E-3 | Id de outro tenant recusado | Isolamento | BR-5 |
| E2E-4 | Honeypot | Erro/exclusão | US-7, BR-7 |
| E2E-5 | Rate limit por IP | Erro/exclusão | US-7, BR-8 |
| E2E-6 | Teto por telefone | Erro/exclusão | US-7, BR-9 |
| E2E-7 | Customer reaproveitado | Caminho feliz | BR-3 |

---

## Success Metrics

### Business Metrics

- Primeiro agendamento criado pelo link público sem intervenção manual
  do dono — **marco qualitativo do piloto**, sem meta numérica ainda.

### User Metrics

- Cliente completa o fluxo (serviço → profissional → data → horário →
  confirmação) sem precisar voltar por erro de validação evitável — sem
  meta numérica formal nesta fase.

### Technical Metrics

- Zero overbooking sob concorrência — **target: 0**, verificado por
  E2E-2.
- Zero vazamento entre tenants pela rota pública — **target: 0**,
  verificado por E2E-3.
- Zero `Customer` duplicado para o mesmo telefone no mesmo tenant —
  **target: 0**, verificado por E2E-7.

---

## Non-Functional Requirements

### Performance

Sem exigência numérica formal — volume esperado é o do piloto. O fluxo
de leitura reaproveita `GetAvailableSlotsHandler` (já otimizado na
TODO-005); a escrita é uma única transação por confirmação.

### Security

Primeira rota HTTP pública sem autenticação do projeto — muda a
superfície de ataque:
- `tenantId` nunca vem do formulário (BR-5), mesma regra fundamental do
  `CLAUDE.md`, agora exercida sem sessão nenhuma para se apoiar.
- CSRF não se aplica a quem nunca logou — a defesa aqui é honeypot, rate
  limit e teto por telefone (BR-7/BR-8/BR-9), não token CSRF.
- XSS: nome do cliente e nome do estabelecimento são renderizados só via
  `th:text` (nunca `th:utext`), mesma convenção do resto do projeto.
- Overbooking é impedido pelo banco, não pela aplicação (BR-4/ADR 0005) —
  regra fundamental do `CLAUDE.md`.
- Threat model formal deste fluxo fica para o DEBT-005, não bloqueia esta
  feature.

---

## Assumptions

- O fluxo de escolha é sempre serviço → profissional → data → horário —
  não existe "qualquer profissional disponível" nesta feature.
- Rate limit e teto por telefone são reavaliáveis com dado real do
  piloto; os números desta spec (5/10min, 3 futuros) são o ponto de
  partida, não uma promessa definitiva ao usuário final.
- Honeypot e rate limit dispensam CAPTCHA nesta fase — CAPTCHA fica como
  ideia futura se o abuso real justificar.
- "Hoje" e o horizonte de 30 dias seguem a mesma convenção de fuso da
  TODO-005 (hora local do servidor, sem campo de fuso do cliente).
- Onde vivem honeypot e rate limit (novo mecanismo em `platform`, ou
  dentro de `scheduling`) é decisão da spec técnica.
