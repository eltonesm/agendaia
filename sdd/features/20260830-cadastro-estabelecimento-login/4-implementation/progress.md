# Progresso da implementação — cadastro-estabelecimento-login

Feature: TODO-001 · Início: 2026-08-30 · Fim: 2026-08-30
Estratégia: `batched`, por nível de dependência.

> **Escrito ao fim, não durante.** O `/sdd.build` foi executado em linha, sem a
> escrituração incremental que o kit prevê, e este arquivo foi reconstruído a
> partir dos commits e dos artefatos no `/sdd.finish`. Os fatos abaixo são
> verificáveis nos commits citados; a cronologia fina, não.

## Resultado

- **Tests passing: 214/214** (181 unitários + 33 de integração)
- **Coverage: 91%** de instruções — piso travado no `pom.xml`, conferido falhando
- Cobertura de ramos: 68 por cento, deliberadamente sem piso
- Linter errors: 0 · Type errors: 0
- `./mvnw clean verify` verde

## Commits

| Commit | Tasks |
|---|---|
| `64075b4` | TASK-002, TASK-005, TASK-009 |
| `2aa3252` | TASK-004 |
| `67847cb` | TASK-006, TASK-007 |
| `8c13919` | TASK-008 |
| `6ce1688` | TASK-010 |
| `8b5cd10` | TASK-011 |
| `9aab456` | TASK-012, TASK-017 |
| `a8abd92` | TASK-013 |
| `93a2f07` | TASK-014, TASK-015, TASK-016 |
| `86446f9` | coerência do meta.md e cobertura no build |

TASK-001 e TASK-003 entraram antes, junto do andaime.

---

## Tasks

#### TASK-001: Adicionar dependencias de seguranca e Thymeleaf
- **Status**: ✅ Completed
- Commit: `64075b4`

#### TASK-002: Migration V2 — tabelas business e app_user
- **Status**: ✅ Completed
- Commit: `64075b4`

#### TASK-003: SlugGenerator e ReservedSlugs com testes puros
- **Status**: ✅ Completed
- Commit: `23610aa`

#### TASK-004: Entidades Business e User com repositorios
- **Status**: ✅ Completed
- Commit: `2aa3252`

#### TASK-005: platform — AuthenticatedUser e TenantContext com filtro
- **Status**: ✅ Completed
- Commit: `64075b4`

#### TASK-006: organization — BusinessUserDetailsService
- **Status**: ✅ Completed
- Commit: `67847cb`

#### TASK-007: Cadeia de filtros de seguranca
- **Status**: ✅ Completed
- Commit: `67847cb`

#### TASK-008: Caso de uso do cadastro de estabelecimento
- **Status**: ✅ Completed
- Commit: `8c13919`

#### TASK-009: Layout base e paginas de erro
- **Status**: ✅ Completed
- Commit: `64075b4`

#### TASK-010: Tela e controller de cadastro
- **Status**: ✅ Completed
- Commit: `6ce1688`

#### TASK-011: Telas de login, logout e painel minimo
- **Status**: ✅ Completed
- Commit: `8b5cd10`

#### TASK-012: Testes de integracao dos tres cenarios E2E
- **Status**: ✅ Completed
- Commit: `9aab456`

#### TASK-013: Teste de isolamento entre tenants
- **Status**: ✅ Completed
- Commit: `a8abd92`

#### TASK-014: Code review
- **Status**: ✅ Completed
- Commit: `93a2f07`

#### TASK-015: Revisao de performance
- **Status**: ✅ Completed
- Commit: `93a2f07`

#### TASK-016: Revisao de seguranca
- **Status**: ✅ Completed
- Commit: `93a2f07`

#### TASK-017: Sugestao de variacao livre quando o link esta em uso
- **Status**: ✅ Completed
- Commit: `9aab456`

---

## Aprendizados

### Spring Boot 4 / Spring Framework 7 — dez quebras confirmadas contra os jars

O aprendizado mais reaproveitável desta feature, e o mais caro: **nenhuma destas
é dedutível**, e tutorial de Boot 3 leva a todas. Cada uma foi confirmada com
`javap` ou `unzip -l` no jar do repositório local, nunca por memória.

