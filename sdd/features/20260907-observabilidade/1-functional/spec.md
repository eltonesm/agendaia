# observabilidade - Functional Spec

**Feature**: observabilidade
**Backlog**: TODO-108
**Status**: approved
**Data**: 2026-09-07
**Aprovado por**: Elton Marques em 2026-09-07T17:09:39Z

---

## Problem Statement

Hoje, quando algo dá errado em produção — um agendamento que falhou, uma
requisição que deu erro, um pico de uso — a única forma de investigar é
entrar no banco de dados diretamente ou ler o log de texto puro linha por
linha, sem conseguir filtrar por estabelecimento nem seguir uma
requisição do início ao fim. Não existe nenhuma métrica de negócio
exposta (quantos agendamentos foram criados, quantos cancelados, quantas
tentativas falharam por conflito de horário) nem um jeito automatizado de
uma ferramenta externa coletar isso.

Esta feature não adiciona nenhuma tela nem agregado novo — é
transversal, tocando `platform` e todo o resto do sistema. O critério de
"pronto" é diferente das features anteriores: não é "o dono consegue
fazer X pela tela", é "dá para entender o que está acontecendo em
produção sem abrir o banco a mão".

---

## Objectives

1. Toda linha de log da aplicação sai em formato estruturado (JSON), com
   `tenantId` e `requestId` quando disponíveis — permitindo filtrar por
   estabelecimento e seguir uma requisição inteira, do início ao fim.
2. Uma ferramenta de monitoramento externa consegue coletar métricas do
   sistema automaticamente, incluindo três métricas de negócio:
   agendamentos criados, agendamentos cancelados e tentativas de
   agendamento que falharam por conflito de horário.
3. A sonda de saúde do container continua funcionando sem autenticação,
   para que o orquestrador (Docker Compose, e futuramente o que rodar na
   TODO-106) saiba quando reiniciar o serviço.
4. Nenhum dado pessoal de cliente (nome, telefone) aparece em nenhuma
   linha de log, em nenhuma circunstância (LGPD).

---

## Scope

### In Scope

- Log estruturado em JSON em toda a aplicação, substituindo o formato de
  texto plano atual.
- `requestId`: um identificador novo, gerado uma vez por requisição HTTP,
  presente em toda linha de log daquela requisição.
- `tenantId` já existe no MDC (`TenantContextFilter`, ADR 0004) — esta
  feature garante que ele aparece no formato estruturado também, sem
  mudar como é resolvido.
- `/actuator/health`: continua público, sem autenticação (sonda de
  container).
- `/actuator/prometheus`: passa a existir de fato (hoje configurado mas
  não funcional) e fica protegido por uma credencial dedicada,
  independente do login do dono.
- Três métricas de negócio, como contadores globais do sistema (sem tag
  de estabelecimento): agendamentos criados, agendamentos cancelados
  (por qualquer via — cliente ou dono) e tentativas de agendamento que
  falharam por conflito de horário (também por qualquer via).

### Out of Scope

- Subir um servidor de Prometheus ou Grafana de verdade — isso é a
  IDEA-013 do backlog, uma feature separada. Aqui só se garante que os
  dados existem e são coletáveis.
- Métricas de negócio por estabelecimento (tag `tenantId`) — decisão
  explícita do usuário: só um piloto hoje, tag por tenant só compensa
  com múltiplos estabelecimentos monitorados.
- Rastreamento distribuído (tracing) entre serviços — o sistema é um
  monólito, não há "entre serviços" ainda.
- Alertas automáticos (e-mail, WhatsApp) quando uma métrica ultrapassa um
  limite — consumir os dados expostos aqui é responsabilidade de uma
  ferramenta externa, fora do escopo desta feature.

---

## User Stories

### US-1: Log estruturado com tenant e requisição identificados

**Como** dono do estabelecimento (ou quem for investigar um problema em
produção),
**Eu quero** que toda linha de log traga o estabelecimento e a
requisição a que pertence, num formato que dá para filtrar,
**Para que** eu consiga entender o que aconteceu com um cliente
específico sem precisar ler o log inteiro linha por linha.

**Acceptance Criteria**:
- Toda linha de log é um objeto JSON válido, numa única linha.
- Uma requisição que passa por `TenantContextFilter` com tenant
  resolvido tem `tenantId` em toda linha de log gerada durante essa
  requisição.
- Toda requisição HTTP, resolvendo tenant ou não, tem um `requestId`
  novo, presente em toda linha de log gerada durante ela.
- Uma requisição que falha antes do tenant ser resolvido (ex.: slug
  inexistente) ainda assim tem `requestId` nas suas linhas de log.

