# ADR 0002 — Clean Architecture com rigor proporcional ao domínio

- **Status:** Aceito
- **Data:** 2026-08-28

## Contexto

A proposta inicial aplicava Clean Architecture de forma idêntica em todos os
módulos: modelo de domínio puro, entidade JPA separada e mapeamento explícito
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

A promoção de um módulo de suporte para o regime completo é **local**: acontece
dentro de um módulo, sem refactor global.

**Risco real:** a assimetria precisa estar escrita e vigiada, ou o rigor vaza
para os módulos errados (excesso de cerimônia) ou some do `scheduling`
(entidade JPA anotada virando modelo de domínio). É por isso que a regra mora
no `PATTERNS.md` e é verificada por ArchUnit.

## Gatilho de reavaliação

Quando um módulo de suporte acumular a terceira regra de negócio de verdade —
não validação de formato, mas regra que se queira testar isoladamente — ele é
promovido ao regime completo.
