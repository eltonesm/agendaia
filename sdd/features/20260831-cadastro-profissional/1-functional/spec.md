# cadastro-profissional - Functional Spec

**Feature**: cadastro-profissional
**Backlog**: TODO-002
**Status**: approved
**Data**: 2026-08-31
**Aprovado por**: Elton Marques em 2026-08-31T23:30:01Z

---

## Problem Statement

Depois da TODO-001, um estabelecimento cadastrado tem `Business` e `User`, mas
**nenhum profissional**. Sem `Professional` não há sobre o que
`WorkSchedule` (TODO-004) ou `ServiceOffering` (TODO-003) se apoiarem — nem
sobre o que a exclusion constraint de agendamento (ADR 0005) discriminaria. O
painel já promete o próximo passo ("cadastrar seus profissionais e
serviços"), mas ainda não existe rota nenhuma para cumprir essa promessa.

**Decisão de produto, tomada antes desta spec**: o cadastro de profissional
é um **passo à parte**, não automático durante o cadastro do estabelecimento
(TODO-001). Um estabelecimento recém-criado passa por uma janela — curta, mas
real — em que tem dono e sessão, mas nenhum profissional ainda.

---

## Objectives

1. O dono consegue cadastrar pelo menos um profissional depois de entrar no
   painel, sem precisar de nenhuma outra feature pronta antes.
2. `organization` ganha o segundo agregado do domínio (`Professional`), no
   mesmo regime CRUD do ADR 0002 e seguindo exatamente o padrão de camadas
   que a TODO-001 deixou como exemplo em `PATTERNS.md`.
3. `tenant_id` do profissional nunca vem do formulário — mesma garantia que
   `CrossTenantIsolationIT` já verifica para `Business`/`User`, agora
   estendida ao segundo agregado.

---

## Scope

### In Scope

- Cadastrar um profissional (nome), associado ao estabelecimento da sessão.
- Listar os profissionais já cadastrados do estabelecimento.
- Cadastrar quantos profissionais forem necessários — sem limite no MVP.
- Formulário **genérico**: não pressupõe que o primeiro profissional seja o
  dono. O dono decide livremente quem cadastrar, inclusive pulando a si
  mesmo.

### Out of Scope

- **Login próprio do profissional.** Decisão de produto: nesta feature,
  profissional é um registro que só o dono (o `User` já autenticado) gerencia
  pelo painel. Sem e-mail, sem senha, sem sessão própria. Consequência
  aceita: um funcionário não-dono não consegue ver a própria agenda sem pedir
  para o dono — aceitável até existir motivo real para mudar.
- **Editar ou desativar profissional.** Só criar e listar nesta feature.
  `Professional.deactivate()` segue o mesmo padrão de `Business`/`User` (ADR
  0011 — nada é apagado), mas a tela de gerenciar/desativar fica para quando
  houver um motivo concreto de usar (ex.: profissional que saiu do
  estabelecimento).
- **Foto, especialidade, telefone ou qualquer campo além do nome.** Decisão
  de produto: só nome no MVP. Nenhum critério de aceite hoje precisa de mais
  que isso — `ServiceOffering` (TODO-003) e `WorkSchedule` (TODO-004) só
  precisam do id do profissional.
- **Jornada de trabalho, horário, serviço oferecido.** TODO-004 e TODO-003,
  respectivamente.
- **Exibição na página pública.** TODO-006.
- **Exigir explicitamente que o dono seja um dos profissionais.** O
  glossário normativo registra a invariante de produto ("todo estabelecimento
  tem ao menos um profissional"), mas ela nasce satisfeita pela própria
  existência desta feature como porta de entrada — não precisa de validação
  ativa amarrando profissional a dono.

---

## User Stories

### US-1: Cadastrar o primeiro profissional

**Como** dono de um estabelecimento recém-cadastrado,
**Eu quero** cadastrar um profissional pelo painel,
**Para que** o estabelecimento tenha alguém sobre quem os agendamentos
futuros vão se apoiar.

**Acceptance Criteria**:
- Formulário pede só o nome do profissional.
- Nome em branco, ou fora do formato aceito, devolve erro no campo — mesma
  tela, mesmo preenchimento preservado (padrão da TODO-001).
- Ao salvar, o profissional aparece imediatamente na lista.
- `tenant_id` do profissional é sempre o da sessão autenticada — o formulário
  não tem, e não pode ter, nenhum campo que o revele ou substitua.

### US-2: Cadastrar profissionais adicionais

**Como** dono de um estabelecimento com mais de uma pessoa atendendo,
**Eu quero** cadastrar quantos profissionais eu precisar,
**Para que** cada um deles possa depois ter sua própria jornada e seus
próprios serviços.

**Acceptance Criteria**:
- Não há limite de profissionais por estabelecimento no MVP.
- Dois profissionais do mesmo estabelecimento podem ter o mesmo nome — nome
  não é identificador, só rótulo.

### US-3: Ver os profissionais já cadastrados

**Como** dono,
**Eu quero** ver a lista dos profissionais do meu estabelecimento,
**Para que** eu saiba quem já está cadastrado sem precisar adivinhar.

**Acceptance Criteria**:
- A lista mostra só os profissionais do tenant da sessão — nunca de outro
  estabelecimento.
- Estabelecimento sem nenhum profissional ainda mostra a lista vazia com uma
  chamada para cadastrar o primeiro, não um erro nem uma tela em branco sem
  explicação.
- O painel (`/admin/dashboard`) passa a linkar para esta tela em vez de só
  mencionar o texto "cadastrar seus profissionais" solto.

---

## Business Rules

### Core Rules

- **BR-1**: Nome do profissional é obrigatório, entre 2 e 120 caracteres —
  mesmo intervalo já usado para `businessName` na TODO-001, por consistência
  de UX entre os dois formulários.
- **BR-2**: `tenant_id` vem exclusivamente do `TenantContext` (sessão),
  nunca de campo de formulário, parâmetro de rota ou cabeçalho. Mesma regra
  do `CLAUDE.md`, agora aplicada ao segundo agregado do domínio.
- **BR-3**: Sem limite de profissionais por estabelecimento.
- **BR-4**: Profissional nasce **ativo**. Sem tela de desativação nesta
  feature (fora de escopo), mas o domínio já expõe `deactivate()` para não
  fechar a porta que o ADR 0011 exige.
- **BR-5**: Profissional não tem credencial própria — não é um `User`, não
  aparece na tabela `app_user`, não pode autenticar.

### Validation Invariants

- Nome vazio, só espaços, ou fora do intervalo de tamanho → erro de campo,
  nunca 500.
- Duas requisições simultâneas cadastrando profissionais do mesmo
  estabelecimento não colidem entre si — não há restrição de unicidade em
  `Professional`, então não há corrida a resolver (diferente do slug em
  `Business`).

---

## Data Model (conceitual, para a spec técnica detalhar)

**Entrada**: nome do profissional (texto).

**Saída**: profissional criado, com id, nome, e vínculo com o tenant da
sessão — mesmo padrão de `RegisteredBusiness` (TODO-001): projeção mínima
para o que a tela precisa mostrar, não a entidade JPA inteira vazando para
fora do contexto.

---

## User Experience

### Fluxo principal

1. Dono, já autenticado, abre `/admin/dashboard`.
2. Clica no link "cadastrar seus profissionais" (hoje é texto solto; esta
   feature o transforma em link).
3. Vê a lista de profissionais (vazia, na primeira vez) e um formulário para
   cadastrar um novo.
4. Preenche o nome, confirma.
5. Volta para a mesma tela, agora com o profissional na lista.

### Edge Cases

- **Nome com acentuação, hífen, apóstrofo** (ex.: "José D'Ávila") — aceito
  sem tratamento especial; não é slug, não precisa de normalização.
- **Sessão expira no meio do formulário** — mesmo comportamento já
  estabelecido pela cadeia de segurança da TODO-001: redireciona para
  `/login`, guardando o destino.
- **Estabelecimento tenta ver a lista de profissionais de outro tenant** —
  impossível pela própria consulta (sempre filtrada pelo `tenant_id` da
  sessão), e é o que a extensão do `CrossTenantIsolationIT` desta feature
  precisa provar.

---

## Critical E2E Test Scenarios

> Sem LTP nesta instalação (`ltp.enabled: false`, herdado da TODO-001). Os
> cenários abaixo viram testes de integração com Testcontainers, como já
> aconteceu na TODO-001.

### E2E-1: Cadastro do primeiro profissional (caminho feliz)

**Criticidade**: 🔴 Critical — sem este caminho a feature não existe.

1. Dono autenticado abre a tela de profissionais.
2. Digita um nome válido.
3. Confirma.

**Resultado esperado**: o profissional aparece na lista imediatamente, com o
`tenant_id` da sessão do dono.

### E2E-2: Nome inválido é recusado sem gravar

**Criticidade**: 🔴 Critical — protege contra registro inválido no banco.

1. Dono tenta cadastrar com o nome em branco.

**Resultado esperado**: erro no campo, mesma tela, nenhum `Professional` é
criado. Nunca 500.

### E2E-3: Cadastro de múltiplos profissionais

**Criticidade**: 🟡 Important — é o motivo de US-2 existir.

1. Dono cadastra um profissional.
2. Cadastra um segundo, com o mesmo nome do primeiro.

**Resultado esperado**: os dois aparecem na lista, cada um com seu próprio
id. Nome duplicado não é erro.

### E2E Test Summary

| ID | Cenário | Tipo | Cobre |
|---|---|---|---|
| E2E-1 | Cadastro do primeiro profissional | Caminho feliz | US-1, BR-1, BR-2 |
| E2E-2 | Nome inválido | Erro | US-1, BR-1 |
| E2E-3 | Múltiplos profissionais | Fluxo alternativo | US-2, BR-3 |

Um quarto cenário, não numerado por ser transversal — igual à TODO-001:
**isolamento entre tenants**, estendendo o `CrossTenantIsolationIT` já
existente para cobrir `Professional`.

---

## Success Metrics

### Business Metrics

- Estabelecimentos que cadastram ao menos um profissional dentro da mesma
  sessão em que se cadastraram — **target: acompanhar no piloto**, sem meta
  numérica ainda (n=1).

### User Metrics

- Passos entre o painel e ter o primeiro profissional cadastrado — **target:
  1 formulário, 0 telas intermediárias**, mesmo padrão de simplicidade da
  TODO-001.

### Technical Metrics

- Profissionais criados com `tenant_id` de sessão diferente do estabelecimento
  que os criou — **target: 0**, verificado pela extensão do
  `CrossTenantIsolationIT`.
- Respostas 500 para nome inválido — **target: 0**.

---

## Non-Functional Requirements

### Performance

Sem exigência especial. Cadastro de profissional é ação administrativa rara
— acontece algumas vezes por estabelecimento, nunca no caminho quente do
agendamento público.

### Security

Nenhuma superfície nova de risco além do que a TODO-001 já cobre: a rota é
`/admin/**`, já exige sessão pela cadeia de filtros existente. Nenhum dado
sensível (o nome do profissional não é PII crítico do mesmo jeito que e-mail
ou senha).

---

## Assumptions

- O dono pode cadastrar profissionais sem limite, incluindo um número que não
  faça sentido de negócio (ex.: 50 profissionais para uma barbearia de
  bairro) — sem validação de bom senso nesta feature. Se isso incomodar na
  prática, vira `DEBT` depois.
- "Profissional" e "funcionário" são o mesmo conceito nesta feature; o
  glossário usa exclusivamente `Professional`/profissional.
