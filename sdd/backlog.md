# Backlog

> Lista única do projeto — item de produto e dívida técnica convivem aqui,
> etiquetados. Ver `docs/README.md`, "Um backlog só".
>
> Gerenciado por `/sdd.backlog`: `add` para incluir, `pick <ID>` para virar
> feature, `resolve <ID>` para encerrar. Itens encerrados não são apagados —
> migram para "Resolved Items" com o motivo, para não voltarem daqui a três
> meses sem que ninguém lembre por que foram descartados.

## Ordem de execução

A numeração agrupa por natureza; a ordem de execução é outra e não é capricho —
cada item existe porque torna o seguinte mais barato:

```
andaime          ✅ concluído — Fase 0 fechada em 2026-08-30
features do MVP  TODO-001 → 002 → 003 → 004 → 005 → 006 → 007 → 008
produção         TODO-106 (quando existir VPS)
```

O andaime veio antes da primeira feature de propósito: se a `TODO-001` tivesse
de montar Flyway e Testcontainers junto com o cadastro, ela deixaria de ser
fatia vertical — que é a regra do `PATTERNS.md`.

O andaime **não passou pelo ciclo SDD**: não tem regra de negócio nem critério
de aceite de produto. Entrou como commit direto. Da `TODO-001` em diante, tudo
passa por `/sdd.start`.

---

## 📋 TODOs

### TODO-001: Cadastro de estabelecimento e login
- **Priority**: High
- **Status**: in-progress
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural
- **Context**: Estabelece o tenant — sem ele nada mais tem onde morar. Exercita `TenantContext`, Spring Security e a primeira migration de verdade. Inclui a escolha do slug, com lista de palavras reservadas.
- **Affected Files**: `organization`, `platform`
- **Complexity**: High

---

### TODO-002: Cadastro de profissional
- **Priority**: High
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural
- **Context**: A menor fatia possível já com tenant. Valida o padrão inteiro de ponta a ponta e é candidata a virar o exemplo pendente do `PATTERNS.md` (DEBT-004). Todo estabelecimento tem ao menos um profissional, mesmo sendo uma pessoa só.
- **Affected Files**: `organization`
- **Complexity**: Medium

---

### TODO-003: Cadastro de serviço e oferta
- **Priority**: High
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural
- **Context**: Introduz `ServiceOffering` — serviço por profissional, com duração, preço e intervalo próprios. Primeira referência cruzando contexto por UUID solto, sem chave estrangeira.
- **Affected Files**: `catalog`
- **Complexity**: Medium

---

### TODO-004: Horário do estabelecimento, jornada e bloqueios
- **Priority**: High
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural
- **Context**: Os dados que alimentam o cálculo de disponibilidade. `BusinessOperatingHours`, `WorkSchedule` e `TimeOff`. Feriado é um `TimeOff` de dia inteiro. Sem estes dados a disponibilidade não tem de onde sair.
- **Affected Files**: `organization`
- **Complexity**: Medium

---

### TODO-005: Consultar horários disponíveis
- **Priority**: High
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural
- **Context**: O cálculo do core, ainda sem escrita. Interseção de horário da empresa com jornada, menos bloqueios e agendamentos, filtrada por quem comporta duração mais intervalo. Grade fixa de 10 min (ADR 0006). É a feature mais importante do projeto e a mais barata de errar cedo.
- **Affected Files**: `scheduling`
- **Complexity**: High

---

### TODO-006: Página pública e agendar
- **Priority**: High
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural
- **Context**: A escrita. Exclusion constraint (ADR 0005), revalidação de todo id do formulário contra o tenant do slug, teste de concorrência com duas reservas simultâneas. Inclui as defesas contra abuso: honeypot, rate limit e teto por telefone.
- **Affected Files**: `scheduling`, `customer`, `platform`
- **Complexity**: High

---

### TODO-007: Confirmação com link de cancelamento
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural — risco D-01
- **Context**: Fecha o ciclo do cliente: token assinado para ver e cancelar, arquivo `.ics` e link `wa.me` pré-preenchido. Sem isto o cliente agenda e não recebe nada, e volta a perguntar por WhatsApp — que é o problema que o produto existe para resolver.
- **Affected Files**: `scheduling`, `platform`
- **Complexity**: Medium

