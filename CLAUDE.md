# AgendaIA

SaaS de agendamento online para barbearias, salões e profissionais que atendem
por horário. Cada estabelecimento tem sua conta, seus dados e um link público
(`/b/{slug}`) que compartilha com os clientes para que agendem sozinhos.

Multi-tenant desde o primeiro dia. Fase atual: **MVP**, validação com um
estabelecimento piloto.

## Comandos

```bash
./mvnw verify                 # build completo com testes — condição de merge
./mvnw spring-boot:run        # sobe a aplicação
docker compose up -d          # sobe o PostgreSQL de desenvolvimento
docker compose down -v        # derruba e apaga o volume
```

`JAVA_HOME` pode apontar para qualquer JDK: o `maven-toolchains-plugin`
seleciona um JDK 21 sozinho.

## Contextos

Um único módulo Maven. Os contextos delimitados são pacotes sob `com.agendaia`:

| Pacote | Responsabilidade |
|---|---|
| `shared` | Tipos puros compartilhados: `TenantId`, `Money`, `TimeRange` |
| `platform` | Infraestrutura transversal: contexto de tenant, erro global, segurança |
| `organization` | `Business` (é o tenant), `User`, `Professional`, `WorkSchedule`, `TimeOff` |
| `catalog` | `Service` e `ServiceOffering` (serviço por profissional, com preço e duração próprios) |
| `scheduling` | **Core domain**: disponibilidade, `Appointment`, cancelamento, reagendamento |
| `customer` | Cliente atendido pelo estabelecimento |

## Regras fundamentais

Estas quebram o build ou o produto. O resto está em `sdd/PATTERNS.md`.

- **`scheduling.domain` é Java puro.** Nada de `org.springframework` ou
  `jakarta.persistence` ali dentro.
- **Contextos só se falam pelo pacote `api` um do outro.** Nunca importar
  `domain`, `application` ou `adapter` de outro contexto. Nunca `JOIN` entre
  tabelas de contextos diferentes.
- **`tenantId` nunca vem do cliente.** Vem da sessão autenticada em `/admin/**`
  ou do slug em `/b/{slug}/**`. Todo id recebido de formulário público é
  revalidado contra o tenant do slug.
- **Overbooking é impedido pelo banco**, não pela aplicação: exclusion constraint
  com `tstzrange` e limites `[)`. Validação em memória é feedback, não garantia.
- **Sem API REST.** Renderização server-side com Thymeleaf. Endpoints internos
  que devolvem fragmento HTML são a exceção, não o padrão.

## Onde está o resto

| Assunto | Arquivo |
|---|---|
| Nomes — **normativo** | [`docs/domain/glossary.md`](docs/domain/glossary.md) |
| Convenções de time | [`sdd/PROJECT.md`](sdd/PROJECT.md) |
| Padrões de código | [`sdd/PATTERNS.md`](sdd/PATTERNS.md) |
| Por que cada decisão | [`docs/architecture/adr/`](docs/architecture/adr/) |
| Como a documentação se organiza | [`docs/README.md`](docs/README.md) |

## SDD Kit

@sdd-kit/CLAUDE.md

O arquivo acima descreve o framework — comandos, fluxo e estrutura. Abaixo, só
o que é específico deste projeto.

### Spec Language

All specifications MUST be written in **Portuguese (Português)** (`pt`).
Do not mix languages in specs. Technical terms (API, REST, CRUD) stay in English.

Identificadores de código são em **inglês** — ver
[`docs/domain/glossary.md`](docs/domain/glossary.md), que é normativo.

### Específico daqui

- Convenções do time: [`sdd/PROJECT.md`](sdd/PROJECT.md) · Padrões de código:
  [`sdd/PATTERNS.md`](sdd/PATTERNS.md) · Backlog: [`sdd/backlog.md`](sdd/backlog.md)
- Nunca criar arquivo sob `sdd/wip/` ou `sdd/features/` à mão — sempre pelo
  `/sdd.start`, que mantém o `state.json` coerente.
- Andaime de infraestrutura não passa pelo ciclo SDD: não tem regra de negócio
  nem critério de aceite de produto. Entra como commit direto.
