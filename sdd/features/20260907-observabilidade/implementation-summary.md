# Implementation Summary — observabilidade (TODO-108)

## Timeline

- Início: 2026-09-07
- Fim: 2026-09-07 (funcional → técnica → tasks → build → finish no
  mesmo dia)

## Tasks

- Total: 16 (`stats.done: 16` em `tasks.json`)
- Estratégia de execução: Batched — nível 0 com 4 tarefas independentes
  (dependência do Prometheus, `RequestIdFilter`, `SchedulingMetrics`,
  `application.yaml`), depois cadeia sequencial até os 3 quality gates.
- 0 tasks adicionadas ou removidas fora do plano aprovado.

## Commits (12, `main..feature/observabilidade`)

```
e05b1f3 docs(sdd): inicia TODO-108 (observabilidade)
9cdd888 docs(sdd): aprova spec funcional da TODO-108
bbddc95 docs(sdd): aprova spec tecnica da TODO-108
757b7ac docs(sdd): aprova as 16 tarefas da TODO-108 (estrategia batched)
1997f22 feat(platform): dependencia do prometheus e log estruturado (TASK-001/004)
eb41264 feat(platform): RequestIdFilter e unificacao com GlobalExceptionHandler (TASK-002/005)
d42cab1 feat(platform,scheduling): SchedulingMetrics e MetricsSecurityConfig (TASK-003/006)
3be3a7f feat(scheduling): instrumenta os tres handlers com SchedulingMetrics (TASK-007/008/009)
96059b0 test(platform,scheduling): testes de RequestIdFilter, SchedulingMetrics e handlers (TASK-010/011/012)
1ca94d5 test(platform): ObservabilidadeIT com E2E-1 a E2E-4 (TASK-013)
bd7c20a chore(sdd): fecha camada 1 da TODO-108 (TASK-001 a TASK-013)
ef9a9cb chore(platform,scheduling): fecha camada 3 da TODO-108 - code review, performance e security
```

## Quality

- Testes: 538 no projeto inteiro (17 novos nesta feature:
  `RequestIdFilterTest` 4, `SchedulingMetricsTest` 4,
  `ObservabilidadeIT` 4, mais os casos novos adicionados a
  `BookAppointmentHandlerTest` (+1), `ManageAppointmentHandlerTest`
  (assertions adicionadas aos existentes), `ProfessionalAgendaHandlerTest`
  (+1) e `GlobalExceptionHandlerTest` (editado)), 0 falhas, 0 erros
  (`./mvnw clean verify`).
- Cobertura de instrução: 91% no projeto inteiro (piso: 80%).
- Linter/type errors: 0.
- Layer 3 (code review, performance, security): todos `APPROVED`, zero
  achado crítico, major ou menor (ver `verdicts/`).

## Gotcha real #1: nome de métrica colide com convenção reservada do OpenMetrics

O nome óbvio para o contador de agendamentos criados era
`simboraagendar.appointments.created`. Ao testar contra a aplicação rodando de
verdade (`curl -u metrics:... http://localhost:8080/actuator/prometheus`),
a métrica saía como `simboraagendar_appointments_total` — sem "created" nenhum
no nome, indistinguível de qualquer outro contador. O Prometheus/
Micrometer trata o sufixo `.created` como a convenção reservada do
OpenMetrics para o timestamp de criação de um contador, e descarta essa
palavra ao gerar o nome de exposição. Renomeado para
`simboraagendar.appointments.booked` — evita a colisão e continua na
linguagem do domínio (glossário: "reservar, agendar").

**Lição**: testar contra a aplicação de verdade (não só contra mocks ou
testes unitários) pegou algo que nenhum teste unitário isolado
detectaria — o comportamento de nomenclatura do Prometheus só aparece na
integração real com o registry de verdade.

## Gotcha real #2: suporte de teste do Spring Boot desliga a exportação de métricas

`ObservabilidadeIT.e2e3PrometheusExigeCredencialEExpoeMetricas` falhava
com 404 mesmo com a credencial de Basic Auth correta — mas o mesmo
endpoint funcionava perfeitamente contra a aplicação rodando via
`./mvnw spring-boot:run`. Investigação revelou que o Spring Boot Test
registra automaticamente um `DisableMetricsExportContextCustomizer` que
seta `management.defaults.metrics.export.enabled=false` para qualquer
`@SpringBootTest`, evitando que testes publiquem métricas de verdade em
sistemas externos (comportamento pensado para métricas *push*, mas que
também impede o bean do `PrometheusMeterRegistry`/endpoint de existir).
Corrigido com a anotação oficial `@AutoConfigureMetrics` na classe de
teste, que reverte esse desligamento.

## Gotcha real #3: capturar log JSON via `System.out` não funciona

A spec técnica original planejava validar o formato JSON do log
redirecionando `System.out` durante o teste e parseando cada linha
capturada. Na prática, o `ConsoleAppender` do Logback resolve
`System.out` uma única vez, na inicialização do contexto Spring — trocar
o fluxo em tempo de teste não é capturado por ele. Substituído por um
`ListAppender` do próprio Logback, anexado à raiz, verificando o MDC de
cada `ILoggingEvent` capturado diretamente (`getMDCPropertyMap()`) — mais
robusto que reproduzir o formato ECS exato, e continua provando o
comportamento que realmente importa (o MDC chega ao log).
