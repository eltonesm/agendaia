# ADR 0001 — Modular Monolith com um módulo Maven por contexto delimitado

- **Status:** Aceito
- **Data:** 2026-08-28

## Contexto

O AgendaIA é um SaaS de agendamento construído por uma equipe muito pequena,
validando o produto com um único estabelecimento piloto, rodando numa VPS. A
infraestrutura precisa custar quase nada.

Ao mesmo tempo, o domínio tem fronteiras conceituais reais: a jornada de um
profissional, o catálogo de serviços e o cálculo de disponibilidade são
assuntos diferentes, com vocabulários diferentes. Sem fronteira explícita, essas
áreas se misturam e o sistema vira um novelo em poucos meses.

Microsserviços resolveriam o isolamento e destruiriam o orçamento e a
velocidade. Um monólito sem estrutura interna resolveria a velocidade e criaria
a dívida.

## Decisão

Nós vamos construir um **Modular Monolith**, com **um módulo Maven por contexto
delimitado**, deployado como uma única aplicação.

Os módulos são: `shared-kernel`, `platform`, `organization`, `catalog`,
`scheduling`, `customer` e `bootstrap`. Apenas `bootstrap` é uma aplicação
Spring Boot executável.

## Consequências

A regra de camada passa a ser garantida pelo compilador: o domínio não compila
com JPA no classpath porque a dependência não está declarada. Isso é mais forte
que qualquer convenção de equipe.

Extrair um contexto para serviço separado, se um dia fizer sentido, é um
trabalho contido — a fronteira já existe.

**O que fica pior:** o build fica mais lento e mais cerimonioso; adicionar uma
classe nova exige saber em que módulo ela mora; e o ciclo de desenvolvimento de
templates Thymeleaf perde o hot reload, porque um template dentro de um módulo
só é visto pelo `bootstrap` depois de o módulo ser recompilado (mitigado por
configuração de perfil de desenvolvimento).

**O que a separação por módulo Maven *não* garante:** a fronteira entre
contextos. Se `scheduling` declarar dependência de `catalog`, enxerga todos os
pacotes de `catalog`. Só o ArchUnit segura isso — ver ADR 0002.

## Gatilho de reavaliação

Se o build local passar de dois minutos, ou se a fricção de desenvolvimento de
templates se mostrar insuportável na prática, considerar colapsar os contextos
em um único módulo com pacotes e manter a fronteira apenas por ArchUnit.
