# Mapa de Contextos

> Escrito em 2026-08-29. Documento de contexto. Os nomes são normativos e vêm
> do [glossário](../domain/glossary.md).

Seis contextos, todos pacotes sob `com.agendaia` num único módulo Maven
([ADR 0001](adr/0001-modular-monolith-com-contextos-como-pacotes.md)).

```
                        ┌──────────────────┐
                        │   organization   │
                        │  Business=tenant │
                        │  User            │
                        │  Professional    │
                        │  WorkSchedule    │
                        │  TimeOff         │
                        └────────┬─────────┘
                                 │ jornada, profissional, tenant
                                 │ (api, síncrono)
                                 ▼
  ┌──────────────┐      ┌──────────────────┐      ┌──────────────┐
  │   catalog    │─────▶│    scheduling    │◀─────│   customer   │
  │  Service     │      │  ★ CORE DOMAIN   │      │  Customer    │
  │  Service     │ dura-│  Appointment     │ cli- │              │
  │  Offering    │ ção, │  AvailableSlot   │ ente │              │
  └──────────────┘ preço│  políticas       │      └──────────────┘
                        └────────┬─────────┘
                                 │ AppointmentBooked (evento, sem
                                 │ consumidor no MVP)
                                 ▼
                          (notification — futuro)

  ┌────────────────────────────────────────────────────────────┐
  │  shared    tipos puros: TenantId, Money, TimeRange          │
  │  platform  TenantContext, erro global, segurança            │
  │            atravessam todos os contextos                    │
  └────────────────────────────────────────────────────────────┘
```

## Classificação dos subdomínios

| Contexto | Tipo | Regime de arquitetura |
|---|---|---|
| `scheduling` | **Core** | Completo: domínio puro, entidade JPA separada, mapeamento explícito |
| `organization` | Suporte | CRUD: a entidade JPA é o modelo |
| `catalog` | Suporte | CRUD |
| `customer` | Suporte | CRUD, deliberadamente magro |
| `shared` | Kernel compartilhado | Java puro, sem dependência |
| `platform` | Genérico / técnico | Infraestrutura, sem regra de negócio |

A assimetria é decisão registrada no
[ADR 0002](adr/0002-clean-architecture-com-rigor-proporcional.md): core domain
merece investimento, subdomínio de suporte merece CRUD.

## Relações

| De | Para | Tipo | O que atravessa |
|---|---|---|---|
| `scheduling` | `organization` | Consulta síncrona pela `api` | Jornada do profissional, horário do estabelecimento, bloqueios |
| `scheduling` | `catalog` | Consulta síncrona pela `api` | Duração, preço e intervalo da oferta |
| `scheduling` | `customer` | Consulta síncrona pela `api` | Identidade do cliente atendido |
| `scheduling` | (futuro) | Evento de domínio | `AppointmentBooked`, `AppointmentCancelled` |
| `catalog` | `organization` | Referência por id | `ServiceOffering.professionalId` — UUID solto, sem FK |

## As regras da fronteira

- **Só o pacote `api` é público.** Nenhum contexto importa `domain`,
  `application` ou `adapter` de outro.
- **O que sai por `api` é `record` imutável.** Tipo de domínio interno não
  atravessa.
- **Sem JOIN entre contextos.** Referência cruzada é UUID solto.
- **Leitura é chamada, escrita é evento.** Nenhuma transação escreve em dois
  contextos.
- **Sem ciclos.** Se `catalog` precisar consultar `scheduling`, a modelagem está
  errada.

Nada disso é garantido pelo compilador: são pacotes no mesmo classpath. **Só o
ArchUnit sustenta essas cinco regras** — é a consequência aceita no ADR 0001 e
o motivo de as regras serem parte da definição de pronto.

## Por que não existe um contexto IAM

Neste produto o tenant **é** o estabelecimento: não há Barbearia do João sem
tenant nem tenant sem Barbearia do João. Separar produziria duas raízes de
agregado para uma entidade do mundo real, e o cadastro de conta — que grava
estabelecimento e usuário de uma vez — viraria escrita atravessando contexto.

Registrado no [ADR 0003](adr/0003-identidade-dentro-de-organization.md), com o
gatilho que justificaria a extração.
