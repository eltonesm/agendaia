# pagina-institucional - Technical Spec

**Feature**: pagina-institucional
**Backlog**: —
**Status**: approved
**Data**: 2026-09-10
**Aprovado por**: Elton Marques em 2026-09-10T00:48:39Z

---

## Executive Summary

Duas mudanças independentes, entregues juntas porque nascem do mesmo
protótipo/logo trazidos pelo dono: (1) uma tela nova, puramente de
apresentação — `GET /` passa a renderizar uma landing institucional em
vez de redirecionar para `/cadastro` — e (2) uma troca de valor nos
tokens de cor já existentes em `fragments/layout.html` (indigo →
coral/navy), sem introduzir nenhum token novo nem tocar a estrutura do
sistema de design documentado em `sdd/PATTERNS.md`.

Nenhum contexto delimitado é tocado (`scheduling`, `customer`,
`catalog`, `organization`, `billing` continuam intactos). Nenhuma
migration, nenhuma tabela, nenhuma API nova. O único código Java que
muda é `WebConfig`, trocando uma linha de redirect por uma linha de
view controller — o mesmo padrão que já registra `/login` e
`/operador/login` hoje.

---

## Architecture Overview

```
GET /  (hoje: redirect 302 -> /cadastro)
  └─ depois desta feature:
     WebConfig.addViewController("/", "landing")   [alterado, platform.web]
       └─ renderiza templates/landing.html          [novo]
            ├─ usa fragments/layout :: head/scripts  [já existe]
            ├─ usa fragments/layout :: logo(tamanho)  [novo fragmento — ícone + wordmark]
            └─ botão "Criar conta" -> th:href="@{/cadastro}"  [sem mudança no destino]

GET /cadastro  (sem mudança nenhuma de comportamento, campos ou controller)

Rebrand de cor:
  fragments/layout.html :: head
    :root { --bs-primary: #4f46e5 -> #ff6b4a; ... }   [valor trocado, mesmas variáveis]
    .btn-primary { ... #4f46e5 -> #ff6b4a ... }        [idem]
  → toda tela que já lê essas variáveis (admin, agenda, cadastro, login,
    página pública) herda a cor nova automaticamente, sem edição
    individual — mesma mecânica de token único já documentada em
    "Sistema de design é token do Bootstrap" (PATTERNS.md).
```

---

## Design Decisions

### DD-1: Rota nova via `WebConfig`, sem controller novo

**Selecionado**: Trocar `registry.addRedirectViewController("/", "/cadastro")`
por `registry.addViewController("/", "landing")` em `WebConfig.java`.

**Opções consideradas**:
- **`WebConfig.addViewController` (selecionado)**: mesmo padrão já usado
  para `/login` e `/operador/login` — rotas que só renderizam um
  template, sem lógica, sem `@Controller` de uma linha. A landing não
  lê nenhum dado de sessão nem de banco (BR nenhuma depende de estado).
- **Novo `@Controller LandingController`**: rejeitado — criaria uma
  classe inteira só para devolver uma `String` de view name fixa, exatamente
  o "classe sem conteúdo" que o Javadoc de `WebConfig` já evita hoje.

**Trade-offs aceitos**: se a landing um dia precisar de dado dinâmico
(ex.: contador de estabelecimentos cadastrados), essa linha migra para
um `@Controller` de verdade — troca de poucas linhas, sem custo
arquitetural pago agora por algo que pode nunca ser necessário.

**Rationale**: seguir o padrão já estabelecido no próprio arquivo
(Javadoc de `WebConfig.java:14-15` já previa exatamente esta mudança) —
consistência com uma decisão que o próprio projeto já tinha tomado.

### DD-2: Segurança — nenhuma mudança em `SecurityConfig`

**Selecionado**: nenhuma. `"/"` já está na lista `permitAll()` de
`SecurityConfig.java:86` (junto com `/cadastro`, `/login`, `/error`) —
trocar o que essa rota renderiza não muda quem pode acessá-la.

**Rationale**: a rota já era pública (redirecionava para uma tela
pública); continua pública, agora com conteúdo próprio em vez de
delegar tudo para `/cadastro`.

### DD-3: Logo — SVG inline num fragmento novo, recriado a partir da
descrição, não do arquivo de imagem original

**Selecionado**: um fragmento `th:fragment="logo(tamanho)"` em
`fragments/layout.html`, com um `<svg>` inline (quadrado arredondado
coral com um ">>" branco) seguido do wordmark em texto real (`<span>`,
não imagem) — "simbora" em coral, "agendar" em navy, sublinhado coral
fino sob "simbora", conforme a descrição da logo compartilhada pelo
dono.

