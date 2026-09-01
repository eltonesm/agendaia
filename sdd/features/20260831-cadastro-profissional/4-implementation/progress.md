# Progresso da implementação — cadastro-profissional

Feature: TODO-002 · Início: 2026-08-31 · Fim: 2026-08-31
Estratégia: `batched` por nível de dependência.

## Resultado

- **Tests passing: 243/243** (203 unitários + 40 de integração)
- **Coverage: 90,6%** de instruções (69,4% de ramos), piso de 80% travado no `pom.xml`
- `./mvnw clean verify` verde

## Tasks

| ID | Título | Status | Commit |
|---|---|---|---|
| TASK-001 | Migration V3: tabela professional | ✅ | `827a684` |
| TASK-002 | Entidade Professional, repositório, testes puros | ✅ | `827a684` |
| TASK-003 | Caso de uso do cadastro (DD-1) | ✅ | `827a684` |
| TASK-004 | Caso de uso da listagem (DD-1) | ✅ | `827a684` |
| TASK-005 | Tela e controller /admin/profissionais (DD-2) | ✅ | `3ce782e` |
| TASK-006 | Link do painel | ✅ | `3ce782e` |
| TASK-007 | E2E-1/2/3 (ProfessionalRegistrationIT) | ✅ | `4ca9438` |
| TASK-008 | Extensão do CrossTenantIsolationIT | ✅ | `4ca9438` |
| TASK-009 | Code review | ✅ | `c32688d` |
| TASK-010 | Revisão de performance | ✅ | `c32688d` |
| TASK-011 | Revisão de segurança | ✅ | `c32688d` |

## Commits

- Commit: `827a684` — entidade Professional e os dois casos de uso (TASK-001..004)
- Commit: `3ce782e` — tela e controller de /admin/profissionais (TASK-005, 006)
- Commit: `4ca9438` — E2E-1/2/3 e extensão do isolamento entre tenants (TASK-007, 008)
- Commit: `c32688d` — achado de code review, revisões de performance e segurança (TASK-009, 010, 011)

## Aprendizados

### FK nova entre agregados exige atualizar a limpeza de TODAS as ITs que compartilham o container

`professional` ganhou `tenant_id REFERENCES business(id)`. Como
`RegistrationIT`, `SlugUnavailableIT`, `LoginIT` e `CrossTenantIsolationIT`
compartilham o mesmo container Testcontainers dentro da mesma execução de
`verify`, a limpeza de cada uma precisou passar a apagar `professional`
**antes** de `business` — senão a segunda classe a rodar na suíte falha com
violação de FK deixada pela primeira.

**Padrão a repetir**: toda vez que uma nova tabela ganhar FK para uma
existente, grep por `deleteAllInBatch` nos testes de integração da tabela
referenciada e corrigir a ordem antes de rodar `verify` pela primeira vez —
não depois de ver o erro.

### `if (!model.containsAttribute(...))` sem `RedirectAttributes` correspondente é código morto

Copiei um padrão defensivo do `RegistrationController` sem verificar se a
condição que ele protege pode de fato ocorrer aqui. Como o `POST` desta
feature nunca faz `redirect:` com `RedirectAttributes` — ele devolve a
`VIEW` diretamente em caso de erro — o `GET` nunca é chamado com `form` já
presente no model. A checagem nunca teria o branch "verdadeiro" executado.
Achado na revisão de código (TASK-009), removido.

### Índice em `tenant_id` sozinho basta para uma lista pequena

`EXPLAIN ANALYZE` com 30 linhas confirmou `Index Scan using
professional_tenant_idx`, 3 buffers, ~2ms. Não precisou de índice composto
`(tenant_id, name)` — o filtro por `active` e a ordenação por `name` operam
sobre um conjunto já pequeno depois do índice de tenant. Fica documentado
para quando a lista crescer o suficiente para justificar revisitar.

## Desvios do processo

Nenhum. As duas perguntas em aberto do `/sdd.start` (dono como profissional
automático, `organization/api`) foram respondidas pelo usuário antes da spec
funcional, então não geraram divergência durante o build — ao contrário da
TODO-001, que precisou da TASK-017 no meio do caminho.
