# Patterns

> **Status: NORMATIVO.** Padrões obrigatórios de código deste projeto.
> Nomes de tipos saem do [glossário](../docs/domain/glossary.md); o porquê de
> cada decisão está nos [ADRs](../docs/architecture/adr/).

## Team Conventions (Manually Added)

**Rigor proporcional ao subdomínio**:
- Em `scheduling` (core): domínio em Java puro, entidade JPA como classe
  separada, mapeamento explícito entre as duas, casos de uso com portas.
- Em `organization`, `catalog` e `customer` (suporte): a entidade JPA **é** o
  modelo. Sem classe espelho, sem mapper, sem adapter de persistência — o caso
  de uso fala direto com a interface do Spring Data.
- Não aplique o regime completo em contexto de suporte "por consistência".
- Não deixe anotação de framework vazar para `scheduling.domain`.
- Why: dez arquivos para cadastrar um serviço com nome e preço é cerimônia sem
  retorno. A atenção vai para onde existe invariante. Ver ADR 0002.
- Promoção de contexto de suporte ao regime completo acontece quando ele
  acumula a terceira regra de negócio de verdade — e é mudança local.

**Repositório é porta de saída, não modelo — mora em `application.port.out`,
nunca em `domain`** (revisto em 2026-09-02, ver ADR 0002):
- `domain` guarda só Entity, Value Object, enum e exceção de domínio. Nunca
  uma interface que existe por causa de persistência.
- A interface `XRepository extends JpaRepository<...>` mora em
  `<contexto>.application.port.out`, ao lado de `application.port.in` (onde
  já vivem as interfaces de `UseCase`) — os dois lados do mesmo desenho de
  portas, entrada e saída.
- Em contexto de suporte, isso **não** introduz adapter nem mapper: a porta
  de saída já é a interface do Spring Data, igual a antes — só o pacote
  mudou, de `domain` para `application.port.out`. O ADR 0002 continua valendo
  (entidade JPA é o modelo); o que mudou é só onde a *interface* de acesso a
  ela mora.
- Em `scheduling` (regime completo), a porta de saída em
  `application.port.out` é implementada por um adapter em
  `adapter.out.persistence` que faz o mapeamento — aí sim como Clean
  Architecture/hexagonal clássica prevê.
- Why: Eric Evans (DDD, 2003) trata Repository como padrão de domínio, mas a
  prática moderna de hexagonal architecture em Java — ver o buckpal de Tom
  Hombergs (`Get Your Hands Dirty on Clean Architecture`), referência mais
  citada da comunidade — separa: `domain` é só modelo puro, e o repositório é
  `application/port/out`, ao lado de `port/in`. `domain` nunca conhece
  persistência, nem como interface.
- ArchUnit trava isso: `dominio_de_suporte_nao_conhece_spring` em
  `ArchitectureTest.java` — domínio de contexto de suporte pode importar
  `jakarta.persistence` (anotação da entidade), nunca `org.springframework`.

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
- **Declarar `allowedDependencies` inclui `shared` E `platform` na lista,
  mesmo os dois sendo `Type.OPEN`.** `allowedDependencies` é uma whitelist
  explícita do módulo que declara — `Type.OPEN` no lado de `shared`/`platform`
  só dispensa quem **não** declara `allowedDependencies` nenhuma (contexto sem
  restrição nenhuma). O primeiro contexto a declarar essa lista (`catalog`, na
  TODO-003) esqueceu os dois, um de cada vez: primeiro `shared`
  (`TenantId`/`UuidV7`/`Money`), depois `platform`
  (`TenantContext`, usado em todo handler). `ModuleStructureTest` falhou duas
  vezes seguidas com "dependência não permitida" — mensagem de erro não avisa
  que são módulos abertos, só lista os alvos já aceitos.
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

### Spring Boot 4 e Spring Security 7 — confirme, não deduza

- **Nunca copie artifactId, pacote ou nome de API de tutorial de Boot 3.** Confirme
  contra o jar: `javap -classpath` para membro de classe, `unzip -l` para pacote,
  `repo1.maven.org/maven2/<path>/maven-metadata.xml` para versão.
- Why: dez quebras foram encontradas na primeira feature, e **nenhuma é
  dedutível**. Uma delas falha em silêncio.

