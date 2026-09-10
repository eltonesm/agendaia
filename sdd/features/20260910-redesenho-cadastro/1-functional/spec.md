# redesenho-cadastro - Functional Spec

**Feature**: redesenho-cadastro
**Backlog**: —
**Status**: approved
**Data**: 2026-09-10
**Aprovado por**: Elton Marques em 2026-09-10T09:11:32Z

---

## Problem Statement

A tela de `/cadastro` (`auth/cadastro.html`) funciona bem, mas é visualmente
enxuta: uma coluna centralizada, sem nenhum reforço de marca ou contexto
sobre o que a pessoa está prestes a criar. Quem chega até aqui já passou
pela landing (`pagina-institucional`) e já entendeu a proposta de valor —
mas o cadastro em si não reforça isso, nem mostra, em tempo real, como vai
ficar o link público do estabelecimento antes de a pessoa confirmar.

O dono trouxe um segundo protótipo (mesma identidade visual coral/navy da
landing) com um layout em duas colunas: um painel de marca à esquerda
(escuro, com um mockup do link público que atualiza ao vivo com o nome
digitado) e o formulário à direita. O pedido é trazer essa experiência
para a tela real, sem duplicar a lógica de derivação de slug que já existe
em `static/js/slug.js`.

---

## Objectives

1. Quem chega em `/cadastro` (vindo da landing ou direto) vê a mesma
   identidade visual reforçada — não um formulário genérico.
2. Ao digitar o nome do estabelecimento, a pessoa vê, em tempo real, como
   vai ficar seu link público e o texto que os clientes vão ver — sem
   surpresa depois de criar a conta.
3. Nenhuma mudança de campo, validação ou fluxo de cadastro — a tela nova
   é a mesma funcionalidade, só com apresentação diferente.

---

## Scope

### In Scope

- Layout em duas colunas: painel de marca (escuro, oculto em telas
  pequenas) + formulário, traduzido do protótipo Tailwind para
  Bootstrap 5.
- Painel de marca: logo, título/subtítulo de reforço, mockup do link
  público que atualiza ao vivo (nome do estabelecimento e slug),
  menção ao trial de 30 dias grátis (mesma informação já usada na
  landing).
- Reaproveitamento de `static/js/slug.js` para a derivação do slug —
  os elementos novos do painel de marca (nome, prévia de link) escutam
  o mesmo campo/evento que o script já atualiza, sem duplicar a função
  de derivação.
- Botão de alternar tema (`temaToggle`) reposicionado conforme o
  protótipo, mantendo o mesmo fragmento/script já existente.
- Todos os campos atuais mantidos, na mesma ordem e com o mesmo
  comportamento de erro (erro de campo preserva o preenchimento).

### Out of Scope

- Qualquer mudança em `RegistrationController`, `RegisterBusinessUseCase`
  ou `RegistrationRequest` — campos, validação e fluxo de negócio
  continuam idênticos.
- Qualquer mudança na página de login, operador ou admin (fora do que
  já foi feito nesta sessão para o ícone de senha).
- Qualquer campo novo no formulário (o protótipo não pede nenhum).
- Verificação de disponibilidade do slug em tempo real (via requisição
  ao servidor) — o protótipo só simula a prévia visualmente; a
  disponibilidade real continua sendo checada no submit, como hoje.

---

## User Stories

### US-1: Reforço de marca ao criar conta

**Como** visitante que decidiu criar uma conta,
**quero** ver a mesma identidade visual da landing (logo, cores, tom)
na tela de cadastro,
**para** ter certeza de que ainda estou no SimboraAgendar, sem a
sensação de ter saído para um formulário genérico.

**Acceptance Criteria**:
- AC-1: A tela usa o mesmo componente de logo (`fragments/layout ::
  logo`) e a mesma paleta coral/navy já vigente no sistema.
- AC-2: Em telas grandes (desktop), um painel escuro de marca aparece
  ao lado do formulário; em telas pequenas, ele não aparece (só o
  formulário, para não competir por espaço).

### US-2: Ver o link público antes de criar a conta

**Como** dono de estabelecimento preenchendo o cadastro,
**quero** ver, enquanto digito o nome do meu negócio, como vai ficar
meu link público,
**para** ajustar o nome ou o link antes de confirmar, sem precisar
criar a conta primeiro para descobrir.

**Acceptance Criteria**:
- AC-1: Ao digitar no campo de nome do estabelecimento, o painel de
  marca mostra o nome digitado e o link público correspondente
  (`simboraagendar.com.br/b/{slug}`), atualizados em tempo real.
- AC-2: Se a pessoa editar o campo de link manualmente, a prévia do
  painel de marca reflete o valor editado, não mais a derivação
  automática — mesmo comportamento de "parar de sobrescrever" que
  `slug.js` já implementa para o campo de link em si (BR-1).
