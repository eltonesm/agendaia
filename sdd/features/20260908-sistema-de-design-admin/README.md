# sistema-de-design-admin (TODO-110)

## O que foi construído

Retrofit visual das telas de `/admin/**` (e do painel do operador) sobre o
sistema de design já documentado em `sdd/PATTERNS.md` — sem tela nem fluxo
novo, o critério de "pronto" era: toda tela de `/admin/**` usa sidebar em
vez de navbar, todo badge de status usa o padrão soft (`bg-*-subtle` +
`text-*-emphasis`), e o dashboard mostra pelo menos um KPI com dado real
por trás.

```
/admin/**              → sidebar de navegação (8 itens), não mais navbar
                          horizontal; barra de topo alternativa em telas
                          pequenas
Badge de status         → 5 estados (SCHEDULED/CONFIRMED/COMPLETED/
                          CANCELLED/NO_SHOW), todos no padrão soft/pill
/admin/dashboard        → 4 KPIs reais: agendamentos de hoje, próximo
                          cliente, atendidos hoje, receita estimada
/admin/agenda           → botão "Concluir" (✓) ao lado de "Cancelar" (✗),
                          por agendamento, condicionado à janela de horário
/operador/painel        → 4 cards de contagem por status de acesso
                          (mesma lista já buscada, sem consulta nova)
```

## O que nasceu/mudou nesta feature

| Item | Onde | Por quê |
|---|---|---|
| `AppointmentStatus.COMPLETED` | `scheduling.domain` (editado) | Pedido explícito do dono durante a entrevista funcional: em vez de inferir "atendido" a partir de outro status, criar um status próprio marcado manualmente |
| `Appointment.complete(Instant)` | `scheduling.domain` (editado) | Transição imutável, mesmo padrão de `confirm`/`cancel` — absorve fora da janela de horário ou se já terminal |
| `CompleteAppointmentUseCase` | `scheduling.application.port.in` (novo) | Porta de entrada para o botão "Concluir" |
| `scheduling.api` (`DailyScheduleDirectory`) | `scheduling.api` (pacote novo, `@NamedInterface`) | 1ª porta `api` de `scheduling` — expõe os 4 números do dashboard sem outro contexto importar `application`/`domain` |
| `DailyScheduleSummaryHandler` | `scheduling.application` (novo, pacote-privado) | Implementa a porta acima; 1 consulta, 4 números em memória |
| `DashboardKpiAdvice` | `scheduling.adapter.in.web` (novo) | `@ControllerAdvice` que injeta os KPIs na view de `organization` sem import direto — evita ciclo de módulo (ver `sdd/PATTERNS.md`) |
| `AppointmentRepository.findByTenantIdAndDate` | `scheduling.application.port.out` (editado) + adapters | Nova consulta: todo agendamento do tenant num dia, sem filtro de profissional |
| `fragments/layout.html` — `adminSidebar`, `adminTopoMobile`, `adminBanners` | `templates/fragments` (editado) | Fragmentos novos, compostos nas 10 telas de `/admin/**` no lugar de `navbar` |
| `OperatorPanelController.carregarPainel` | `billing.adapter.in.web` (editado) | 4 contadores por status, leitura da mesma lista já buscada — sem consulta nova |

## Decisões de design centrais

- **`AppointmentStatus.COMPLETED`, marcado manualmente — mudança de escopo
  pedida pelo dono durante a spec funcional**: a pergunta original era só
  "como representar 'atendidos hoje'". Nenhum status existente encaixava.
  Em vez de escolher entre as opções propostas, o dono pediu explicitamente
  um status novo com marcação manual (um clique, ✓/✗ ao lado do status na
  lista do dia) — o mesmo padrão do protótipo Gemini. Vira US-4 da spec
  funcional.
- **Duas tentativas para os KPIs do dashboard (DD-6)**: a primeira fazia
  `organization` depender de `scheduling.api` diretamente — fechava um
  ciclo, porque `scheduling` já depende de `organization.api`. A segunda
  (adotada) usa `@ControllerAdvice` dentro de `scheduling`, mesmo padrão de
  `BillingBannerAdvice` — o contexto que tem o dado o injeta na view do
  outro, sem inverter a dependência.
- **`ObjectProvider<DailyScheduleDirectory>` em vez de dependência direta
  no construtor do advice**: um `@ControllerAdvice` é escaneado por todo
  `@WebMvcTest` da suíte, não só pelos testes relacionados — descoberto
  quando 94 classes de teste passaram a falhar de uma vez.
- **Achado real durante o code review**: `cancelByOwner()` não excluía
  `COMPLETED` do guard de absorção — permitia reabrir um agendamento
  concluído direto para `CANCELLED`, contradizendo a regra "COMPLETED é
  terminal". Corrigido antes do fechamento da Layer 3 (ver
  `verdicts/code_review.json`).
- **Fora de escopo, decidido na spec funcional**: sidebar agrupada por
  seção (fica flat, 8 itens); ação de marcar `NO_SHOW` pela tela (só existe
  hoje por outro fluxo); detalhamento de receita por serviço;
  `admin/conta-suspensa.html` não ganha sidebar (tela de beco sem saída
  intencional, decisão confirmada com o dono).

## Testes

- Domínio: `AppointmentTest` — 5 casos novos para `complete()` e para o
  fechamento da brecha em `cancelByOwner()`.
- Aplicação: `ProfessionalAgendaHandlerTest` — 7 casos novos (canComplete,
  transições, isolamento de tenant); `DailyScheduleSummaryHandlerTest`
  (novo, 4 casos, AC-1 a AC-4); `SchedulingMetricsTest` — 1 caso novo.
- Web: `AgendaControllerTest` — 3 casos novos (rota concluir, CSRF,
  isolamento de tenant); `DashboardKpiAdviceTest` (novo, 3 casos — URI
  certa, URI errada, bean ausente).
- Integração (Testcontainers, Postgres real): `DashboardIT` (novo, e2e1 e
  e2e3 — sidebar renderizada, KPIs reais excluindo cancelado);
  `AgendaProfissionalIT` — 2 casos novos (concluir agendamento passado,
  não concluir fora da janela permitida).
- 581 testes no projeto inteiro, 0 falhas, 0 erros; 91% de cobertura de
  instrução (`./mvnw clean verify`).

## Quality gates (Layer 3)

Todos `APPROVED` — ver `verdicts/{code_review,performance,security}.json`.

- **Code review**: achado crítico real (`cancelByOwner` não terminal para
  `COMPLETED`) encontrado e corrigido durante a própria revisão, com testes
  novos comprovando o fechamento. As 10 telas migradas para sidebar
  verificadas byte-a-byte iguais exceto o parâmetro `activeItem`.
- **Performance**: `DailyScheduleSummaryHandler` faz 1 consulta só;
  `CustomerDirectory.find()` chamado no máximo 1 vez; `DashboardKpiAdvice`
  não tem custo real nas ~20 outras rotas (checagem de URI é a primeira
  linha). 1 recomendação não bloqueante: `DEBT-019` (índice para
  `findByTenantIdAndDate`, mesma família da `DEBT-018`).
- **Security**: rota nova `/admin/agenda/agendamentos/{id}/concluir` cai no
  mesmo `hasRole("OWNER")` de `/admin/**`, CSRF obrigatório, tenantId
  sempre resolvido via `TenantContext`, nunca de parâmetro — id de outro
  tenant devolve 404. `scheduling.api` nunca recebe tenantId como
  argumento. Nenhum segredo novo, nenhuma dependência nova.