| Boot 3 / Security 6 | Boot 4 / Security 7 |
|---|---|
| `flyway-core` sozinho roda as migrations | exige `spring-boot-starter-flyway` |
| `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |
| `HttpStatus.UNPROCESSABLE_ENTITY` | `UNPROCESSABLE_CONTENT` (RFC 9110) |
| `...boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc` | `...boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` |
| `@MockBean` | `@MockitoBean` |
| — | `thymeleaf-extras-springsecurity6` mantém o "6" |
| `...test.web.servlet.result.SecurityMockMvcResultMatchers` | `...test.web.servlet.response.SecurityMockMvcResultMatchers` |
| `SecurityProperties.DEFAULT_FILTER_ORDER` | `SecurityFilterProperties.DEFAULT_FILTER_ORDER`, jar `spring-boot-security` |
| `formLogin().session(...)` | não existe; usar `post("/login")` comum |
| `new NoResourceFoundException(method, path)` | exige três argumentos |

> A primeira é a perigosa: **o build passa, a aplicação sobe e zero migrations
> são aplicadas.** Só foi pega porque havia um teste afirmando que o Flyway
> tinha rodado. Toda configuração que pode falhar em silêncio precisa de um
> teste que afirme que ela funcionou.

### Ordem de filtro em relação à cadeia do Spring Security

- A cadeia é **um único filtro** no chain do servlet, em
  `SecurityFilterProperties.DEFAULT_FILTER_ORDER` (`-100`). Filtro que precise
  do principal tem que declarar ordem **maior** que isso.
- Não confie no javadoc: o do `TenantContextFilter` afirmava o contrário do que
  o `@Order` fazia, e o tenant nunca era populado.
- Why: quem vem antes lê um `SecurityContextHolder` vazio, e a falha aparece
  como 500 numa rota autenticada, longe da causa.

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

## Frontend

**Tela é parte da feature, não uma fase depois**:
- Uma feature só está pronta quando dá para usá-la pelo navegador.
- A ordem dentro da feature é domain → application → adapter de saída →
  controller e template. O template é o **último passo da mesma feature**.
- Não crie feature de "camada web" separada.
- Why: a tela é onde se descobre que a spec estava errada. Melhor descobrir na
  terça da mesma feature do que três features depois.

**Estrutura de templates**:

```
templates/
├── fragments/   layout.html, head.html, nav.html, footer.html, form-errors.html
├── error/       404.html, 500.html
├── auth/        login.html, cadastro.html
├── admin/       dashboard.html, empresa/, profissionais/, servicos/, agenda/, clientes/
└── public/      empresa.html, agendamento.html, confirmacao.html, meu-agendamento.html
```

- **Nome de template é em português**, espelhando a URL: `/admin/servicos` é
  renderizado por `admin/servicos/lista.html`, a partir de `ServiceController`.
- Why: é exceção explícita à regra de idioma. O caminho do template é parte do
  roteamento visível ao usuário, não um identificador de código — espelhar a URL
  torna a navegação óbvia.

**Fragmento nasce na segunda repetição, não na primeira**:
- A primeira tela carrega um layout mínimo. Extraia `th:fragment` quando a
  segunda tela mostrar o que de fato se repete.
- Não desenhe um design system antes de existir tela.
- Why: layout sem tela é especulação — a mesma ficção do runbook escrito antes
  do procedimento.

**Bootstrap 5 por CDN, versão fixada** (ADR 0012):
- Sempre com `integrity` e `crossorigin`. Nunca versão flutuante.
- No admin, use Bootstrap como vem — é ferramenta de trabalho, não vitrine.
- Na página pública, o tema sobrescreve as custom properties `--bs-*`
  (tipografia, cor, raio, sombra, ritmo vertical). **Não** escreva CSS ad-hoc
  por tela.
- Evite na página pública os componentes que denunciam Bootstrap: navbar e card
  padrão.

**A página pública é desenhada para o polegar**:
- Mobile-first. Alvo de toque com no mínimo 44px.
- O cliente está no 3G, em pé, na rua. Cada campo a mais é desistência.
- Why: é a única tela que representa o estabelecimento para os clientes dele.

**Sem JavaScript até existir interação real**:
- Formulário HTML e redirect resolvem cadastro, login e edição.
- HTMX entra apenas no seletor de horários (`TODO-005`/`TODO-006`), que é a
  única tela com interação de verdade.
- Why: adotar HTMX na tela de login seria adotá-lo por moda.

**Template não decide nada**:
- Thymeleaf formata o que o controller já resolveu. Sem `th:if` com regra de
  negócio, sem cálculo, sem chamada a serviço a partir da view.
- O controller entrega ao model um objeto já pronto para exibir.
- Why: regra dentro de template não é testável nem encontrável — e com
  `open-in-view: false` uma chamada lazy no template estoura em produção.

**Teste de tela verifica contrato, não markup**:
- `@WebMvcTest` com `MockMvc`: status, view escolhida, atributos do model e a
  presença dos elementos que importam.
- Não asserte sobre o HTML inteiro nem sobre classes de CSS.
- Why: teste sobre markup quebra a cada ajuste visual e ensina o time a ignorar
  teste vermelho.

## Testes que realmente garantem

**Script versionado precisa do bit de execução no índice do git**:
- Ao acrescentar qualquer `.sh` ou wrapper que o CI invoque como `./arquivo`,
  rode `git update-index --chmod=+x <arquivo>` e confirme com `git ls-files -s`
  que ele está `100755`, não `100644`.
- Why: o Windows não tem bit de execução, então o git registra `100644` e o
  runner Linux responde `Permission denied` com exit 126. O `.gitattributes`
  resolve fim de linha, **não** permissão — são dois problemas diferentes, e o
  primeiro estar resolvido não diz nada sobre o segundo.
- `.cmd` e `.bat` ficam `100644`: nunca são executados como programa Unix.

**Todo portão precisa ser visto falhando pelo menos uma vez**:
- Depois de escrever uma regra de ArchUnit, um piso de cobertura ou um teste de
  isolamento, **desligue o mecanismo de propósito** e confirme que ele falha.
  Depois restaure.
- Why: três portões desta base foram conferidos assim e os três estavam certos —
  mas dois outros, não conferidos, não pegavam nada. Portão que nunca falhou é
  portão que ninguém sabe se funciona.

**Teste que não roda é pior que teste ausente**:
- Ao acrescentar a primeira classe `*IT`, confirme que o `maven-failsafe-plugin`
  está no `pom.xml` e que ela aparece na saída do `verify`.
- Why: `SecurityRoutesIT` foi escrito e nunca executado. A suíte estava verde
  **porque o arquivo era ignorado**, e teria engolido em silêncio as quatro
  classes seguintes.

**Regra que cobre metade do que promete é pior que regra ausente**:
- `noClasses().beAnnotatedWith(...)` olha **anotação de classe**. Para cobrir
  método, é preciso a regra irmã com `noMethods()`.
- Escope todo predicado por nome ao pacote do projeto:
  `resideInAPackage("com.agendaia..")`. Sem isso, `SecurityContextRepository`
  do Spring casa com uma regra sobre "repositório".
- Why: falso positivo treina o time a ignorar a ferramenta, e cobertura parcial
  dá a sensação de garantia sem a garantia.

**Teste de fluxo autenticado segue o redirecionamento**:
- Capture a `MockHttpSession` do resultado e faça a requisição seguinte com ela.
  Parar no `302` não prova nada.
- Why: desde o Spring Security 6 o `SecurityContextHolder` vive na thread da
  requisição. Autenticar sem gravar no `SecurityContextRepository` produz um
  usuário autenticado no POST e deslogado no redirecionamento — **com a suíte
  verde**.

**Cenário que coincide com o padrão não testa nada**:
- Ao verificar "volta para a rota pretendida", use uma rota que **não** seja o
  `defaultSuccessUrl`. Ao verificar "usa o valor configurado", use um valor
  diferente do default.
- Why: o teste passa igual se o comportamento for ignorado.

**Sem `@Transactional` em teste de integração de fluxo web**:
- Limpe o estado no `@BeforeEach`. Em produção cada requisição abre a sua
  transação, e uma transação ambiente do teste esconde exatamente isso.

**FK nova exige atualizar a limpeza de toda IT que referencia a tabela apontada**:
- Quando uma tabela ganha `REFERENCES` para outra já existente, faça
  `grep -rn "deleteAllInBatch" src/test` sobre a tabela **referenciada** e
  ajuste a ordem — a nova tabela sai primeiro — antes de rodar `verify` pela
  primeira vez, não depois de ver o erro.
- Why: ITs que compartilham o mesmo container Testcontainers entre classes
  (a maioria neste projeto) executam na mesma base. A TODO-002 introduziu
  `professional.tenant_id → business.id` e quebrou a limpeza de três ITs da
  TODO-001 que já existiam — `DataIntegrityViolationException` só aparece
  quando a classe errada roda depois da que criou a linha referenciada,
  então pode passar despercebido localmente e falhar só na ordem do CI.
- **Aconteceu de novo na TODO-004**, apesar da regra já estar escrita aqui:
  `work_schedule`/`time_off → professional` e `business_operating_hours →
  business` quebraram a limpeza de **cinco** ITs de três features anteriores
  (`LoginIT`, `RegistrationIT`, `SlugUnavailableIT`, `ProfessionalRegistrationIT`,
  `CrossTenantIsolationIT`) de uma vez — só apareceu no `./mvnw verify`
  completo, não ao rodar a IT nova isolada com `-Dtest=`. **Rodar o `grep`
  antes de escrever a task de migration, não depois de ver o erro** — é
  exatamente o que esta entrada já pedia, e foi pulado mesmo assim.

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

A fatia vertical do cadastro de estabelecimento é o modelo a copiar. Ela existe,
compila e passa nos testes — leia o código, não a descrição.

```
HTTP  →  RegistrationController          adapter/in/web       devolve tela, nunca objeto
         ↓ RegisterBusinessCommand       application/command  o que entra, sem tipo de web
      →  RegisterBusinessUseCase         application/port/in  INTERFACE
         ↑ implementada por
         RegisterBusinessHandler         application          @Transactional mora aqui, e só aqui
         ↓
      →  Business.register(...)          domain               fábrica; construtor privado, sem setter
         SlugGenerator                   domain               Java puro, testável em milissegundos
         ↓
      →  BusinessRepository              domain               interface do Spring Data
         ↓
         V2__organization_create_...sql  db/migration         Flyway é dono do schema; ddl-auto: validate
