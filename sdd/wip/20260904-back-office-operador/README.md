# back-office-operador (TODO-009)

## O que foi construído

Sétimo contexto delimitado do projeto — `billing` — e um papel de usuário
inédito: o **operador da plataforma** (o dono do AgendaIA), que não é
tenant de nenhum estabelecimento e enxerga todos eles. Cobre o pedido
original: acompanhar quantos estabelecimentos existem, quem pagou, quem
está em trial, e liberar isso já no MVP com 30 dias de teste grátis.

```
Cadastro do Business → BillingAccount.startTrial() sob demanda
    → trialEndsAt = accessValidUntil = createdAt + 30 dias
    → AccessGuardFilter calcula status a cada requisição /admin/**
        TRIAL/PAID (dentro do prazo) → segue normal
        GRACE_PERIOD (até 5 dias vencido) → banner de aviso, sem bloquear
        BLOCKED (mais de 5 dias vencido) → redireciona a /admin/conta-suspensa
    → operador marca prazo novo a qualquer momento (Pix recebido por fora)
```

## O que nasceu nesta feature

| Item | Onde | Por quê |
|---|---|---|
| `billing` (contexto novo) | `billing` | Sétimo módulo — `BillingAccount`, `AccessStatus`, `BillingAccountService`, guarda e banner de acesso |
| `organization.api.BusinessDirectory` | `organization` | Segundo par de métodos sem tenant implícito: `listAll()` (só para o operador) e `find(UUID)` (get-or-create de um tenant) |
| `platform.security.OperatorSecurityConfig` | `platform` | Segunda cadeia de autenticação (`@Order(1)`, `securityMatcher("/operador/**")`), credencial única por configuração |
| `AccessGuardFilter` / `BillingBannerAdvice` | `billing.adapter.in.web` | Mecanismo transversal que decide bloqueio — mora em `billing`, não em `platform`, porque é regra de negócio (DD-4) |

## Decisões de design centrais

- **`accessValidUntil` como linha do tempo única** (DD-6/DD-7) — trial,
  pagamento e carência são todos a mesma data comparada com hoje;
  `trialEndsAt` fica imutável só para distinguir TRIAL de PAID
  (`accessValidUntil > trialEndsAt` prova que alguém marcou pagamento).
- **Cálculo sob demanda, sem job agendado** — `statusOn(LocalDate)` roda a
  cada requisição a `/admin/**`; não existe `@Scheduled` nem infraestrutura
  nova só para notar que um prazo venceu.
- **Falha aberta em `AccessGuardFilter`** (Security, spec técnica) — o
  oposto de `TenantContext.require()`: um erro ao calcular status deixa a
  requisição seguir, com log em `WARN`. Bloquear por engano quem pagou é
  pior que uma falha temporária deixar passar.
- **DD-5 atualizado durante o `TASK-005`**: `BusinessDirectory.find(UUID)`
  foi adicionado além de `listAll()` — o get-or-create de um único tenant
  não devia depender de carregar todos os estabelecimentos.
- **Achado de segurança durante o `TASK-012`**: as duas cadeias de
  segurança compartilham o mesmo `SecurityContextRepository` (necessário
  para o cadastro autenticar a sessão logo após o registro) e checavam só
  `authenticated()`, não a role — uma sessão de dono passaria pelo gate do
  operador (e veria a lista de todos os tenants) e vice-versa. Corrigido
  para `hasRole("OWNER")`/`hasRole("OPERATOR")` antes do arquivamento,
  confirmado por `OperadorBackofficeIT.e2e5` (403 nas duas direções).

## Testes

- Domínio puro: `BillingAccountTest` (10 casos — `startTrial`,
  `extendUntil`, os 4 quadrantes de `statusOn` nos limites exatos).
- Aplicação (mocks): `BillingAccountServiceTest` (6 casos — get-or-create
  sem duplicar, `listForOperator` em lote).
- Camada web: `OperatorPanelControllerTest` (6 casos) e
  `AccessGuardFilterTest` (7 casos — os 4 status, falha aberta,
  `/admin/conta-suspensa` nunca redireciona para si mesma).
- Integração (Testcontainers): `OperadorBackofficeIT` (5 casos, E2E-1 a
  E2E-5 ponta a ponta, incluindo o isolamento operador/dono).
- 34 testes novos nesta feature; 90% de cobertura de instrução no
  projeto inteiro (`./mvnw clean verify`).

## Quality gates (Layer 3)

Todos `APPROVED` — ver `verdicts/{code_review,performance,security}.json`.

- **Code review**: DD-1 a DD-7 confirmadas no código; `platform` continua
  sem regra de negócio; sem setter público além de `extendUntil`; sem
  sufixo `Impl`; `@Transactional` só em `application`.
- **Performance**: `AccessGuardFilter` faz no máximo uma consulta indexada
  por requisição; `listForOperator` busca em lote (`findByTenantIdIn`),
  sem N+1; `BillingBannerAdvice` reaproveita o atributo de requisição do
  filtro. Uma recomendação menor (não bloqueante): o índice explícito
  sobre `tenant_id` em `billing_account` é redundante com a `UNIQUE` que
  o Postgres já cria.
- **Security**: isolamento operador/dono por role (BR-6); conta do
  operador só por configuração, nunca por formulário (BR-7); CSRF ativo
  em todos os formulários novos; sem segredo hardcoded.
