# Patterns

> **Status: NORMATIVO.** Padrões obrigatórios de código deste projeto.
> Nomes de tipos saem do [glossário](../docs/domain/glossary.md); o porquê de
> cada decisão está nos [ADRs](../docs/architecture/adr/).

## Team Conventions (Manually Added)

**Rigor proporcional ao subdomínio**:
- Em `scheduling` (core): domínio em Java puro, entidade JPA como classe
  separada, mapeamento explícito entre as duas, casos de uso com portas.
- Em `organization`, `catalog` e `customer` (suporte): a entidade JPA **é** o
  modelo. Sem classe espelho, sem mapper, sem porta de saída — o caso de uso
  fala direto com o repositório Spring Data.
- Não aplique o regime completo em contexto de suporte "por consistência".
- Não deixe anotação de framework vazar para `scheduling.domain`.
- Why: dez arquivos para cadastrar um serviço com nome e preço é cerimônia sem
  retorno. A atenção vai para onde existe invariante. Ver ADR 0002.
- Promoção de contexto de suporte ao regime completo acontece quando ele
  acumula a terceira regra de negócio de verdade — e é mudança local.

**Fronteira entre contextos**:
- Um contexto só importa o pacote `api` de outro: `com.agendaia.catalog.api`.
- O que sai por `api` é `record` imutável. Tipo de domínio interno nunca
  atravessa a fronteira.
- Não importe `domain`, `application` ou `adapter` de outro contexto.
- Não faça JOIN entre tabelas de contextos diferentes. Referência cruzada é
  UUID solto, sem chave estrangeira.
- A dependência é **declarada**, não inferida: `allowedDependencies` no
  `package-info.java` do contexto, e `@NamedInterface("api")` no pacote `api`.
  Dependência nova exige editar essa lista — e aí aparece no diff.
- Why: com um único módulo Maven (ADR 0001), nada físico impede o import. A
  fronteira existe porque o Spring Modulith a verifica no build (ADR 0010).

**API entre contextos é grossa, nunca conversadeira**:
- Desenhe toda operação da `api` **como se já fosse uma chamada de rede**: em
  lote, granularidade grossa, sem N+1.
- Não exponha busca por um id para ser chamada dentro de um laço.

```java
// Ruim — parece inocente hoje
Offering findOffering(UUID id);

// Bom — uma chamada, independente de quantos
List<OfferingView> findOfferings(TenantId tenant, Set<UUID> professionalIds);
```

- Why: in-process a chamada custa nanossegundos e o desenho ruim não dói. É
  exatamente assim que se constrói um monólito distribuído: o dia da extração
  chega e cada tela vira quarenta chamadas remotas.

**Leitura é chamada, escrita é evento**:
- Consulta a outro contexto: chamada síncrona na `api` dele.
- Efeito em outro contexto: evento de domínio registrado no agregado.
- Não escreva em dois contextos na mesma transação.
- Why: se um caso de uso precisa gravar em dois contextos de uma vez, a
  fronteira está no lugar errado. Foi esse sintoma que revelou que tenant e
  empresa eram a mesma entidade (ADR 0003).
- No MVP não há despacho de evento: o agregado registra o evento e o teste
  verifica que ele foi emitido. Infraestrutura entra com o primeiro consumidor.

**Estrutura de pacote dentro de um contexto**:

```
com.agendaia.<contexto>
├── api/          contrato público — o único pacote visível de fora
├── domain/       model, service, policy, event, repository (interfaces)
├── application/  usecase, command, query, port/out
├── adapter/
│   ├── in/web/          controller, request, response
│   └── out/persistence/ entidade JPA, Spring Data, adapter
└── config/       @Configuration do contexto
```

- Repositório do **agregado** é interface em `domain/repository`: fala a
  linguagem do negócio.
- Porta para **sistema externo** (e-mail, LLM, calendário) fica em
  `application/port/out`: é detalhe de integração, não conceito do negócio.
- Why: é a distinção que mais gera dúvida. Regra prática — se um analista de
  negócio reconheceria o nome da interface, ela é do domínio.

**Interface entre camadas, sempre**:
- Toda comunicação entre camadas passa por interface: controller → caso de uso,
  caso de uso → repositório, caso de uso → adapter, contexto → contexto.
- Vale em **todos** os contextos, inclusive nos de regime CRUD.
- Não injete classe concreta de outra camada.
- Why: convenção do time, por inversão de dependência e para manter as camadas
  substituíveis. O custo é um arquivo por caso de uso; o ganho é que nenhuma
  camada conhece a implementação da vizinha.

