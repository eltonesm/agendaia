# ADR 0009 — UUIDv7 como identificador

- **Status:** Aceito
- **Data:** 2026-08-28

## Contexto

Num SaaS multi-tenant, identificador sequencial exposto em URL vaza informação
(quantos clientes existem, quanto o concorrente cresceu) e facilita enumeração.
UUID resolve isso.

Mas UUID **aleatório** (v4) como chave primária espalha as inserções pelo índice
B-tree inteiro. Numa tabela que só cresce — e `appointment` só cresce — isso
aumenta a escrita e fragmenta o índice ao longo do tempo.

UUIDv7 é ordenado no tempo: mantém as inserções na ponta do índice, preservando
a opacidade externa e a localidade de escrita.

Trocar o tipo de chave primária depois do primeiro cliente pagante é migração de
dados com janela de indisponibilidade.

## Decisão

Nós vamos usar **UUIDv7** como identificador de todas as raízes de agregado.

A geração fica **na aplicação**, não no banco.

> **Confirmado na Fase 0 (2026-08-28):** a imagem `postgres:18-alpine` é a
> 18.6 e tem `uuidv7()` nativo, verificado por consulta direta. O item que este
> ADR deixou em aberto está fechado — e a decisão de gerar na aplicação
> **permanece**, por uma razão melhor que a original.

A razão original era defensiva: não depender de uma versão de PostgreSQL que
ainda não havia sido confirmada. A razão real é arquitetural: **o agregado
precisa ter identidade no instante em que nasce no domínio**, antes de qualquer
repositório vê-lo. Identidade gerada no `INSERT` obrigaria a ida ao banco para
saber quem o objeto é, fazendo a identidade do agregado depender da
persistência — exatamente a inversão que o ADR 0002 existe para evitar no core.

`uuidv7()` do banco continua útil onde não há agregado envolvido: migrations,
scripts de carga e dados de teste.

## Consequências

Identificadores opacos em URL, com boa localidade de escrita no índice.

Adicionamos uma dependência pequena (ou uma implementação própria) para gerar
v7, já que o `java.util.UUID` do Java 21 não oferece geração v7.

O identificador carrega o instante de criação embutido — o que é conveniente
para ordenação e depuração, e é uma exposição pequena e aceitável, já que a
data de criação de um agendamento não é informação sensível.

## Gatilho de reavaliação

Nenhum previsto. A razão é arquitetural, não circunstancial: mesmo com
`uuidv7()` disponível no banco, gerar no `INSERT` continuaria acoplando a
identidade do agregado à persistência.
