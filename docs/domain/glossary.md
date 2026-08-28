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

## Contexto Organization

| Português | Código | Tipo | Definição |
|---|---|---|---|
| Empresa, estabelecimento | `Business` | Raiz de agregado | O salão, a barbearia. **É o tenant.** Dona de profissionais, serviços, clientes e agendamentos. |
| Tenant | `TenantId` | Value object (shared-kernel) | Identificador de isolamento. No MVP existe 1:1 com `Business`; o tipo separado existe para que o isolamento não dependa dessa coincidência continuar valendo. |
| Link público, apelido | `slug` | Campo de `Business` | Trecho de URL único e estável: `/b/barbearia-do-joao`. |
| Usuário | `User` | Entidade de `Business` | Quem autentica em `/admin`. No MVP, o dono. |
| Profissional | `Professional` | Raiz de agregado | Quem executa o atendimento. Pode ou não ter um `User` associado. |
| Horário de funcionamento | `BusinessOperatingHours` | Entidade de `Business` | Quando a empresa **pode** abrir. Limite externo da disponibilidade. |
| Jornada | `WorkSchedule` | Raiz de agregado | Jornada recorrente semanal do profissional. Dado declarado, não calculado. |
| Bloqueio, folga, almoço | `TimeOff` | Entidade de `WorkSchedule` | Indisponibilidade **excepcional** e datada. Feriado é um `TimeOff` de dia inteiro. |

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
| Cliente | `Customer` | Raiz de agregado | A pessoa **atendida** pelo estabelecimento. Nome e telefone; sem login no MVP. |
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

`Plano`, `Assinatura`, `Pagamento`, `Comissão`, `Notificação`, `Lembrete`,
`ListaDeEspera`, `Encaixe`, `Recurso`/`Sala`, `Permissão`, `Papel`.
