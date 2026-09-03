# horario-jornada-bloqueios - Functional Spec

**Feature**: horario-jornada-bloqueios
**Backlog**: TODO-004
**Status**: approved
**Data**: 2026-09-02
**Aprovado por**: Elton Marques em 2026-09-03T01:17:35Z

---

## Problem Statement

Depois da TODO-003, um estabelecimento tem serviços, ofertas e profissionais
cadastrados — mas nenhum dado de disponibilidade. O glossário já distingue
três conceitos que faltam: `BusinessOperatingHours` (quando o estabelecimento
**pode** abrir — limite externo), `WorkSchedule` (quando o profissional
**declara** que trabalha, recorrente por semana) e `TimeOff` (quando um
profissional, ou o estabelecimento inteiro, está **excepcionalmente**
indisponível — folga, feriado, fechamento para reforma).

Sem estes três, o cálculo de disponibilidade (feature futura) não tem de
onde tirar horário nenhum — ele cruza `WorkSchedule` menos `TimeOff` menos
agendamentos existentes, mas hoje não existe nenhum dos três primeiros
termos dessa conta.

Esta feature **só declara o dado**. Não calcula, não cruza, não ocupa
horário nenhum — isso é da feature de agendamento (`scheduling`, ainda
vazio).

---

## Objectives

1. O dono consegue declarar quando o estabelecimento funciona, por dia da
   semana, em uma ou mais faixas.
2. O dono consegue declarar a jornada semanal recorrente de cada
   profissional, em uma ou mais faixas por dia — o mesmo mecanismo de
   "duas faixas no mesmo dia" que já representa o intervalo de almoço, sem
   precisar de um conceito novo.
3. O dono consegue registrar uma indisponibilidade excepcional e datada —
   folga de um profissional específico, ou fechamento que vale para o
   estabelecimento inteiro (feriado, reforma) — sem precisar de uma tabela
   diferente para cada caso.
4. Nenhuma faixa se sobrepõe a outra do mesmo profissional no mesmo dia,
   dentro da mesma jornada — evita o estado sem sentido de "o profissional
   trabalha em dois lugares ao mesmo tempo".

---

## Scope

### In Scope

- Cadastrar o horário de funcionamento do estabelecimento: dia da semana,
  horário de abertura e de fechamento. Várias faixas por dia são permitidas.
  Dia sem nenhuma faixa é dia fechado.
- Listar o horário de funcionamento já cadastrado.
- Cadastrar a jornada semanal de um profissional: dia da semana, início e
  fim. Várias faixas por dia são permitidas — é assim que um intervalo de
  almoço recorrente é representado (duas faixas no mesmo dia; o vão entre
  elas é o almoço), sem precisar de um campo separado para almoço.
- Listar a jornada já cadastrada de um profissional.
- Registrar um bloqueio (`TimeOff`): início, fim (data e hora), motivo
  opcional. Pode ser de um profissional específico, ou sem profissional —
  e nesse caso vale para o estabelecimento inteiro (feriado, fechamento).
- Listar os bloqueios já cadastrados.
- Recusar duas faixas de jornada do mesmo profissional, no mesmo dia, que
  se sobrepõem no tempo.

### Out of Scope

- **Calcular disponibilidade de verdade** (cruzar jornada, bloqueio e
  agendamento existente para saber os horários livres). Isso é
  `scheduling`, feature futura — esta feature só guarda o dado declarado.
- **Editar ou desativar horário de funcionamento, jornada ou bloqueio.**
  Mesma decisão das três features anteriores: só criar e listar.
  `deactivate()` fica disponível no domínio (ADR sobre ciclo de vida — nada
  é apagado), mas sem tela.
- **Validação cruzada entre `WorkSchedule` e `BusinessOperatingHours`.**
  Um profissional pode ter jornada declarada fora do horário de
  funcionamento do estabelecimento naquele dia — não é bloqueado. São dados
  declarados independentes; a consequência prática (se existe alguma) é do
  cálculo de disponibilidade, não do cadastro.
- **Sobreposição entre bloqueios (`TimeOff`).** Dois bloqueios cobrindo o
  mesmo intervalo não são contraditórios — são redundantes, sem problema.
  Só a sobreposição de faixas de jornada (BR-3) é recusada.
- **Recorrência automática de feriado.** Cada feriado é um `TimeOff`
  cadastrado à mão, para a data certa. Sem repetição anual automática.
