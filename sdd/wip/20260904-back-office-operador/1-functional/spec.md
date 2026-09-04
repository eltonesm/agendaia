# back-office-operador - Functional Spec

**Feature**: back-office-operador
**Backlog**: TODO-009
**Status**: approved
**Data**: 2026-09-04
**Aprovado por**: Elton Marques em 2026-09-04T02:19:20Z

---

## Problem Statement

Depois da TODO-001 a TODO-005, o AgendaIA sabe cadastrar estabelecimentos e
calcular disponibilidade — mas ninguém, além de uma consulta direta ao
banco, sabe **quantos estabelecimentos existem, desde quando, e se estão
em dia**. Todo o produto até aqui foi construído do ponto de vista de quem
usa o AgendaIA (dono de estabelecimento, cliente). Esta feature constrói a
primeira tela do ponto de vista de quem **opera** o AgendaIA.

Sem isso, o dono da plataforma não tem como oferecer um período de teste
gratuito de forma confiável (não há onde ele vença automaticamente), nem
como saber quem precisa pagar, sem abrir o banco de dados a cada consulta —
o que já deixa de escalar no segundo estabelecimento.

---

## Objectives

1. O operador da plataforma acessa um painel próprio, fora do modelo de
   estabelecimento/tenant, e vê todos os estabelecimentos cadastrados com
   seu status de acesso.
2. Todo estabelecimento novo recebe automaticamente 30 dias corridos de
   teste gratuito a partir do cadastro.
3. Ao vencer o teste, o estabelecimento entra em 5 dias corridos de
   carência: o painel administrativo do dono passa a mostrar um aviso com
   instrução de pagamento (Pix), sem cortar o acesso ainda.
4. Se ninguém regularizar até o fim da carência, o acesso ao painel
   administrativo do estabelecimento é bloqueado automaticamente.
5. O operador pode, a qualquer momento, marcar um estabelecimento como
   pago até uma nova data, liberando (ou estendendo) o acesso.
6. O dono do estabelecimento tem um canal direto (WhatsApp) para falar com
   o operador, disponível mesmo quando o acesso está bloqueado.

---

## Scope

### In Scope

- Login próprio do operador, numa rota separada do login de estabelecimento.
- Painel do operador: lista de todos os estabelecimentos (nome, slug, data
  de cadastro, "modelo" e status de acesso calculado).
- Trial automático de 30 dias corridos a partir do cadastro do
  estabelecimento (retroativo ao(s) estabelecimento(s) já existente(s),
  calculado a partir da data de cadastro já gravada).
- Carência de 5 dias corridos após o fim do período válido de acesso: o
  painel administrativo do dono mostra aviso fixo com QR code/chave Pix
  (configuração única do operador, a mesma para todos os estabelecimentos),
  sem bloquear nada ainda.
- Bloqueio automático do painel administrativo do estabelecimento ao fim
  da carência, sem pagamento marcado — redireciona para uma tela de "conta
  suspensa" com link de WhatsApp para o operador.
- O operador marca um estabelecimento como pago até uma nova data — a
  mesma ação serve tanto para confirmar um pagamento recebido quanto para
  estender o prazo manualmente por outro motivo qualquer.
- Botão de WhatsApp no painel administrativo do dono, sempre visível
  (inclusive na tela de conta suspensa), apontando para o número do
  operador.

### Out of Scope

- **Gateway de pagamento** (Stripe, Mercado Pago assinatura ou qualquer
  cobrança automática). Pix é recebido fora do sistema; o operador só
  registra manualmente.
- **Notificação proativa ao operador** sobre vencimento (e-mail, WhatsApp).
  O operador confere o status no próprio painel, quando acessar.
- **Múltiplos planos ou preços.** Existe um "modelo" só, com nome fixo,
  sem tela de configuração.
- **Múltiplos operadores ou papéis distintos.** Uma conta só.
- **Cadastro público de operador.** A conta nasce por configuração, nunca
  por formulário.
- **Avaliação ou formulário estruturado de feedback do dono sobre a
  plataforma.** O canal é o WhatsApp já incluído nesta feature.

---

## User Stories

### US-1: Operador acessa o painel