- AC-3: A prévia é só visual — não faz nenhuma requisição ao servidor
  nem valida disponibilidade; quem valida de verdade é o submit, como
  hoje.

### US-3: Nada muda no fluxo de cadastro em si

**Como** dono de estabelecimento preenchendo o formulário,
**quero** que o cadastro funcione exatamente como antes,
**para** não ser surpreendido por um comportamento novo só porque a
aparência mudou.

**Acceptance Criteria**:
- AC-1: Os mesmos 5 campos existem, na mesma ordem relativa (nome,
  link, e-mail, WhatsApp opcional, senha).
- AC-2: Um erro de validação (formato) ou de negócio (link/e-mail já
  em uso) continua devolvendo a mesma tela com o erro no campo certo,
  preservando o que já foi digitado.
- AC-3: O botão de mostrar/ocultar senha usa o mesmo componente já
  corrigido nesta sessão (ícone SVG do protótipo, `.js-alternar-senha`)
  — sem reimplementação.

---

## Business Rules

### Core Rules

- BR-1: A prévia do link no painel de marca segue a mesma regra de
  "parar de sobrescrever após edição manual" que já existe em
  `slug.js` para o campo de link — não é uma segunda derivação
  independente, é uma segunda **exibição** do mesmo valor já derivado/
  editado.
- BR-2: Nenhuma validação nova acontece no cliente — toda validação de
  formato e disponibilidade continua no servidor (`RegistrationRequest`
  + `RegisterBusinessUseCase`), como hoje.
- BR-3: A URL exibida na prévia usa o roteamento real
  (`simboraagendar.com.br/b/{slug}`) — nunca subdomínio (decisão já
  registrada, ver memória do projeto).

### Validation Invariants

- Nenhum campo novo é criado; nenhuma migration; nenhuma mudança de
  contrato entre a view e o controller (o `model.addAttribute("form",
  ...)` continua igual).

---

## Data Model (conceitual, para a spec técnica detalhar)

- Nenhuma entidade nova. Toda a "prévia ao vivo" é renderização
  client-side sobre dados que o usuário já digitou no próprio
  navegador — nada é persistido nem consultado até o submit.

---

## User Experience

- **Painel de marca** (visível só em telas grandes): logo no topo;
  título/subtítulo de reforço ("Sua agenda pronta pra receber o
  primeiro cliente hoje" ou equivalente); mockup do link público
  (nome do estabelecimento + URL, atualizados ao vivo); menção ao
  trial de 30 dias grátis.
- **Formulário** (sempre visível): logo compacta no topo (só quando o
  painel de marca está oculto, em telas pequenas); título "Crie a
  conta do seu estabelecimento"; os 5 campos já existentes, na mesma
  ordem; botão de alternar tema; botão de submit; link para "Já tem
  conta? Entrar".
- **Responsivo**: painel de marca desaparece abaixo do breakpoint
  `lg` (mesma lição aprendida na landing — testar de verdade em
  celular/tablet antes de considerar concluído, não só redimensionar
  a janela do desktop).

---

## Critical E2E Test Scenarios

### E2E-1: Cadastro continua funcionando de ponta a ponta

Preencher os 5 campos corretamente e submeter cria a conta, autentica
a sessão e redireciona para o painel — mesmo comportamento de hoje,
agora na tela nova.

### E2E-2: Prévia ao vivo reflete o nome digitado

Digitar um nome de estabelecimento atualiza, sem reload, o texto e o
link mostrados no painel de marca.

### E2E-3: Edição manual do link é respeitada na prévia

Depois de editar o campo de link à mão, continuar digitando no nome do
estabelecimento não sobrescreve mais o link editado nem a prévia
correspondente.

### E2E-4: Erro de validação preserva o preenchimento

Submeter com um e-mail já cadastrado devolve a mesma tela com o erro
no campo de e-mail, mantendo os outros campos preenchidos.

---

## Success Metrics

- A tela de cadastro passa a identidade visual do SimboraAgendar tão
  claramente quanto a landing.
- Ninguém precisa completar o cadastro para descobrir como o link
  público vai ficar.

---

## Non-Functional Requirements

- Nenhuma requisição HTTP nova — a prévia é 100% client-side.
- Reaproveita `slug.js` existente; não duplica a função de derivação
  de slug em dois lugares do código.

---

## Assumptions

- Texto exato do painel de marca (título, subtítulo, menção ao trial)
  é adaptado do protótipo, revisado para bater com fatos reais (mesmo
  cuidado da landing: preço, trial, URL).
- Layout de duas colunas é decisão já validada pelo protótipo — não é
  reaberta como pergunta na spec técnica.