```

| Camada | Arquivo |
|---|---|
| Controller | `organization/adapter/in/web/RegistrationController.java` |
| Porta de entrada | `organization/application/port/in/RegisterBusinessUseCase.java` |
| Caso de uso | `organization/application/RegisterBusinessHandler.java` |
| Domínio | `organization/domain/Business.java`, `SlugGenerator.java` |
| Migration | `db/migration/V2__organization_create_business_and_user.sql` |
| Teste puro | `organization/domain/SlugGeneratorTest.java` |
| Teste de camada web | `organization/adapter/in/web/RegistrationControllerTest.java` |
| Teste de ponta a ponta | `organization/RegistrationIT.java` |

O que este exemplo demonstra e vale imitar:

- **O controller não conhece repositório nem entidade.** Fala com a interface do
  caso de uso e devolve nome de view.
- **Erro de negócio vira erro no campo que o causou**, preservando o
  preenchimento — não mensagem solta no topo, que faz o usuário reescrever tudo.
- **A garantia é do banco.** A verificação em memória existe para dar mensagem
  boa; quem impede duplicata é o `UNIQUE`, e o adapter traduz a violação em
  exceção de domínio.
- **Três níveis de teste, com propósitos distintos**: puro para a regra, camada
  web isolada para a tradução HTTP, e integração com Postgres real para o fluxo.

> Contexto completo, incluindo o que deu errado no caminho:
> [`sdd/features/20260830-cadastro-estabelecimento-login/`](features/20260830-cadastro-estabelecimento-login/README.md).

## Last Updated

2026-08-29 — versão inicial, derivada dos ADRs 0001 a 0009.
2026-08-30 — promovidos os aprendizados da TODO-001 no `/sdd.finish`: as dez
quebras do Boot 4 / Security 7, a ordem de filtro em relação à cadeia do Spring
Security, a seção "Testes que realmente garantem" e o exemplo de ponta a ponta,
que estava pendente esperando a primeira fatia vertical (DEBT-004).
