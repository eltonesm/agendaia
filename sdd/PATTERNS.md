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
- Why: com um único módulo Maven (ADR 0001), nada físico impede o import — a
  fronteira só existe porque o ArchUnit a verifica.

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

**Sufixos obrigatórios**:
- `...UseCase` — caso de uso em `application`. Em `catalog` **nunca** use
  `Service` sozinho: colide com o `Service` do domínio e com `@Service`.
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

## Exemplo de ponta a ponta

<!--
  PENDENTE: adicionar após a primeira fatia vertical (cadastro de empresa e
  login). Um caso de uso completo e curto, de controller a migration, é de onde
  o agente copia o padrão — vale mais que todos os parágrafos acima juntos.
  Não inventar antes de existir código real que compile e passe nos testes.
-->

## Last Updated

2026-08-29 — versão inicial, derivada dos ADRs 0001 a 0009.
