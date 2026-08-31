# Cadastro de estabelecimento e login

**Backlog**: TODO-001 · **Concluída**: 2026-08-30 · **Contextos**: `organization`, `platform`

A primeira feature de negócio do AgendaIA, e a que estabelece o tenant — sem ela
nada mais tem onde morar. Um dono de barbearia cria a conta do estabelecimento,
escolhe o link público que vai compartilhar com os clientes, e entra no painel
**já autenticado**, sem passar pelo login.

## O que foi entregue

| Rota | Quem acessa | O que faz |
|---|---|---|
| `GET/POST /cadastro` | qualquer um | cria `Business` + `User` numa transação e autentica a sessão |
| `GET/POST /login` | qualquer um | entra, voltando à rota originalmente pretendida |
| `POST /logout` | autenticado | invalida a sessão no servidor |
| `GET /admin/dashboard` | autenticado | nome do estabelecimento e link público |
| `GET /` | qualquer um | redireciona ao cadastro |

Duas tabelas novas: `business` — que **é** o tenant, sem coluna `tenant_id` — e
`app_user`, prefixada porque `user` é palavra reservada no PostgreSQL.

## O que a feature estabeleceu para o resto do projeto

- **`TenantContext`**, populado por filtro a partir da sessão autenticada. O
  `tenantId` nunca vem do cliente — e hoje isso é garantido por construção:
  não existe um `@PathVariable` ou `@RequestParam` no projeto inteiro.
- **`CrossTenantIsolationIT`**, o teste da premissa do produto. Nasce aqui e
  **cresce a cada feature**: rota nova sem caso correspondente é rota não
  verificada.
- **Piso de cobertura no build**, com JaCoCo somando unitários e integração.
- **Duas regras de ArchUnit novas**, uma delas criada porque a original
  cobria metade do que prometia e deixou passar uma violação real.

## Números

| | |
|---|---|
| Tasks | 17 (16 planejadas + 1 nascida de divergência entre specs) |
| Testes | 214 — 181 unitários, 33 de integração contra Postgres real |
| Cobertura | 91% de instruções (piso de 80% travado no `pom.xml`) |
| Commits | 10 |

## Decisões que valem reler

- **DD-1 e DD-2** — por que `UserDetailsService` mora em `organization` e o
  principal mora em `platform`. Os dois casos são a mesma regra: onde a classe
  *parece* pertencer perde para onde ela **pode** morar sem inverter a seta de
  dependência.
- **DD-3** — `business_slug_history` **não** foi criada. Escrever a migration
  revelou que a unicidade do slug não fecha entre duas tabelas. Há um aviso na
  spec técnica para quem for implementar troca de slug.
- **DD-5** — autenticação programática precisa gravar no
  `SecurityContextRepository`, não só no `SecurityContextHolder`. É o defeito
  mais provável desta feature, e um teste que parasse no `302` não o pegaria.

## Dois defeitos que os testes pegaram

1. **`TenantContextFilter` com a ordem invertida.** Declarava
   `@Order(Integer.MIN_VALUE + 100)`, rodando *antes* da cadeia do Spring
   Security, onde o principal ainda não existe. `/admin/dashboard` devolvia 500
   para qualquer sessão válida — inclusive no redirecionamento logo após o
   cadastro. O javadoc afirmava exatamente o contrário do que o código fazia.
2. **`GlobalExceptionHandler` transformando 404 em 500.** O
   `@ExceptionHandler(Exception.class)` engolia as exceções do próprio Spring
   que já carregam status.

## Ficou para depois

`TODO-109` (recuperação de senha, declarada fora de escopo na spec funcional) e
as dívidas `DEBT-011` a `DEBT-014`. Todas no `sdd/backlog.md`, com o gatilho de
quando deixam de ser aceitáveis.

## Onde está o resto

- [`1-functional/spec.md`](1-functional/spec.md) — user stories e cenários E2E
- [`2-technical/spec.md`](2-technical/spec.md) — as decisões, com alternativas
- [`3-tasks/tasks.json`](3-tasks/tasks.json) — as 17 tasks
- [`4-implementation/progress.md`](4-implementation/progress.md) — **os aprendizados**, incluindo as dez quebras do Boot 4
- [`4-implementation/artifacts/revisoes.md`](4-implementation/artifacts/revisoes.md) — as revisões, com as saídas de `EXPLAIN`
- [`meta.md`](meta.md) — o estado e os desvios de processo
