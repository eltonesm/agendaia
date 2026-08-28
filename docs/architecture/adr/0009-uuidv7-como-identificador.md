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

A geração fica na aplicação, para não depender da versão do PostgreSQL da
imagem — `uuidv7()` nativo existe apenas a partir do Postgres 18. A versão
exata da imagem deve ser confirmada durante a Fase 0.

## Consequências

Identificadores opacos em URL, com boa localidade de escrita no índice.

Adicionamos uma dependência pequena (ou uma implementação própria) para gerar
v7, já que o `java.util.UUID` do Java 21 não oferece geração v7.

O identificador carrega o instante de criação embutido — o que é conveniente
para ordenação e depuração, e é uma exposição pequena e aceitável, já que a
data de criação de um agendamento não é informação sensível.

## Gatilho de reavaliação

Se a geração na aplicação se mostrar um incômodo e a imagem do Postgres for
atualizada para 18 ou superior, migrar a geração para o banco — mudança de
implementação, não de decisão.