- **Fuso horário diferente do local do estabelecimento.** Todos os
  horários — funcionamento, jornada e bloqueio — são em hora local, sem
  campo de fuso.

---

## User Stories

### US-1: Cadastrar o horário de funcionamento do estabelecimento

**Como** dono de um estabelecimento,
**Eu quero** declarar em quais dias e horários meu estabelecimento funciona,
**Para que** essa informação exista antes de qualquer cálculo de
disponibilidade precisar dela.

**Acceptance Criteria**:
- Formulário pede dia da semana, horário de abertura e horário de
  fechamento.
- Fechamento antes ou igual à abertura devolve erro no campo, sem gravar.
- Várias faixas no mesmo dia são aceitas (ex.: 08:00–12:00 e 13:00–18:00).
- Ao salvar, a faixa aparece imediatamente na lista.

### US-2: Ver o horário de funcionamento cadastrado

**Como** dono,
**Eu quero** ver as faixas de funcionamento já cadastradas, agrupadas por
dia da semana,
**Para que** eu confira o que já declarei antes de adicionar mais uma
faixa.

**Acceptance Criteria**:
- A lista mostra só as faixas do tenant da sessão.
- Estabelecimento sem nenhuma faixa cadastrada mostra a lista vazia com uma
  chamada para cadastrar a primeira.

### US-3: Cadastrar a jornada semanal de um profissional

**Como** dono,
**Eu quero** declarar em quais dias e horários um profissional trabalha,
**Para que** essa informação exista antes de qualquer cálculo de
disponibilidade precisar dela.

**Acceptance Criteria**:
- Formulário pede profissional (dropdown, entre os já cadastrados), dia da
  semana, horário de início e horário de fim.
- Sem nenhum profissional cadastrado, a tela explica isso em vez de mostrar
  um dropdown vazio sem contexto.
- Fim antes ou igual ao início devolve erro no campo, sem gravar.
- Várias faixas no mesmo dia, para o mesmo profissional, são aceitas — é
  assim que um almoço recorrente é representado (duas faixas, o vão entre
  elas é o almoço).
- Duas faixas do mesmo profissional, no mesmo dia, que se sobrepõem no
  tempo, são recusadas com erro de campo — ele não pode estar em dois
  lugares ao mesmo tempo (BR-3).
- Ao salvar, a faixa aparece imediatamente na lista.

### US-4: Ver a jornada de um profissional

**Como** dono,
**Eu quero** ver as faixas de jornada já cadastradas de um profissional,
agrupadas por dia da semana,
**Para que** eu confira o que já declarei antes de adicionar mais uma
faixa ou de cadastrar uma folga.

**Acceptance Criteria**:
- A lista mostra só as faixas do tenant da sessão, e permite ver as faixas
  de um profissional específico.
- Profissional sem nenhuma faixa cadastrada mostra a lista vazia com uma
  chamada para cadastrar a primeira.

### US-5: Registrar um bloqueio (folga ou feriado)

**Como** dono,
**Eu quero** registrar que um profissional (ou o estabelecimento inteiro)
está indisponível num período específico,
**Para que** essa exceção exista antes de qualquer cálculo de
disponibilidade precisar dela.

**Acceptance Criteria**:
- Formulário pede início, fim (data e hora), motivo (opcional), e
  profissional — com uma opção explícita de "vale para o estabelecimento
  inteiro" em vez de escolher um profissional.
- Fim antes ou igual ao início devolve erro no campo, sem gravar.
- Escolher "estabelecimento inteiro" grava sem profissional associado —
  é assim que feriado e fechamento para reforma são representados, sem
  tabela nova.
- Ao salvar, o bloqueio aparece imediatamente na lista.

### US-6: Ver os bloqueios cadastrados

**Como** dono,
**Eu quero** ver os bloqueios já cadastrados,
**Para que** eu confira o que já registrei, distinguindo bloqueio de um
profissional específico do que vale para todo o estabelecimento.

**Acceptance Criteria**:
- A lista mostra só os bloqueios do tenant da sessão.
- Cada linha deixa claro se o bloqueio é de um profissional específico
  (mostra o nome) ou do estabelecimento inteiro.
- Estabelecimento sem nenhum bloqueio cadastrado mostra a lista vazia, sem
  chamada obrigatória (bloqueio é exceção, não é esperado que todo
  estabelecimento tenha um).

---

## Business Rules

### Core Rules

