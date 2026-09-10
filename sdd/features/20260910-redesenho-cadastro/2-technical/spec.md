# redesenho-cadastro - Technical Spec

**Feature**: redesenho-cadastro
**Backlog**: —
**Status**: approved
**Data**: 2026-09-10
**Aprovado por**: Elton Marques em 2026-09-10T17:20:15Z

---

## Executive Summary

Reescrita visual de `auth/cadastro.html` em duas colunas (painel de marca
+ formulário), sem tocar `RegistrationController`, `RegisterBusinessUseCase`
ou `RegistrationRequest` — mesmo contrato, mesmos campos, mesma validação.

Duas decisões centrais: (1) o estilo "painel sempre escuro" já existe
desde a `pagina-institucional` (`.painel-escuro`, hoje só em
`landing.html`) — como esta é a segunda tela a precisar dele, ele sobe
para `fragments/layout.html` (regra do próprio projeto: fragmento/token
nasce na segunda repetição, não na primeira); (2) a prévia ao vivo do
link público **estende** `static/js/slug.js`, que já deriva o slug a
partir do nome — não recria a lógica, só acrescenta dois elementos
opcionais que o mesmo script já atualiza.

---

## Architecture Overview

```
GET /cadastro  (RegistrationController.formulario — SEM MUDANÇA)
  └─ renderiza auth/cadastro.html  [reescrito]
       ├─ fragments/layout :: head/scripts  [já existe]
       ├─ fragments/layout :: logo(tamanho) [já existe]
       ├─ fragments/layout :: temaToggle    [já existe]
       ├─ painel de marca (col-lg-6, d-none d-lg-flex)
       │    ├─ logo + texto de reforço (estático)
       │    └─ mockup de link — dois elementos NOVOS, com id:
       │         #preview-nome  (nome do estabelecimento)
       │         #preview-link  (simboraagendar.com.br/b/{slug})
       └─ formulário (col-lg-6) — MESMOS 5 campos, mesmo th:object

static/js/slug.js  [estendido, não recriado]
  nome.addEventListener('input', ...)
    └─ já deriva slug.value (se não editado manualmente)
    └─ NOVO: se #preview-nome existir, atualiza com nome.value
  slug.addEventListener('input', ...)
    └─ já marca editadoManualmente = true
  atualizarPrevia()
    └─ já atualiza #slug-previa
    └─ NOVO: se #preview-link existir, atualiza com o mesmo valor

POST /cadastro  (RegistrationController.cadastrar — SEM MUDANÇA NENHUMA)
```

---

## Design Decisions

### DD-1: `.painel-escuro` sobe para `fragments/layout.html` na segunda repetição

**Selecionado**: mover a classe `.painel-escuro` (e sua variante
`[data-bs-theme="dark"]`) do `<style>` local de `landing.html` para o
`<style>` compartilhado em `fragments/layout.html`. `landing.html`
deixa de ter a declaração local e passa a usar a classe global.

**Opções consideradas**:
- **Subir para o fragmento compartilhado (selecionado)**: o próprio
  `PATTERNS.md` documenta a regra "fragmento nasce na segunda
  repetição, não na primeira" — exatamente o caso aqui. `auth/
  cadastro.html` precisa do mesmo "painel sempre escuro,
  independente do tema" que a landing já tem.
- **Duplicar a declaração local em `auth/cadastro.html`**: rejeitado —
  é exatamente o que a regra do projeto pede para não fazer na segunda
  vez que um padrão se repete; duas cópias da mesma regra CSS
  divergem com o tempo (uma sofre um ajuste, a outra não).

**Trade-offs aceitos**: nenhum — é mover código existente de lugar, sem
mudar comportamento visual em `landing.html`.

**Rationale**: consistência com a própria convenção do projeto, e evita
que uma mudança futura de tom do painel escuro precise ser replicada
manualmente em duas telas.

### DD-2: Prévia ao vivo estende `slug.js`, não recria a derivação

**Selecionado**: adicionar, dentro dos mesmos listeners já existentes
em `slug.js`, chamadas condicionais (`if (elemento) elemento.textContent
= ...`) para dois elementos novos e opcionais (`#preview-nome`,
`#preview-link`).

**Opções consideradas**:
- **Estender `slug.js` (selecionado)**: BR-1 da spec funcional é
  explícito — a prévia do painel de marca não é uma segunda derivação,
  é uma segunda **exibição** do mesmo valor. Um script novo faria a
  mesma conta duas vezes, arriscando divergir (ex.: normalização de
  acento diferente entre os dois).
- **Script novo e independente no painel de marca**: rejeitado — pelo
  motivo acima, e porque duplicaria a guarda de "parar de sobrescrever
  após edição manual" (`editadoManualmente`), que só faz sentido existir
  uma vez.
- **Reescrever a lógica em Alpine.js/Vue para reatividade**: rejeitado
  sem nem avaliar a fundo — introduziria uma dependência de frontend
  nova para um comportamento de 6 linhas que `addEventListener` já
  resolve (mesmo raciocínio de "sem biblioteca nova" já aplicado a
  ícones em `PATTERNS.md`).

