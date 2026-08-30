# ADR 0010 — Spring Modulith na fronteira entre contextos, ArchUnit dentro deles

- **Status:** Aceito
- **Data:** 2026-08-29

## Contexto

O [ADR 0001](0001-modular-monolith-com-contextos-como-pacotes.md) abriu mão dos
módulos Maven e colocou **toda** a garantia de fronteira nas costas do ArchUnit.
A decisão continua certa, mas ela deixou duas fragilidades:

1. **As regras precisam ser escritas e mantidas por nós.** Encapsulamento e
   detecção de ciclo são código nosso, com bug nosso.
2. **A dependência entre contextos fica implícita.** Para saber que
   `scheduling` depende de `catalog`, é preciso ler os imports. Um acoplamento
   novo entra sem que ninguém perceba, porque não há lugar onde ele apareça.

O mesmo ADR deixou registrado "avaliar Spring Modulith", com a ressalva de
confirmar a compatibilidade com o Spring Boot 4.1.

**Confirmado em 2026-08-29** pelos metadados do Maven Central: a linha estável é
a **2.1.1**, e o versionamento pareia com o Boot na mesma cadência da linha
anterior (Modulith 1.x ↔ Boot 3.x, 2.x ↔ Boot 4.x). O índice de busca do
`search.maven.org` mostrava 1.4.1 como última versão — estava defasado em mais
de um ano, e teria levado à conclusão errada de que Modulith não serve para o
Boot 4.

## Decisão

Nós vamos usar **os dois, com escopos distintos e sem sobreposição**.

**Spring Modulith cuida do que atravessa a fronteira:**

- encapsulamento — o que é interno de um contexto é inacessível de fora;
- ausência de ciclos entre contextos;
- dependências entre contextos **declaradas explicitamente**, não inferidas;
- documentação gerada da estrutura.

**ArchUnit cuida do que acontece dentro do contexto:**

- `scheduling.domain` não importa `org.springframework` nem `jakarta.persistence`;
- `application` não importa `adapter`;
- controller não injeta repositório;
- sufixos obrigatórios de nome;
- regras específicas: sem `@Data` em entidade JPA, sem Spring em teste de domínio.

### A convenção `api` precisa de uma anotação

O Modulith trata cada **subpacote direto** de `com.agendaia` como um módulo, e
por padrão considera público apenas o que está na **raiz** do módulo —
subpacotes são internos. Nossa convenção põe o contrato em
`com.agendaia.<contexto>.api`, que pelo padrão seria interno.

A correção é declarar o pacote como interface nomeada, no `package-info.java`:

```java
@org.springframework.modulith.NamedInterface("api")
package com.agendaia.catalog.api;
```

E cada contexto declara de quem depende:

```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "catalog :: api", "organization :: api", "customer :: api" })
package com.agendaia.scheduling;
```

### A API entre contextos é grossa, não conversadeira

Toda operação exposta na `api` de um contexto é desenhada **como se já fosse
remota**: em lote, de granularidade grossa, sem N+1.

```java
// Ruim — parece inocente hoje, é fatal se um dia virar chamada de rede
Offering findOffering(UUID id);

// Bom — uma chamada, independente de quantos
List<OfferingView> findOfferings(TenantId tenant, Set<UUID> professionalIds);
```

## Consequências

**Acoplamento novo passa a ser visível.** Adicionar uma dependência entre
contextos exige editar `allowedDependencies` num `package-info.java`. Isso
aparece no diff, e é revisável — hoje um import novo passa despercebido.

**Menos código nosso para manter.** Encapsulamento e ciclo saem do nosso
ArchUnit e viram um `ApplicationModules.of(Application.class).verify()`.

**Documentação que não drifta.** O Modulith gera diagrama de componentes e
"module canvas" a partir do código.

O custo é pequeno: uma dependência, uma anotação por contexto e um teste.

**O que fica pior:**

- Ficamos amarrados ao modelo de módulos do Modulith — subpacote direto do
  pacote raiz. Uma estrutura de pacotes diferente exigiria configuração extra
  ou abandono da ferramenta.
- Mais uma dependência para acompanhar em atualização de Boot. O Modulith tem
  cadência própria e pode atrasar em relação a uma versão de Boot que
  precisemos.
- A biblioteca traz recursos que **não vamos usar agora** — registro de
  publicação de eventos, `@ApplicationModuleTest`, integração com observabilidade.
  Estar disponível convida a usar. No MVP, usamos apenas verificação de
  estrutura e nomeação de interfaces.

**O que continua não sendo verdade:** nada disso é garantido pelo compilador. É
tudo verificado em teste. Um build que não roda os testes não tem proteção
nenhuma — por isso `./mvnw verify` verde é condição de merge.

## Sobre microsserviços

Esta decisão **não se justifica** por microsserviços futuros. Desenhar para uma
arquitetura que talvez nunca seja construída é a forma clássica de pagar caro
por nada, e o escopo do MVP exclui microsserviços explicitamente.

A justificativa é o enforcement de hoje, que ficou frágil no ADR 0001. Extração
mais barata amanhã é subproduto.

Dito isso, as três coisas que tornariam a extração impossível continuam
proibidas, e agora duas delas são verificadas: transação atravessando contexto,
JOIN entre tabelas de contextos diferentes, e API conversadeira.

## Gatilho de reavaliação

Se o Spring Modulith atrasar em relação a uma versão de Spring Boot que
precisemos adotar, remover a dependência e reescrever à mão as regras que ele
verificava — são quatro, e o ArchUnit já está no projeto para as demais. A
saída é barata justamente porque o escopo dele aqui é estreito.
