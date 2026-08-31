# Resumo de execução — cadastro-estabelecimento-login

Feature: TODO-001 · Ciclo completo em 2026-08-30 · Estratégia: `batched`

## Tarefas

| | |
|---|---|
| Planejadas no `/sdd.plan` | 16 |
| Acrescentadas durante o build | 1 (TASK-017) |
| Concluídas | 17 / 17 |
| Canceladas | 0 |

A TASK-017 nasceu de uma divergência entre camadas: a spec funcional pedia, em
dois pontos, que o erro de link em uso sugerisse uma variação disponível, e nem
a spec técnica nem as 16 tasks cobriam. Encontrada ao escrever o teste do E2E-3.

## Testes

| Tipo | Quantidade | Onde roda |
|---|---|---|
| Unitários | 181 | Surefire, `./mvnw test` |
| Integração | 33 | Failsafe, `./mvnw verify`, Postgres real via Testcontainers |
| **Total** | **214** | todos verdes |

Cobertura: **91% de instruções**, 68% de ramos. O piso de 80% está travado no
JaCoCo dentro do próprio `verify`, e foi conferido falhando.

## Portões de qualidade

| Portão | Veredito | Achados |
|---|---|---|
| Build e testes (`sdd-validator`) | PASSED | — |
| Revisão de código (`sdd-code-reviewer`) | APPROVED | 2, corrigidos na TASK-014 |
| Revisão de segurança (`sdd-code-reviewer`) | CAN_PROCEED_WITH_WARNINGS | 1 menor → DEBT-014 |
| Revisão de performance | PASSED | 0 |
| Consistência entre camadas (`sdd-layer-analyzer`) | 3 achados reais, corrigidos; 1 falso positivo | ver abaixo |

O `sdd-layer-analyzer` devolveu `CANNOT_PROCEED` por um achado **HIGH incorreto**
— afirmou que TASK-010 e TASK-011 nunca tinham sido commitadas e que
`SecurityConfig.java` tinha alteração local pendente. Ele leu o snapshot de
`git status` do início da sessão em vez de consultar o git: os três arquivos
estavam em `HEAD` (`git ls-tree`), `SecurityConfig.java` não tinha diff, e o
branch estava publicado. Os outros três achados eram reais e foram corrigidos:

1. **`tasks.json` sub-declarava os arquivos de cada task.** 66 arquivos
   declarados agora, contra 37 antes. Todos conferidos como existentes; os
   únicos de produção que continuam fora são o `AgendaIaApplication` e os seis
   `package-info`, do andaime da Fase 0, que por regra não passa pelo ciclo SDD.
2. **A spec técnica listava `organization/config/OrganizationConfig.java`**, que
   nunca foi criado e não faz falta. Linha removida; o diagrama ganhou os
   arquivos que existiam e ele omitia.
3. **A tabela de rotas divergia do código** em dois pontos: dizia
   `/login?logout` onde o `SecurityConfig` redireciona para `/login?saiu`, e não
   listava `/`, `/error`, `/img/**` e `/favicon.ico` entre as rotas públicas.

## Defeitos encontrados durante a implementação

| Defeito | Quem pegou | Onde ficou travado |
|---|---|---|
| `TenantContextFilter` com ordem invertida — tenant nunca populado, painel 500 | `LoginIT` | `CrossTenantIsolationIT` |
| `GlobalExceptionHandler` transformando 404 em 500 | `SecurityRoutesIT` | `GlobalExceptionHandlerTest` |
| `maven-failsafe-plugin` ausente — `SecurityRoutesIT` nunca executava | inspeção do build | o próprio plugin |
| `@Transactional` em adapter, invisível para a regra de ArchUnit | revisão de código | regra nova, com `noMethods()` |
| Hash BCrypt guardado na sessão | revisão de segurança | um teste por caminho de autenticação |
| Barra final na URL base duplicando a barra do link público | segunda passada | `ViewDashboardHandlerTest` |
| Piso de cobertura declarado que nenhum plugin verificava | segunda passada | JaCoCo, conferido falhando |
| `voltaParaDestinoPretendido` usando a rota que coincide com o `defaultSuccessUrl` | leitura da spec | `/admin/agenda` |

## Desvios de processo

1. **`progress.md` foi escrito ao fim**, não durante. O `/sdd.build` correu em
   linha, sem a escrituração incremental que o kit prevê.
2. **As revisões da camada 3 foram feitas em linha na TASK-014/015/016**, e só
   depois pelos Skills, no `/sdd.finish`. Os Skills confirmaram os achados já
   corrigidos e acrescentaram um (DEBT-014).
3. **TASK-017 foi acrescentada à mão** durante o build; o caminho previsto era
   `/sdd.fix`.

## Defeitos do próprio kit encontrados no caminho

- `validate-complete.sh` conta tarefas com `grep "^#### TASK-"` **dentro do
  `tasks.json`** — sintaxe markdown contra um arquivo JSON. Sempre lê 0 tarefas,
  então essa verificação nunca vale nada.
- `detect-phase.sh` só reconhece `status: in-progress` como fase de
  implementação. Com `completed`, ele regride a feature para `tasks`.
- `validate-tests.sh` e `validate-spec-conflicts.sh`, citados pelo `sdd.finish`,
  não existem nesta instalação.

Registrados como DEBT-015 no backlog.
