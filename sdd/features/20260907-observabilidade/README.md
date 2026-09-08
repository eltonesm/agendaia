# observabilidade (TODO-108)

## O que foi construído

Fecha o item de convenção de time do backlog: até aqui, investigar um
problema em produção exigia entrar no banco a mão, e não havia nenhuma
métrica de negócio exposta. Esta feature é transversal (`platform`), sem
tela nem agregado novo — o critério de "pronto" é "dá para saber o que
está acontecendo em produção sem abrir o banco".

```
Todo log da aplicação   → JSON estruturado (ECS), com tenantId + requestId
GET /actuator/health    → inalterado, público (sonda de container)
GET /actuator/prometheus → exige HTTP Basic Auth dedicado; expõe 3
                            contadores de negócio: agendamentos
                            reservados, cancelados, falhas por conflito
                            de horário
```

## O que nasceu/mudou nesta feature

| Item | Onde | Por quê |
|---|---|---|
| `RequestIdFilter` | `platform.web` | Gera um id por requisição, roda antes de tudo (inclusive Security) |
| `GlobalExceptionHandler` (editado) | `platform.web` | Passa a ler o `requestId` do MDC em vez de gerar um id só para a tela de erro |
| `SchedulingMetrics` | `scheduling.application` (pacote-privado) | 3 contadores Micrometer, instrumentados nos 3 handlers existentes |
| `MetricsSecurityConfig` | `platform.security` | 3ª `SecurityFilterChain`, protege `/actuator/prometheus` com Basic Auth dedicado |
| `micrometer-registry-prometheus` | `pom.xml` | Dependência que faltava — sem ela o endpoint nunca funcionava de fato |
| `logging.structured.format.console: ecs` | `application.yaml` | Log JSON nativo do Spring Boot, sem dependência externa |

## Decisões de design centrais

- **Log estruturado nativo do Spring Boot (ECS)**, sem
  `logstash-logback-encoder` — nenhuma ferramenta de coleta existe ainda
  (fica para a IDEA-013), e o framework já resolve isso desde a série
  3.4.
- **`RequestIdFilter` unifica com um identificador que já existia,
  isolado**: `GlobalExceptionHandler` já gerava um id truncado só para a
  tela de erro 500 — descoberto ao desenhar a spec técnica. Agora os dois
  usam a mesma fonte (MDC), e o id da tela é o mesmo de toda linha de log
  da requisição.
- **Reagendar não conta como "criar" nem "cancelar"** nas métricas de
  negócio — é semanticamente "mover"; só a falha por conflito no novo
  horário é contada. Sem essa regra, o dono corrigindo um erro de horário
  infla os dois contadores e esconde o número real de clientes novos e
  desistências.
- **`/actuator/prometheus` com credencial dedicada, `/health` público**:
  o backlog original pedia os dois protegidos, mas o `SecurityConfig` já
  havia decidido (antes desta feature) deixar `/health` público para a
  sonda de container — mantido assim, de propósito, na entrevista com o
  usuário.
- **Achado real durante a implementação**: o nome de métrica
  `agendaia.appointments.created` colide com a convenção reservada do
  OpenMetrics para timestamp de criação de contador — o Prometheus
  descartava o "created" e expunha só `agendaia_appointments_total`.
  Renomeado para `agendaia.appointments.booked`.
- **Segundo achado**: o suporte de teste do Spring Boot desliga a
  exportação de métricas por padrão
  (`management.defaults.metrics.export.enabled=false`) — sem
  `@AutoConfigureMetrics` no teste de integração, `/actuator/prometheus`
  simplesmente não existe no contexto de teste (404), mesmo com a
  credencial certa.
- **Terceiro achado**: a estratégia original da spec técnica (capturar
  log via redirecionamento de `System.out`) não funciona — o
  `ConsoleAppender` do Logback fixa a referência de `System.out` na
  inicialização do contexto. Trocado por um `ListAppender` anexado à
  raiz, verificando o MDC de cada evento capturado diretamente.

## Testes

- Unit: `RequestIdFilterTest` (4 casos), `SchedulingMetricsTest` (4
  casos), `GlobalExceptionHandlerTest` (editado).
- Aplicação (mocks): `BookAppointmentHandlerTest`,
  `ManageAppointmentHandlerTest`, `ProfessionalAgendaHandlerTest`
  (editados) — cada um cobrindo a tabela de contagem de métricas da DD-3.
- Integração (Testcontainers, Postgres real): `ObservabilidadeIT`, E2E-1
  a E2E-4 — log com tenantId/requestId, `/health` sem auth,
  `/prometheus` com credencial e contadores incrementados de verdade,
  nenhum dado pessoal em log.
- 538 testes no projeto inteiro, 0 falhas, 0 erros; 91% de cobertura de
  instrução (`./mvnw clean verify`).

## Quality gates (Layer 3)

Todos `APPROVED` — ver `verdicts/{code_review,performance,security}.json`.

- **Code review**: `scheduling.domain` continua Java puro, sem import de
  Micrometer; `SchedulingMetrics` pacote-privado, sem vazamento entre
  contextos; `MetricsSecurityConfig` segue exatamente o padrão de
  `OperatorSecurityConfig`.
- **Performance**: `RequestIdFilter`/`SchedulingMetrics.increment()` são
  O(1), sem I/O; nenhuma consulta nova ao banco.
- **Security**: credencial de métricas nunca hardcoded (env var + hash
  BCrypt); `/actuator/prometheus` exige autenticação de verdade,
  confirmado por teste e por verificação manual; LGPD confirmada por
  E2E-4.