- **BR-1**: `BusinessOperatingHours` tem `day_of_week`, `opens_at`,
  `closes_at`, em hora local. `closes_at` é estritamente maior que
  `opens_at`. Várias faixas por dia são permitidas; dia sem nenhuma faixa é
  dia fechado — não existe um campo "fechado" separado.
- **BR-2**: `WorkSchedule` tem `professional_id`, `day_of_week`,
  `starts_at`, `ends_at`, em hora local. `ends_at` é estritamente maior que
  `starts_at`.
- **BR-3**: Duas faixas de `WorkSchedule` do **mesmo profissional**, no
  **mesmo dia da semana**, não podem se sobrepor no tempo. Sobreposição é
  quando o início de uma faixa é menor que o fim da outra, e vice-versa —
  faixas encostadas (fim de uma igual ao início da outra) não se
  sobrepõem, e são o mecanismo do intervalo de almoço.
- **BR-4**: `WorkSchedule` **não** é validado contra `BusinessOperatingHours`
  do mesmo dia. São dados declarados independentes (decisão explícita —
  ver Out of Scope).
- **BR-5**: `TimeOff` tem `starts_at`, `ends_at` (timestamptz — data e hora,
  não só dia da semana, porque é excepcional e datado, não recorrente),
  `reason` opcional. `ends_at` é estritamente maior que `starts_at`.
- **BR-6**: `professional_id` de `TimeOff` é anulável. Nulo significa que o
  bloqueio vale para o estabelecimento inteiro — é assim que feriado e
  fechamento para reforma são representados, sem tabela nova.
- **BR-7**: `tenant_id` de `BusinessOperatingHours`, `WorkSchedule` e
  `TimeOff` vem exclusivamente do `TenantContext` — nunca de campo de
  formulário.
- **BR-8**: `professional_id` de `WorkSchedule` e de `TimeOff` (quando
  presente) precisa pertencer ao mesmo tenant da sessão. Diferente da
  TODO-003, aqui `Professional` é do **mesmo contexto** (`organization`) —
  a garantia pode ser uma chave estrangeira normal, não uma validação via
  `api`.

### Validation Invariants

- `closes_at` ≤ `opens_at` em `BusinessOperatingHours` → erro de campo,
  nunca 500.
- `ends_at` ≤ `starts_at` em `WorkSchedule` ou `TimeOff` → erro de campo.
- Faixa de `WorkSchedule` sobreposta a outra do mesmo profissional no mesmo
  dia → erro de campo, nenhuma segunda linha gravada.
- `professionalId` que não existe, ou existe em outro tenant, em
  `WorkSchedule` ou `TimeOff` → erro de campo.

---

## Data Model (conceitual, para a spec técnica detalhar)

**`BusinessOperatingHours`** — entrada: `dayOfWeek`, `opensAt`, `closesAt`.
Saída: id, mais os três campos de entrada. Entidade de `Business`, sem
identidade própria fora dele.

**`WorkSchedule`** — entrada: `professionalId`, `dayOfWeek`, `startsAt`,
`endsAt`. Saída: id, mais os campos de entrada, com o nome do profissional
resolvido para exibição. Raiz de agregado.

**`TimeOff`** — entrada: `professionalId` (opcional), `startsAt`, `endsAt`,
`reason` (opcional). Saída: id, mais os campos de entrada, com o nome do
profissional resolvido para exibição quando presente, ou uma indicação
explícita de "estabelecimento inteiro" quando ausente. Entidade de
`WorkSchedule` (mesmo agregado, ver glossário).

---

## User Experience

### Fluxo principal

1. Dono, autenticado, declara o horário de funcionamento do estabelecimento
   — abre segunda a sexta, 08:00–18:00.
2. Declara a jornada de um profissional já cadastrado — segunda a sexta,
   08:00–12:00 e 13:00–18:00 (almoço entre as duas faixas).
3. Meses depois, registra um bloqueio: esse profissional avisa que vai
   faltar numa quinta-feira específica — bloqueio datado, só para ele.
4. Perto do fim do ano, registra outro bloqueio, sem escolher profissional
   nenhum: 25 de dezembro, feriado — vale para todo o estabelecimento.

### Edge Cases

- **Tentar cadastrar jornada sem nenhum profissional cadastrado** — a tela
  explica que é preciso cadastrar um profissional primeiro (TODO-002), em
  vez de mostrar um dropdown vazio.
- **Cadastrar duas faixas de jornada que se tocam** (ex.: 08:00–12:00 e
  12:00–18:00) — aceito; é diferente de sobreposição (BR-3), e é um dia
  sem almoço, uma jornada legítima.