### US-2: Métricas de negócio coletáveis automaticamente

**Como** dono do estabelecimento,
**Eu quero** que o sistema exponha quantos agendamentos foram criados,
cancelados, e quantas tentativas falharam por conflito de horário,
**Para que** eu (ou uma ferramenta que eu conecte depois) consiga
acompanhar o uso real do sistema sem consultar o banco a mão.

**Acceptance Criteria**:
- Existe um contador que soma toda vez que um `Appointment` novo é
  criado com sucesso — pela reserva pública (TODO-006) e pela criação
  manual do dono (TODO-008).
- Existe um contador que soma toda vez que um `Appointment` é cancelado
  — pelo cliente (TODO-007) e pelo dono (TODO-008).
- Existe um contador que soma toda vez que uma tentativa de agendamento
  (criação ou reagendamento) falha porque o horário acabou de ser
  ocupado (`SlotUnavailableException`) — de qualquer origem.
- Os três contadores nunca diminuem — são cumulativos desde que a
  aplicação subiu (mesmo raciocínio de métrica do Prometheus).

### US-3: Sonda de saúde sem autenticação

**Como** orquestrador de containers (Docker Compose, ou o que rodar na
TODO-106),
**Eu quero** consultar se a aplicação está saudável sem precisar de
credencial,
**Para que** eu saiba reiniciar o serviço automaticamente se ele parar
de responder.

**Acceptance Criteria**:
- `GET /actuator/health` responde sem exigir login nem credencial
  nenhuma.
- Continua assim mesmo depois desta feature proteger outros endpoints do
  actuator.

### US-4: Métricas protegidas por credencial própria

**Como** dono do estabelecimento,
**Eu quero** que as métricas do sistema só sejam acessíveis por quem
tiver uma credencial específica para isso,
**Para que** dados operacionais não fiquem expostos publicamente na
internet.

**Acceptance Criteria**:
- `GET /actuator/prometheus` sem credencial devolve erro de autenticação
  (401), nunca os dados.
- A credencial de acesso a `/actuator/prometheus` é independente do
  login do dono (`/login`, sessão `OWNER`) — funciona por HTTP Basic
  Auth, sem depender de cookie de sessão nem formulário.
- Com a credencial correta, o endpoint devolve as métricas no formato
  que uma ferramenta de coleta espera.

### US-5: Nenhum dado pessoal em log

**Como** responsável pela conformidade com a LGPD,
**Eu quero** ter certeza de que nome e telefone de cliente nunca
aparecem em nenhuma linha de log,
**Para que** o sistema não vaze dado pessoal por um canal que ninguém
está olhando de propósito.

**Acceptance Criteria**:
- Nenhuma linha de log gerada durante um fluxo completo de agendamento
  (criar, confirmar, cancelar, reagendar) contém o nome nem o telefone
  do cliente envolvido.
- Isso vale tanto para log de sucesso quanto de erro — inclusive uma
  falha de validação do formulário, que poderia (por engano) ecoar o
  valor recebido.

---

## Business Rules

### Core Rules

- **BR-1**: `/actuator/health` permanece público, sem autenticação —
  decisão explícita: a prioridade é a sonda de container continuar
  funcionando, mesmo em troca de um endpoint de saúde tecnicamente
  "aberto" (não vaza dado sensível, só `UP`/`DOWN`).
- **BR-2**: `/actuator/prometheus` exige credencial própria (HTTP Basic
  Auth), independente da sessão `OWNER` — uma ferramenta de coleta
  automatizada não faz login por formulário.
- **BR-3**: As três métricas de negócio (criado, cancelado, falha por
  conflito) são contadores globais do sistema, sem tag de
  estabelecimento — decisão explícita do usuário para o estágio atual
  (piloto único).
- **BR-4 (LGPD)**: Nome e telefone de cliente nunca aparecem em nenhuma
  linha de log, em nenhuma circunstância — nem em log de sucesso, nem de
  erro, nem de validação.
- **BR-5**: `requestId` é gerado uma vez por requisição HTTP e vale para
  toda a duração dela — igual ao `tenantId` já existente no MDC
  (`TenantContextFilter`), `requestId` também precisa ser limpo ao fim
  da requisição, para não vazar para a próxima que reusar a mesma
  thread.
- **BR-6**: Cancelamento conta na métrica de "cancelado" não importa se
  veio do cliente (`CancelAppointmentUseCase`, TODO-007) ou do dono
  (`CancelAppointmentByOwnerUseCase`, TODO-008) — é uma métrica de
  resultado, não de origem.
