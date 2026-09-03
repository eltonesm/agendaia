# ADR 0002 — Clean Architecture com rigor proporcional ao domínio

- **Status:** Aceito
- **Data:** 2026-08-28

## Contexto

A proposta inicial aplicava Clean Architecture de forma idêntica em todos os
contextos: modelo de domínio puro, entidade JPA separada e mapeamento explícito
entre os dois, em cada contexto.

Feita a conta sobre o MVP real, isso significava dez arquivos para cadastrar um
serviço com nome, preço e duração — um CRUD sem uma única invariante
interessante. Metade do código do MVP seria cerimônia.

Ao mesmo tempo, `scheduling` tem regra de verdade: conflito de horário,
interseção de jornadas, política de cancelamento, transição de status. Essa
regra precisa ser testável em milissegundos, sem subir Spring nem banco.

DDD sempre disse isso: core domain merece investimento, subdomínio de suporte
merece CRUD.

## Decisão

Nós vamos aplicar **rigor arquitetural proporcional ao tipo de subdomínio**.

Em **`scheduling`** (core): domínio em Java puro, entidade JPA separada,
mapeamento explícito, portas e casos de uso completos.

Em **`organization`, `catalog` e `customer`** (suporte): a entidade JPA **é** o
modelo, e o caso de uso fala direto com o repositório Spring Data. A separação
`domain` / `application` / `adapter` em pacotes permanece; o que desaparece é a
classe espelho e o mapper.

A regra de dependência continua valendo em todos: `adapter → application →
domain`, nunca o contrário.

## Consequências

Aproximadamente metade do código do MVP deixa de existir, e a atenção fica onde
o risco está.

Módulos de suporte ficam acoplados ao JPA: mudar de ORM neles seria reescrita.
Aceitamos isso conscientemente — o custo de trocar de ORM num CRUD é baixo, e a
probabilidade de fazê-lo é menor ainda.

A promoção de um contexto de suporte para o regime completo é **local**: acontece
dentro de um pacote, sem refactor global.

**Risco real:** a assimetria precisa estar escrita e vigiada, ou o rigor vaza
para os contextos errados (excesso de cerimônia) ou some do `scheduling`
(entidade JPA anotada virando modelo de domínio). É por isso que a regra mora
no `PATTERNS.md` e é verificada por ArchUnit.

## Gatilho de reavaliação

Quando um contexto de suporte acumular a terceira regra de negócio de verdade —
não validação de formato, mas regra que se queira testar isoladamente — ele é
promovido ao regime completo.

## Amendment — 2026-09-02: Repository sai de `domain`, vai para `application.port.out`

Esta ADR nunca disse explicitamente onde a *interface* de repositório deveria
morar — só que "o caso de uso fala direto com o repositório Spring Data". Na
prática, as cinco interfaces implementadas (TODO-001/002/003) foram parar em
`<contexto>.domain`, ao lado de `Business`, `Professional`, `Service` etc.

Isso segue Eric Evans (DDD clássico, 2003), que trata Repository como padrão
de domínio — mas diverge da prática moderna de hexagonal architecture em Java,
que trata repositório como **porta de saída** (`application/port/out`), não
como parte do modelo. É o desenho do buckpal, de Tom Hombergs
(`Get Your Hands Dirty on Clean Architecture`), referência mais citada da
comunidade Java/Spring para o assunto: `domain` é só modelo puro (entidade,
value object), e as portas — de entrada (`UseCase`) e de saída (`Repository`)
— moram em `application/port/{in,out}`.

**Decisão**: mover as cinco interfaces para `<contexto>.application.port.out`.
`domain` passa a conter só Entity, Value Object, enum e exceção — nunca uma
interface motivada por persistência, nem em contexto de suporte.

**O que não muda**: o resto desta ADR continua valendo integralmente. Em
contexto de suporte, a entidade JPA continua sendo o modelo, sem classe
espelho nem mapper — a porta de saída **é** a interface do Spring Data, sem
adapter no meio. Mover de pacote não introduziu abstração nova nem cerimônia;
foi mecânico (`package` + imports), zero mudança de comportamento, verificado
pelo `./mvnw clean verify` completo antes e depois. Em `scheduling` (regime
completo), a porta de saída já nasce implementada por um adapter em
`adapter.out.persistence` com mapeamento explícito — isso sempre esteve
correto e não mudou.

Travado por uma regra nova de ArchUnit
(`dominio_de_suporte_nao_conhece_spring` em `ArchitectureTest.java`): domínio
de contexto de suporte pode importar `jakarta.persistence` (a entidade
continua anotada), nunca `org.springframework`.