---

### TODO-008: Agenda do profissional — criar, cancelar, reagendar
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural
- **Context**: Fecha o ciclo do dono. Muitos clientes vão continuar ligando, então o agendamento manual é requisito, não conveniência. Depois desta feature existe um produto que um barbeiro real consegue usar.
- **Affected Files**: `scheduling`
- **Complexity**: High

---

### TODO-106: Compose de produção com TLS
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural — risco T-08
- **Context**: App, banco e Caddy com certificado automático. Nasce junto com a VPS, para poder ser testado de verdade. Link público em HTTP não fecha: coleta nome e telefone em claro.
- **Affected Files**: `compose.prod.yaml`
- **Complexity**: Low

---

### TODO-108: Observabilidade — log estruturado e métricas
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-30
- **Origin**: convenções do time
- **Context**: Log em JSON com `tenantId` e `requestId` no MDC, saindo em toda linha da requisição. Actuator com `/health` e `/prometheus`, ambos protegidos. Métricas de negócio junto com as técnicas: agendamentos criados, cancelados e falhas por conflito de horário. Proibido logar telefone e nome de cliente (LGPD).
- **Affected Files**: `platform`, `application.yaml`
- **Complexity**: Medium

---

### TODO-109: Recuperação de senha
- **Priority**: High
- **Status**: pending
- **Created**: 2026-08-30
- **Origin**: TODO-001 — declarado fora de escopo na spec funcional
- **Context**: Sem isto, o dono que esquecer a senha perde o acesso ao próprio negócio e a única saída é alterar o hash direto no banco. Aceitável enquanto o piloto é um barbeiro conhecido; inaceitável no segundo cliente. Depende de e-mail transacional, que o projeto ainda não tem — a mesma dependência que manteve a confirmação de e-mail fora da TODO-001.
- **Affected Files**: `organization`, `platform`
- **Complexity**: Medium

---

## 🔧 Technical Debt

### DEBT-010: Plugin do Modulith na IDE acusa falso positivo em platform -> shared
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-30
- **Origin**: TASK-005 do build
- **Context**: A IDE marca `MODULITH_TYPE_REF_VIOLATION` em toda referencia de `platform` a `TenantId` e `DomainException`, que estao na raiz do modulo `shared` e portanto sao API publica. O `ModuleStructureTest` passa — o plugin da IDE nao esta honrando `@ApplicationModule(type = OPEN)`. Risco real: erro vermelho constante treina o olho a ignorar diagnostico da IDE.
- **Affected Files**: configuracao da IDE
- **Complexity**: Low
- **Risk if Ignored**: Diagnostico verdadeiro passa despercebido no meio do ruido

---

### DEBT-009: Skill java-spring-expert mira Spring Boot 3.x
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-30
- **Origin**: spec técnica da TODO-001
- **Context**: O skill instalado declara "Java 17+, Spring Boot 3.x". O projeto é Java 21 e Boot 4.1.1. Carregá-lo injetaria orientação de Boot 3 — que já custou três erros no andaime: nome de starter, modularização das auto-configurações do Flyway e renomeação dos módulos do Testcontainers. Foi deliberadamente **não** carregado na spec técnica. Atualizar o skill, ou desativá-lo neste projeto para não ser invocado por engano no `/sdd.build`.
- **Affected Files**: `.claude/skills/java-spring-expert/SKILL.md`
- **Complexity**: Low
- **Risk if Ignored**: O `/sdd.build` invoca o skill por gatilho de palavra-chave e gera código com API de Boot 3

---

### DEBT-003: IDE precisa apontar para JDK 21
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-28
- **Origin**: configuração da máquina
- **Context**: O `maven-toolchains-plugin` resolve o JDK para o Maven, mas não para o compilador interno da IDE. `JAVA_HOME` do usuário é JDK 17 por causa de outros projetos.
- **Affected Files**: configuração local
- **Complexity**: Low
- **Risk if Ignored**: Erro de compilação só na IDE, confuso de diagnosticar

---

### DEBT-004: PATTERNS.md sem o exemplo de ponta a ponta
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: `sdd/PATTERNS.md`
- **Context**: A seção está marcada como pendente de propósito — inventar exemplo antes de existir código que compila seria pior que não ter. Preencher com um caso de uso real depois da TODO-002.
- **Affected Files**: `sdd/PATTERNS.md`
- **Complexity**: Low
- **Risk if Ignored**: O agente copia padrão de prosa em vez de código, com mais variação

