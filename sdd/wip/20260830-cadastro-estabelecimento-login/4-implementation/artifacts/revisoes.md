# Revisões de qualidade — TASK-014, TASK-015, TASK-016

Feature: `cadastro-estabelecimento-login` · Data: 2026-08-30
Base: `./mvnw clean verify` verde — 172 unitários + 33 de integração.

As três revisões foram feitas em linha, não pelos Skills `sdd-code-reviewer` e
`sdd-performance-expert`. Motivo: a instrução operacional em vigor nesta sessão
proíbe delegar a subagente sem pedido explícito. O conteúdo do portão foi
cumprido; o mecanismo, não. Fica registrado para não parecer que os Skills
rodaram.

---

## TASK-014 — Code review

### AC-1: nenhum achado de corretude em aberto

Dois defeitos de corretude foram encontrados **durante** a implementação, não
nesta revisão, e já estão corrigidos com teste que trava a regressão:

| Defeito | Como apareceu | Onde travou |
|---|---|---|
| `TenantContextFilter` com `@Order(Integer.MIN_VALUE + 100)`, rodando antes da cadeia do Spring Security — tenant nunca populado, painel 500 para qualquer sessão válida | `LoginIT.painelMostraOLink` | `CrossTenantIsolationIT` inteiro |
| `LoginIT.voltaParaDestinoPretendido` usava `/admin/dashboard`, que é também o `defaultSuccessUrl` — passaria mesmo se o destino guardado fosse ignorado | leitura da spec ao escrever o E2E-3 | agora usa `/admin/agenda` e afirma que **não** é o painel |

Nenhum achado de corretude em aberto.

### AC-2: conformidade com PATTERNS.md

| Regra | Verificação | Resultado |
|---|---|---|
| Sem setter público | `grep -rn "public void set" src/main/java/` | nenhum |
| Sem sufixo `Impl` | `find src/main/java -name "*Impl.java"` | nenhuma |
| Interface entre camadas | `RegisterBusinessUseCase`/`Handler`, `ViewDashboardUseCase`/`Handler` | conforme |
| Transação só na `application` | ver achado abaixo | **1 achado, corrigido** |

**Achado — `@Transactional` em adapter.** `BusinessUserDetailsService.loadUserByUsername`
carregava `@Transactional(readOnly = true)`. A regra ArchUnit
`transacao_so_na_application` não pegou porque usa `noClasses(...).beAnnotatedWith(...)`,
que olha **anotação de classe**; a anotação estava no método.

Duas correções, não uma:

1. A anotação saiu. Não comprava nada: são duas leituras independentes, cada
   repositório do Spring Data já abre a sua transação, e não há associação lazy
   para estourar depois — não há associação nenhuma.
2. A regra ganhou a irmã `transacao_em_metodo_so_na_application`, com
   `noMethods()`. Regra que cobre metade do que promete é pior que regra
   ausente — foi assim que esta violação sobreviveu.

---

## TASK-015 — Revisão de performance

### AC-1: nenhuma associação EAGER; nenhuma consulta em laço

`grep -rn "FetchType\|@ManyToOne\|@OneToMany\|@ManyToMany\|@OneToOne" src/main/java/`
→ **nenhuma associação JPA no projeto**. N+1 e EAGER são impossíveis por
construção nesta feature, não por disciplina. `Business` e `User` se ligam por
`tenantId`, valor, não por referência de objeto — é a mesma decisão que impede
`JOIN` entre contextos.

Consulta em laço: uma só, deliberada. `RegisterBusinessHandler.sugerirVariacao`
faz até 8 `existsBySlug` no **caminho de erro** de um formulário. Cada uma é um
index only scan de 3 buffers (abaixo). O limite de nove tentativas é o que
impede isso de virar varredura.

### AC-2: login usa o índice único de e-mail — confirmado por EXPLAIN

Contra `postgres:18-alpine`, 20.000 usuários semeados dentro de uma transação
revertida ao fim (o banco de desenvolvimento não foi alterado):

```
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT * FROM app_user WHERE email = 'dono15000@exemplo.com';

 Index Scan using app_user_email_unique on app_user (actual time=0.035..0.036 rows=1.00 loops=1)
   Index Cond: ((email)::text = 'dono15000@exemplo.com'::text)
   Buffers: shared hit=3
 Execution Time: 0.086 ms
```