**Como** operador da plataforma,
**Eu quero** logar numa área própria, separada do login de estabelecimento,
**Para que** eu veja todos os estabelecimentos sem depender de nenhuma
conta de dono.

**Acceptance Criteria**:
- Existe uma rota de login exclusiva do operador, sem nenhuma ligação com
  o login de `/admin`.
- A conta do operador não é criada por nenhum formulário público.
- Login de dono não dá acesso ao painel do operador, e vice-versa.

### US-2: Operador vê todos os estabelecimentos e seus status

**Como** operador,
**Eu quero** ver a lista de estabelecimentos cadastrados, com o status de
acesso de cada um,
**Para que** eu saiba quem está em teste, quem está em carência, quem está
bloqueado e quem está pago.

**Acceptance Criteria**:
- A lista mostra nome, slug, data de cadastro, "modelo" e status calculado
  de cada estabelecimento.
- Status possíveis: em teste, em carência, bloqueado, pago.
- A lista inclui todos os tenants, sem nenhum filtro — o operador não tem
  tenant.

### US-3: Dono vê aviso de carência com instrução de pagamento

**Como** dono de estabelecimento em carência,
**Eu quero** ver, assim que entro no painel administrativo, um aviso claro
com o QR code/chave Pix,
**Para que** eu saiba que preciso regularizar e como fazer isso, sem
precisar perguntar a ninguém.

**Acceptance Criteria**:
- O aviso aparece em toda página do painel administrativo enquanto o
  estabelecimento estiver em carência.
- O aviso mostra até quando a carência vale.
- O acesso ao painel continua funcionando normalmente durante a carência.

### US-4: Acesso bloqueado automaticamente após a carência

**Como** operador,
**Eu quero** que o acesso ao painel administrativo de um estabelecimento
seja bloqueado sozinho se ninguém regularizar até o fim da carência,
**Para que** eu não precise lembrar manualmente de cortar acesso de quem
não pagou.

**Acceptance Criteria**:
- Depois do fim da carência sem pagamento marcado, qualquer acesso ao
  painel administrativo do estabelecimento mostra uma tela de "conta
  suspensa", em vez do conteúdo normal.
- A tela de conta suspensa mostra o link de WhatsApp para o operador.
- O bloqueio é reavaliado a cada acesso — não depende de nenhum processo
  rodando em segundo plano.

### US-5: Operador marca um estabelecimento como pago

**Como** operador,
**Eu quero** marcar que um estabelecimento está pago até uma data,
**Para que** o acesso dele volte a funcionar (ou continue funcionando),
sem precisar de integração com nenhum gateway.

**Acceptance Criteria**:
- O operador informa uma nova data de validade de acesso para o
  estabelecimento.
- A partir dessa ação, o status do estabelecimento reflete a nova data
  imediatamente — se estava bloqueado, volta a funcionar.
- A mesma ação serve para estender o prazo por qualquer motivo, não só
  para confirmar pagamento.

### US-6: Dono fala com o operador pelo WhatsApp

**Como** dono de estabelecimento,
**Eu quero** um jeito direto de falar com quem opera o AgendaIA,
**Para que** eu tire dúvidas ou dê sugestões sem precisar procurar um
contato em outro lugar.

**Acceptance Criteria**:
- Existe um link de WhatsApp visível no painel administrativo, em toda
  página — inclusive na tela de conta suspensa.
- O link abre uma conversa já endereçada ao número do operador.

---

## Business Rules

### Core Rules

- **BR-1**: Todo estabelecimento novo recebe uma data de validade de
  acesso (`accessValidUntil`) igual à data de cadastro mais 30 dias
  corridos, calculada no momento do cadastro.
- **BR-2**: O status de acesso de um estabelecimento é sempre **calculado**
  a partir de `accessValidUntil` e da data de hoje — nunca um campo solto
  gravado à parte:
  - hoje ≤ `accessValidUntil` → **em teste** (ou **pago**, se
    `accessValidUntil` foi estendido por uma marcação de pagamento — o
    cálculo é o mesmo, só a origem da data muda).
  - `accessValidUntil` < hoje ≤ `accessValidUntil` + 5 dias → **em
    carência**.
  - hoje > `accessValidUntil` + 5 dias → **bloqueado**.
