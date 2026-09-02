# cadastro-servico-oferta - Functional Spec

**Feature**: cadastro-servico-oferta
**Backlog**: TODO-003
**Status**: approved
**Data**: 2026-09-01
**Aprovado por**: Elton Marques em 2026-09-01T23:03:38Z

---

## Problem Statement

Depois da TODO-002, um estabelecimento tem profissionais, mas nenhum
serviço cadastrado — não há sobre o que o cálculo de disponibilidade
(TODO-005) ou a página pública (TODO-006) se apoiarem. O glossário distingue
dois conceitos que o cadastro precisa refletir: `Service` é o item do
catálogo ("Corte de Cabelo", sem preço nem duração), e `ServiceOffering` é o
que o cliente de fato agenda — o mesmo serviço, executado por um
profissional específico, com sua própria duração, preço e intervalo.

Esta é também a primeira feature a tocar `catalog` (hoje um pacote vazio) e
a primeira a exigir que `organization` exponha algo pelo pacote `api` — até
aqui, nenhum outro contexto precisou enxergar `organization` de fora.

---

## Objectives

1. O dono consegue cadastrar um serviço e, para cada profissional que o
   executa, uma oferta com duração, preço e intervalo próprios.
2. `organization/api/` nasce com a menor superfície possível: uma operação
   de leitura, sem parâmetro, no mesmo padrão de "tenant nunca é argumento"
   que a TODO-002 estabeleceu dentro de um contexto — agora atravessando a
   fronteira entre dois.
3. `Money` nasce em `shared`, com o mínimo que este feature exige: guardar
   um valor em centavos e exibi-lo formatado. Nenhuma operação especulativa.
4. Nenhum `ServiceOffering` pode referenciar profissional de outro tenant —
   verificado por teste, não só por convenção, já que não existe chave
   estrangeira entre `catalog` e `organization` (ADR de agregação por UUID).

---

## Scope

### In Scope

- Cadastrar um serviço: nome e descrição (opcional).
- Listar os serviços já cadastrados do estabelecimento.
- Cadastrar uma oferta: escolher um serviço existente e um profissional
  existente, informar duração, preço e intervalo entre clientes.
- Listar as ofertas já cadastradas, mostrando o nome do serviço e do
  profissional.
- `organization.api` expõe `ProfessionalDirectory.listActive()` — a lista de
  profissionais ativos do tenant da sessão. É a primeira operação do
  primeiro pacote `api` do projeto.
- `Money` como tipo em `shared`: construção a partir do valor digitado,
  exibição formatada ("R$ 30,00").
- Validação de que o profissional escolhido na oferta pertence ao mesmo
  tenant do serviço (e da sessão) — via `organization.api`, não por FK.

### Out of Scope

- **Editar ou desativar serviço/oferta.** Mesma decisão da TODO-002: só
  criar e listar nesta feature. `deactivate()` fica disponível no domínio
  (ADR de ciclo de vida — nada é apagado), mas sem tela.
- **Categorias, tags ou foto do serviço.** Nenhum critério de aceite
  precisa disso hoje.
- **Exibição na página pública.** TODO-006.
- **Uso da duração/intervalo no cálculo de disponibilidade.** TODO-005 —
  esta feature só cadastra o dado, não o consome.
- **Operações aritméticas em `Money`** (soma, comparação, subtração). Não
  há critério de aceite que precise disso — agendamento de serviço
  combinado é a IDEA-005, não priorizada.
- **Qualquer outra operação em `organization.api` além de
  `listActive()`.** Criar, editar ou desativar profissional continua
  exclusivo da TODO-002; a API não replica esse controle.
- **Reordenar ou destacar serviços na lista.** Ordem é a de cadastro na
  listagem de serviços, alfabética na de ofertas — sem prioridade manual.

---

## User Stories

### US-1: Cadastrar um serviço

**Como** dono de um estabelecimento,
**Eu quero** cadastrar os serviços que ofereço,
**Para que** eu possa depois definir quem os executa e por quanto.

**Acceptance Criteria**:
- Formulário pede nome (obrigatório) e descrição (opcional).
- Nome vazio, ou já usado por outro serviço do mesmo estabelecimento,
  devolve erro no campo, sem gravar.
- Ao salvar, o serviço aparece imediatamente na lista.

### US-2: Ver os serviços já cadastrados

**Como** dono,
**Eu quero** ver a lista de serviços do meu estabelecimento,
**Para que** eu saiba o que já cadastrei antes de criar uma oferta.