E a consulta de disponibilidade do slug, com 20.000 estabelecimentos:

```
 Index Only Scan using business_slug_unique on business (actual time=0.044..0.045 rows=1.00 loops=1)
   Index Cond: (slug = 'barbearia-15000'::text)
   Heap Fetches: 1
   Buffers: shared hit=3
 Execution Time: 0.121 ms
```

Nenhuma varredura sequencial nos dois caminhos quentes.

### Observação fora do AC

`BCryptPasswordEncoder` com custo padrão (10) leva ~60 ms por verificação. É o
maior custo de CPU do login, e é assim de propósito — é a defesa contra força
bruta. Não é gargalo no volume do MVP; vira um se o login for chamado em laço,
o que não acontece em lugar nenhum.

---

## TASK-016 — Revisão de segurança

### AC-1: senha nunca em log, mensagem de erro ou resposta

| Superfície | Verificação | Resultado |
|---|---|---|
| Log | `RegistrationController` registra `businessId` e `slug`; `GlobalExceptionHandler`, URI e mensagem | senha não aparece |
| `toString` | `RegistrationRequest.toString` sobrescrito sem senha; `AuthenticatedUser.toString` sem senha nem e-mail | conforme |
| Resposta HTML | campo de senha em `cadastro.html` **sem** `th:field`, de propósito | não é devolvido ao reexibir com erro |
| Banco | `RegistrationIT.senhaNuncaEmTexto` afirma hash `$2...` e ausência do texto | conforme |

**Achado — hash BCrypt guardado na sessão.** `AuthenticatedUser` implementava só
`UserDetails`, não `CredentialsContainer`. O `ProviderManager` chama
`eraseCredentials()` por padrão, mas ele só age em principal que seja
`CredentialsContainer` — então o hash ficava no principal, dentro da sessão HTTP.

Risco hoje: baixo, a sessão é memória do processo. Risco na primeira vez que a
sessão for para o Redis (já previsto no backlog): o hash vai junto.

Corrigido em dois pontos, porque há **dois** caminhos de autenticação:

1. `AuthenticatedUser` passa a implementar `CredentialsContainer`. O
   `passwordHash` deixa de ser `final` — é o preço, e está anotado no javadoc.
2. `RegistrationController.autenticarSessao` chama `eraseCredentials()` à mão. O
   cadastro autentica sem passar por provider nenhum, então ninguém apagaria por
   ele. É exatamente o tipo de esquecimento que só o segundo caminho tem.

Travado por `RegistrationIT.hashNaoFicaNaSessao` e `LoginIT.hashNaoFicaNaSessao`
— um para cada caminho.

### AC-2: CSRF ligado; login não permite enumeração de conta

CSRF: nenhuma chamada a `csrf(...)` em `SecurityConfig`, ou seja, o padrão do
Spring Security vale. `RegistrationIT.semCsrfNaoGrava` afirma 403 e banco
intocado.

Enumeração: os três modos de falhar produzem redirecionamento **idêntico** para
`/login?erro` — senha errada, e-mail inexistente e estabelecimento desativado.
Coberto por três testes distintos em `LoginIT` que afirmam o mesmo resultado, e
`BusinessUserDetailsService` não registra o e-mail tentado em log.

### AC-3: nenhum caminho aceita `tenant_id` vindo do cliente

`CrossTenantIsolationIT` exerce as duas tentativas óbvias — parâmetro de query e
cabeçalho forjado com o `tenantId` do vizinho — e afirma que o painel continua
mostrando o estabelecimento da sessão.

O AC-3 da TASK-013 foi conferido desligando o mecanismo: com o
`TenantContextFilter` de volta à ordem quebrada, três dos cinco casos falham.
Falham com **500**, não com dado do vizinho na tela: o `TenantContext.require()`
recusa em vez de devolver algo, então o mecanismo falha fechado.

---

## Pendências deixadas em aberto

Nenhuma bloqueante. Duas anotações para o backlog:

- **Sessão em memória.** Reiniciar a aplicação desloga todo mundo, e não há como
  rodar duas instâncias. Aceitável no MVP com um estabelecimento piloto; vira
  problema no primeiro deploy sem janela.
- **Sem limite de tentativa de login.** Força bruta é contida só pelo custo do
  BCrypt. Suficiente enquanto a base é pequena e o produto não é alvo.
