# Documentação do AgendaIA

## Cinco princípios

Estes valem para qualquer projeto, não só para este.

### 1. Normativo vs. contexto

Nem todo documento tem o mesmo peso, e não distinguir é o que transforma uma
biblioteca de documentos em várias fontes da verdade concorrentes.

**Normativo** — deve ser obedecido e mantido atualizado. **No máximo três:**

| Documento | O que governa |
|---|---|
| [`docs/domain/glossary.md`](domain/glossary.md) | Nomes. Todo identificador do código sai daqui. |
| `sdd/PROJECT.md` | O que o projeto é: stack, comandos, contextos, definition of done. |
| `sdd/PATTERNS.md` | Como o código se parece: camadas, sufixos, transação, erro. |

**Contexto** — registra o raciocínio de um momento. Não é fonte da verdade e
pode estar desatualizado. Todo arquivo de contexto começa com a data em que foi
escrito.

Se um quarto documento normativo parecer necessário, quase sempre a resposta
certa é que um dos três está mal escrito.

### 2. Documento que descreve código não mora em `docs/`

Convenção de nome, estratégia de teste, estrutura de template, guia de
desenvolvimento: tudo isso é `PATTERNS.md` e `PROJECT.md`, que ficam colados no
código e são lidos a cada feature. Duplicar em `docs/` cria a versão que
ninguém atualiza.

`docs/` responde **por quê**. O código e os dois arquivos normativos respondem
**o quê** e **como**.

### 3. Documento que pode ser gerado nunca é escrito à mão

Modelo físico do banco, diagrama de dependências, contrato de API. Gerado da
fonte, sempre. Mantido à mão, mente já na terceira alteração.

### 4. Pasta vazia é passivo

Uma pasta vazia é um convite para preencher com documento que ninguém pediu.
Crie a pasta no commit em que o primeiro arquivo dela nasce.

### 5. ADR não se edita — depois que a arquitetura congela

Um ADR aceito nunca é alterado. Ele é substituído por outro, que referencia o
anterior. O histórico das decisões — inclusive das que se mostraram erradas — é
o valor.

**A regra passa a valer quando a Fase 0 fecha.** Enquanto a arquitetura está em
análise e não existe código de negócio, ADR é rascunho: reescrever é mais
honesto que empilhar substituições sobre decisões que ninguém chegou a executar.
O ADR 0001 foi reescrito assim, e a seção "Correção de rota" dele registra o que
mudou e por quê.

## Estrutura

```
docs/
├── product/        por que existe, para quem, o que define sucesso
├── domain/         glossário (normativo), event storming, modelo, casos de uso
├── architecture/   haiku, context map, adr/
├── security/       threat model, isolamento de tenant, LGPD
└── operations/     runbook: deploy, backup e restore, incidentes
```

`security/` e `operations/` existem por motivo operacional, não documental: num
SaaS multi-tenant com dado pessoal pode ser preciso **mostrar** o threat model,
e o procedimento de restore precisa ser encontrável às duas da manhã por alguém
com sono.

## Um backlog só

`sdd/backlog.md`, gerenciado por `/sdd.backlog`, com as categorias TODO, DEBT e
IDEA. Item de produto e dívida técnica convivem na mesma lista, etiquetados.

Duas listas com propósito sobreposto sempre divergem, e a que é mais difícil de
atualizar perde. A intenção de produto mora em `product/vision.md`; tudo que é
acionável mora no backlog do kit.
