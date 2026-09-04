# consultar-horarios-disponiveis - Functional Spec

**Feature**: consultar-horarios-disponiveis
**Backlog**: TODO-005
**Status**: approved
**Data**: 2026-09-03
**Aprovado por**: Elton Marques em 2026-09-04T01:08:24Z

---

## Problem Statement

Depois da TODO-004, `organization` sabe declarar quando o estabelecimento
funciona (`BusinessOperatingHours`), quando cada profissional trabalha
(`WorkSchedule`) e quando alguém está indisponível (`TimeOff`). Mas nada no
projeto ainda **calcula** um horário livre de verdade — `scheduling` está
vazio.

Sem esse cálculo, a TODO-006 (página pública e agendar) não tem o que
mostrar ao cliente. Esta feature entrega só o cálculo — dado um profissional,
uma oferta de serviço e uma data, quais horários de início estão realmente
disponíveis. Não escreve nada, não reserva nada, não tem tela nova: é o
motor que a TODO-006 vai chamar.

---

## Objectives

1. Calcular a lista de horários de início possíveis (`AvailableSlot`) para
   um profissional, uma oferta (`ServiceOffering`) e uma data específicos.
2. O cálculo cruza três fontes de dado já existentes: horário do
   estabelecimento (`BusinessOperatingHours`, via `organization.api`) ∩
   jornada do profissional (`WorkSchedule`, via `organization.api`) −
   bloqueios que se sobrepõem à data (`TimeOff`, via `organization.api`).
   `Appointment` (agendamento de verdade) não existe ainda — fica para a
   TODO-006 — então o cálculo não desconta nenhum agendamento por enquanto.
3. Horários candidatos nascem numa grade fixa de 10 minutos (ADR 0006), e só
   viram `AvailableSlot` válido se a duração da oferta mais o intervalo
   (`bufferMinutes`, via `catalog.api`) couber inteiro dentro de uma janela
   livre.
4. A consulta tem um horizonte máximo de 30 dias a partir de hoje.

---

## Scope

### In Scope

- Calcular horários disponíveis para um profissional + uma oferta + uma
  data, cruzando `BusinessOperatingHours`, `WorkSchedule` e `TimeOff` do
  tenant.
- Gerar horários candidatos em grade fixa de 10 minutos dentro de cada
  janela livre do dia.
- Filtrar candidatos pela duração da oferta mais o intervalo
  (`bufferMinutes`), sem cruzar bloqueio nem ultrapassar o fim da janela.
- Rejeitar consulta para data fora do intervalo [hoje, hoje + 30 dias].
- Consulta isolada por tenant: profissional e oferta precisam pertencer ao
  mesmo tenant da sessão/slug (mesma garantia de todas as features
  anteriores).

### Out of Scope

- **Reservar um horário de verdade.** Isso é a TODO-006 — esta feature só
  calcula, nunca grava.
- **`Appointment`, a tabela e a exclusion constraint do ADR 0005.** Nascem
  na TODO-006, quando há escrita de verdade para proteger. Aqui, "menos
  agendamentos existentes" é implicitamente vazio.
- **Qualquer tela nova.** Ninguém vê isso ainda — é o motor interno que a
  TODO-006 vai expor ao cliente.
