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

This project uses **SDD Kit** for spec-driven development.

### Spec Language

All specifications MUST be written in **Portuguese (Português)** (`pt`).
Do not mix languages in specs. Technical terms (API, REST, CRUD) stay in English.

### Quick Reference

- Framework expert: `Skill("sdd-kit-expert")`
- Workflow: `/sdd.start` → `/sdd.spec` → `/sdd.plan` → `/sdd.build` → `/sdd.finish`
- Project conventions: `sdd/PROJECT.md`
- Discovered patterns: `sdd/PATTERNS.md`

### Rules

- Never create files under `sdd/specs/`, `sdd/wip/`, or `sdd/features/` manually
- Always go through the `/sdd.start` workflow
- Respect the phased workflow — don't skip phases