---

### DEBT-005: Threat model não escrito
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural
- **Context**: Isolamento entre tenants, IDOR no fluxo público, rate limit, LGPD. Melhor escrito depois que autenticação existir, contra código real. Num SaaS com dado pessoal pode ser preciso mostrá-lo.
- **Affected Files**: `docs/security`
- **Complexity**: Medium
- **Risk if Ignored**: Defesas decididas caso a caso, sem visão do conjunto

---

### DEBT-006: Runbook de operação não escrito
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural
- **Context**: Deploy, restore de backup, resposta a incidente. Escrever **durante** o deploy e o restore reais — runbook escrito antes do procedimento é ficção.
- **Affected Files**: `docs/operations`
- **Complexity**: Medium
- **Risk if Ignored**: Procedimento de restore descoberto às duas da manhã

---

### DEBT-008: Ociosidade da agenda não instrumentada
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: ADR 0006 — gatilho de reavaliação
- **Context**: Somar diariamente os intervalos livres invendáveis entre atendimentos. Sem esse dado, a decisão sobre encaixe dinâmico (IDEA-004) não pode ser tomada — vira opinião.
- **Affected Files**: `scheduling`
- **Complexity**: Medium
- **Risk if Ignored**: O gatilho do ADR 0006 nunca dispara por falta de dado

---

## 💡 Ideas

### IDEA-001: Contexto de notificação — confirmação e lembrete
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: escopo deliberadamente excluído do MVP
- **Context**: Cortado por não ter consumidor no MVP. `AppointmentBooked` já é registrado no agregado, então o contexto nasce com evento pronto.
- **Potential Impact**: Redução de falta
- **Notes**: Gatilho — quando o piloto mostrar falta por esquecimento

---

### IDEA-002: Outbox para entrega confiável de evento
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: escopo deliberadamente excluído do MVP
- **Context**: Sem canal externo, outbox é cerimônia sem nada para entregar.
- **Potential Impact**: Confiabilidade
- **Notes**: Gatilho — quando existir canal externo com garantia de entrega (WhatsApp, SMS)

---

### IDEA-003: Row Level Security no Postgres
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: ADR 0004
- **Context**: Rede de segurança sob o filtro da aplicação. Policy sobre `current_setting('app.tenant_id')`, setado por transação. Com RLS, nem um bug de aplicação vaza dado entre clientes.
- **Potential Impact**: Segurança
- **Notes**: Gatilho — mais de um punhado de clientes pagantes

---

### IDEA-004: DYNAMIC_DURATION como segunda estratégia de slot
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: ADR 0006
- **Context**: A coluna `booking_strategy` já existe com um valor legal. O pipeline de cálculo é o mesmo; muda só a geração de starts candidatos, umas dez linhas.
- **Potential Impact**: Ocupação da agenda
- **Notes**: Gatilho — se DEBT-008 mostrar perda relevante. Se mostrar poucos minutos por dia, remover a coluna e encerrar como Won't Do

---

### IDEA-005: Serviço combinado num agendamento
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural — risco D-03
- **Context**: "Corte + barba" é o pedido mais comum do ramo. `Appointment` já guarda duração e preço próprios, então a duração vira uma soma e o agregado não muda de forma.
- **Potential Impact**: Aderência ao uso real
- **Notes**: Provável primeiro pedido do piloto

---

### IDEA-006: Financeiro — pago, pendente, fiado
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: escopo excluído do MVP
- **Context**: Começa como campo no agendamento, não como módulo. É assim que se descobre se o financeiro merece existir.
- **Potential Impact**: Retenção
- **Notes**: Gatilho — se o barbeiro pedir "marcar como pago"

---

### IDEA-007: Resumo diário por e-mail ao dono
- **Priority**: Medium
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural — risco D-02
- **Context**: Cron e SMTP simples, sem contexto de notificação e sem outbox. É a menor coisa que substitui o hábito do caderno: se o profissional não souber que entrou agendamento às 22h, ele continua conferindo no WhatsApp.
- **Potential Impact**: Adoção
- **Notes**: Barato e de alto retorno — reavaliar já durante o piloto