**Nomeação de interface e implementação**:
- Interface: `BookAppointmentUseCase`. Implementação: `BookAppointmentHandler`.
- **Nunca** o sufixo `Impl`.
- Why: `XImpl` é sintoma de que ninguém achou um segundo nome — porque não há um
  segundo conceito. `Handler` diz o que a classe faz: trata um comando. E evita
  a colisão que `...Service` teria em `catalog`, onde `Service` é entidade do
  domínio e `@Service` é anotação do Spring.
- A mesma lógica vale nas portas de saída: `AppointmentRepository` é
  implementada por `AppointmentPersistenceAdapter` — nomes diferentes porque são
  coisas diferentes.

**Sufixos obrigatórios**:
- `...UseCase` — interface do caso de uso, em `application/port/in`.
- `...Handler` — implementação do caso de uso, em `application`.
- `...Controller`, `...Request`, `...Response` — adapter de entrada web.
- `...JpaEntity`, `...JpaRepository`, `...PersistenceAdapter` — saída.
- `...Policy` — regra de negócio isolada e testável.
- Why: sufixo previsível é o que faz busca por nome funcionar e o que permite
  ao ArchUnit escrever regra sobre a classe.

**Idioma**:
- Identificadores, nomes de arquivo, pacotes e mensagens de commit: **inglês**.
- Interface do usuário, URLs, mensagens de erro exibidas e specs: **português**.
- Não misture: `/admin/servicos` é renderizado por `ServiceController`.
- Why: o ecossistema é inglês; o negócio é português. O glossário é a ponte.

**Onde a cobertura deve vir**:
- Piso global do build: 80%. É piso, não meta.
- `scheduling.domain`: perto de 100%, vindo de teste de regra — tabela de casos
  de sobreposição, transições de status, cálculo de disponibilidade. É barato:
  Java puro, milissegundos, sem Spring.
- `platform`: alto. `TenantContext` e tratamento de erro são críticos.
- Contextos de suporte: cobertura vem de teste de caso de uso e de integração.
- **Nunca** escreva teste de getter, de construtor ou de mapeamento trivial para
  subir o número.
- Why: se a cobertura de um contexto de suporte não fecha sem teste de getter,
  o que falta é teste de caso de uso — não teste de getter.

**Fatia vertical**:
- Uma feature atravessa todas as camadas e entrega algo que o estabelecimento
  consegue usar.
- Não crie feature do tipo "criar as entidades JPA" ou "montar a camada de
  domínio" — isso é tarefa dentro de uma feature.
- Why: fatia horizontal só é verificável no fim, quando já é tarde.

## Java

**Value object é record**:
- Use `record`, com validação no construtor compacto, lançando
  `IllegalArgumentException` para violação de contrato de tipo.
- Não use Lombok para value object: `record` já faz mais e melhor.
- `TimeRange` valida fim depois do início e expõe `overlaps(other)` com
  semântica de intervalo **fechado-aberto**: início incluído, fim excluído.
- Why: essa semântica precisa casar exatamente com a exclusion constraint do
  banco (ADR 0005), senão domínio e Postgres discordam sobre o que é conflito.
  Com ela, 10:00–10:30 e 10:30–11:00 não colidem.

**Sem setter, em lugar nenhum**:
- Nenhuma classe expõe `setX()`. Estado muda por método de negócio com nome do
  domínio: `confirm()`, `cancel(reason)`, `reschedule(newRange)`.
- Vale também para entidade JPA em contexto de suporte: `@Getter` sim,
  `@Setter` **não**.
- O Hibernate não precisa de setter: use acesso a campo (`@Access(FIELD)`) e um
  construtor sem argumentos `protected`, que só ele usa.
- Why: setter público devolve ao chamador a responsabilidade de manter a
  invariante — e o objeto deixa de poder garantir qualquer coisa sobre si. Um
  `setStatus()` joga fora a máquina de estados inteira.

**Lombok em entidade JPA**:
- Use `@Getter` e `@Setter` pontuais quando necessário.
- **Nunca** `@Data`, `@EqualsAndHashCode` ou `@ToString` em entidade JPA.
- Why: `equals`/`hashCode` gerados sobre campos mutáveis mudam de valor entre
  antes e depois do flush, quebrando o comportamento em coleções; e `@ToString`
  percorre coleção lazy e estoura `LazyInitializationException` fora da sessão.