**Trade-offs aceitos**: `slug.js` passa a conhecer, de forma opcional
(guardada por `if (elemento)`), a existência de elementos de outra
parte da tela — acoplamento leve, mas já é o padrão do arquivo (ele já
atualiza `#slug-previa` do mesmo jeito). Se `auth/cadastro.html` não
tiver os elementos novos (ex.: numa tela futura hipotética que reuse
`slug.js` sem painel de marca), nada quebra — os `if` simplesmente não
disparam.

**Rationale**: um script, uma fonte de verdade, zero duplicação —
mesmo princípio de DRY já seguido pelo restante do projeto.

### DD-3: Painel de marca é `d-none d-lg-flex` — mesmo breakpoint da landing

**Selecionado**: painel de marca visível só a partir de `lg` (992px);
abaixo disso, só a logo compacta + formulário aparecem, ocupando a
tela inteira.

**Opções consideradas**:
- **Breakpoint `lg`, mesmo da landing (selecionado)**: consistência
  entre as duas telas que já usam esse padrão de "painel de marca
  aparece só em desktop"; e é o breakpoint que a própria landing
  corrigiu depois do dono relatar problema de responsividade em
  celular/tablet — reaproveitar a lição já validada.
- **Breakpoint `md` (768px), como o protótipo original sugeria**:
  rejeitado — foi exatamente o breakpoint que causou overflow na
  landing antes da correção (ver commit `9704ac1`); usar `md` aqui
  arriscaria o mesmo problema num tablet em pé.

**Trade-offs aceitos**: em tablets grandes (768-991px) o painel de
marca não aparece, só o formulário — um pouco mais conservador que o
protótipo original, mas evita reintroduzir um bug já corrigido.

**Rationale**: não reabrir um problema já resolvido; a lição da landing
se aplica diretamente aqui.

### DD-4: Nenhuma mudança em `RegistrationController`/`RegistrationRequest`

**Selecionado**: zero alteração de Java nesta feature.

**Rationale**: confirmado na investigação (meta.md) — mesmos 5 campos,
mesma validação, mesmo fluxo de erro. A feature é 100% `auth/
cadastro.html` + `static/js/slug.js`.

---

## Existing Data & Migrations

Nenhuma.

## Data Model

Nenhuma mudança. `RegistrationRequest` continua com os mesmos 5 campos.

---

## Cross-Context API Contracts

Nenhum — feature não cruza contexto.

---

## Security

- Nenhuma mudança de rota, `permitAll()`, CSRF ou validação — tudo
  herdado do que já existe em `RegistrationController` e
  `SecurityConfig`.
- A prévia ao vivo não envia nada ao servidor — não há superfície de
  segurança nova.

---

## Performance

- Zero requisição HTTP nova. A prévia é puramente client-side, sobre
  um `input` que já dispara os listeners existentes.

---

## Testing Strategy

**Unit/Web** (`RegistrationControllerTest`, `@WebMvcTest` já existente):
- Estender `formularioVazio()` (ou acrescentar um caso novo) para
  confirmar que o corpo da resposta contém os elementos estruturais
  novos (`id="preview-nome"`, `id="preview-link"`) — garante que o
  template novo renderiza sem erro e que os ids que `slug.js` vai
  procurar realmente existem no HTML.
- Os 10 testes já existentes (`cadastroValido`, `preservaOPreenchimento`,
  `slugIndisponivelViraErroDeCampo`, etc.) continuam validando o
  `RegistrationController` sem nenhuma mudança — cobertura de
  regressão automática, já que o controller não muda.

**Manual** (sem automação, mesma limitação já aceita para
`temaToggle`/dark mode na landing):
- E2E-2 (prévia atualiza ao digitar) e E2E-3 (edição manual do link é
  respeitada na prévia) são comportamento client-side puro — não
  testável via `MockMvc`. Verificação manual ao abrir `/cadastro`.

---

## Implementation Locations

| Arquivo | Mudança |
|---|---|
| `src/main/resources/templates/auth/cadastro.html` | Reescrito — layout de duas colunas, painel de marca com mockup de link, mesmos 5 campos e mesmo `th:object` |
| `src/main/resources/static/js/slug.js` | Estendido — dois `if (elemento)` novos para `#preview-nome`/`#preview-link`, mesma função `atualizarPrevia()` |
| `src/main/resources/templates/fragments/layout.html` | `.painel-escuro` promovido do `landing.html` local para o `<style>` compartilhado (DD-1) |
| `src/main/resources/templates/landing.html` | Remove a declaração local de `.painel-escuro` (agora herda do fragmento) — nenhuma mudança visual |
| `src/test/java/com/simboraagendar/organization/adapter/in/web/RegistrationControllerTest.java` | `formularioVazio()` ganha asserção sobre os novos ids |
| `sdd/PATTERNS.md` | Nota sobre `.painel-escuro` ter virado componente compartilhado (não mais local da landing) |

---

## References

- `RegistrationController.java`, `RegistrationRequest.java` — contrato
  intocado, confirmado na investigação do `meta.md`
- `static/js/slug.js` — lógica de derivação já existente, reaproveitada
- `landing.html` — origem de `.painel-escuro` e do breakpoint `lg`
  (DD-1, DD-3)
- Memória do projeto `roteamento-por-caminho-nao-subdominio` — URL da
  prévia usa `/b/{slug}`, nunca subdomínio
