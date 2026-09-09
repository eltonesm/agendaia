# Implementation Summary — gestao-de-clientes (IDEA-018, IDEA-019, IDEA-006)

## Timeline

- Início: 2026-09-09
- Fim: 2026-09-09 (funcional → técnica → tasks → build → finish no
  mesmo dia)

## Tasks

- Total: 22 (`stats.done: 22` em `tasks.json`)
- Estratégia de execução: Batched — cadeia de dependência clara (domínio →
  persistência → aplicação → web → templates → testes → qualidade).
- 0 tasks adicionadas ou removidas fora do plano aprovado.

## Commits (9, `main..feature/gestao-de-clientes`)

```
37b8669 docs(sdd): inicia gestao-de-clientes (IDEA-018/019/006)
632f216 docs(sdd): aprova specs funcional e tecnica da gestao-de-clientes
dff2fec docs(sdd): aprova as 22 tarefas da gestao-de-clientes (estrategia batched)
632fdbf feat(scheduling,customer): status de pagamento e atividade do cliente (gestao-de-clientes)
1787e13 feat(scheduling): telas de clientes (lista, detalhe) e controle de pagamento na agenda
0edf6e2 test(scheduling,customer): cobertura de status de pagamento e atividade do cliente
bc0057d fix(platform): trata MethodArgumentTypeMismatchException como uso incorreto, nao defeito
7a97a4d docs(sdd): fecha camada 3 da gestao-de-clientes - code review, performance e security
ed1e3d9 docs(sdd): promove aprendizados da gestao-de-clientes ao PATTERNS.md
```

> Mesmo padrão da TODO-110: sem commit incremental por task individual —
> as 22 tasks foram implementadas numa sessão longa e commitadas
> agrupadas por camada (domínio/persistência, templates, testes,
> correção de code review, quality gates, patterns).

## Quality

- Testes: 614 no projeto inteiro (23 novos ou editados nesta feature —
  ver detalhamento no README.md, seção Testes), 0 falhas, 0 erros
  (`./mvnw clean verify`).
- Linter/type errors: 0.
- Layer 3 (code review, performance, security): todos `APPROVED`, zero
  achado crítico, major ou minor pendente ao final (ver `verdicts/`) — um
  achado real (`MethodArgumentTypeMismatchException`) foi encontrado e
  corrigido durante o próprio code review (ver Gotcha real #2 abaixo).

## Gotcha real #1: qual contexto "possui" uma tela que cruza dois contextos

A pergunta natural era "onde mora `/admin/clientes`?" — pelo nome da URL,
pareceria óbvio que fosse em `customer`. Mas `scheduling` já dependia de
`customer.api` desde a TODO-008 (para resolver nome/telefone na agenda) —
colocar a tela em `customer` exigiria abrir a dependência contrária
(`customer` → `scheduling.api`), fechando um ciclo `customer ⇄
scheduling`, a mesma classe de erro que a DD-6 da TODO-110 já tinha
pagado o preço de descobrir (só que daquela vez, `organization ⇄
scheduling`). A decisão de colocar `CustomerActivityHandler` e
`CustomerAdminController` dentro de `scheduling` foi tomada **antes** de
escrever qualquer código, perguntando explicitamente "quem já depende de
quem?" — nenhum ciclo chegou a ser criado desta vez. Promovido ao
`PATTERNS.md` como pergunta obrigatória para a próxima feature que cruzar
contextos.

## Gotcha real #2: `@RequestParam` de enum inválido não vira 400 sozinho

A nova rota `POST /admin/agenda/agendamentos/{id}/pagamento` usa
`@RequestParam PaymentStatus status` — o primeiro bind direto de enum via
`@RequestParam` do projeto inteiro. Um valor fora do enum (ex.:
`status=XPTO`) gera `MethodArgumentTypeMismatchException`, que **não**
implementa a interface `ErrorResponse` do Spring Framework (confirmado
via `javap` no jar real, não deduzido) — diferente de outras exceções do
próprio framework que já carregam o status certo sozinhas
(`NoResourceFoundException`, `HttpRequestMethodNotSupportedException`).
Sem tratamento dedicado, ela caía no handler genérico de "defeito"
(`GlobalExceptionHandler.erroInesperado`): log `ERROR` com stack trace e
500, contradizendo a própria filosofia documentada na classe ("uso
incorreto de formulário não é bug"). Corrigido com um
`@ExceptionHandler(MethodArgumentTypeMismatchException.class)` dedicado
(400, log `WARN`). Registrado também `DEBT-020` para um gap relacionado
(qualquer `@RequestParam int` aceita valor negativo sem tratamento
dedicado), fora do escopo desta feature.

## Gotcha real #3: `PaymentStatus` não é uma máquina de estados

Diferente de `AppointmentStatus` (`confirm`/`cancel`/`complete`, cada um
com guard de transição e absorção condicional), `PaymentStatus` foi
desenhado deliberadamente **sem** restrição de transição — qualquer valor
pode virar qualquer outro a qualquer momento (BR-2 da spec funcional,
pedido explícito do dono: "fiado" pode virar "pago" quando o cliente
volta e quita, sem burocracia). A única disciplina mantida foi a
absorção quando o valor pedido já é o atual (evita gravação
desnecessária, mesmo padrão de comparação `antes`/`depois` já usado em
`confirm()`/`cancel()`/`complete()`).