| # | Boot 3 / Security 6 | Boot 4 / Security 7 |
|---|---|---|
| 1 | `flyway-core` sozinho roda as migrations | exige `spring-boot-starter-flyway` |
| 2 | `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |
| 3 | `HttpStatus.UNPROCESSABLE_ENTITY` | `UNPROCESSABLE_CONTENT` (RFC 9110) |
| 4 | `...boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc` | `...boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` |
| 5 | `@MockBean` | `@MockitoBean` |
| 6 | — | `thymeleaf-extras-springsecurity6` mantém o "6" |
| 7 | `...test.web.servlet.result.SecurityMockMvcResultMatchers` | `...test.web.servlet.response.SecurityMockMvcResultMatchers` |
| 8 | `SecurityProperties.DEFAULT_FILTER_ORDER` | `SecurityFilterProperties.DEFAULT_FILTER_ORDER`, no jar `spring-boot-security` |
| 9 | `formLogin().session(...)` | não existe; usar `post("/login")` comum |
| 10 | `new NoResourceFoundException(method, path)` | exige três argumentos |

**A nº 1 é a mais perigosa: falhou em silêncio.** O build passava, a aplicação
subia, e zero migrations eram aplicadas. Só foi pega porque havia um teste que
afirmava que o Flyway tinha rodado.

### Um teste que não roda é pior que teste ausente

O `maven-failsafe-plugin` não estava no `pom.xml`. `SecurityRoutesIT` foi
escrito e **nunca executado** — a suíte estava verde porque o arquivo era
ignorado. Teria engolido em silêncio as quatro classes `*IT` seguintes.

### Regra que cobre metade do que promete é pior que regra ausente

Duas vezes, e das duas o defeito atravessou a rede:

- `transacao_so_na_application` usa `noClasses().beAnnotatedWith()`, que olha
  **anotação de classe**. Um `@Transactional` de método passou livre.
- `controller_nao_fala_com_repositorio` casava `SecurityContextRepository`, do
  Spring, por sufixo de nome. Três falsos positivos. Corrigido escopando o
  predicado ao pacote do projeto — a regra, não o código.

### Piso de qualidade declarado em documento apodrece

O `meta.md` dizia `coverage_target: "80%"` como "piso do build". **Não havia
JaCoCo no `pom.xml`.** Sempre que houver um número num documento, o portão que
o verifica tem que existir — e tem que ser visto falhando pelo menos uma vez.

### Portão só conta depois de você o ver falhar

Três portões desta feature foram conferidos desligando o mecanismo de propósito:
as regras de ArchUnit (com uma classe violadora temporária), o
`CrossTenantIsolationIT` (reintroduzindo o `@Order` quebrado) e o piso de
cobertura (subindo-o a 99%). Os três falharam como deviam.

### Autenticação programática não chega à sessão sozinha

Desde o Spring Security 6, `SecurityContextHolder` vive na thread da requisição.
Autenticar sem gravar no `SecurityContextRepository` produz um usuário
autenticado no POST e deslogado no redirecionamento seguinte — **com a suíte
verde**, se o teste parar no `302`. Teste de fluxo autenticado tem que seguir o
redirecionamento carregando a mesma sessão.

### Ordem de filtro em relação à cadeia do Spring Security

A cadeia é um único filtro no chain do servlet, em `-100`. Quem vem antes lê um
`SecurityContextHolder` vazio; quem vem depois roda dentro dela, com o principal
resolvido. Um filtro que precise do principal e declare ordem menor que `-100`
falha em silêncio — e o javadoc do nosso afirmava o contrário do que o código
fazia.

### Principal na sessão precisa implementar CredentialsContainer

`ProviderManager.eraseCredentials` só age em principal que seja
`CredentialsContainer`. Sem isso o hash BCrypt fica na sessão. E cada caminho de
autenticação que não passe por provider tem que apagar por conta própria.

### Cenário de teste que coincide com o padrão não testa nada

`voltaParaDestinoPretendido` usava `/admin/dashboard` como rota pretendida — que
é também o `defaultSuccessUrl`. Passaria mesmo se o destino guardado fosse
ignorado.

### PostgreSQL 18 mudou o ponto de montagem do volume

`/var/lib/postgresql`, não `/var/lib/postgresql/data`. Montar no lugar antigo põe
o contêiner em crash loop. E `LANG=pt_BR.UTF-8` não existe no Alpine (musl): usar
`--locale=C.UTF-8 --locale-provider=icu --icu-locale=pt-BR`.

### search.maven.org estava mais de um ano defasado

Mostrava Spring Modulith 1.4.1 como a última versão; a real era 2.x. Consultar
`repo1.maven.org/maven2/<path>/maven-metadata.xml` diretamente.

---

## Desvios do processo

1. **As três revisões da camada 3 foram feitas em linha** na primeira passada, e
   só depois pelos Skills, no `/sdd.finish`. Ver `meta.md`.
2. **TASK-017 foi acrescentada à mão** durante o build, ao descobrir que a spec
   funcional pedia a sugestão de slug e a técnica omitia. O caminho previsto era
   `/sdd.fix`. A spec técnica ganhou o DD-4.1 junto.
3. **Este `progress.md` foi escrito ao fim**, não durante.
