# ADR 0012 — Bootstrap 5 sem build, com camada de tema na página pública

- **Status:** Aceito
- **Data:** 2026-08-30

## Contexto

O [ADR 0007](0007-thymeleaf-server-side-sem-api-rest.md) decidiu renderização no
servidor sem build de frontend. Falta decidir o que estiliza as telas.

Há duas superfícies com exigências opostas:

| | `/admin/**` | `/b/{slug}` |
|---|---|---|
| quem usa | o dono, todo dia | o cliente, uma vez a cada semanas |
| dispositivo | celular e desktop | celular, quase sempre |
| natureza | formulários e tabelas densas | um fluxo de quatro passos |
| tolerância a feiura | alta — é ferramenta de trabalho | baixa — representa o estabelecimento |

A página pública é o que o barbeiro compartilha com os clientes dele. Se parecer
amadora, ele não compartilha — e o produto não é usado. Foi decisão explícita do
time que ela precisa impressionar já no piloto.

## Decisão

Nós vamos usar **Bootstrap 5 via CDN, com versão fixada e sem build**, nas duas
superfícies — e uma **camada de tema própria na página pública**.

O tema não é troca de cor. Ele sobrescreve as custom properties `--bs-*` de
tipografia, cor, raio, sombra e ritmo vertical, e evita os componentes que
denunciam Bootstrap à primeira vista (navbar e card padrão).

O investimento visual fica concentrado em `TODO-006` e `TODO-007`, as duas
únicas features que tocam a página pública. O admin usa Bootstrap como vem.

## Consequências

Grid, formulário, tabela e responsividade saem de graça no admin, que é CRUD e
não merece tempo de design.

Nenhum passo de build, nenhum `node_modules`, nenhum pipeline de frontend — o
deploy continua sendo um jar.

**O que fica pior:**

- **A camada de tema é trabalho real, e recorrente.** Cada tela pública nova
  precisa respeitar o tema em vez de usar o componente pronto. É o custo direto
  de exigir que a página impressione.
- **Bootstrap inteiro pelo CDN** carrega CSS que não usamos. No 3G do celular do
  cliente isso pesa. Se a medição mostrar problema, a saída é gerar um subconjunto
  — o que reintroduz um passo de build e contraria em parte o ADR 0007.
- **Dependência de CDN externo** na página que representa o estabelecimento. CDN
  fora do ar deixa a página sem estilo. Mitigação: `integrity` e `crossorigin`
  no `<link>`, e servir localmente se a disponibilidade incomodar.

## Gatilho de reavaliação

Se a página pública ficar lenta em conexão móvel medida de verdade, ou se o tema
crescer a ponto de estar lutando contra o Bootstrap em vez de usá-lo, trocar por
CSS próprio só na superfície pública — mantendo Bootstrap no admin. A separação
entre as duas superfícies já existe e torna essa troca contida.

## Amendment — 2026-09-01: exceção pontual de cor no admin

"O admin usa Bootstrap como vem" ganhou uma exceção pequena: paleta indigo
(`--bs-primary`) e raio de borda um pouco maior (`--bs-border-radius`), via
`fragments/layout.html`, mesma técnica de sobrescrever `--bs-*` descrita acima
— só que aplicada em escala mínima (cor e raio, não tipografia/sombra/layout
inteiro) e só nas classes que as telas atuais realmente usam.

Motivação: o dono trouxe um protótipo de referência (React, feito no Gemini,
fora do projeto) com essa paleta, e pediu para aplicar o que já existe hoje.
Não abre precedente para recriar o protótipo inteiro — a decisão original
continua valendo para o resto: **sem** menu lateral fixo, **sem** cards de
KPI (dependem de `scheduling`, que não existe), **sem** rebuild de SASS.
Ver `sdd/backlog.md`: IDEA-009 (dashboard/relatórios) e IDEA-011 (navegação
em sidebar) registram o que ficou de fora, para quando fizer sentido puxar.
