# redesenho-cadastro

## O que foi construído

Segunda tela a receber a identidade visual da marca nova, depois da
`pagina-institucional`: `/cadastro` ganhou um layout em duas colunas —
painel de marca (escuro, com um mockup do link público que atualiza ao
vivo) e o formulário, que continua com exatamente os mesmos 5 campos,
mesma validação e mesmo fluxo de sempre.

```
GET/POST /cadastro         → RegistrationController — SEM MUDANÇA
                              (mesmo RegistrationRequest, mesma
                              validação, mesmo fluxo de erro)
auth/cadastro.html          → reescrito: painel de marca (col-lg-6,
                              some abaixo de 992px) + formulário
static/js/slug.js           → estendido (não recriado): agora também
                              alimenta #preview-nome e #preview-link
                              no painel de marca
```

## O que nasceu/mudou nesta feature

| Item | Onde | Por quê |
|---|---|---|
| `.painel-escuro` | `fragments/layout.html` (promovido) | Era local de `landing.html`; segunda tela a usar (`auth/cadastro.html`) — regra do projeto: fragmento/token nasce na segunda repetição |
| `#preview-nome`, `#preview-link` | `auth/cadastro.html` (novo) + `slug.js` (estendido) | Prévia ao vivo do link público, sem duplicar a lógica de derivação já existente |
| `painelDeMarcaRenderizaComOsIdsDaPreviaAoVivo` | `RegistrationControllerTest` (novo) | Regressão determinística de que os ids que `slug.js` procura existem no HTML |
| `simboraagendar.com.br` (com `.br`) | `auth/cadastro.html`, `landing.html` | Pequena correção incidental — texto exibido usava `simboraagendar.com` (sem `.br`), inconsistente com o domínio real confirmado com o dono |

**Nenhuma mudança**: `RegistrationController.java`, `RegistrationRequest.java`, `RegisterBusinessUseCase` — confirmado na investigação antes de especificar e nos vereditos de qualidade.

## Decisões de design centrais

- **`.painel-escuro` sobe para o fragmento compartilhado (DD-1)**: em vez
  de duplicar a regra CSS numa segunda tela, ela vira componente
  compartilhado — mesma convenção já usada para logo e ícones.
- **Prévia estende `slug.js`, não recria (DD-2)**: `#preview-nome`/
  `#preview-link` são só uma segunda exibição do valor que o script já
  deriva — uma fonte de verdade, sem risco de as duas divergirem.
- **Painel de marca só a partir de `lg` — 992px (DD-3)**: o protótipo
  original sugeria `md` (768px), mas esse foi exatamente o breakpoint
  que causou overflow horizontal na landing antes de ser corrigido —
  reaproveitar `lg` evita reabrir o mesmo bug num tablet em pé.
- **Zero mudança de backend (DD-4)**: confirmado antes de especificar
  (mesmos campos, mesma validação) e depois na revisão de qualidade
  (nenhum arquivo Java de produção mudou).

## Testes

- `RegistrationControllerTest`: 10 testes pré-existentes continuam
  verdes sem edição (prova de que nenhum contrato mudou) + 1 caso novo
  confirmando a presença dos ids da prévia ao vivo.
- 628 testes no projeto inteiro, 0 falhas, 0 erros (`./mvnw clean
  verify`).
- Prévia ao vivo em si (atualização ao digitar, respeito à edição
  manual do link) não é testável via `MockMvc` — mesma limitação já
  aceita para o `temaToggle`/dark mode na landing; verificação manual.

## Quality gates (Layer 3)

Todos `APPROVED` — ver `verdicts/{code_review,performance,security}.json`.

- **Code review**: nenhuma duplicação de lógica (slug.js estendido, não
  recriado); `.painel-escuro` sem declaração residual em `landing.html`.
- **Performance**: zero requisição HTTP nova — prévia 100% client-side.
- **Security**: nenhuma mudança de rota/CSRF/validação; prévia não
  envia dado nenhum ao servidor.