- **Configuração de grade por estabelecimento.** A grade de 10 minutos é
  uma constante do sistema (ADR 0006: "não haverá tela de configuração de
  estratégia"), não um campo por tenant, apesar do glossário descrever
  `slotInterval` como "configuração do tenant" em nível conceitual.
- **`DYNAMIC_DURATION` como segunda estratégia de slot.** IDEA-004,
  gatilho é dado real de ociosidade (DEBT-008), não existe ainda.
- **Reagendamento e cancelamento.** TODO-006/008, dependem de `Appointment`
  existir.

---

## User Stories

> Não há usuário humano interagindo com tela nesta feature — quem "usa" o
> resultado é a TODO-006. As histórias abaixo descrevem o comportamento
> esperado do cálculo, do ponto de vista de quem vai depender dele.

### US-1: Ver os horários realmente livres de um profissional num dia

**Como** cliente que está tentando agendar (via TODO-006, ainda não
existe),
**Eu quero** que o sistema me mostre só horários que o profissional
realmente pode atender,
**Para que** eu não tente marcar um horário que na prática está ocupado ou
fora do expediente.

**Acceptance Criteria**:
- Dado profissional, oferta e data, o cálculo retorna uma lista de
  `AvailableSlot`, cada um com horário de início.
- Nenhum horário fora do expediente do estabelecimento naquele dia da
  semana aparece na lista.
- Nenhum horário fora da jornada do profissional naquele dia da semana
  aparece na lista.

### US-2: Bloqueios nunca aparecem como horário livre

**Como** cliente,
**Eu quero** que uma folga do profissional ou um feriado do estabelecimento
nunca apareça como horário disponível,
**Para que** eu não tente marcar um atendimento que não vai acontecer.

**Acceptance Criteria**:
- Um `TimeOff` do profissional específico remove exatamente o intervalo
  bloqueado da lista de horários daquele profissional, sem afetar outros
  profissionais.
- Um `TimeOff` sem profissional (vale para o estabelecimento inteiro) zera
  a disponibilidade de **todos** os profissionais que se sobrepõem àquele
  intervalo.

### US-3: Só aparece horário onde o atendimento inteiro cabe

**Como** cliente,
**Eu quero** que o horário mostrado garanta que o atendimento inteiro
(duração mais intervalo) caiba antes do próximo compromisso ou do fim do
expediente,
**Para que** o profissional nunca precise cortar meu atendimento pela
metade ou atrasar o próximo cliente.

**Acceptance Criteria**:
- Um horário candidato só é um `AvailableSlot` válido se `[candidato,
  candidato + duração + bufferMinutes)` couber inteiro dentro de uma única
  janela livre.
- Um candidato que cruzaria um bloqueio, ou que ultrapassaria o fim da
  janela livre, não aparece na lista.

### US-4: Consulta fora do horizonte permitido é rejeitada

**Como** consumidor do cálculo (a TODO-006, futuramente),
**Eu quero** que uma consulta para uma data fora de [hoje, hoje + 30 dias]
seja rejeitada explicitamente,
**Para que** eu não confunda "sem dado ainda calculado" com "não há
horário livre".

**Acceptance Criteria**:
- Consulta para data anterior a hoje, ou posterior a hoje + 30 dias,
  levanta um erro — não retorna lista vazia silenciosamente.
- Consulta dentro do intervalo permitido, mas para um dia sem nenhuma
  jornada cadastrada (profissional) ou sem expediente (estabelecimento),
  retorna lista vazia — isso **é** uma resposta válida, não um erro.

---

## Business Rules

### Core Rules

- **BR-1**: `AvailableSlot` é um value object — nunca persistido, sempre
  recalculado sob demanda.
- **BR-2**: A janela livre de um profissional num dia é a interseção entre
  `BusinessOperatingHours` (do dia da semana) e `WorkSchedule` do
  profissional (do mesmo dia da semana), menos qualquer `TimeOff` — do
  profissional específico ou do estabelecimento inteiro (`professionalId`
  nulo) — que se sobreponha à data consultada.
- **BR-3**: Dentro de cada janela livre, os horários candidatos nascem em
  grade fixa de 10 minutos (ADR 0006), a partir do início da janela.
- **BR-4**: Um candidato só é um `AvailableSlot` válido se `[candidato,
  candidato + duração da oferta + bufferMinutes da oferta)` couber
  inteiramente dentro de uma única janela livre.
- **BR-5**: A data consultada precisa estar entre hoje e hoje + 30 dias
  (inclusive), em hora local do servidor — mesma convenção do resto do
  projeto (sem campo de fuso). Fora desse intervalo, a consulta é
  rejeitada (erro) — não é uma lista vazia válida.
- **BR-6**: Se o profissional não tiver nenhuma `WorkSchedule` para aquele
  dia da semana, ou o estabelecimento não tiver nenhuma
  `BusinessOperatingHours` para aquele dia, a consulta retorna lista
  vazia — dia sem expediente é um resultado válido, não uma falha.
- **BR-7**: Profissional e oferta consultados precisam pertencer ao mesmo
  tenant da sessão/slug. `tenantId` nunca vem do parâmetro do chamador.
- **BR-8**: `Appointment` não existe nesta feature — o termo "menos
  agendamentos existentes" do cálculo geral (ver glossário) é
  implicitamente vazio até a TODO-006.

### Validation Invariants

- Data fora de [hoje, hoje + 30 dias] → erro, nunca lista vazia.
- Profissional ou oferta que não existem, ou existem em outro tenant → erro.
- Dia sem expediente ou sem jornada cadastrada → lista vazia, não erro.

---

## Data Model (conceitual, para a spec técnica detalhar)

**`AvailableSlot`** — value object, nunca persistido. Campos conceituais:
`professionalId`, `serviceOfferingId`, `startsAt` (horário local de
início), `endsAt` (horário local de fim, informativo — início mais duração
mais intervalo).

Fontes de leitura (nenhuma tabela nova nesta feature):
- `BusinessOperatingHours`, `WorkSchedule`, `TimeOff` — via
  `organization.api` (TODO-004).
- Duração e `bufferMinutes` da `ServiceOffering` — via `catalog.api`
  (TODO-003).

---

## User Experience

> Sem tela. O "fluxo" é o consumo futuro pela TODO-006.

### Fluxo principal (consumidor futuro, TODO-006)

1. Cliente escolhe um profissional e uma oferta na página pública.
2. Cliente escolhe uma data dentro do horizonte permitido.
3. O sistema calcula e devolve a lista de horários livres para aquele
   profissional, oferta e data.
4. Cliente escolhe um horário da lista (a reserva de verdade é a TODO-006).

### Edge Cases

- **Profissional sem nenhuma jornada cadastrada** (esqueceu de cadastrar
  na TODO-004, ou é um profissional novo) — lista vazia, não erro (BR-6).
- **Estabelecimento fechado naquele dia da semana** (sem
  `BusinessOperatingHours` para o dia) — lista vazia, mesmo que o
  profissional tenha jornada cadastrada — o limite externo prevalece
  (BR-2, interseção).
- **Feriado que cobre o dia inteiro** (`TimeOff` sem profissional, do meio-dia
  ao fim do expediente, ou o dia inteiro) — lista vazia para todos os
  profissionais que se sobrepõem ao intervalo bloqueado.
- **Oferta com duração maior que qualquer janela livre do dia** — lista
  vazia; nenhum candidato cabe inteiro em nenhuma janela.
- **Consulta para "hoje" já perto do fim do expediente** — só os horários
  que ainda cabem antes do fim da janela aparecem; não há tratamento
  especial de "hora atual" nesta feature (isso é uma decisão da TODO-006,
  quando existir cliente de verdade consultando em tempo real).

---

## Critical E2E Test Scenarios

> Sem LTP nesta instalação, e sem tela para exercitar. Os cenários viram
> testes de integração contra Postgres real, chamando o caso de uso
> diretamente — como nas features anteriores, mas sem a camada web.

### E2E-1: Cálculo em caminho feliz

**Criticidade**: 🔴 Critical — sem este caminho a feature não existe.

1. Estabelecimento funciona segunda 08:00–18:00. Profissional tem jornada
   segunda 08:00–12:00 e 13:00–18:00 (almoço). Oferta dura 30 min, buffer
   0.
2. Consulta disponibilidade para essa segunda-feira.

**Resultado esperado**: horários de 30 em 30 min... na verdade, na grade
fixa de 10 min, isto é, candidatos a cada 10 minutos dentro de cada janela
(08:00, 08:10, 08:20, ... até o último que ainda cabe 30 min antes de
12:00; e de novo entre 13:00 e 18:00). Nenhum horário aparece entre 12:00
e 13:00.

### E2E-2: Bloqueio do profissional remove só a janela dele

**Criticidade**: 🔴 Critical — protege BR-2 e US-2.

1. Mesmo cenário do E2E-1. Profissional registra um `TimeOff` das 10:00 às
   11:00 dessa segunda.
2. Consulta disponibilidade.

**Resultado esperado**: nenhum candidato entre 10:00 e 11:00; candidatos
antes das 10:00 e depois das 11:00 (até o limite de 12:00) continuam
aparecendo.

### E2E-3: Feriado do estabelecimento zera todos os profissionais

**Criticidade**: 🔴 Critical — protege BR-2 e US-2 para o caso "sem
profissional".

1. Dois profissionais com jornada na mesma segunda-feira. Estabelecimento
   registra um `TimeOff` sem profissional (feriado) cobrindo o dia
   inteiro.
2. Consulta disponibilidade para os dois profissionais nessa data.

**Resultado esperado**: lista vazia para os dois.

### E2E-4: Isolamento entre tenants

**Criticidade**: 🔴 Critical — mesma garantia estabelecida desde a
TODO-001.

1. Tenant A tem um profissional e uma oferta cadastrados.
2. Consulta disponibilidade usando o `professionalId` do tenant A, mas
   autenticado/no slug do tenant B.

**Resultado esperado**: erro — profissional não pertence ao tenant da
consulta (BR-7).

### E2E Test Summary

| ID | Cenário | Tipo | Cobre |
|---|---|---|---|
| E2E-1 | Caminho feliz, grade de 10 min | Caminho feliz | US-1, US-3, BR-3, BR-4 |
| E2E-2 | Bloqueio do profissional | Erro/exclusão | US-2, BR-2 |
| E2E-3 | Feriado do estabelecimento | Erro/exclusão | US-2, BR-2 |
| E2E-4 | Isolamento entre tenants | Isolamento | BR-7 |

---

## Success Metrics

### Business Metrics

- Nenhuma métrica de negócio nova nesta feature — ela não é visível para
  ninguém ainda. O valor de negócio se realiza na TODO-006.

### User Metrics

- N/A — sem usuário final nesta feature.

### Technical Metrics

- Zero horários sobrepostos ou fora de expediente oferecidos como
  disponíveis — **target: 0**, verificado pelos testes (E2E-1 a E2E-3).
- Zero vazamento de dado entre tenants — **target: 0**, verificado por
  E2E-4.

---

## Non-Functional Requirements

### Performance

Sem exigência numérica formal ainda (não há tela consumindo em produção).
O cálculo deve ser O(faixas do dia do profissional), não O(todo o
histórico de jornada) — a consulta de `WorkSchedule`/`TimeOff` já é
filtrada por profissional + dia da semana (mesmo índice da TODO-004).

### Security

Nenhuma superfície nova de autenticação — este é um cálculo interno,
chamado por outro caso de uso (a TODO-006, futuramente), não por uma rota
HTTP própria. Isolamento entre tenants coberto por BR-7 e E2E-4.

---

## Assumptions

- A grade de 10 minutos (`slotInterval`) é uma constante do sistema no
  MVP, não configurável por tenant — apesar do glossário descrever o
  conceito como "configuração do tenant", não existe tela nem campo para
  isso, e ADR 0006 é explícito que não haverá tela de configuração de
  estratégia. Fica revisitável junto com IDEA-004.
- "Hoje" é a data do servidor (mesmo fuso do resto do projeto — hora
  local, sem campo de fuso do cliente).
- Esta feature não é exposta por nenhum controller HTTP. O caso de uso é
  testado isoladamente (unitário de domínio + integração contra Postgres
  real com dados de `organization`/`catalog`); a primeira chamada de
  verdade, por uma rota pública, é a TODO-006.
- Sem `Appointment`, não há concorrência a testar aqui — o cálculo é
  leitura pura, sem escrita, portanto sem risco de condição de corrida
  nesta feature (isso volta a importar na TODO-006, com a reserva de
  verdade).