---

### IDEA-008: Branding da página pública
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: escopo excluído do MVP
- **Context**: Logo, imagem, slogan, cores.
- **Potential Impact**: Percepção de valor
- **Notes**: Gatilho — depois da validação

---

### IDEA-009: Dashboard e relatórios
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: escopo excluído do MVP
- **Context**: Agendamentos do dia, faturamento, cancelamentos, falta, ociosidade.
- **Potential Impact**: Retenção
- **Notes**: Gatilho — depois da validação. Parte do dado vem de DEBT-008

---

### IDEA-010: Lista de espera e encaixe
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: escopo excluído do MVP
- **Context**: Avisar cliente quando abrir horário por cancelamento.
- **Potential Impact**: Ocupação da agenda
- **Notes**: Gatilho — depois da validação

---

### IDEA-011: Extrair IAM de Organization
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: ADR 0003 — gatilho de reavaliação
- **Context**: Na extração, `iam` fica com credencial e papel e referencia `tenantId` por id, sem levar `Business` junto.
- **Potential Impact**: Modelagem
- **Notes**: Gatilho — SSO, múltiplos usuários por empresa com permissões distintas, convites, auditoria de acesso, ou login do cliente final

---

### IDEA-012: Promover contextos a módulos Maven
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-29
- **Origin**: ADR 0001 — gatilho de reavaliação
- **Context**: Migração mecânica nos dois sentidos: mover diretórios e escrever poms. É por isso que a decisão mais simples é a certa agora.
- **Potential Impact**: Isolamento
- **Notes**: Gatilho — mais de três pessoas simultâneas, extração de serviço, ou histórico de falha de ArchUnit sendo ignorada

---

### IDEA-013: Subir Prometheus e Grafana na VPS
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-30
- **Origin**: convenções do time
- **Context**: O endpoint `/actuator/prometheus` já é exposto pela TODO-108. Falta o servidor que raspa e o painel que exibe. São dois containers a mais disputando memória com o Postgres na mesma VPS.
- **Potential Impact**: Operação
- **Notes**: Gatilho — quando houver mais de um estabelecimento em produção, ou quando um incidente exigir olhar série temporal em vez de log

---

### IDEA-014: Trocar cache em memória por Redis
- **Priority**: Low
- **Status**: pending
- **Created**: 2026-08-30
- **Origin**: convenções do time
- **Context**: O código usa a abstração `@Cacheable` do Spring, então a troca é configuração, não reescrita. Primeiro candidato a cache é a resolução de slug para tenant, que roda em toda visita à página pública e quase nunca muda. Disponibilidade **não** deve ser cacheada — muda a cada agendamento.
- **Potential Impact**: Latência
- **Notes**: Gatilho — mais de uma instância da aplicação, ou pressão de memória medida. Antes disso, Redis é um container a mais sem nada para cachear

---

## ✅ Resolved Items

### DEBT-002: ArchUnit deve proibir Spring em teste de domínio
- **Priority**: Medium
- **Status**: resolved
- **Created**: 2026-08-29
- **Origin**: reversão para módulo Maven único
- **Context**: Quando `shared-kernel` era módulo separado, `spring-boot-starter-test` não estava no classpath dele. Com módulo único, nada impede um `@SpringBootTest` para testar um `TimeRange`. A garantia física virou regra a escrever.
- **Affected Files**: `src/test`
- **Complexity**: Low
- **Risk if Ignored**: Teste de domínio lento e acoplado ao framework

- **Resolved**: 2026-08-30
- **Resolution**: Completed — a regra o_dominio_do_nucleo_nao_conhece_framework analisa tambem as classes de teste, entao um @SpringBootTest em scheduling.domain e detectado pela mesma regra

---

### TODO-101: Flyway, datasource e migration inicial
- **Priority**: High
- **Status**: resolved
- **Created**: 2026-08-29
- **Origin**: Fase 0
- **Context**: Migration inicial habilita `btree_gist`, exigida pela exclusion constraint do ADR 0005. Locations por contexto, nomenclatura `V{n}__{contexto}_{o_que_faz}.sql`. Andaime, não feature.
- **Affected Files**: `pom.xml`, `application.yaml`, `db/migration`
- **Complexity**: Low