**Acceptance Criteria**:
- A lista mostra só os serviços do tenant da sessão.
- Estabelecimento sem nenhum serviço mostra a lista vazia com uma chamada
  para cadastrar o primeiro.

### US-3: Cadastrar uma oferta

**Como** dono,
**Eu quero** dizer quanto tempo e quanto custa um serviço quando executado
por um profissional específico,
**Para que** cada profissional possa ter seu próprio preço e duração para o
mesmo serviço.

**Acceptance Criteria**:
- Formulário pede: serviço (dropdown, entre os já cadastrados), profissional
  (dropdown, entre os já cadastrados), duração em minutos, preço, intervalo
  entre clientes.
- Sem nenhum serviço cadastrado, ou sem nenhum profissional cadastrado, a
  tela explica isso em vez de mostrar um dropdown vazio sem contexto.
- O profissional escolhido é validado contra a sessão via
  `organization.api` — nunca aceito só porque veio no formulário.
- Duração deve ser maior que zero. Intervalo pode ser zero. Preço pode ser
  zero (serviço de cortesia) mas nunca negativo.
- Um profissional não pode ter duas ofertas do mesmo serviço — a segunda
  tentativa é erro de campo, não um segundo registro.
- Ao salvar, a oferta aparece imediatamente na lista.

### US-4: Ver as ofertas já cadastradas

**Como** dono,
**Eu quero** ver a lista de ofertas do meu estabelecimento, com o nome do
serviço e do profissional,
**Para que** eu confira o que já configurei sem precisar cruzar duas telas
de cabeça.

**Acceptance Criteria**:
- Cada linha mostra o nome do serviço, o nome do profissional, a duração e
  o preço formatado ("R$ 30,00").
- A lista mostra só as ofertas do tenant da sessão.

---

## Business Rules

### Core Rules

- **BR-1**: Nome do serviço é obrigatório, entre 2 e 120 caracteres, e
  **único por estabelecimento** — dois "Corte de Cabelo" no mesmo tenant
  confundiriam o cliente na página pública. Diferente de `Professional.name`
  (TODO-002), que é rótulo e aceita duplicata.
- **BR-2**: Descrição do serviço é opcional, até 500 caracteres.
- **BR-3**: `tenant_id` de `Service` e `ServiceOffering` vem exclusivamente
  do `TenantContext` — nunca de campo de formulário.
- **BR-4**: `duration_minutes` é maior que zero. **Não precisa ser múltiplo
  de 10** — a grade de 10 minutos (ADR sobre estratégia de slot) governa os
  horários oferecidos ao cliente, não a duração do serviço em si; durações
  reais do piloto (30, 20, 10, 45 min) já incluem um valor que não é
  múltiplo de 10.
- **BR-5**: `buffer_minutes` é maior ou igual a zero.
- **BR-6**: `price` é maior ou igual a zero. Nunca negativo.
- **BR-7**: Uma oferta é única por `(tenant, service, professional)` — um
  profissional tem no máximo uma oferta de cada serviço.
- **BR-8**: O `professionalId` de uma oferta precisa pertencer ao mesmo
  tenant da sessão. Verificado ativamente (consulta a `organization.api`),
  não presumido pela origem do formulário — não há chave estrangeira entre
  `catalog` e `organization` que pudesse garantir isso pelo banco.

### Validation Invariants

- Nome de serviço vazio, curto demais, ou duplicado → erro de campo, nunca
  500.
- Duração zero ou negativa → erro de campo.
- Preço negativo → erro de campo.
- `professionalId` que não existe, ou existe em outro tenant → erro de
  campo — mesma mensagem para os dois casos, para não revelar se um id
  específico "existe" em algum lugar.

---

## Data Model (conceitual, para a spec técnica detalhar)

**`Service`** — entrada: nome, descrição. Saída: id, nome.

**`ServiceOffering`** — entrada: `serviceId`, `professionalId`,
`durationMinutes`, `price`, `bufferMinutes`. Saída: id, mais os dados
resolvidos para exibição (nome do serviço, nome do profissional — via
consulta a `organization.api` para o segundo).

**`organization.api.ProfessionalRef`** — projeção exportada: `id`, `name`.
Não é a entidade `Professional`; é o contrato que `organization` promete a
quem consumir de fora.

---

## User Experience

### Fluxo principal

1. Dono, autenticado, abre `/admin/servicos`.
2. Cadastra "Corte de Cabelo" (nome, sem descrição).
3. Abre `/admin/ofertas`.
4. Escolhe "Corte de Cabelo", escolhe o profissional, informa 30 minutos,
   R$ 30,00, 10 minutos de intervalo.