**Opções consideradas**:
- **SVG inline recriado (selecionado)**: não depende de receber o
  arquivo de imagem original (que chegou nesta conversa só como imagem
  visualizada, sem arquivo salvo no projeto) — recolore com CSS/tokens
  em vez de precisar de duas versões de arquivo (claro/escuro), sem
  requisição HTTP extra, sem pipeline de asset novo.
- **Arquivo estático em `/img/logo.svg`**: rejeitado por ora — exigiria
  o arquivo vetorial original, que não está disponível neste momento.
  `SecurityConfig.java:88` já libera `/img/**`, então essa opção fica
  pronta para o futuro se o dono fornecer o arquivo original (troca
  local, sem mudança de arquitetura).
- **PNG/raster exportado da imagem enviada no chat**: rejeitado —
  reproduzir um raster a partir de uma imagem vista na conversa
  arriscaria qualidade ruim (bordas serrilhadas ao redimensionar) e não
  há garantia de fidelidade de cor exata sem o arquivo fonte.

**Trade-offs aceitos**: o SVG recriado é uma aproximação fiel à
descrição (forma, cores, dispersão do texto), não uma cópia
pixel-a-pixel do arquivo original do dono. Se o dono tiver o arquivo
vetorial de verdade, substituir é uma troca local de fragmento, sem
qualquer outra mudança.

**Rationale**: entrega a marca visível já nesta feature sem bloquear em
um asset que não existe no repositório, e sem inventar dependência de
biblioteca de ícones nova.

### DD-4: Escopo do rebrand é só a variável `--bs-primary` e suas derivadas — nenhum token novo

**Selecionado**: trocar exclusivamente o valor das variáveis já
existentes ligadas à cor de marca em `fragments/layout.html`:
`--bs-primary`, `--bs-primary-rgb`, `--bs-link-color`,
`--bs-link-color-rgb`, `--bs-link-hover-color`,
`--bs-link-hover-color-rgb`, e os quatro pares de `.btn-primary`
(`--bs-btn-*-bg`/`--bs-btn-*-border-color`). Indigo (`#4f46e5`/`#4338ca`)
vira coral (`#ff6b4a`/`#e85a3a`) nesses mesmos lugares, claro e escuro
(a tabela de `PATTERNS.md` já documenta `--bs-primary` com o mesmo valor
nos dois temas — comportamento preservado).

**Opções consideradas**:
- **Só `--bs-primary` e derivadas (selecionado)**: BR-3 pede troca total
  da cor de marca — e é exatamente essa a variável que já concentra
  100% dos usos de marca no sistema hoje (botão primário, links, badges
  `-subtle`/`-emphasis` via `color-mix` automático do Bootstrap,
  sidebar ativa via `--bs-primary-bg-subtle`). Nenhuma tela lê cor de
  marca fora dessas variáveis.
- **Introduzir também um token `--brand-navy` de sistema**: rejeitado —
  nenhuma role hoje documentada em `PATTERNS.md` (fundo, texto, borda,
  sucesso/atenção/erro) pede navy; a paleta atual de neutros (slate) já
  cumpre esse papel. Navy fica restrito ao wordmark da logo (DD-3), não
  vira token reutilizável sem um uso concreto que peça isso — evita
  especular um papel de cor que nenhuma tela usa (mesmo princípio de
  "não desenhe sistema antes de existir tela", `PATTERNS.md`).

**Trade-offs aceitos**: nenhum — é uma troca de valor em variáveis já
existentes, sem novo código, sem nova tela para adaptar.

**Rationale**: menor mudança possível que satisfaz BR-3 por completo —
toda tela herda a cor nova através da mesma variável, sem tocar
nenhuma outra parte do sistema de design.

### DD-5: Preço (R$49,90/mês) é conteúdo estático do template, não configuração nem domínio

**Selecionado**: o texto do preço fica direto em `landing.html`, como
texto comum (não `th:text` vindo de um model attribute, não propriedade
em `application.yaml`).

**Opções consideradas**:
- **Texto estático no template (selecionado)**: BR-1 já deixa explícito
  que isso é conteúdo informativo, não dado de sistema — não existe
  hoje (nem esta feature cria) nenhum campo de plano/preço no domínio de
  billing. Trocar o valor no futuro é uma edição de uma linha de HTML.
- **Propriedade em `application.yaml` (`simboraagendar.preco-mensal`)**:
  rejeitado — over-engineering para um valor usado em exatamente um
  lugar; introduziria uma camada de indireção (property → `@Value` →
  model attribute → template) sem nenhum consumidor além desta própria
  tela.

**Rationale**: mesmo princípio de "rigor proporcional" (ADR 0002) já
aplicado no projeto — abstrair um valor de uso único é custo pago à
toa.

---

## Existing Data & Migrations

Nenhuma. Feature não cria, altera nem lê nenhuma tabela.

## Data Model

Nenhuma entidade, enum ou campo de domínio novo. Nenhum contexto
delimitado (`scheduling`, `customer`, `catalog`, `organization`,
`billing`) é tocado.