- **Resolved**: 2026-08-30
- **Resolution**: Completed

---

### TODO-102: Testcontainers com a imagem do compose
- **Priority**: High
- **Status**: resolved
- **Created**: 2026-08-29
- **Origin**: Fase 0
- **Context**: Mesma imagem do `compose.yaml` (`postgres:18-alpine`). H2 não implementa `EXCLUDE USING gist` — testar contra ele é testar outro sistema.
- **Affected Files**: `pom.xml`, `src/test`
- **Complexity**: Low

- **Resolved**: 2026-08-30
- **Resolution**: Completed

---

### TODO-103: Regras de ArchUnit — camadas dentro do contexto
- **Priority**: High
- **Status**: resolved
- **Created**: 2026-08-29
- **Origin**: ADR 0001, ADR 0010
- **Context**: `scheduling.domain` sem Spring e sem JPA; `application` sem `adapter`; controller sem repositório; sufixos obrigatórios; sem `@Data` em entidade JPA. Usar `allowEmptyShould(true)` enquanto os pacotes estiverem vazios.
- **Affected Files**: `src/test`
- **Complexity**: Medium

- **Resolved**: 2026-08-30
- **Resolution**: Completed

---

### TODO-104: Pipeline de CI
- **Priority**: Medium
- **Status**: resolved
- **Created**: 2026-08-29
- **Origin**: Fase 0
- **Context**: Depois que existir teste que valha a pena rodar. Depende de DEBT-001: o `mvnw` com CRLF não executa em Linux.
- **Affected Files**: `.github/workflows`
- **Complexity**: Low

- **Resolved**: 2026-08-30
- **Resolution**: Completed

---

### TODO-105: Backup pg_dump com restore testado
- **Priority**: High
- **Status**: resolved
- **Created**: 2026-08-29
- **Origin**: revisão arquitetural — risco T-03
- **Context**: Uma VPS, um Postgres, a agenda inteira de clientes pagantes dentro. Destino fora da VPS, retenção de 30 dias. Obrigatório antes do primeiro cliente pagante — backup nunca restaurado não é backup.
- **Affected Files**: `scripts`, `docs/operations`
- **Complexity**: Medium

- **Resolved**: 2026-08-30
- **Resolution**: Completed

---

### TODO-107: Spring Modulith — fronteira entre contextos
- **Priority**: High
- **Status**: resolved
- **Created**: 2026-08-29
- **Origin**: ADR 0010
- **Context**: Versão 2.1.1. `@NamedInterface("api")` em cada pacote `api`, `allowedDependencies` por contexto e um teste `ApplicationModules.of(...).verify()`. Cuida do que atravessa a fronteira; o ArchUnit (TODO-103) cuida do que acontece dentro dela.
- **Affected Files**: `pom.xml`, `package-info.java`, `src/test`
- **Complexity**: Medium

- **Resolved**: 2026-08-30
- **Resolution**: Completed

---

### DEBT-001: mvnw com CRLF não executa em Linux
- **Priority**: Medium
- **Status**: resolved
- **Created**: 2026-08-28
- **Origin**: commit inicial
- **Context**: O `.gitattributes` do Initializr normaliza para CRLF. Marcar `mvnw` como LF obrigatório antes do CI.
- **Affected Files**: `.gitattributes`
- **Complexity**: Low
- **Risk if Ignored**: O pipeline de CI não roda

- **Resolved**: 2026-08-30
- **Resolution**: Won't Do — alarme falso: o .gitattributes do Initializr ja marca /mvnw como eol

---

### DEBT-007: Avaliar Spring Modulith
- **Priority**: Medium
- **Status**: resolved
- **Created**: 2026-08-29
- **Resolved**: 2026-08-29
- **Resolution**: Completed
- **Resolved In**: ADR 0010
- **Context**: Avaliado e adotado na versão 2.1.1, com escopo estreito — fronteira entre contextos apenas. A verificação de compatibilidade rendeu um achado: o índice do `search.maven.org` mostrava 1.4.1 como última versão, defasado em mais de um ano, e teria levado à conclusão errada de que Modulith não serve para Boot 4. Implementação em TODO-107.

---

## Last Updated

2026-08-30 — Fase 0 fechada: andaime concluído e movido para Resolved Items.