**Agregado é criado por método de fábrica**:
- Construtor privado; criação por método estático com nome do domínio:
  `Appointment.schedule(...)`, não `new Appointment(...)`.
- Identidade (UUIDv7) é gerada **na aplicação**, no momento da criação.
- Estado muda por método de negócio: `confirm()`, `cancel(reason)`,
  `markNoShow()`. **Nunca** `setStatus()` público.
- Why: identidade gerada no INSERT acoplaria o agregado à persistência
  (ADR 0009). E `setStatus()` público joga fora a máquina de estados inteira.

**Agregado guarda retrato, não ponteiro**:
- `Appointment` armazena duração e preço vigentes no momento da reserva.
- Não derive preço nem duração do catálogo atual na hora de exibir.
- Why: se o estabelecimento reajustar o corte de R$ 30 para R$ 35, os
  agendamentos de ontem não podem mudar de valor retroativamente.

**Transação mora na application**:
- `@Transactional` só em classe de `application`.
- Não anote controller, não anote método de domínio, não anote repositório.
- Why: o domínio não sabe que existe banco; o controller não decide fronteira
  de consistência.

**Controller não conhece repositório**:
- Fluxo obrigatório: Controller → UseCase → Domain → Port → Adapter.
- Controller não injeta repositório nem entidade JPA.
- Why: é a regra que o ArchUnit verifica e a que mais se quebra sob pressa.

**Injeção por construtor**:
- Dependência por construtor, campo `final`, sem `@Autowired`.
- Não use injeção em campo nem em setter.
- Why: torna a dependência explícita e a classe instanciável em teste unitário
  sem contexto Spring.

**Erro de negócio vira tela, não stack trace**:
- Regra violada lança exceção de domínio nomeada (`SlotUnavailableException`),
  definida no contexto que a possui.
- O `@ControllerAdvice` do `platform` traduz para mensagem em português na view
  correspondente.
- Não devolva 500 para conflito de horário: o usuário precisa ler "esse horário
  acabou de ser reservado".

## Database Patterns

**Toda tabela de negócio tem `tenant_id`**:
- Coluna `tenant_id uuid not null` em toda tabela de negócio.
- Todo índice de consulta começa por `tenant_id`.
- `tenant_id` nunca vem do cliente: vem da sessão em `/admin/**` ou da resolução
  do slug em `/b/{slug}/**`.
- Todo id recebido de formulário público é revalidado contra o tenant do slug
  antes de qualquer escrita.
- Why: é a fronteira de segurança inteira do produto. Ver ADR 0004.

**Tempo é `timestamptz`, sempre**:
- Armazene em UTC com `timestamptz`. Nunca `timestamp` sem fuso.
- O fuso IANA do estabelecimento é dado; o cálculo de disponibilidade acontece
  no fuso local, a comparação e o armazenamento em UTC.

**Overbooking é impedido pelo banco**:
- `EXCLUDE USING gist` sobre o intervalo de tempo, discriminando por `tenant_id`
  e `professional_id`, parcial por status ativo.
- Escreva os limites de intervalo explicitamente, ainda que sejam o padrão.
- O adapter captura a violação da constraint e traduz para
  `SlotUnavailableException`.
- Não confie em "consultar disponibilidade e depois inserir": duas requisições
  simultâneas passam nas duas validações.
- Cancelado e falta ficam **fora** da cláusula de status, para liberar o
  horário — status ocupante esquecido ali mata o slot para sempre.
- Why: validação em memória é feedback, não garantia. Ver ADR 0005.

**Migration é imutável depois de aplicada**:
- Migration Flyway já aplicada nunca é editada: corrija com uma nova.
- Nomeie por contexto: `V{n}__{contexto}_{o_que_faz}.sql`.

**Sem chave estrangeira entre contextos**:
- FK só entre tabelas do mesmo contexto.
- Referência a outro contexto é UUID solto, validado no caso de uso.
- Why: FK entre contextos transforma fronteira lógica em acoplamento físico e
  impede evolução independente dos schemas.

**Teste de banco usa Postgres real**:
- Testcontainers com a mesma imagem do `compose.yaml`.
- H2 não implementa `EXCLUDE USING gist`: testar contra ele é testar outro
  sistema.
- Todo agregado com invariante de concorrência tem um teste que dispara duas
  operações simultâneas e exige que exatamente uma vença.

## Observabilidade