- **BR-3**: Marcar um estabelecimento como pago substitui
  `accessValidUntil` pela nova data informada pelo operador — a mesma ação
  vale para confirmar pagamento ou para estender prazo por outro motivo.
- **BR-4**: Durante a carência, o painel administrativo do estabelecimento
  mostra o aviso com Pix em toda página; durante o bloqueio, qualquer rota
  do painel administrativo mostra a tela de conta suspensa em vez do
  conteúdo normal.
- **BR-5**: O botão de WhatsApp aparece em toda página do painel
  administrativo, independente do status de acesso.
- **BR-6**: O login do operador é isolado do login de dono de
  estabelecimento — nenhuma sessão vale para os dois.
- **BR-7**: A conta do operador é única e nasce por configuração; não
  existe formulário público que crie uma conta de operador.
- **BR-8**: Estabelecimentos cadastrados antes desta feature recebem
  `accessValidUntil` calculado retroativamente a partir da data de
  cadastro já existente, sem tratamento especial.

### Validation Invariants

- Nova data informada ao marcar pagamento precisa ser uma data futura
  (posterior a hoje) — data no passado não faz sentido para "liberar
  acesso".
- Estabelecimento sem nenhuma marcação de pagamento sempre calcula o
  status a partir do trial original (BR-1/BR-2).

---

## Data Model (conceitual, para a spec técnica detalhar)

**Operador** — credencial única (usuário/senha), sem vínculo com nenhum
`Business`. Nasce por configuração, não por cadastro.

**Estabelecimento (visão do operador)** — cada `Business` ganha uma data
de validade de acesso (`accessValidUntil`), da qual todo o resto (teste,
carência, bloqueado, pago) é calculado, e um "modelo" (valor fixo nesta
feature). Onde exatamente esse dado mora (campo em `organization.Business`
ou agregado em contexto próprio) é decisão da spec técnica — aqui importa
só que ele existe e pertence, conceitualmente, a "quem opera a
plataforma", não ao dono do estabelecimento.

---

## User Experience

### Fluxo principal do operador

1. Operador acessa a rota de login própria e entra com a credencial única.
2. Vê a lista de todos os estabelecimentos, com status calculado de cada
   um.
3. Clica num estabelecimento vencido ou em carência e marca uma nova data
   de validade de acesso (pagamento recebido).
4. O estabelecimento volta a aparecer como "pago" na lista.

### Fluxo do dono, afetado por esta feature

1. Durante o teste ou com pagamento em dia, o painel administrativo
   funciona normalmente, sem nenhum aviso.
2. Ao entrar em carência, toda página do painel mostra um aviso fixo com
   Pix.
3. Se a carência vencer sem pagamento, qualquer tentativa de acessar o
   painel mostra a tela de conta suspensa, com o link de WhatsApp.
4. O botão de WhatsApp está sempre visível, em qualquer um dos três
   estados.

### Edge Cases

- **Estabelecimento cadastrado antes desta feature** (o piloto) — recebe
  `accessValidUntil` calculado a partir do `created_at` já gravado, sem
  tratamento especial (BR-8). Se isso resultar num estado de carência ou
  bloqueio já ao ligar a feature, é um resultado válido, não um bug — o
  operador resolve marcando como pago.
- **Sessão do dono já aberta no momento em que o bloqueio acontece** — o
  bloqueio não precisa invalidar a sessão ativamente; a próxima requisição
  ao painel administrativo já é avaliada e bloqueada (BR-4/US-4).
- **Operador tenta acessar `/admin/**` ou dono tenta acessar o painel do
  operador** — os dois logins são isolados (BR-6); cada um só enxerga a
  área própria.

---

## Critical E2E Test Scenarios

> Sem LTP nesta instalação. Os cenários viram testes de integração com
> Postgres real, como nas features anteriores.

### E2E-1: Cadastro gera trial e aparece no painel do operador

**Criticidade**: 🔴 Critical — sem isto a feature não existe.

1. Um estabelecimento novo é cadastrado.
2. Operador loga e consulta a lista.

**Resultado esperado**: o estabelecimento aparece com status "em teste",
`accessValidUntil` igual à data de cadastro mais 30 dias.

### E2E-2: Carência mostra aviso, sem bloquear

