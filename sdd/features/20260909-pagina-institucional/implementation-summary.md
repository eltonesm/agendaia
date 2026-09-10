# Implementation Summary — pagina-institucional

## Timeline

- Início: 2026-09-09
- Fim: 2026-09-10 (funcional → técnica → tasks → build → finish no
  mesmo ciclo, atravessando a virada do dia)

## Tasks

- Total: 8 (`stats.done: 8` em `tasks.json`)
- Estratégia de execução: Sequencial — cadeia de dependência linear
  (logo/paleta → template → rota → testes → qualidade), sem serviço de
  infra novo, feature pequena demais para compensar paralelismo.
- 0 tasks adicionadas ou removidas fora do plano aprovado.

## Commits (4, `main..feature/pagina-institucional`)

```
8375d73 docs(sdd): inicia, especifica e planeja a pagina-institucional
fd77ce2 feat(platform): landing institucional e rebrand de marca para coral/navy
813b1f1 test(platform): cobertura da rota da landing e regressao de paleta
395002d docs(sdd): fecha camada 3 da pagina-institucional - code review, performance e security
```

> Mesmo padrão das features anteriores: sem commit incremental por task
> individual — as 8 tasks foram implementadas numa sessão única e
> commitadas agrupadas por categoria (planejamento SDD, implementação,
> testes, quality gates).

## Quality

- Testes: 627 no projeto inteiro (5 novos nesta feature — 3 em
  `LandingRouteIT`, 2 em `PaletaDeMarcaTest`), 0 falhas, 0 erros
  (`./mvnw clean verify`, contagem por soma dos XML de
  surefire+failsafe, não pela linha "Tests run" do `.txt` — ver
  `PATTERNS.md`).
- Linter/type errors: 0.
- Layer 3 (code review, performance, security): todos `APPROVED`, zero
  achado crítico, major ou minor (ver `verdicts/`).

## Gotcha real: logo compartilhada só existia como imagem vista na conversa

O dono compartilhou a logo nova ("simboraagendar", ícone coral com
chevron duplo, wordmark navy/slate) como uma imagem dentro do chat —
sem arquivo vetorial anexado ao projeto. Em vez de bloquear a feature
esperando o arquivo original, ou tentar reproduzir um raster de baixa
fidelidade a partir da imagem vista, a decisão (DD-3) foi recriar o
ícone como SVG inline a partir da descrição (forma geométrica simples:
quadrado arredondado + duas setas). Isso foi documentado explicitamente
como uma aproximação, não uma cópia pixel-a-pixel — se o dono tiver o
arquivo de verdade depois, a troca é local, sem impacto de arquitetura.
Lição geral: quando um asset visual chega só como imagem de conversa
(não como arquivo no repositório), recriar uma versão simplificada e
documentar o trade-off é melhor do que bloquear a entrega ou arriscar
qualidade ruim reproduzindo um raster.

## Gotcha real: cor de marca só tem UM ponto de mudança no sistema inteiro

Confirmação prática do "Sistema de design é token do Bootstrap"
(`PATTERNS.md`, já promovido na TODO-108/sistema-de-design-admin): trocar
`--bs-primary` e as ~10 variáveis derivadas num único arquivo
(`fragments/layout.html`) foi suficiente para atualizar a cor de marca
em **todas** as telas do sistema — landing, cadastro, login (cliente e
operador), admin, agenda pública — sem editar nenhuma delas
individualmente. O teste de regressão (`PaletaDeMarcaTest`) prova isso
de forma determinística, lendo o arquivo-fonte em vez de inspecionar
tela por tela.
