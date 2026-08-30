# Modelo Conceitual de Dados

> Escrito em 2026-08-30. Documento de contexto.
>
> É o modelo **conceitual** — os fatos duráveis do negócio, o que identifica cada
> coisa e o que não pode ser violado. O modelo **físico** (tipos exatos, DDL) não
> é documentado: as migrations Flyway são a fonte da verdade, e um documento
> paralelo mentiria já na terceira migration.
>
> Nomes vêm do [glossário](glossary.md), que é normativo.

## Convenções que valem para tudo

- **Identidade**: UUIDv7 gerado na aplicação ([ADR 0009](../architecture/adr/0009-uuidv7-como-identificador.md)).
- **Tenant**: `tenant_id` obrigatório em toda tabela de negócio ([ADR 0004](../architecture/adr/0004-multi-tenancy-por-discriminador.md)). `business.id` *é* o `tenant_id`.
- **Tempo**: `timestamptz` em UTC. Horário local só para exibição e para regra de jornada.
- **Nada de negócio é apagado** ([ADR 0011](../architecture/adr/0011-ciclo-de-vida-dos-dados.md)): o que sai de circulação recebe `active = false`; dado pessoal removido a pedido é anonimizado.
- **Auditoria**: `created_at` e `updated_at` em toda tabela.
- **Sem FK entre contextos**: referência cruzada é UUID solto, validada no caso de uso.

---

## Contexto Organization

### Business — raiz de agregado, *é* o tenant

| campo | nota |
|---|---|
| `id` | serve como `tenant_id` nas demais tabelas |
| `name` | |
| `slug` | **único global**. Mutável, com histórico — ver `BusinessSlugHistory` |
| `description`, `phone`, `address` | exibidos na página pública |
| `timezone` | IANA (`America/Sao_Paulo`). Dado, não constante |
| `active` | suspensão por inadimplência |

### BusinessSlugHistory — entidade de Business

| campo | nota |
|---|---|
| `slug`, `business_id`, `replaced_at` | slug antigo continua resolvendo, com redirecionamento |

Existe porque o dono compartilha o link por WhatsApp e não tem como recolhê-lo.
A resolução de tenant consulta o slug atual e, não achando, o histórico.

### User

| campo | nota |
|---|---|
| `email` | **único global** — um e-mail, uma conta, um estabelecimento |
| `password_hash` | BCrypt |
| `name`, `role`, `active` | `role` = `OWNER` no MVP |

### Professional — raiz de agregado

| campo | nota |
|---|---|
| `name`, `active` | desativado some da página pública; agendamentos passados permanecem |
| `user_id` | opcional e nulo no MVP — só o dono autentica |

### BusinessOperatingHours — entidade de Business

`day_of_week` + `opens_at` + `closes_at`, em **hora local**.

**Várias faixas por dia são permitidas.** Dia sem nenhuma linha é dia fechado.

### WorkSchedule — raiz de agregado

Jornada recorrente do profissional: `professional_id`, `day_of_week`,
`starts_at`, `ends_at`, em hora local.

**Almoço recorrente é modelado como duas faixas**, não como `TimeOff`:

```
segunda  08:00 → 12:00
segunda  13:00 → 18:00     ← o intervalo entre as faixas É o almoço
```

### TimeOff — entidade de WorkSchedule

Indisponibilidade **excepcional e datada**: `starts_at`, `ends_at` (timestamptz),
`reason` (opcional).

`professional_id` **anulável**: nulo significa que vale para o estabelecimento
inteiro. É assim que feriado e fechamento para reforma são representados, sem
tabela nova.

---

## Contexto Catalog

### Service — raiz de agregado

`name`, `description`, `active`. **Sem preço e sem duração** — eles pertencem à
oferta.

### ServiceOffering — raiz de agregado

O que o cliente de fato agenda.

| campo | nota |
|---|---|
| `service_id` | mesmo contexto — FK normal |
| `professional_id` | **outro contexto** — UUID solto, sem FK |
| `duration_minutes` | varia por profissional |
| `price` | varia por profissional |
| `buffer_minutes` | intervalo após o atendimento. Pode ser zero |
| `active` | |

Único por `(tenant_id, service_id, professional_id)` — um profissional tem no
máximo uma oferta de cada serviço.

---

## Contexto Customer

### Customer — raiz de agregado

| campo | nota |
|---|---|
| `name` | |
| `phone` | normalizado em E.164 (`+5511987654321`) |
| `anonymized_at` | preenchido quando o titular pede exclusão (LGPD) |

**Único por `(tenant_id, phone)`.** O telefone é a chave natural dentro do
tenant: é por ele que o fluxo público localiza ou cria o cliente.

A mesma pessoa atendida em duas barbearias são **dois `Customer`**, um por
tenant. É consequência direta do isolamento e está correto — nenhum
estabelecimento enxerga a base do outro.

