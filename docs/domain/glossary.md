# Glossário — Linguagem Ubíqua

> **Status: NORMATIVO.** Todo identificador no código sai daqui. Se um conceito
> do negócio não está nesta tabela, ele não deve virar classe antes de entrar.
>
> Última revisão: 2026-08-28

## Regra de idioma

**Identificadores em inglês. Interface e URLs em português.**

O ecossistema inteiro (Spring, JPA, bibliotecas) é inglês, e misturar produz
`AgendarAtendimentoUseCase` num arquivo e `BookAppointmentUseCase` no arquivo ao
lado. A rota `/admin/servicos` renderiza `ServiceController` — e este glossário é
a ponte entre as duas metades.

## Três palavras que não são sinônimos

Subdomínio, contexto delimitado e módulo são usados como se fossem a mesma
coisa, e não são. Confundi-los leva a decisões erradas de fronteira.

| | Espaço | O que é | Muda quando |
|---|---|---|---|
| **Subdomínio** | do **problema** | Uma parte do negócio. Existe mesmo que ninguém escreva software. | O negócio muda |
| **Contexto delimitado** | da **solução** | Uma fronteira de **linguagem**: dentro dela, cada termo tem um significado só. | Você reaprende o domínio |
| **Módulo** | do **código** | Unidade de empacotamento: pacote Java, módulo Maven. | Conveniência de build |

Subdomínio você **descobre** conversando com o estabelecimento. Contexto
delimitado você **decide** ao modelar. Módulo você **escolhe** ao empacotar.

### O mapeamento neste projeto

| Subdomínio | Tipo | Contexto | Módulo |
|---|---|---|---|
| Agendamento | **Core** | `scheduling` | `com.agendaia.scheduling` |
| Cadastro do estabelecimento | Suporte | `organization` | `com.agendaia.organization` |
| Identidade e acesso | Genérico | `organization` — **o mesmo** | idem |
| Catálogo de serviços | Suporte | `catalog` | `com.agendaia.catalog` |
| Cliente atendido | Suporte | `customer` | `com.agendaia.customer` |

**O mapeamento não é 1:1:1.** Dois subdomínios — cadastro e identidade — moram
num contexto só, por decisão registrada no
[ADR 0003](../architecture/adr/0003-identidade-dentro-de-organization.md).
Sempre que o mapeamento deixar de ser um-para-um, isso é decisão e vira ADR.

### A prova de que contexto é sobre linguagem

`Service` no `catalog` é um item vendável, com nome e descrição. `Service` no
`scheduling` é apenas uma duração e um identificador. **Mesma palavra, dois
significados — logo, dois contextos.** Se o significado fosse o mesmo, seria um
contexto só, e a separação seria burocracia.

É esse o teste para decidir se algo merece contexto próprio: existe uma palavra
que muda de sentido ao cruzar a fronteira?

## Contexto Organization

| Português | Código | Tipo | Definição |
|---|---|---|---|
| Empresa, estabelecimento | `Business` | Raiz de agregado | O salão, a barbearia. **É o tenant.** Dona de profissionais, serviços, clientes e agendamentos. |
| Tenant | `TenantId` | Value object (shared-kernel) | Identificador de isolamento. No MVP existe 1:1 com `Business`; o tipo separado existe para que o isolamento não dependa dessa coincidência continuar valendo. |
| Link público, apelido | `slug` | Campo de `Business` | Trecho de URL único e estável: `/b/barbearia-do-joao`. |
| Usuário | `User` | Entidade de `Business` | Quem autentica em `/admin`. No MVP, o dono. |
| Profissional | `Professional` | Raiz de agregado | Quem executa o atendimento. Pode ou não ter um `User` associado. |
| Horário de funcionamento | `BusinessOperatingHours` | Entidade de `Business` | Quando a empresa **pode** abrir. Limite externo da disponibilidade. |
| Jornada | `WorkSchedule` | Raiz de agregado | Jornada recorrente semanal do profissional, em faixas. Dado declarado, não calculado. **Almoço recorrente são duas faixas no mesmo dia** — o vão entre elas é o almoço. |
| Bloqueio, folga | `TimeOff` | Entidade de `WorkSchedule` | Indisponibilidade **excepcional e datada**. Feriado e fechamento do estabelecimento são `TimeOff` sem profissional — valem para todos. |
| Histórico de link | `BusinessSlugHistory` | Entidade de `Business` | Slug antigo, que continua resolvendo com redirecionamento. Existe porque o link já foi compartilhado e não há como recolhê-lo. |