- **Cadastrar um feriado que cai num dia em que um profissional específico
  também tem folga cadastrada** — os dois bloqueios coexistem sem conflito;
  bloqueio não valida contra outro bloqueio (Out of Scope).
- **Estabelecimento A tenta registrar bloqueio ou jornada usando o id de um
  profissional do estabelecimento B** (formulário forjado) — recusado.
  Aqui `Professional` é do mesmo contexto, então a rejeição pode vir de uma
  chave estrangeira normal, não de uma consulta a outro contexto (diferente
  da TODO-003).

---

## Critical E2E Test Scenarios

> Sem LTP nesta instalação. Os cenários viram testes de integração com
> Testcontainers, como nas três features anteriores.

### E2E-1: Cadastro completo, caminho feliz

**Criticidade**: 🔴 Critical — sem este caminho a feature não existe.

1. Dono cadastra o horário de funcionamento do estabelecimento.
2. Dono cadastra a jornada semanal de um profissional, com duas faixas no
   mesmo dia (almoço).
3. Dono registra um bloqueio para esse profissional, e outro sem
   profissional (feriado).

**Resultado esperado**: os quatro registros aparecem nas listas
correspondentes; o bloqueio sem profissional aparece marcado como
"estabelecimento inteiro".

### E2E-2: Faixas de jornada sobrepostas são recusadas

**Criticidade**: 🔴 Critical — protege BR-3.

1. Dono cadastra a faixa de segunda 08:00–12:00 para um profissional.
2. Tenta cadastrar a faixa de segunda 10:00–14:00 para o **mesmo**
   profissional.

**Resultado esperado**: erro de campo na segunda tentativa, nenhuma segunda
linha gravada.

### E2E-3: Isolamento entre tenants

**Criticidade**: 🔴 Critical — mesma garantia estabelecida desde a TODO-001,
estendida aos três agregados novos.

1. Tenant A cadastra horário de funcionamento, jornada e bloqueio.
2. Tenant B consulta as próprias listas.

**Resultado esperado**: nenhum dado do tenant A aparece para o tenant B.
Estende o `CrossTenantIsolationIT`.

### E2E Test Summary

| ID | Cenário | Tipo | Cobre |
|---|---|---|---|
| E2E-1 | Cadastro completo dos três agregados | Caminho feliz | US-1 a US-6 |
| E2E-2 | Faixas de jornada sobrepostas | Erro | US-3, BR-3 |
| E2E-3 | Isolamento entre tenants | Isolamento | BR-7, BR-8 |

---

## Success Metrics

### Business Metrics

- Estabelecimentos com horário de funcionamento e ao menos uma jornada de
  profissional cadastrados — **target: acompanhar no piloto**, sem meta
  numérica ainda (n=1).

### User Metrics

- Nenhuma métrica de UX nova além das já estabelecidas (formulário +
  lista imediata).

### Technical Metrics

- Faixas de jornada sobrepostas gravadas com sucesso — **target: 0**,
  verificado por teste de integração (E2E-2).
- Registros de um tenant visíveis para outro — **target: 0**, verificado
  pela extensão do `CrossTenantIsolationIT` (E2E-3).

---

## Non-Functional Requirements

### Performance

Sem exigência especial. Cadastro de horário/jornada/bloqueio é ação
administrativa pouco frequente (configurada uma vez, ajustada raramente).

### Security

Nenhuma superfície nova de risco de autenticação — rotas sob `/admin/**`,
já protegidas. Isolamento entre tenants coberto por BR-7, BR-8 e E2E-3;
aqui a garantia pode vir de chave estrangeira normal (mesmo contexto),
diferente da TODO-003.

---

## Assumptions

- Um profissional sem nenhuma jornada cadastrada é um estado válido (o
  dono cadastrou o profissional antes de declarar quando ele trabalha) —
  não é erro, só incompleto.
- "Horário de funcionamento", "jornada" e "bloqueio" usam os termos do
  glossário (`BusinessOperatingHours`, `WorkSchedule`, `TimeOff`); a UI usa
  linguagem coloquial em português, mas o código nunca mistura os três
  conceitos.
- As duas perguntas em aberto do `meta.md` (validação cruzada com
  `BusinessOperatingHours`, sobreposição de faixas) foram decididas antes
  desta spec: sem validação cruzada (BR-4), com bloqueio de sobreposição
  dentro do mesmo `WorkSchedule` (BR-3).