---

## Cross-Context API Contracts

Nenhum — feature não cruza contexto nenhum. É inteiramente
`platform.web` (rota) + `templates/` (apresentação).

---

## Security

- `"/"` já é `permitAll()` (`SecurityConfig.java:86`) — nenhuma mudança
  de configuração de segurança.
- Nenhum formulário na landing (o único formulário do fluxo continua
  sendo `/cadastro`, sem alteração) — nenhum CSRF token novo a gerenciar.
- Nenhum dado de sessão, `tenantId` ou `Principal` é lido pela landing —
  a mesma página é servida a qualquer visitante, autenticado ou não.
- Nenhum secret novo, nenhuma dependência nova no `pom.xml`.

---

## Performance

- `GET /` não faz nenhuma consulta a banco — é renderização pura de
  template, mesmo perfil de custo de `/login` hoje.
- SVG inline (DD-3) evita uma requisição HTTP extra que um arquivo de
  imagem separado exigiria.
- Sem paginação, sem N+1, sem coleção não limitada — não há dado
  nenhum sendo consultado.

---

## Testing Strategy

**Unit**: nenhum — não há lógica de negócio nesta feature (view
controller puro + template).

**Integration** (`SecurityRoutesIT` ou classe nova análoga,
`@SpringBootTest` + `MockMvc`):
- `GET /` retorna `200` e a view `landing` (não mais `3xxRedirection`
  para `/cadastro`) — cobre E2E-1.
- `GET /` contém, no corpo da resposta, o texto do CTA ("Criar conta")
  e o `href` apontando para `/cadastro` — cobre E2E-2.
- `GET /cadastro` continua respondendo `200` sem nenhuma mudança de
  comportamento (regressão — já coberto por teste existente, mas vale
  reconfirmar após mexer em `WebConfig`).

**Regressão de paleta** (teste novo, unitário, sem Spring context —
lê o classpath resource diretamente):
- `fragments/layout.html` NÃO contém mais a cor antiga (`4f46e5` nem
  `4338ca`, case-insensitive) em nenhuma variável `--bs-*` ou regra
  `.btn-primary` — cobre E2E-3/BR-3 de forma determinística, sem
  depender de renderização visual.
- `fragments/layout.html` contém a cor nova (`ff6b4a`) na variável
  `--bs-primary`.

> Por que um teste de texto em vez de screenshot: o projeto não tem
> infraestrutura de teste visual (LTP/E2E desabilitado, ver meta.md).
> Ler o arquivo de origem é o jeito determinístico de garantir BR-3 —
> "nenhuma tela fica pra trás" — sem inspeção manual tela por tela.

**E2E-4 (dark mode preserva marca nova)**: não é testável via
`MockMvc` (é comportamento client-side, JS + `data-bs-theme`) — fica
como verificação manual ao abrir o app (mesma limitação já aceita para
o `temaToggle` existente, que também não tem teste automatizado).

---

## Implementation Locations

| Arquivo | Mudança |
|---|---|
| `src/main/java/com/simboraagendar/platform/web/WebConfig.java` | Troca `addRedirectViewController("/", "/cadastro")` por `addViewController("/", "landing")`; Javadoc atualizado (a frase que previa esta mudança deixa de fazer sentido como está) |
| `src/main/resources/templates/landing.html` | Novo — hero, segmentos, como funciona, preço, CTA |
| `src/main/resources/templates/fragments/layout.html` | Novo fragmento `logo(tamanho)`; valores de `--bs-primary` e derivadas trocados (indigo → coral) |
| `sdd/PATTERNS.md` | Tabela "Sistema de design é token do Bootstrap": linha "Marca (primária)" atualizada de `#4F46E5` para `#FF6B4A`; nota sobre a origem da mudança (rebrand pagina-institucional, 2026-09-10) |
| `src/test/java/com/simboraagendar/platform/web/LandingRouteIT.java` | Novo — `GET /` renderiza landing, CTA aponta para `/cadastro` |
| `src/test/java/com/simboraagendar/platform/web/PaletaDeMarcaTest.java` | Novo — regressão de cor (lê `fragments/layout.html` do classpath) |

---

## References

- `WebConfig.java:14-15` — comentário que já previa esta mudança
- `SecurityConfig.java:86-89` — `"/"`, `/cadastro`, `/img/**` já
  `permitAll()`
- `BillingAccount.TRIAL_DAYS = 30` — base factual do "30 dias grátis"
- `sdd/PATTERNS.md`, "Sistema de design é token do Bootstrap" — mecânica
  de token único que este rebrand reaproveita sem alterar
- `sdd/PATTERNS.md`, "Bootstrap 5 por CDN" — página pública sobrescreve
  `--bs-*`, sem CSS ad-hoc por tela (seguido nesta feature)