## Contexto Catalog

| Português | Código | Tipo | Definição |
|---|---|---|---|
| Serviço | `Service` | Raiz de agregado | O conceito: "Corte de cabelo", "Barba". Só nome e descrição — **sem preço e sem duração**. |
| Oferta | `ServiceOffering` | Raiz de agregado | O serviço executado por um profissional específico, com **duração, preço e intervalo próprios**. É o que de fato se agenda. |
| Duração | `duration` | Campo de `ServiceOffering` | Tempo de atendimento. Varia por profissional. |
| Preço | `price` (`Money`) | Campo de `ServiceOffering` | Valor vigente. Varia por profissional. |
| Intervalo entre clientes | `bufferMinutes` | Campo de `ServiceOffering` | Tempo reservado **depois** do atendimento. Pode ser zero. |

## Contexto Scheduling

| Português | Código | Tipo | Definição |
|---|---|---|---|
| Agendamento | `Appointment` | Raiz de agregado | Um atendimento marcado. Guarda o **retrato** de duração e preço no momento da reserva. |
| Horário disponível | `AvailableSlot` | Value object | Um início possível, já validado contra jornada, bloqueios e agendamentos. **Calculado, nunca persistido.** |
| Disponibilidade | `Availability` | Resultado de cálculo | A lista de `AvailableSlot` para um profissional, uma oferta e uma data. |
| Grade | `slotInterval` | Configuração do tenant | Passo entre horários oferecidos. Padrão: 10 minutos. |
| Reservar, agendar | `BookAppointmentUseCase` | Caso de uso | Criar o agendamento. |
| Cancelamento | `CancelAppointmentUseCase` | Caso de uso | Libera o horário. |
| Reagendamento | `RescheduleAppointmentUseCase` | Caso de uso | Move o agendamento. Sujeito às mesmas invariantes de uma reserva nova. |
| Falta | `NO_SHOW` | Status | Cliente não compareceu. Libera o horário retroativamente. |

## Contexto Customer

| Português | Código | Tipo | Definição |
|---|---|---|---|
| Cliente | `Customer` | Raiz de agregado | A pessoa **atendida** pelo estabelecimento. Nome e telefone; sem login no MVP. O telefone, normalizado em E.164, é a chave natural dentro do tenant. |
| Anonimização | `anonymized_at` | Campo de `Customer` | Pedido de exclusão do titular (LGPD): nome e telefone são substituídos, o agendamento permanece. Ver [ADR 0011](../architecture/adr/0011-ciclo-de-vida-dos-dados.md). |
| Histórico | — | Consulta | **Não é entidade.** É uma consulta em `scheduling` filtrada por `customerId`. |

---

## Armadilhas de nomenclatura

Estas quatro já causaram inconsistência no documento de arquitetura. Estão aqui
para não voltarem.

### 1. "Cliente" é ambíguo em português — e perigosamente

- O **cliente do AgendaIA** é o barbeiro: quem paga a mensalidade. No código: `Business`.
- O **cliente do barbeiro** é quem senta na cadeira. No código: `Customer`.

Na UI e nas conversas, chame o primeiro de **estabelecimento** e reserve
"cliente" para o segundo. Em código, `Customer` nunca se refere a quem paga
a assinatura.

### 2. "Disponibilidade" tem dois significados

- **Declarada** — o que o profissional diz que trabalha: `WorkSchedule` + `TimeOff`. Mora em `organization`.
- **Calculada** — os horários realmente livres: `AvailableSlot`. Mora em `scheduling`.

