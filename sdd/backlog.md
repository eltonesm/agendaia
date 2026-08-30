# Backlog

> Lista única do projeto. Item de produto e dívida técnica convivem aqui,
> etiquetados — ver `docs/README.md`, "Um backlog só".
>
> Gerenciado por `/sdd.backlog`. Um item vira feature com
> `/sdd.start --from-backlog <ID>`.

## TODO

Features do MVP, em ordem. A ordem não é arbitrária: cada uma existe porque
torna a seguinte mais barata.

| ID | Feature | Por que nesta posição |
|---|---|---|
| TODO-001 | Cadastro de estabelecimento e login | Estabelece o tenant. Sem ele nada mais tem onde morar. Exercita `TenantContext`, Spring Security e a primeira migration de verdade. |
| TODO-002 | Cadastro de profissional | A menor fatia possível já com tenant. Valida o padrão inteiro de ponta a ponta e vira o exemplo do `PATTERNS.md`. |
| TODO-003 | Cadastro de serviço e oferta | Introduz `ServiceOffering` e a referência cruzada por UUID solto, sem FK entre contextos. |
| TODO-004 | Horário do estabelecimento, jornada e bloqueios | Os dados que alimentam o cálculo. Sem eles a disponibilidade não tem de onde sair. |
| TODO-005 | Consultar horários disponíveis | O cálculo do core, ainda sem escrita. A feature mais importante do projeto e a mais barata de errar cedo. |
| TODO-006 | Página pública e agendar | A escrita: exclusion constraint, revalidação de id contra o tenant do slug, teste de concorrência. |
| TODO-007 | Confirmação com link de cancelamento | Fecha o ciclo do cliente: token assinado, `.ics`, link `wa.me`. Sem isto ele agenda e não recebe nada. |
| TODO-008 | Agenda do profissional: criar, cancelar, reagendar | Fecha o ciclo do dono. Muitos clientes vão continuar ligando. |

Depois de TODO-008 existe um produto que um barbeiro real consegue usar — que
é a definição de MVP.

### Andaime (não é feature)

| ID | Item | Nota |
|---|---|---|
| TODO-101 | Flyway, datasource e migration inicial com `btree_gist` | Deve entrar antes de TODO-001, senão a feature 1 carrega o andaime e deixa de ser fatia vertical. |
| TODO-102 | Testcontainers com a imagem do `compose.yaml` | H2 não serve: não implementa `EXCLUDE USING gist`. |
| TODO-103 | Regras de ArchUnit | Carregam sozinhas todo o peso das fronteiras desde o ADR 0001. Usar `allowEmptyShould(true)` enquanto os pacotes estiverem vazios. |
| TODO-104 | Pipeline de CI | Depois que existir teste que valha a pena rodar. |
| TODO-105 | Backup `pg_dump` com **restore testado** | Obrigatório antes do primeiro cliente pagante. Backup não restaurado não é backup. |
| TODO-106 | Compose de produção: app, banco e Caddy com TLS | Nasce junto com a VPS, para poder ser testado. |

## DEBT

| ID | Item | Origem |
|---|---|---|
| DEBT-001 | `mvnw` com CRLF não executa em Linux — marcar como LF no `.gitattributes` antes do CI | Notado no commit inicial |
| DEBT-002 | Regra de ArchUnit proibindo `org.springframework` em teste de `..domain..` | Perdeu-se a garantia que o módulo separado dava ao voltar para módulo único |
| DEBT-003 | IDE precisa apontar para JDK 21 no runtime do projeto — o toolchain vale só para o Maven | `JAVA_HOME` do usuário é JDK 17 |
| DEBT-004 | `PATTERNS.md` sem o exemplo de ponta a ponta | Marcado como pendente; entra depois de TODO-002 |
| DEBT-005 | `docs/security/threat-model.md` | Melhor escrito depois que autenticação existir |
| DEBT-006 | `docs/operations/runbook.md` | Escrever durante o deploy e o restore reais, não antes |
| DEBT-007 | Avaliar Spring Modulith para dar dentes ao ADR 0001 | Confirmar versão compatível com Boot 4.1 |
| DEBT-008 | Instrumentar ociosidade da agenda | É o gatilho de reavaliação do ADR 0006 — sem o dado, a decisão sobre encaixe dinâmico não pode ser tomada |

## IDEA

Deferidos por decisão, com o gatilho que os traria de volta.

| ID | Ideia | Gatilho |
|---|---|---|
| IDEA-001 | Contexto de notificação: confirmação e lembrete | Quando o piloto mostrar falta por esquecimento |
| IDEA-002 | Outbox | Quando existir canal externo com garantia de entrega (WhatsApp, SMS) |
| IDEA-003 | Row Level Security no Postgres | Quando houver mais de um punhado de clientes pagantes |
| IDEA-004 | `DYNAMIC_DURATION` como segunda estratégia de slot | Se DEBT-008 mostrar perda relevante de agenda |
| IDEA-005 | Serviço combinado (corte + barba num agendamento) | Pedido mais provável do ramo. `Appointment` já guarda duração própria, então a mudança é contida |
| IDEA-006 | Financeiro: pago, pendente, fiado | Se o barbeiro pedir. Começa como campo no agendamento, não como módulo |
| IDEA-007 | Lembrete e resumo diário por e-mail ao dono | Substitui o hábito do caderno sem exigir contexto de notificação |
| IDEA-008 | Branding da página pública: logo, cores, slogan | Depois da validação |
| IDEA-009 | Dashboard e relatórios | Depois da validação |
| IDEA-010 | Lista de espera e encaixe | Depois da validação |
| IDEA-011 | Extrair `iam` de `organization` | Gatilho no ADR 0003: SSO, múltiplos usuários com permissões distintas, ou login do cliente final |
| IDEA-012 | Promover contextos a módulos Maven | Gatilho no ADR 0001: mais de três pessoas, extração de serviço, ou ArchUnit sendo ignorado |

## Last Updated

2026-08-29
