# ADR 0001 — Modular Monolith com contextos como pacotes, num único módulo Maven

- **Status:** Aceito
- **Data:** 2026-08-28
- **Revisado:** 2026-08-28 — a primeira versão deste ADR decidia por um módulo
  Maven por contexto, com base numa premissa errada. Ver "Correção de rota".

## Contexto

O AgendaIA é um SaaS de agendamento construído por uma pessoa, validando o
produto com um único estabelecimento piloto, rodando numa VPS.

O domínio tem fronteiras conceituais reais: jornada de profissional, catálogo de
serviços e cálculo de disponibilidade são assuntos diferentes, com vocabulários
diferentes. Sem fronteira explícita, essas áreas se misturam e o sistema vira um
novelo em poucos meses.

A pergunta não é *se* haverá fronteira, e sim **o que a garante**: o build ou um
teste.

## Correção de rota

A primeira versão deste ADR escolheu um módulo Maven por contexto, justificando
que "a regra de camada passa a ser garantida pelo compilador: o domínio não
compila com JPA no classpath".

**Isso é falso no desenho que estava proposto.** Como o módulo era o *contexto* e
não a *camada*, o módulo `scheduling` precisava declarar JPA — o adapter de
persistência dele usa. Uma vez declarada, a dependência está no classpath do
módulo inteiro, inclusive do pacote `domain`. Nada impediria um `@Entity` dentro
de `scheduling.domain`.

Compilador só garantiria a regra de camada com módulos *por camada*
(`domain`, `application`, `infrastructure`) — o que espalharia cada contexto por
quatro módulos e destruiria a coesão que motivou a modularização.

Ou seja: o principal benefício alegado não existia, e a decisão precisava ser
refeita sem ele.

## Decisão

Nós vamos construir um **Modular Monolith num único módulo Maven**. Os contextos
delimitados são **pacotes** sob `com.agendaia`, e as fronteiras são garantidas
por **ArchUnit**, não pelo build.

```
com.agendaia
├── shared        tipos puros compartilhados
├── platform      infraestrutura transversal
├── organization  empresa (tenant), usuário, profissional, jornada
├── catalog       serviços e ofertas
├── scheduling    core domain
└── customer      cliente atendido
```

O ArchUnit garante, no `verify`:

- nenhuma classe em `scheduling.domain` importa `org.springframework` ou `jakarta.persistence`;
- `application` não importa `adapter`, em nenhum contexto;
- um contexto só importa o pacote `api` de outro contexto;
- não há ciclos entre contextos.

## Consequências

O que se perde ao abrir mão dos módulos Maven é **menos do que parecia**: das
quatro regras acima, apenas a proibição de ciclo teria garantia automática do
Maven — e o ArchUnit também a oferece. As outras três nunca foram garantidas
pelo build; sempre dependeram de ArchUnit.

O que se ganha é concreto:

- **O hot reload de template volta a funcionar.** Metade do valor de escolher
  Thymeleaf (ADR 0007) é o ciclo salvar → F5, e ele se perdia quando o template
  vivia num módulo diferente do executável. Manter os dois ADRs juntos exigia
  esta escolha.
- Build mais rápido, um `pom.xml` em vez de sete.
- Menos atrito de IDE: sem rebuild de módulo para enxergar mudança.

O que fica pior, e é preciso encarar:

- **A fronteira passa a ser verificada em teste, não em compilação.** Um import
  proibido compila; só quebra no `verify`. O feedback é minutos mais lento, e um
  time que ignore falha de ArchUnit não tem proteção nenhuma.
- Todo o classpath fica disponível a todo pacote. Nada *físico* impede
  `scheduling.domain` de importar Spring — só a regra.
- Extrair um contexto para serviço separado fica um passo mais longe, porque
  seria preciso primeiro promovê-lo a módulo. Um cenário que o escopo do MVP
  exclui explicitamente.

## Gatilho de reavaliação

Promover contextos a módulos Maven quando aparecer o primeiro destes:

- mais de três pessoas mexendo no código simultaneamente, onde a fronteira
  física evita pisar no pé do outro;
- intenção real de extrair um contexto como serviço;
- histórico de falhas de ArchUnit sendo ignoradas ou desabilitadas.

A migração é mecânica nos dois sentidos — mover diretórios e escrever poms —,
o que é justamente por que a decisão mais simples é a certa agora: o custo de
estar errado é uma tarde de trabalho.

Vale avaliar também o **Spring Modulith**, que foi feito para dar dentes a este
desenho: verifica fronteiras entre módulos em teste, gera documentação da
estrutura e traz registro confiável de eventos. Confirmar a versão compatível
com Spring Boot 4.1 antes de adotar.