**Log estruturado, com tenant em toda linha**:
- Log em JSON, não texto corrido.
- `tenantId` e `requestId` entram no MDC no filtro de resolução de tenant e
  saem em **toda** linha da requisição.
- Why: num sistema multi-tenant, "está lento" é sempre a pergunta errada. A
  pergunta é "está lento para qual estabelecimento" — e sem `tenantId` no log
  não há como responder.

**Nunca logar dado pessoal**:
- Proibido em log: telefone, nome de cliente, e-mail, senha, token público.
- Use o id: `customerId=01a04ae1-...`, nunca `phone=+5511987654321`.
- Why: log vai para arquivo, para agregador e para backup — e vira dado pessoal
  fora do controle do titular. É obrigação da LGPD, não higiene opcional.

**Métricas via Actuator e Prometheus**:
- Expor `/actuator/health` e `/actuator/prometheus`, ambos protegidos e fora do
  alcance público.
- Métricas de negócio contam tanto quanto as técnicas: agendamentos criados,
  cancelados, falhas por conflito de horário, tempo do cálculo de
  disponibilidade.
- Why: `SlotUnavailableException` subindo é sinal de disputa real por horário —
  informação de produto, não só de infraestrutura.
- O endpoint custa uma dependência e fica pronto desde o início. **Subir o
  servidor Prometheus e o Grafana na VPS é decisão separada**: consome memória
  da mesma máquina que roda o banco. Ligar quando houver o que observar.

**Erro que o usuário vê e erro que o operador vê**:
- Exceção de negócio (`SlotUnavailableException`) é log em `WARN`, sem stack
  trace: é fluxo esperado.
- Exceção inesperada é `ERROR` com stack trace e `requestId`, e o usuário vê uma
  página genérica com esse mesmo `requestId`.
- Why: stack trace de regra de negócio polui o log e esconde o defeito real.

## Performance e Cache

**O caminho quente é o cálculo de disponibilidade**:
- Buscar jornada, bloqueios e agendamentos do dia em **uma consulta cada**,
  nunca dentro de laço.
- O cálculo em si é função pura, em memória. Não vá ao banco dentro dele.
- Why: é a consulta mais frequente do sistema — roda a cada troca de data na
  página pública.

**Proibido N+1**:
- Toda associação é `LAZY`. Nunca `FetchType.EAGER`.
- Precisa da associação? Busque explicitamente com `join fetch` ou
  `@EntityGraph`, na consulta que precisa dela.
- Why: `EAGER` resolve um caso e degrada todos os outros, silenciosamente.

**Consulta de leitura não carrega agregado**:
- Tela de listagem usa projeção (`record` com só os campos exibidos), não a
  entidade inteira.
- Consulta sem escrita é `@Transactional(readOnly = true)`.
- Why: carregar o agregado para exibir três colunas paga o custo de tudo o que
  não vai ser usado.

**Toda lista que cresce é paginada**:
- Histórico de cliente, lista de agendamentos, lista de clientes.
- Why: funciona nos três primeiros meses do piloto e quebra no décimo.

**Cache: medir antes, e começar em memória**:
- Use a abstração `@Cacheable` do Spring, nunca a API do provedor direto.
- Comece com cache **em memória** (Caffeine). Trocar para **Redis** é mudar
  configuração, não código — e só se justifica quando houver mais de uma
  instância da aplicação ou pressão real de memória.
- Candidato natural quando chegar a hora: resolução de `slug` para tenant, que
  roda em toda visita à página pública e quase nunca muda.
- **Não cacheie disponibilidade**: ela muda a cada agendamento, e cache
  desatualizado aqui oferece horário que já não existe.
- Why: Redis no MVP é mais um container disputando memória com o Postgres na
  mesma VPS, para cachear um sistema com dezenas de requisições por dia. A
  decisão certa é deixar o caminho pronto e ligar quando a medição pedir.

**Índice sempre começa por `tenant_id`**:
- Exceto os globais por natureza: `slug`, `email`, `public_token`.
- Why: toda consulta de negócio filtra por tenant primeiro. Índice que não
  começa por ele não é usado.

## Exemplo de ponta a ponta

<!--
  PENDENTE: adicionar após a primeira fatia vertical (cadastro de empresa e
  login). Um caso de uso completo e curto, de controller a migration, é de onde
  o agente copia o padrão — vale mais que todos os parágrafos acima juntos.
  Não inventar antes de existir código real que compile e passe nos testes.
-->

## Last Updated

2026-08-29 — versão inicial, derivada dos ADRs 0001 a 0009.
