# Implementation Summary — redesenho-cadastro

## Timeline

- Início: 2026-09-10
- Fim: 2026-09-10 (funcional → técnica → tasks → build → finish no
  mesmo dia)

## Tasks

- Total: 8 (`stats.done: 8` em `tasks.json`)
- Estratégia de execução: Sequencial — cadeia de dependência linear
  (CSS → template → JS → teste → docs → qualidade).
- 0 tasks adicionadas ou removidas fora do plano aprovado.

## Quality

- Testes: 628 no projeto inteiro (1 novo nesta feature, 10
  pré-existentes revalidados sem edição — ver README.md), 0 falhas,
  0 erros (`./mvnw clean verify`).
- Linter/type errors: 0.
- Layer 3 (code review, performance, security): todos `APPROVED`, zero
  achado crítico, major ou minor.

## Gotcha real: nenhum — feature transcorreu sem achado de code review

Diferente das duas features anteriores (gestao-de-clientes teve o
`MethodArgumentTypeMismatchException`; pagina-institucional teve a
fidelidade ao protótipo e o bug de dark mode do painel), esta feature
não teve nenhum achado de revisão — a investigação prévia (confirmar
que `RegistrationController` não precisava mudar, que `slug.js` já
existia) eliminou a maior parte do risco antes de escrever qualquer
código.

## Decisão de reaproveitamento: `.painel-escuro` promovido, não duplicado

O único ponto de atenção real foi resistir à tentação de copiar e
colar o bloco CSS `.painel-escuro` de `landing.html` para
`auth/cadastro.html` — a alternativa mais rápida no momento, mas que
teria criado duas cópias da mesma regra visual fadadas a divergir com
o tempo. Seguindo a própria convenção do projeto ("fragmento nasce na
segunda repetição"), a classe foi promovida para o `<style>`
compartilhado de `fragments/layout.html`, e `landing.html` passou a
herdar dali — sem nenhuma mudança visual perceptível, só uma fonte de
verdade a menos para manter sincronizada no futuro.

## Adendo: três rodadas de revisão no mesmo dia, após o arquivamento

Pouco depois de arquivada, a feature passou por três correções diretas
na `main` (fora do ciclo `/sdd.finish` — mudança puramente visual, sem
reabrir Layer 3):

1. **`bc9c7ef`** — o dono trouxe um segundo protótipo (coluna única
   centralizada) e pediu para substituir o layout de duas colunas.
   `.painel-escuro` ficou órfão em `auth/cadastro.html` (só
   `landing.html` usa hoje); `slug.js` voltou à forma simples
   (`#preview-nome`/`#preview-link` removidos, sem elemento
   correspondente na página).
2. **`f58015a`** — achado real: `th:replace` no mesmo elemento onde se
   tentava adicionar `position-absolute` descartava a classe (o host
   inteiro é substituído pelo conteúdo do fragmento). Corrigido
   envolvendo o `th:replace` num `<div>` de posicionamento — promovido
   a regra em `PATTERNS.md` ("Fragmento nasce na segunda repetição").
   Título também ganhou cor navy e peso maior, alinhado à esquerda
   (estava centralizado e com a cor genérica do corpo do texto).
3. **`08e96e6`** — fidelidade campo a campo: placeholders que faltavam
   em quatro dos cinco campos, cor do preview do link restrita ao
   trecho do domínio/slug (não a frase inteira), texto de ajuda do
   WhatsApp com a redação exata do protótipo, rótulos em navy.

Nenhuma dessas três rodadas tocou `RegistrationController` ou qualquer
regra de negócio — só `auth/cadastro.html`, `slug.js` e o teste que
cobre os ids da página. `./mvnw clean verify` (628 testes) foi
revalidado ao final de cada rodada.