**A palavra "disponibilidade" sozinha é proibida em nome de classe.** Uma é dado,
a outra é função de vários dados.

### 3. "Serviço" tem três significados, dois deles no código

- `Service` — o conceito no catálogo. Sem preço, sem duração.
- `ServiceOffering` — o que o cliente de fato agenda, com preço e duração.
- `@Service` — a anotação do Spring, que não tem relação nenhuma com o domínio.

Consequência prática: em `catalog`, classes de aplicação usam o sufixo
`UseCase`, nunca `Service` sozinho. `CreateServiceUseCase`, não `ServiceService`.

Na tela pública o cliente escolhe primeiro o `Service` (nome amigável) e depois
o profissional; a `ServiceOffering` é resolvida da combinação dos dois, e é dela
que saem o preço e a duração exibidos.

Consequência de UX, resolvida na tela e não no modelo:

- **Estabelecimento com um profissional** — o passo de escolha é pulado, a
  oferta é resolvida direto e o preço aparece já no primeiro passo.
- **Com vários** — o primeiro passo mostra faixa (`Corte · a partir de R$ 30`) e
  o valor exato aparece depois da escolha do profissional.

Todo estabelecimento tem **ao menos um `Professional`**, mesmo quando é uma
pessoa só: o dono se cadastra como profissional. Sem isso não haveria
`WorkSchedule`, nem `ServiceOffering`, nem sobre o que a exclusion constraint
discriminaria — o caso especial custaria mais que o cadastro.

### 4. "Horário" é usado para três coisas

- `BusinessOperatingHours` — quando a empresa abre.
- `WorkSchedule` — quando o profissional trabalha.
- `AvailableSlot` — um horário que o cliente pode escolher.

A disponibilidade final é a interseção: empresa ∩ profissional − bloqueios −
agendamentos, filtrada por quem cabe a duração + intervalo da oferta.

---

## Termos deliberadamente ausentes

Não existem no MVP e não devem aparecer em código sem decisão nova:

`Comissão`, `Notificação`, `Lembrete`, `ListaDeEspera`, `Encaixe`,
`Recurso`/`Sala`, `Permissão`, `Papel`.

`Plano`, `Assinatura` e `Pagamento` **saíram desta lista na TODO-009**
(back-office-operador) — ver seção "Contexto Billing" abaixo. A decisão
nova prevista aqui foi: acompanhar prazo de acesso por estabelecimento,
sem gateway de pagamento nenhum. `Assinatura` recorrente de verdade
(cobrança automática, múltiplos planos com preços) continua fora do MVP.

## Contexto Billing

| Português | Código | Tipo | Definição |
|---|---|---|---|
| Conta de cobrança | `BillingAccount` | Raiz de agregado | Uma por estabelecimento. Guarda até quando o acesso é válido — nunca dado de cartão, nunca gateway. |
| Fim do teste gratuito | `trialEndsAt` | Campo de `BillingAccount` | Gravado uma vez, no cadastro (`createdAt` + 30 dias corridos). Nunca muda depois. |
| Validade do acesso | `accessValidUntil` | Campo de `BillingAccount` | Data até quando o painel administrativo funciona sem restrição. Começa igual a `trialEndsAt`; o operador substitui por uma data nova ao marcar pagamento ou estender prazo — é a mesma ação para os dois casos. |
| Status de acesso | `AccessStatus` | Calculado, nunca persistido | `TRIAL` (nunca foi estendido), `PAID` (`accessValidUntil` já foi estendido além de `trialEndsAt`), `GRACE_PERIOD` (venceu, dentro dos 5 dias corridos de carência), `BLOCKED` (venceu a carência). |
| Operador | — | Sessão sem tenant | Quem opera o AgendaIA (não é dono de nenhum estabelecimento). Login isolado do login de dono; conta única, criada por configuração, nunca por formulário. |

**Gateway de pagamento, planos com preço/recorrência automática e múltiplos
operadores continuam fora de escopo** — ver
`docs/architecture/architecture-haiku.md`.
