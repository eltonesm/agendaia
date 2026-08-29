# Project Configuration

> Contém **apenas overrides**. Propriedades não listadas usam os defaults do
> framework SDD Kit.
>
> Escopo deste arquivo: convenções de time. Stack é detectada do `pom.xml`.
> Estrutura de contextos, comandos de build e regras de código estão em
> `CLAUDE.md`, `sdd/PATTERNS.md` e `docs/`.

## Backend Conventions

```yaml
architecture:
  pattern: clean
```

> Override explícito, ainda que coincida com o default. É decisão registrada no
> [ADR 0002](../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md),
> não omissão — e o ADR acrescenta uma qualificação que este campo não expressa:
> o rigor é **proporcional ao subdomínio**. Regime completo (domínio puro,
> entidade JPA separada, mapeamento explícito) só em `scheduling`. Nos contextos
> de suporte, a entidade JPA é o modelo.

## Quality Gates

```yaml
coverage:
  min_coverage: 90
```

> Decisão do time, acima do default de 80.
>
> Atenção ao efeito colateral: 90% global, somado ao rigor assimétrico do ADR
> 0002, empurra na direção de escrever teste sem valor para CRUD só para atingir
> o número. A regra de como gastar essa cobertura está em `PATTERNS.md` —
> resumidamente: cobertura em `scheduling` vem de teste de regra; nos contextos
> de suporte, vem de teste de caso de uso e de integração, nunca de teste de
> getter ou de construtor.

## Language

```yaml
language:
  specs: pt
```

> Specs em português, coerente com a regra do glossário: identificadores em
> inglês, linguagem de negócio em português. Termos técnicos consagrados (API,
> REST, CRUD, endpoint) permanecem em inglês.

## Usando defaults do framework

| Propriedade | Default | Por quê |
|---|---|---|
| `testing.ratio_unit_integration` | `4:1` | O domínio de `scheduling` é Java puro e gera muito teste unitário barato; os de integração são poucos e caros (Testcontainers com Postgres real). |
| `pr.max_lines` | `400` | Sem motivo para divergir. |

## Branching Strategy

> **Referência obrigatória.** O template do kit não está presente nesta
> instalação, então esta tabela foi autorada para o modelo que o projeto usa de
> fato: **trunk-based com branches curtas**, não GitFlow completo.
>
> Não há `develop` nem `release/*`: um desenvolvedor, uma VPS, deploy contínuo a
> partir de `main`. Introduzir branch de integração aqui adicionaria cerimônia
> sem nenhum problema para resolver. Quando houver mais de uma pessoa ou
> ambiente de homologação, esta seção é revista.

| Branch | Propósito | Origem | Destino | Vida |
|---|---|---|---|---|
| `main` | Estado de produção. Sempre verde. | — | — | permanente |
| `feature/<nome>` | Nova funcionalidade, uma feature do SDD | `main` | `main` | curta |
| `fix/<nome>` | Correção de defeito | `main` | `main` | curta |
| `refactor/<nome>` | Mudança estrutural sem alterar comportamento | `main` | `main` | curta |
| `chore/<nome>` | Build, dependências, configuração, tooling | `main` | `main` | curta |
| `docs/<nome>` | Somente documentação | `main` | `main` | curta |
| `hotfix/<nome>` | Correção urgente em produção | `main` | `main` | mínima |

**Regras**

- Branch curta: dias, não semanas. Branch longa vira conflito de merge.
- `main` sempre buildável: `./mvnw verify` verde é condição de merge.
- Nome da branch de feature espelha a pasta em `sdd/wip/`.
- Mensagem de commit segue Conventional Commits (padrão, não configurável aqui).

## Project Vision

<!-- Ainda não definida. Preencher com /sdd.project vision quando as respostas
     de posicionamento e meta do piloto estiverem fechadas. Ver docs/product/. -->