5. Confirma. A oferta aparece na lista, com os dois nomes resolvidos.

### Edge Cases

- **Tentar cadastrar oferta sem nenhum serviço cadastrado** — a tela de
  `/admin/ofertas` explica que é preciso cadastrar um serviço primeiro, com
  um link para `/admin/servicos`, em vez de mostrar um dropdown vazio.
- **Tentar cadastrar oferta sem nenhum profissional cadastrado** — mesma
  ideia, com link para `/admin/profissionais` (TODO-002).
- **Dois profissionais oferecendo o mesmo serviço** — permitido e esperado;
  é exatamente o motivo de `ServiceOffering` existir separado de `Service`.
- **Estabelecimento A tenta cadastrar oferta usando o id de um profissional
  do estabelecimento B** (formulário forjado) — recusado com o mesmo erro
  de "profissional inválido", nunca revela que o id pertence a alguém.

---

## Critical E2E Test Scenarios

> Sem LTP nesta instalação. Os cenários viram testes de integração com
> Testcontainers, como nas duas features anteriores.

### E2E-1: Cadastro de serviço e oferta, caminho feliz

**Criticidade**: 🔴 Critical — sem este caminho a feature não existe.

1. Dono cadastra um serviço.
2. Dono cadastra uma oferta para esse serviço com um profissional existente.

**Resultado esperado**: a oferta aparece na lista, com o nome do serviço e
do profissional resolvidos, duração e preço formatado.

### E2E-2: Oferta duplicada é recusada

**Criticidade**: 🔴 Critical — protege a unicidade (tenant, service, professional).

1. Dono cadastra uma oferta de um serviço com um profissional.
2. Tenta cadastrar outra oferta do **mesmo** serviço com o **mesmo**
   profissional.

**Resultado esperado**: erro de campo na segunda tentativa, nenhuma segunda
linha gravada.

### E2E-3: Isolamento entre tenants na escolha de profissional

**Criticidade**: 🔴 Critical — é a garantia que substitui a chave estrangeira
ausente entre `catalog` e `organization`.

1. Tenant A cadastra um serviço.
2. Tenant A tenta cadastrar uma oferta desse serviço usando o
   `professionalId` de um profissional do **tenant B** (requisição forjada,
   não pelo dropdown).

**Resultado esperado**: erro de campo, nenhuma oferta gravada. Estende o
`CrossTenantIsolationIT`.

### E2E Test Summary

| ID | Cenário | Tipo | Cobre |
|---|---|---|---|
| E2E-1 | Cadastro de serviço e oferta | Caminho feliz | US-1, US-2, US-3, US-4 |
| E2E-2 | Oferta duplicada | Erro | US-3, BR-7 |
| E2E-3 | Profissional de outro tenant | Isolamento | BR-8 |

---

## Success Metrics

### Business Metrics

- Estabelecimentos com ao menos um serviço e uma oferta cadastrados dentro
  da mesma sessão em que cadastraram o primeiro profissional — **target:
  acompanhar no piloto**, sem meta numérica ainda (n=1).

### User Metrics

- Passos entre "profissional cadastrado" e "primeira oferta com preço
  definido" — **target: duas telas, nenhuma tela intermediária além delas**.

### Technical Metrics

- Ofertas gravadas com `professionalId` de tenant diferente do da sessão —
  **target: 0**, verificado pela extensão do `CrossTenantIsolationIT`.
- Chamadas a `organization.api` por requisição de cadastro de oferta —
  **target: 1**, a mesma serve popular o dropdown e validar o envio.

---

## Non-Functional Requirements

### Performance

Sem exigência especial. Cadastro de serviço/oferta é ação administrativa
rara. `organization.api.listActive()` devolve uma lista pequena (dezenas de
profissionais, não milhares) — sem paginação nesta feature.

### Security

Nenhuma superfície nova de risco de autenticação — rotas sob `/admin/**`,
já protegidas. O risco novo é de isolamento entre tenants pela ausência de
chave estrangeira entre contextos, coberto por BR-8 e E2E-3.

---

## Assumptions

- Um serviço sem nenhuma oferta cadastrada é um estado válido (o dono
  cadastrou o conceito antes de decidir quem o executa) — não é erro, só
  incompleto.
- "Serviço" e "oferta" usam exatamente os termos do glossário; a UI usa
  "serviço" para os dois em português coloquial, mas o código nunca mistura
  `Service` com `ServiceOffering`.
