# pagina-institucional - Functional Spec

**Feature**: pagina-institucional
**Backlog**: —
**Status**: approved
**Data**: 2026-09-09
**Aprovado por**: Elton Marques em 2026-09-10T00:44:16Z

---

## Problem Statement

Hoje `/` só redireciona direto para `/cadastro` (`WebConfig.java:24`, com
um comentário já existente no código prevendo esta mudança: "quando
houver página institucional, esta linha muda"). Quem chega no sistema
pela primeira vez cai direto num formulário — nome do estabelecimento,
link, e-mail, senha — sem nenhuma explicação do que é o SimboraAgendar,
para quem serve, como funciona ou quanto custa. Para um dono de barbearia
ou salão que nunca ouviu falar do produto, isso é fricção: ele é
convidado a criar uma conta antes de entender se vale a pena.

Ao mesmo tempo, o sistema tem uma marca nova — nome "simboraagendar",
logo com paleta coral/navy — mas o produto inteiro (admin, agenda,
cadastro) ainda usa a cor indigo (`#4F46E5`) documentada como marca desde
o início do projeto. A identidade visual do produto está descolada da
identidade da marca.

O dono trouxe um protótipo próprio (feito com Claude chat, paleta
Tailwind) e pediu para começar os ajustes de UI pela entrada do sistema,
antes de mexer em operador, admin ou agenda pública — por ser o primeiro
contato de qualquer cliente novo do SimboraAgendar.

---

## Objectives

1. Quem chega em `/` pela primeira vez entende, sem precisar perguntar,
   o que é o SimboraAgendar, para quem serve, como funciona e quanto
   custa — antes de decidir criar conta.
2. O botão de ação da landing leva para o cadastro existente
   (`/cadastro`), sem duplicar nem alterar esse formulário.
3. O sistema inteiro (landing, cadastro, login, admin, agenda) passa a
   usar a paleta de cor da marca nova (coral/navy) em vez do indigo
   atual — uma identidade visual só, sem período de transição com duas
   paletas coexistindo.

---

## Scope

### In Scope

- Nova página institucional pública em `/`, substituindo o redirect
  atual para `/cadastro` (`WebConfig.java`).
- Conteúdo da landing: cabeçalho/hero com a proposta de valor, seção
  "para quem é" (segmentos: barbearia, salão de beleza, manicure e
  estética, outros serviços), seção "como funciona" (passo a passo),
  seção de preço (trial de 30 dias grátis + R$49,90/mês depois), rodapé.
- Botão de chamada para ação que leva para `/cadastro` (formulário
  existente, sem alteração de campos ou comportamento).
- Uso da logo nova (ícone + wordmark "simboraagendar") no cabeçalho da
  landing e, no mínimo, no cabeçalho/sidebar das telas autenticadas
  (mesmo lugar onde hoje não há logo nenhuma — a confirmar na spec
  técnica onde a marca aparece hoje).
- Rebrand completo da paleta de cor: troca dos tokens `--bs-primary` (e
  qualquer outro token de marca derivado) de indigo para a paleta
  coral/navy da logo nova, em `sdd/PATTERNS.md` e `fragments/layout.html`
  — aplicado de uma vez, para todo o sistema (landing, cadastro, login,
  admin, agenda pública).
- Atualização de `sdd/PATTERNS.md` com a nova paleta de marca (mesma
  seção que hoje documenta `--bs-primary: #4F46E5`).

### Out of Scope

- Qualquer mudança de conteúdo, campos ou fluxo do formulário de
  `/cadastro` — continua exatamente como está hoje, só passa a ser
  alcançado por um botão na landing em vez de redirect automático.
- Cobrança real/automática do valor de R$49,90/mês — o sistema de
  billing continua manual (Pix combinado fora do sistema, TODO-001);
  esta feature só **exibe** o preço como informação, não implementa
  cobrança.
- Qualquer ajuste nas telas de operador, admin (fora da paleta de cor) ou
  agenda pública — o dono pediu explicitamente para tratar essas telas
  em features futuras separadas, depois desta.
- Internacionalização ou suporte a mais de um idioma na landing.
- Qualquer link/CTA que não seja "Criar conta" (ex.: sem seção de
  depoimentos, sem newsletter, sem chat ao vivo) — protótipo trazido
  pelo dono não pede isso.
- Analytics/tracking de conversão da landing (fica para decisão futura,
  se o dono quiser medir).

---

## User Stories

### US-1: Entender o produto antes de criar conta

**Como** dono de barbearia, salão, manicure ou outro serviço por
horário, que nunca ouviu falar do SimboraAgendar,
**quero** entender rapidamente o que é o produto, para quem serve, como
funciona e quanto custa, ao abrir o link pela primeira vez,
**para** decidir se vale a pena criar uma conta, sem precisar perguntar
para ninguém.

**Acceptance Criteria**:
- AC-1: Ao acessar `/`, a página exibe a proposta de valor do produto
  (não redireciona mais direto para `/cadastro`).
- AC-2: A página lista os segmentos para quem o produto serve
  (barbearia, salão de beleza, manicure e estética, outros serviços por
  horário).
- AC-3: A página explica o funcionamento em passos simples (ex.: cliente
  agenda sozinho, dono confirma/gerencia).
- AC-4: A página mostra o preço: 30 dias grátis, depois R$49,90/mês
  (BR-1).

### US-2: Criar conta a partir da landing

**Como** visitante convencido pela landing,
**quero** um botão claro de "Criar conta",
**para** ir direto ao formulário de cadastro, sem precisar procurar o
link.

**Acceptance Criteria**:
- AC-1: A landing tem pelo menos um botão/link visível que leva para
  `/cadastro`.
- AC-2: O formulário de `/cadastro` não muda — mesmos campos, mesma
  validação, mesmo comportamento de hoje.

### US-3: Reconhecer a marca em qualquer tela do sistema

**Como** dono já cliente do SimboraAgendar,
**quero** ver a mesma identidade visual (cor, logo) na landing, no
cadastro, no login e no painel administrativo,
**para** ter certeza de que é o mesmo produto em todas as telas —
sem uma cor "nova" só na porta de entrada e o resto do sistema com a cor
antiga.

**Acceptance Criteria**:
- AC-1: A cor primária (`--bs-primary` e derivados) é a mesma em toda
  tela do sistema — landing, cadastro, login (cliente e operador), admin,
  agenda pública — sem exceção.
- AC-2: Não existe, depois desta feature, nenhuma tela ainda usando o
  indigo antigo (`#4F46E5`) enquanto outra já usa a paleta nova — a troca
  é atômica (BR-3).
- AC-3: `sdd/PATTERNS.md` reflete a paleta nova como a documentação
  normativa vigente — a paleta antiga não fica como opção válida em
  nenhum lugar do documento.

---

## Business Rules

### Core Rules

- BR-1: O preço exibido é fixo e vem de uma decisão de negócio tomada
  nesta conversa (2026-09-09): 30 dias grátis (mesmo valor de
  `BillingAccount.TRIAL_DAYS`), depois R$49,90/mês. Não existe hoje
  nenhum campo de plano/preço no domínio de billing — este valor é
  conteúdo informativo da landing, não um dado de sistema que alimenta
  cobrança automática.
- BR-2: O link de "Criar conta" sempre aponta para `/cadastro` — não
  existe formulário de cadastro duplicado ou simplificado na landing.
- BR-3: A troca de paleta de cor é total e simultânea — não fica nenhuma
  tela do sistema com a cor antiga depois desta feature ser concluída
  (US-3, AC-2). Não é uma migração gradual tela por tela.
- BR-4: A URL de exemplo usada na landing (se houver mockup de
  agendamento) reflete o roteamento real do produto — caminho
  (`/b/{slug}`), nunca subdomínio, porque o produto não roteia por
  subdomínio hoje.

### Validation Invariants

- Nenhum dado de negócio novo é criado por esta feature — preço e
  conteúdo são estáticos na página; nenhuma tabela, nenhuma coluna, nenhum
  enum de domínio novo.
- `/cadastro` continua a única porta de criação de conta — a landing é
  puramente informativa, sem lógica de negócio própria.

---

## Data Model (conceitual, para a spec técnica detalhar)

- Nenhuma entidade de domínio nova. O preço (R$49,90/mês) e o trial (30
  dias) são conteúdo de página — a spec técnica decide se o valor fica
  hardcoded no template ou externalizado (ex.: propriedade de
  configuração), para facilitar troca futura sem depender de código
  Java.
- Nenhuma migration de banco — feature é 100% apresentação (rota nova +
  template novo + tokens de cor).

---

## User Experience

- **Landing (`/`)**: cabeçalho com logo nova + botão "Entrar"/"Criar
  conta"; hero com proposta de valor e (opcionalmente) um mockup visual
  do agendamento — se houver URL de exemplo no mockup, usar formato
  `/b/{slug}`, nunca subdomínio (BR-4); seção "para quem é" com os 4
  segmentos; seção "como funciona" em passos; seção de preço (30 dias
  grátis + R$49,90/mês); rodapé simples.
- **Estrutura visual**: traduzida do protótipo Tailwind trazido pelo
  dono para Bootstrap 5 (ADR 0012, `sdd/PATTERNS.md`) — mesma forma
  (cards arredondados, hero, seções), sem adotar Tailwind como
  framework.
- **Paleta**: coral/navy da logo nova substitui indigo em todos os
  tokens `--bs-primary`-derivados, claro e escuro, em
  `fragments/layout.html` — mesma mecânica de token único já usada hoje
  (`sdd/PATTERNS.md`, seção "Sistema de design é token do Bootstrap").
- **Cadastro (`/cadastro`)**: nenhuma mudança de estrutura ou campos —
  só herda a cor nova em todo o sistema (via token).
- **Admin/agenda/login**: nenhuma mudança de estrutura — só herdam a cor
  nova via token (BR-3).

---

## Critical E2E Test Scenarios

### E2E-1: Acessar "/" mostra a landing, não redireciona mais

Acessar `/` sem sessão autenticada mostra a página institucional
completa (hero, segmentos, como funciona, preço) — não há mais redirect
automático para `/cadastro`.

### E2E-2: Botão "Criar conta" leva ao cadastro existente

Clicar no botão principal da landing leva para `/cadastro`, que continua
funcionando exatamente como antes (mesmos campos, mesma validação).

### E2E-3: Cor da marca é consistente em todo o sistema

Visitar a landing, `/cadastro`, `/login`, `/operador/login` e uma tela
autenticada de `/admin/**` (após login) mostra a mesma cor primária
(paleta nova) em todas — nenhuma tela ainda mostra o indigo antigo.

### E2E-4: Modo escuro preserva a marca nova

Alternar o tema (claro/escuro) em qualquer tela mantém a paleta nova
(coral/navy), sem voltar ao indigo em nenhum dos dois modos.

---

## Success Metrics

- Um visitante que nunca ouviu falar do SimboraAgendar consegue explicar,
  depois de ler a landing, o que o produto faz, para quem serve, e
  quanto custa.
- Nenhuma tela do sistema (login, cadastro, admin, agenda, landing)
  mostra a cor antiga (indigo) depois desta feature.

---

## Non-Functional Requirements

- A landing é uma página estática do ponto de vista de dados — sem
  consulta a banco, sem `tenantId`, sem sessão — carrega tão rápido
  quanto qualquer outro template Thymeleaf simples do projeto.
- Nenhuma dependência nova (biblioteca, serviço externo): a logo é
  incorporada como asset estático (SVG/imagem), sem serviço de
  hospedagem de imagem novo.

---

## Assumptions

- A logo compartilhada pelo dono (ícone + wordmark) é usada como
  fornecida, sem redesenho — a spec técnica decide o formato de arquivo
  (SVG inline vs. arquivo estático em `/static`).
- O conteúdo de texto exato de cada seção (títulos, descrições dos
  passos) é adaptado do protótipo do dono, mantendo a mesma estrutura,
  mas revisado para remover qualquer dado fictício (ex.: URL de exemplo
  com subdomínio) que não reflita o produto real (BR-4).
- Preço e trial (BR-1) são valores de negócio informados pelo dono nesta
  conversa (2026-09-09) — não são derivados de nenhum código existente
  além do trial de 30 dias, que já é real (`BillingAccount.TRIAL_DAYS`).
- Rebrand de cor (US-3) é escopo desta mesma feature, por decisão
  explícita do dono (2026-09-09): troca tudo de uma vez, não em etapas.
