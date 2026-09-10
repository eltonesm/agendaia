# pagina-institucional

## O que foi construído

A primeira impressão do produto deixa de ser um formulário de cadastro
sem contexto. `/` passa a renderizar uma landing institucional — hero,
para quem é, como funciona, preço — e o sistema inteiro (landing,
cadastro, login, admin, agenda) ganha a identidade visual da marca nova
"simboraagendar" (coral/navy), substituindo o indigo usado desde o
início do projeto.

```
GET /                     → landing institucional (antes: redirect
                             direto para /cadastro)
                             hero, 4 segmentos, "como funciona" em 3
                             passos, preço (30 dias grátis + R$49,90/mês),
                             botão "Criar conta" → /cadastro
GET /cadastro              → sem mudança nenhuma de campos ou
                             comportamento — só herda a cor nova
Toda tela do sistema        → --bs-primary: indigo #4F46E5 → coral
                             #FF6B4A (mesmo mecanismo de token único)
```

## O que nasceu/mudou nesta feature

| Item | Onde | Por quê |
|---|---|---|
| `landing.html` | `templates/` (novo) | Conteúdo institucional traduzido do protótipo Tailwind do dono para Bootstrap 5 |
| `fragments/layout :: logo(tamanho)` | `templates/fragments/layout.html` (novo) | SVG inline (ícone + wordmark) recriado a partir da descrição da logo — arquivo original não chegou ao repositório |
| `--bs-primary` e derivadas | `templates/fragments/layout.html` (editado) | Indigo → coral, único ponto de mudança que propaga para todo o sistema |
| `WebConfig.addViewController("/", "landing")` | `platform.web` (editado) | Troca o redirect antigo por renderização direta — mesmo padrão já usado para `/login` |
| `LandingRouteIT`, `PaletaDeMarcaTest` | `platform.web` (novo, teste) | Cobrem a rota nova e a regressão de paleta de forma determinística |
| Tabela de paleta + novo padrão de teste | `sdd/PATTERNS.md` | Cor de marca atualizada; documentado o padrão "invariante de template vira teste lendo o classpath" |

## Decisões de design centrais

- **Nenhum `@Controller` novo (DD-1)**: a rota é um `addViewController`,
  mesmo padrão já usado para `/login`/`/operador/login` — a landing não
  lê sessão nem banco. O próprio Javadoc de `WebConfig.java` já previa
  esta troca desde a criação do arquivo.
- **Nenhuma mudança de segurança (DD-2)**: `"/"` já era `permitAll()`;
  trocar o que ela renderiza não muda quem acessa.
- **Logo recriada, não copiada (DD-3)**: o arquivo vetorial original
  compartilhado pelo dono só foi visto na conversa, nunca chegou ao
  repositório — o ícone (quadrado coral, ">>"" branco) e o wordmark foram
  recriados como SVG inline a partir da descrição. Trade-off assumido e
  documentado; trocar pelo arquivo real depois é uma edição local.
- **Rebrand restrito a `--bs-primary` e derivadas (DD-4)**: nenhum token
  novo de sistema. Navy fica só no wordmark da logo, não vira uma segunda
  cor de marca documentada — nenhuma tela hoje pede esse papel.
- **Preço é conteúdo estático, não configuração (DD-5)**: R$49,90/mês
  é texto direto no template. Não existe (nem esta feature cria) campo
  de plano/preço no domínio de billing — abstrair um valor de uso único
  seria custo pago à toa (ADR 0002).
- **Fora de escopo, decidido com o dono em 2026-09-09**: qualquer ajuste
  em operador, admin (além da cor) ou agenda pública — pedido explícito
  para tratar em features futuras separadas, depois desta.

## Testes

- `LandingRouteIT` (novo, 3 casos): `GET /` renderiza `landing` (não
  redireciona mais); CTA aponta para `/cadastro`; `/cadastro` continua
  funcionando sem mudança.
- `PaletaDeMarcaTest` (novo, 2 casos, sem `@SpringBootTest`): lê
  `fragments/layout.html` do classpath — confirma que a cor antiga
  (`4f46e5`/`4338ca`) não sobrou em nenhum token, e que `--bs-primary` é
  a cor nova.
- 627 testes no projeto inteiro, 0 falhas, 0 erros (`./mvnw clean
  verify`, contagem por soma dos XML de surefire+failsafe).

## Quality gates (Layer 3)

Todos `APPROVED` — ver `verdicts/{code_review,performance,security}.json`.

- **Code review**: feature puramente de apresentação, sem dado dinâmico
  na landing (sem superfície de XSS), sem duplicação (logo reusada via
  fragmento), sem dependência nova.
- **Performance**: `GET /` não faz consulta a banco; SVG inline evita
  requisição HTTP extra de um arquivo de imagem separado.
- **Security**: `"/"` confirmado `permitAll()` sem exceção nova; nenhum
  dado de sessão/tenantId lido pela landing; nenhum secret ou dependência
  nova (`grep` de padrões de segredo, sem ocorrência).
