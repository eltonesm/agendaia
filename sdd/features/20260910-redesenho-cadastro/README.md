# redesenho-cadastro

> **Nota de revisão (2026-09-10, mesmo dia do arquivamento)**: a primeira
> versão desta feature (descrita nas specs funcional/técnica abaixo, DD-1
> a DD-3) usava um layout de duas colunas com painel de marca lateral e
> prévia ao vivo em dois lugares (`#preview-nome`/`#preview-link`). Ainda
> no mesmo dia, o dono trouxe um **segundo protótipo** — coluna única
> centralizada, sem painel lateral — e pediu para seguir esse em vez do
> primeiro. Este README descreve o que **está de fato no ar** hoje (a
> versão revisada); as specs em `1-functional/` e `2-technical/`
> continuam sendo o registro histórico de como a primeira versão foi
> decidida, mas DD-1 a DD-3 (painel de marca, prévia em dois lugares,
> breakpoint `lg` para o painel) foram **superadas**, não estão mais
> implementadas. Ver commits `bc9c7ef`, `f58015a`, `08e96e6` (posteriores
> ao arquivamento) para o histórico da revisão.

## O que foi construído (versão atual)

Segunda tela a receber a identidade visual da marca nova, depois da
`pagina-institucional`: `/cadastro` em coluna única centralizada — logo
acima do título, botão de tema no canto superior direito, formulário com
os mesmos 5 campos de sempre. Título e rótulos em navy (claro) para
reforçar a marca; nenhuma mudança de campo, validação ou fluxo.

```
GET/POST /cadastro         → RegistrationController — SEM MUDANÇA
                              (mesmo RegistrationRequest, mesma
                              validação, mesmo fluxo de erro)
auth/cadastro.html          → coluna única centralizada: logo, título
                              (navy, negrito), formulário, rodapé
static/js/slug.js           → forma original (só #slug-previa) — a
                              extensão para o painel de marca foi
                              revertida junto com a remoção do painel
```

## O que nasceu/mudou nesta feature (estado final)

| Item | Onde | Por quê |
|---|---|---|
| Layout coluna única | `auth/cadastro.html` (reescrito 2x no mesmo dia) | Segundo protótipo do dono — mais simples que o painel de marca da primeira versão |
| Rótulos e título em navy | `auth/cadastro.html` (`<style>` local) | Igual ao protótipo, que fixa navy como cor de texto padrão da página; não vira token de sistema (DD-4 da pagina-institucional) |
| Placeholders (`voce@email.com`, `barbearia-do-joao`, `55 11 99999-9999`, `Pelo menos 8 caracteres`) | `auth/cadastro.html` | Fidelidade campo a campo ao protótipo, corrigida após o dono comparar lado a lado |
| Preview do link só parcialmente colorido | `auth/cadastro.html` | Só o trecho do domínio/slug fica coral (`<strong style="color:#ff6b4a">`), não a frase inteira |
| `simboraagendar.com.br` (com `.br`) | `auth/cadastro.html`, `landing.html` | Correção incidental — texto usava `simboraagendar.com` (sem `.br`) |
| **Achado real**: `th:replace` no mesmo elemento onde se tenta adicionar classe | `PATTERNS.md` | `th:replace` troca a tag host inteira — classes de posicionamento postas nela são descartadas. Corrigido envolvendo `th:replace` num `<div>` de posicionamento. Promovido a regra do projeto. |

**Nenhuma mudança, do início ao fim**: `RegistrationController.java`,
`RegistrationRequest.java`, `RegisterBusinessUseCase` — confirmado antes
de especificar e revalidado a cada rodada de ajuste visual.

## Decisões de design (histórico — ver nota de revisão no topo)

As decisões DD-1 (`.painel-escuro` compartilhado), DD-2 (prévia
estendendo `slug.js`) e DD-3 (breakpoint `lg` para o painel) valeram
para a **primeira versão**, hoje superada. `.painel-escuro` continua
compartilhado em `fragments/layout.html`, mas hoje só é usado por
`landing.html`. DD-4 (zero mudança de backend) continua válida e foi
reconfirmada em cada revisão.

## Testes

- `RegistrationControllerTest`: 10 testes pré-existentes continuam
  verdes sem edição (prova de que nenhum contrato mudou) + 1 caso
  ajustado ao longo das revisões (hoje confirma só `#slug-previa`, não
  mais os ids do painel removido).
- 628 testes no projeto inteiro, 0 falhas, 0 erros (`./mvnw clean
  verify`), reconfirmado a cada rodada de ajuste.
- Comportamento client-side (prévia do link, alternância de tema) não é
  testável via `MockMvc` — mesma limitação já aceita para o `temaToggle`
  na landing; verificação manual a cada mudança.

## Quality gates (Layer 3)

Todos `APPROVED` no momento do arquivamento — ver
`verdicts/{code_review,performance,security}.json`. As três rodadas de
ajuste pós-arquivamento (layout, título/posição, campos) foram feitas
como commits diretos de correção, com `./mvnw clean verify` revalidado
em cada uma, sem reabrir o ciclo completo de quality gates (mudança
puramente de apresentação, sem risco novo de segurança/performance).
