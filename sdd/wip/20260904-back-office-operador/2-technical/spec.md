# back-office-operador - Technical Spec

**Feature**: back-office-operador
**Status**: approved
**Data**: 2026-09-04
**Aprovado por**: Elton Marques em 2026-09-04T02:26:20Z

---

## Executive Summary

Primeira feature a introduzir um bounded context novo desde a Fase 0:
`billing`. Regime CRUD (ADR 0002, subdomínio de suporte) — `BillingAccount`
é uma entidade JPA simples, um registro por tenant, guardando a data até
quando o acesso do estabelecimento é válido.

Também a primeira feature a introduzir um **segundo tipo de sessão**: o
operador da plataforma, autenticado por uma `SecurityFilterChain` separada
(`/operador/**`), com credencial única vinda de configuração — nunca de
banco, nunca de formulário público.

O bloqueio de `/admin/**` e o aviso de carência são dois mecanismos
transversais novos, cada um usando a ferramenta certa do Spring: um
`Filter` para bloquear (decide antes do controller rodar) e um
`@ControllerAdvice` para avisar (só injeta dado na view). Os dois moram em
`billing`, não em `platform` — decidir "estabelecimento pode acessar?" é
regra de negócio, e `platform/package-info.java` proíbe explicitamente
regra de negócio ali.

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────────┐
│  platform.security                                                 │
│                                                                      │
│  SecurityConfig (existente)          OperatorSecurityConfig (novo)  │
│    @Order(2), securityMatcher          @Order(1), securityMatcher   │
│    default (tudo que não é             ("/operador/**")             │
│    /operador/**)                       InMemoryUserDetailsManager   │
│    AuthenticatedUser (organization)    (credencial via config,       │
│                                          BR-7 — nunca banco)          │
└────────────────────────────────────────────────────────────────────┘
                    │                              │
                    ▼                              ▼
         sessão do dono (tenant)          sessão do operador (sem tenant)
                    │                              │
                    ▼                              ▼
┌──────────────────────────────┐   ┌──────────────────────────────────┐
│ TenantContextFilter           │   │ billing.adapter.in.web            │
│ (existente, @Order n)         │   │  OperatorPanelController          │
│ só seta TenantContext se o    │   │   GET  /operador/painel            │
│ principal for AuthenticatedUser│  │   POST /operador/estabelecimentos │
│ — não seta para o operador     │  │        /{tenantId}/prazo          │
└──────────────────────────────┘   └──────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────┐
│ billing.adapter.in.web        │
│  AccessGuardFilter (novo)      │   ── bloqueia /admin/** se BLOCKED
│  (@Order n+1, só age em       │       (redireciona /admin/conta-suspensa)
│  /admin/**)                    │
│  BillingBannerAdvice (novo)    │   ── injeta aviso de carência na view
│  (@ControllerAdvice)           │       (nenhuma mudança em organization)
└──────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────┐        ┌─────────────────────────────┐
│ billing.application            │───────▶│ organization.api (novo)    │
│  BillingAccountService         │        │  BusinessDirectory          │
│  (get-or-create, status,       │        │   .listAll()                 │
│  marcar prazo)                 │        │  -> List<BusinessRef>        │
└──────────────────────────────┘        └─────────────────────────────┘
                    │
                    ▼
         billing_account (Postgres, nova tabela)
```

**Fluxo de bloqueio** (a cada requisição a `/admin/**`):

1. `TenantContextFilter` já resolveu `TenantContext` (dono autenticado).
2. `AccessGuardFilter` pergunta a `BillingAccountService.statusFor(tenantId)`.
3. Se `BLOCKED` e a rota não é `/admin/conta-suspensa`: redireciona para lá.
4. Senão, segue a cadeia. Se `GRACE_PERIOD`, o resultado fica disponível
   para `BillingBannerAdvice` via atributo de requisição (evita calcular o
   status duas vezes).

**Fluxo do operador**:

1. Login em `/operador/login` (chain própria, credencial de config).
2. `GET /operador/painel` — `BillingAccountService.listForOperator()`
   cruza `organization.api.BusinessDirectory.listAll()` com
   `BillingAccountRepository`, criando a conta de billing na hora (get-or-
   create) para qualquer `Business` que ainda não tenha uma — cobre o(s)
   estabelecimento(s) cadastrado(s) antes desta feature (BR-8).
3. `POST /operador/estabelecimentos/{tenantId}/prazo` — marca uma nova
   data de validade de acesso (BR-3).

---

## Design Decisions

### DD-1: `billing` como bounded context novo

**Contexto**: acompanhar prazo de acesso e pagamento de cada
estabelecimento é uma responsabilidade com motivo de mudar diferente do
de `organization` (que modela a identidade do tenant). Colocar isso em
`organization` misturaria "quem é o estabelecimento" com "ele está em
dia".

**Opções consideradas**:
- **A — Campo direto em `organization.Business`**: mais simples de
  escrever, mas `organization` passaria a conhecer conceitos de cobrança
  (justamente os que o glossário mantinha banidos "sem decisão nova") e
  cresceria por dois motivos de mudança diferentes.
- **B (selecionada) — Contexto novo `billing`**: `organization` continua
  sabendo só de identidade de tenant; `billing` é o único lugar que sabe o
  que é `Plano`/`Pagamento`/prazo de acesso. Seguindo o mesmo raciocínio
  que já levou a `catalog` e `scheduling` a existirem como pacotes
  próprios (ADR 0001).

**Trade-offs Accepted**: mais um pacote, mais um `package-info.java`,
mais uma fronteira `api` a manter — custo mecânico pequeno, já pago três
vezes antes neste projeto.

**Rationale**: é exatamente a "decisão nova" que o glossário previa antes
de liberar `Plano`/`Assinatura`/`Pagamento`.

### DD-2: `BillingAccount` nasce sob demanda (get-or-create), não por evento

**Contexto**: BR-1 exige que todo estabelecimento (inclusive os
cadastrados antes desta feature) tenha uma data de validade de acesso.

**Opções consideradas**:
- **A — Evento de domínio**: `organization` publica `BusinessRegistered`,
  `billing` escuta e cria a conta. Exigiria infraestrutura de evento que o
  projeto não tem (Spring Modulith Events, mesmo já usando Modulith para
  fronteira), e criaria uma dependência de `organization` sabendo que
  `billing` existe.
- **B (selecionada) — Get-or-create sob demanda**: toda vez que
  `billing` precisa do status de um tenant (painel do operador, ou o
  `AccessGuardFilter`) e não existe `BillingAccount` para ele, cria um na
  hora, com `trialEndsAt` calculado a partir de
  `organization.api.BusinessDirectory` (que expõe `createdAt`). Cobre
  automaticamente o(s) estabelecimento(s) já existente(s) (BR-8), sem
  migration de backfill nem job.

**Trade-offs Accepted**: a dependência entre contextos é só numa direção
(`billing → organization.api`); `organization` nunca sabe que `billing`
existe.

### DD-3: Autenticação do operador — `SecurityFilterChain` separada, credencial em configuração

**Contexto**: BR-6/BR-7 exigem isolamento total do login de dono, e conta
única sem cadastro público.

**Decisão**: uma segunda `@Bean SecurityFilterChain`, em
`platform.security.OperatorSecurityConfig`, com `securityMatcher("/operador/**")`
e `@Order(1)` (avaliada antes da cadeia existente, que fica `@Order(2)`).
Backend de autenticação: `InMemoryUserDetailsManager` com um único usuário,
username e hash de senha vindos de `application.yaml`/variável de
ambiente (`agendaia.operador.username`, `agendaia.operador.password-hash`
— já em BCrypt, gerado uma vez pelo operador). Principal é o `User` padrão
do Spring Security — não `AuthenticatedUser` (que carrega `tenantId`) —
então `TenantContextFilter` (checagem `instanceof AuthenticatedUser`) não
seta tenant nenhum para essa sessão, sem precisar de nenhuma mudança nele.

**Trade-offs Accepted**: trocar a senha do operador exige reiniciar a
aplicação (é configuração, não é uma tela). Aceitável para uma conta única
que só você usa.

### DD-4: Bloqueio via `Filter` novo, aviso via `@ControllerAdvice` novo — ambos em `billing`, não em `platform`

**Contexto**: `platform/package-info.java` proíbe explicitamente regra de
negócio no pacote ("se aparecer aqui uma classe cujo nome um analista de
negócio reconheceria, ela está no contexto errado"). Decidir "este
estabelecimento está bloqueado" é regra de negócio de `billing`.

**Decisão**: `billing.adapter.in.web.AccessGuardFilter` (`OncePerRequestFilter`,
`@Order` logo depois de `TenantContextFilter`) age só em `/admin/**`;
redireciona para `/admin/conta-suspensa` quando `BLOCKED`, e guarda o
`AccessStatus` calculado como atributo de requisição para
`BillingBannerAdvice` (`@ControllerAdvice`, também em
`billing.adapter.in.web`) reaproveitar sem consultar o banco de novo —
esta expõe um `@ModelAttribute` nulo quando não há carência, lido pelo
layout compartilhado (`fragments/layout.html`) sem que `organization`
precise saber que `billing` existe.

**Trade-offs Accepted**: `platform` continua sem importar `billing`; é
`billing` quem se registra como filtro/advice transversal — mecanismo já
usado por `TenantContextFilter`/`LayoutAdvice`, só que a partir de outro
pacote.

### DD-5: `organization.api.BusinessDirectory.listAll()` não filtra por tenant — exceção documentada

**Contexto**: o operador precisa ver **todos** os estabelecimentos; ele
não tem tenant para filtrar por.

**Decisão**: novo método em `organization.api`, deliberadamente sem
`TenantContext.require()` interno — diferente de `ProfessionalDirectory`/
`AvailabilityDirectory`, que sempre leem tenant da sessão. `BusinessRef`
carrega só `tenantId`, `name`, `slug`, `createdAt` — nenhum dado sensível
(sem e-mail de dono, sem senha). Único chamador desta operação é
`billing.application.BillingAccountService`, que só roda atrás do login
isolado do operador (DD-3) — nenhum caminho de um tenant chega a este
método.

**Trade-offs Accepted**: quebra, de propósito e documentado, a convenção
"toda operação de `api` lê tenant da sessão" — primeira exceção do
projeto. Aceitável porque `Business` já é "a tabela de tenants" (mesma
exceção que `BusinessRepository` já assume internamente) e o dado exposto
é o mínimo necessário para o painel.

### DD-6: `trialEndsAt` imutável ao lado de `accessValidUntil` mutável

**Contexto**: US-2 pede distinguir "em teste" de "pago" na lista do
operador; BR-2 calcula o status inteiro a partir de uma única data.

**Decisão**: `BillingAccount` guarda `trialEndsAt` (gravado uma vez no
`register()`, nunca muda) e `accessValidUntil` (começa igual a
`trialEndsAt`, muda toda vez que o operador marca uma nova data). Status
`PAID` quando `accessValidUntil` foi estendido além de `trialEndsAt`
(prova de que alguém marcou pagamento); `TRIAL` quando os dois ainda são
iguais. `GRACE_PERIOD`/`BLOCKED` usam só `accessValidUntil`, igual nos
dois casos.

**Trade-offs Accepted**: um campo a mais, que existe só para exibição —
nenhuma lógica de bloqueio depende dele.

### DD-7: "Marcar como pago" e "estender prazo" são a mesma operação

**Contexto**: a spec funcional decidiu isso explicitamente (BR-3,
Assumptions).

**Decisão**: um único endpoint, `POST /operador/estabelecimentos/{tenantId}/prazo`,
recebendo só a nova data. Sem campo "motivo" nem distinção de tipo de
ação.

**Trade-offs Accepted**: nenhum — é a spec funcional já decidida.

---

## Existing Data & Migrations

**Migration nova** (`V6__billing_create_billing_account.sql`):

```sql
CREATE TABLE billing_account (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE,
    trial_ends_at DATE NOT NULL,
    access_valid_until DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX billing_account_tenant_idx ON billing_account (tenant_id);
```

Sem FK para `business`: `billing` é outro contexto — mesmo padrão de
`catalog.ServiceOffering.professionalId` (UUID solto, garantia de
aplicação, não de banco).

**`organization.api` ganha `BusinessDirectory`** (novo método em
`BusinessRepository`: `findAllByOrderByCreatedAtAsc()`, sem filtro de
tenant — mesma exceção já documentada em `BusinessRepository`, "sem
recorte por tenant... é a única exceção do projeto").

---

## Data Model

**`BillingAccount`** (`billing.domain`, regime CRUD — entidade JPA é o
modelo):

```java
@Entity
@Table(name = "billing_account")
public class BillingAccount {
    UUID id;
    UUID tenantId;          // único; não é FK, outro contexto
    LocalDate trialEndsAt;  // imutável (DD-6)
    LocalDate accessValidUntil;
    Instant createdAt;
    Instant updatedAt;

    static BillingAccount startTrial(UUID tenantId, LocalDate registeredOn) {
        // accessValidUntil = trialEndsAt = registeredOn.plusDays(30)
    }

    void extendUntil(LocalDate newDate) {
        // valida newDate.isAfter(LocalDate.now()) -- BR invariant
    }

    AccessStatus statusOn(LocalDate today) {
        // BR-2, ver AccessStatus abaixo
    }
}
```

**`AccessStatus`** (`billing.domain`, enum):

```java
public enum AccessStatus { TRIAL, PAID, GRACE_PERIOD, BLOCKED }
```

Calculado por `BillingAccount.statusOn(LocalDate)` (BR-2):
- `today <= accessValidUntil` → `PAID` se `accessValidUntil` >
  `trialEndsAt`, senão `TRIAL`.
- `accessValidUntil < today <= accessValidUntil + 5 dias` → `GRACE_PERIOD`.
- `today > accessValidUntil + 5 dias` → `BLOCKED`.

**`organization.api.BusinessRef`** (novo, mesma forma de
`ProfessionalRef`):

```java
public record BusinessRef(UUID tenantId, String name, String slug, Instant createdAt) {}
```

---

## Cross-Context & Web Contracts

### `organization.api.BusinessDirectory` (novo)

| Método | Entrada | Saída | Tenant |
|---|---|---|---|
| `listAll` | — | `List<BusinessRef>` | **Nenhum** — deliberado (DD-5) |

### Rotas web novas

| Rota | Método | Sessão exigida | Descrição |
|---|---|---|---|
| `/operador/login` | GET/POST | nenhuma (login) | Chain própria (DD-3) |
| `/operador/painel` | GET | operador | Lista estabelecimentos + status |
| `/operador/estabelecimentos/{tenantId}/prazo` | POST | operador | BR-3: marca nova data |
| `/admin/conta-suspensa` | GET | dono (qualquer status) | Tela de bloqueio (BR-4), sempre com link de WhatsApp |

Todas as rotas `/admin/**` existentes ganham, sem nenhuma mudança de
código nelas: (1) o aviso de carência (via `BillingBannerAdvice`,
model attribute lido pelo layout) e (2) o bloqueio automático (via
`AccessGuardFilter`, antes de qualquer controller rodar).

---

## Security

- **Isolamento entre os dois logins (BR-6)**: duas `SecurityFilterChain`
  distintas, cada uma com seu próprio `securityMatcher` — Spring Security
  usa a primeira que casar com a URL; uma sessão criada numa cadeia nunca
  autentica na outra, porque os principais são de tipos diferentes
  (`AuthenticatedUser` vs. `User` padrão) e `TenantContextFilter` só
  reconhece o primeiro.
- **Conta do operador nunca por formulário (BR-7)**: credencial só em
  configuração (`application.yaml`/variável de ambiente), nunca uma
  tabela, nunca uma rota de cadastro.
- **`BusinessDirectory.listAll()` sem tenant (DD-5)**: único método do
  projeto assim — documentado, dado exposto é mínimo (nome, slug, data),
  único chamador é código que só roda atrás do login do operador.
- **CSRF**: ligado nos dois formulários novos (login do operador já vem
  de graça do Spring Security; o de marcar prazo usa o mesmo padrão
  Thymeleaf `th:action` dos formulários existentes).
- **Bloqueio falha aberto, não fechado — ao contrário do isolamento entre
  tenants.** Se `AccessGuardFilter` não conseguir calcular o status (erro
  inesperado ao consultar `BillingAccountRepository`), a requisição
  **segue normalmente**, com o erro logado em `WARN`. É o oposto da regra
  de `TenantContext.require()` (que falha fechado, porque vazar dado de
  outro tenant é o pior cenário possível). Aqui o pior cenário de falhar
  aberto é um estabelecimento pagante acessar o próprio painel por mais
  alguns minutos durante um defeito do sistema — aceitável; bloquear por
  engano quem pagou não é.

---

## Performance

`AccessGuardFilter` roda em toda requisição a `/admin/**` — uma consulta
(`BillingAccountRepository.findByTenantId`, indexado) por requisição, sem
N+1. `BillingBannerAdvice` reaproveita o resultado via atributo de
requisição, sem segunda consulta. Painel do operador faz uma leitura de
`organization.api.BusinessDirectory.listAll()` mais um `findByTenantIdIn`
em lote no `BillingAccountRepository — sem loop de consulta por
estabelecimento, mesmo padrão "grosso, em lote" de `ProfessionalDirectory`.

---

## Testing Strategy

| Camada | Arquivo | Cobre |
|---|---|---|
| Domínio puro | `BillingAccountTest` | BR-1 (trial), BR-2 (statusOn nos 4 quadrantes), BR-3 (extendUntil, validação de data futura) |
| Aplicação (mocks) | `BillingAccountServiceTest` | get-or-create, listForOperator cruzando organization.api mockado |
| Web isolada | `OperatorPanelControllerTest`, `AccessGuardFilterTest` | bloqueio, redirecionamento, CSRF |
| Integração | `OperadorBackofficeIT` (Testcontainers) | E2E-1 a E2E-5 da spec funcional |

---

## Implementation Locations

```
src/main/resources/db/migration/
└── V6__billing_create_billing_account.sql                [NOVO]

src/main/java/com/agendaia/billing/
├── package-info.java                                      [NOVO] allowedDependencies: organization::api, shared, platform
├── domain/
│   ├── BillingAccount.java                                [NOVO]
│   └── AccessStatus.java                                  [NOVO]
├── application/
│   ├── port/out/BillingAccountRepository.java              [NOVO]
│   ├── BillingAccountService.java                          [NOVO] (get-or-create, statusFor, listForOperator, extendUntil)
│   └── EstablishmentView.java                               [NOVO] (projeção para o painel)
└── adapter/in/web/
    ├── OperatorPanelController.java                         [NOVO]
    ├── AccessGuardFilter.java                                [NOVO]
    ├── BillingBannerAdvice.java                              [NOVO]
    └── SuspendedAccountController.java                       [NOVO] (GET /admin/conta-suspensa)

src/main/java/com/agendaia/organization/
├── api/
│   ├── BusinessDirectory.java                              [NOVO]
│   └── BusinessRef.java                                    [NOVO]
└── application/
    ├── BusinessDirectoryHandler.java                       [NOVO]
    └── port/out/BusinessRepository.java                    [EDITADO — novo método]

src/main/java/com/agendaia/platform/security/
└── OperatorSecurityConfig.java                              [NOVO]

src/main/resources/templates/
├── operador/login.html                                     [NOVO]
├── operador/painel.html                                    [NOVO]
└── admin/conta-suspensa.html                                [NOVO]

src/test/java/com/agendaia/
├── billing/domain/BillingAccountTest.java                  [NOVO]
├── billing/application/BillingAccountServiceTest.java      [NOVO]
├── billing/adapter/in/web/{OperatorPanelControllerTest,AccessGuardFilterTest}.java [NOVO]
└── billing/OperadorBackofficeIT.java                        [NOVO]
```

---

## References

- [ADR 0001](../../../docs/architecture/adr/0001-modular-monolith-com-contextos-como-pacotes.md) — contextos como pacotes
- [ADR 0002](../../../docs/architecture/adr/0002-clean-architecture-com-rigor-proporcional.md) — `billing` é subdomínio de suporte, regime CRUD
- [ADR 0004](../../../docs/architecture/adr/0004-multi-tenancy-por-discriminador.md) — multi-tenancy; a sessão do operador é a exceção deliberada
- `docs/domain/glossary.md` — termos banidos `Plano`/`Assinatura`/`Pagamento` (amendment necessário)
- `docs/architecture/architecture-haiku.md` — exclui gateway de pagamento e planos (amendment necessário)
- `sdd/PATTERNS.md` — regime CRUD, `allowedDependencies` como whitelist