**Criticidade**: 🔴 Critical — protege BR-2/BR-4.

1. Um estabelecimento tem `accessValidUntil` no passado, dentro da janela
   de 5 dias de carência.
2. Dono acessa o painel administrativo.

**Resultado esperado**: painel funciona normalmente, com o aviso de Pix
visível; operador vê status "em carência".

### E2E-3: Bloqueio automático após a carência

**Criticidade**: 🔴 Critical — protege BR-2/BR-4, é o coração da feature.

1. Um estabelecimento tem `accessValidUntil` mais de 5 dias no passado,
   sem pagamento marcado.
2. Dono tenta acessar qualquer rota do painel administrativo.

**Resultado esperado**: tela de conta suspensa, com link de WhatsApp;
operador vê status "bloqueado".

### E2E-4: Marcar como pago libera o acesso

**Criticidade**: 🔴 Critical — protege US-5/BR-3.

1. Estabelecimento bloqueado (cenário do E2E-3).
2. Operador marca uma nova data de validade de acesso, no futuro.
3. Dono acessa o painel administrativo novamente.

**Resultado esperado**: acesso liberado, sem tela de suspensão; operador
vê o novo status refletido na lista.

### E2E-5: Isolamento entre login de operador e login de dono

**Criticidade**: 🔴 Critical — mesma classe de garantia de todas as
features anteriores, agora entre dois tipos de sessão, não entre tenants.

1. Dono loga normalmente em `/admin`.
2. Tenta acessar a rota do painel do operador.
3. Operador loga na rota própria.
4. Tenta acessar `/admin` de um estabelecimento qualquer.

**Resultado esperado**: nenhum dos dois consegue usar a sessão do outro.

### E2E Test Summary

| ID | Cenário | Tipo | Cobre |
|---|---|---|---|
| E2E-1 | Trial automático no cadastro | Caminho feliz | US-1, US-2, BR-1 |
| E2E-2 | Carência sem bloqueio | Caminho feliz | US-3, BR-2, BR-4 |
| E2E-3 | Bloqueio automático | Erro/restrição | US-4, BR-2, BR-4 |
| E2E-4 | Marcar como pago | Caminho feliz | US-5, BR-3 |
| E2E-5 | Isolamento de login | Isolamento | US-1, BR-6 |

---

## Success Metrics

### Business Metrics

- Estabelecimentos com status calculado corretamente, sem falso positivo
  de bloqueio — **target: 0 bloqueios incorretos**, acompanhado no piloto.

### User Metrics

- Tempo entre o vencimento da carência e o operador tomar conhecimento —
  sem meta numérica ainda (depende de quando o operador acessa o painel,
  decisão já tomada de não notificar proativamente).

### Technical Metrics

- Vazamento entre sessão de operador e sessão de estabelecimento —
  **target: 0**, verificado por E2E-5.
- Estabelecimento acessando o painel administrativo além do prazo de
  carência sem ser bloqueado — **target: 0**, verificado por E2E-3.

---

## Non-Functional Requirements

### Performance

Sem exigência especial — poucos estabelecimentos esperados no piloto. O
cálculo de status é uma comparação de datas, não uma consulta pesada.

### Security

A sessão do operador enxerga dado de **todos** os tenants — o oposto da
garantia de isolamento das features anteriores. Por isso a autenticação do
operador precisa ser tão ou mais rigorosa que a de um dono de
estabelecimento: login isolado (BR-6), conta única sem cadastro público
(BR-7), nenhuma superfície de escalonamento entre os dois tipos de sessão.

---

## Assumptions

- Um único operador (você) nesta feature — sem múltiplos usuários
  operadores nem papéis diferentes.
- "Modelo" é um valor fixo ("Padrão") — não há tela de configuração de
  planos nesta feature.
- A chave Pix/QR code mostrados durante a carência são uma configuração
  única do operador, a mesma para todos os estabelecimentos — não há um
  Pix por tenant.
- Carência de 5 dias **corridos**, não úteis — decisão já tomada antes
  desta spec, para não depender de calendário de feriados.
- "Marcar como pago" e "estender prazo" são a mesma ação técnica: definir
  uma nova data de validade de acesso. Não há dois botões, nem dois
  conceitos diferentes no código.