- **BR-7**: Falha por conflito de horário conta na métrica não importa
  se veio da reserva pública, da criação manual pelo dono, ou de um
  reagendamento — mesma regra de BR-6.

### Validation Invariants

- Requisição a `/actuator/prometheus` sem credencial, ou com credencial
  errada → 401, nunca os dados.
- Log gerado fora do ciclo de uma requisição HTTP (ex.: na inicialização
  da aplicação) → sem `requestId` nem `tenantId`, o que é esperado, não
  um erro.

---

## Data Model (conceitual, para a spec técnica detalhar)

Esta feature não introduz nenhum agregado nem tabela nova. Os conceitos
envolvidos são todos transversais:

- **`requestId`**: um identificador opaco, novo por requisição HTTP —
  onde ele vive (MDC, cabeçalho de resposta, os dois) é decisão da spec
  técnica.
- **Contador de negócio**: um valor cumulativo por tipo de evento
  (criado / cancelado / falha por conflito), sem chave de negócio além
  do próprio nome da métrica — tecnologia de armazenamento (em memória,
  exposta via endpoint) é decisão da spec técnica.

---

## User Experience

Não há tela nova. A "experiência" desta feature é para quem opera o
sistema, não para quem usa o produto:

- Quem olhar o log da aplicação (via `docker compose logs`, ou
  redirecionado para arquivo) vê linhas JSON, uma por evento, com
  `tenantId` e `requestId` quando aplicável.
- Quem tiver a credencial de métricas consegue fazer
  `curl -u usuario:senha https://.../actuator/prometheus` e ver os
  contadores no formato que o Prometheus entende.
- Quem (ou o que) fizer `curl https://.../actuator/health` recebe
  `{"status":"UP"}` sem precisar de nada além disso.

---

## Critical E2E Test Scenarios

> Sem ferramenta de E2E externa neste projeto — "E2E" aqui significa
> teste de integração contra a aplicação subida de verdade (mesmo padrão
> das features anteriores), não um agregado de domínio novo para testar
> em isolamento.

### E2E-1: Log de uma requisição autenticada traz tenantId e requestId

Dono autenticado faz uma requisição a `/admin/**`; a linha de log
resultante é JSON válido e contém o `tenantId` da sessão e um
`requestId` não vazio.

### E2E-2: /actuator/health responde sem autenticação

`GET /actuator/health` sem nenhuma credencial devolve 200 e `status:
UP`.

### E2E-3: /actuator/prometheus exige credencial e expõe as métricas de negócio

`GET /actuator/prometheus` sem credencial devolve 401. Com a credencial
correta, depois de criar um agendamento, cancelá-lo e forçar uma falha
por conflito de horário, os três contadores aparecem incrementados no
corpo da resposta.

### E2E-4: Nenhum dado pessoal aparece no log de um fluxo de agendamento

Um fluxo completo de reserva pública (nome e telefone reais informados)
não deixa nome nem telefone em nenhuma linha de log capturada durante o
fluxo.

---

## Success Metrics

- 100% das linhas de log da aplicação em produção são JSON válido.
- `/actuator/prometheus` responde com os três contadores de negócio
  presentes, mesmo que zerados, assim que a aplicação sobe.
- Zero ocorrência de nome ou telefone de cliente em log, auditável pelos
  testes automatizados desta feature.

---

## Non-Functional Requirements

- **Desempenho**: gerar `requestId` e formatar log em JSON não pode
  adicionar latência perceptível a nenhuma requisição — é overhead de
  biblioteca já otimizada para isso (decisão de qual biblioteca é da
  spec técnica).
- **Segurança**: a credencial de `/actuator/prometheus` nunca fica
  hardcoded no código-fonte — vem de variável de ambiente, mesmo padrão
  já usado para a conta do operador (`SIMBORAAGENDAR_OPERADOR_*`).
- **LGPD**: ver BR-4 — requisito não-funcional transversal a todo log
  gerado pela aplicação, não só o desta feature.

---

## Assumptions

- O formato de log estruturado escolhido pela spec técnica é livre para
  usar o que já vem com o Spring Boot desta versão, sem precisar
  necessariamente de uma biblioteca externa nova — a spec técnica decide.
- As métricas de negócio contam eventos que já acontecem hoje
  (`BookAppointmentHandler`, `ManageAppointmentHandler`,
  `ProfessionalAgendaHandler`) — nenhum comportamento de negócio muda,
  só passa a ser contado.
- `/actuator/info` não foi mencionado no backlog nem discutido — fica
  como está hoje (implicitamente protegido pela regra geral
  `anyRequest().hasRole("OWNER")`, sem virar rota pública nem exigir a
  nova credencial de métricas).