---

## Contexto Scheduling

### Appointment — raiz de agregado, o coração do sistema

| campo | nota |
|---|---|
| `professional_id`, `customer_id`, `service_offering_id` | UUIDs soltos — todos de outros contextos |
| `service_name`, `duration_minutes`, `price`, `buffer_minutes` | **retrato** do momento da reserva |
| `starts_at`, `ends_at` | o que o cliente vê |
| `blocked_until` | `ends_at` + intervalo. **É este que a exclusion constraint usa** |
| `status` | ver máquina de estados abaixo |
| `origin` | `PUBLIC_LINK` ou `ADMIN` |
| `public_token` | UUID aleatório do link de cancelamento do cliente |
| `cancelled_at`, `cancellation_reason` | |
| `version` | bloqueio otimista — protege o reagendamento |

**Por que `blocked_until` é coluna separada:** o cliente precisa ver "10:00 às
10:30", mas o horário fica bloqueado até 10:40 se houver intervalo de 10 min.
Guardar só `ends_at` obrigaria a constraint a calcular o intervalo em uma
expressão; guardar só `blocked_until` mostraria ao cliente um fim que não
existe. Duas colunas, cada uma com um dono claro.

### Máquina de estados

```
SCHEDULED ──▶ CONFIRMED ──▶ COMPLETED
    │             │
    │             ├──▶ NO_SHOW
    └─────────────┴──▶ CANCELLED
```

Ocupam horário: `SCHEDULED`, `CONFIRMED`.
Liberam: `CANCELLED`, `NO_SHOW`, `COMPLETED`.

> `COMPLETED` liberar é intencional: o atendimento terminou, e um horário no
> passado não precisa continuar bloqueado para a constraint.

---

## Invariantes

Estas não podem ser violadas em nenhuma hipótese.

| # | Invariante | Onde é garantida |
|---|---|---|
| 1 | Dois agendamentos ativos do mesmo profissional não se sobrepõem | **Banco** — exclusion constraint sobre `[starts_at, blocked_until)` |
| 2 | `ends_at > starts_at` e `blocked_until >= ends_at` | Domínio + check constraint |
| 3 | Duração e preço do agendamento são os do momento da reserva | Domínio — colunas próprias, não derivadas |
| 4 | Todo agendamento está dentro do horário da empresa ∩ jornada do profissional | Domínio |
| 5 | Nenhum agendamento cai dentro de um `TimeOff` vigente | Domínio |
| 6 | Transição de status segue a máquina acima | Domínio — sem `setStatus()` público |
| 7 | Todo id vindo de formulário público pertence ao tenant do slug | Aplicação |
| 8 | Um profissional tem no máximo uma oferta por serviço | Banco — unique |
| 9 | Um telefone identifica um cliente dentro do tenant | Banco — unique |

Só a 1, a 8 e a 9 têm garantia física. As demais dependem de teste — é a
consequência aceita nos ADRs 0001 e 0010.

---

## Padrões de acesso

A ordem importa: é o que define quais índices existem.

| # | Consulta | Frequência | Índice |
|---|---|---|---|
| 1 | Agendamentos de um profissional num dia — **cálculo de disponibilidade** | altíssima | `(tenant_id, professional_id, starts_at)`, e o índice GiST da exclusion constraint |
| 2 | Estabelecimento por slug — toda visita à página pública | alta | único em `slug`; fallback no histórico |
| 3 | Agenda do dia no painel | alta | `(tenant_id, starts_at)` |
| 4 | Ofertas ativas de um serviço | alta | `(tenant_id, service_id) WHERE active` |
| 5 | Localizar cliente por telefone | média | único em `(tenant_id, phone)` |
| 6 | Histórico de um cliente | baixa | `(tenant_id, customer_id, starts_at DESC)` |
| 7 | Agendamento pelo token público | baixa | único em `public_token` |

**Todo índice começa por `tenant_id`**, exceto os globais por natureza (`slug`,
`email`, `public_token`).

### Volume esperado

Um estabelecimento com 3 profissionais e 10 atendimentos por dia gera ~9 mil
agendamentos por ano. Cem estabelecimentos, ~900 mil. **Nenhum problema para o
Postgres numa VPS** — o que confirma que desempenho não entra na lista de
atributos de qualidade do
[architecture haiku](../architecture/architecture-haiku.md).

---

## O que deliberadamente não modelamos

`Payment`, `Subscription`, `Plan`, `Commission`, `Notification`, `Reminder`,
`WaitingList`, `Resource` (sala, cadeira), `Permission`.

Cada um tem gatilho registrado no [backlog](../../sdd/backlog.md). O que existe
hoje é o mínimo que sustenta as oito features do MVP.
