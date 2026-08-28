# Documentação do AgendaIA

## Normativo vs. contexto

Nem todo documento aqui tem o mesmo peso. A distinção é obrigatória — sem ela, 16 documentos viram 16 fontes da verdade concorrentes, e código gerado por IA passa a sair da metade desatualizada.

**Normativo** — o agente e o desenvolvedor devem obedecer. Mantido atualizado. São apenas três:

| Documento | O que governa |
|---|---|
| [`docs/02-domain/glossary.md`](02-domain/glossary.md) | Nomes. Todo identificador no código sai daqui. |
| `sdd/PROJECT.md` | O que o projeto é: stack, comandos, módulos, definition of done. |
| `sdd/PATTERNS.md` | Como o código se parece: camadas, sufixos, transação, erro. |

**Contexto** — todo o resto. Registra o raciocínio de um momento. Não é fonte da verdade e pode estar desatualizado. Todo arquivo de contexto começa com a data em que foi escrito.

**ADR** — categoria própria. Um ADR nunca é editado depois de aceito; ele é substituído por outro. O histórico das decisões é o valor.

## Estrutura

```
docs/
├── 01-product/       visão, regras de negócio, backlog de produto
├── 02-domain/        glossário (normativo), event storming, modelo, casos de uso
├── 03-architecture/  haiku, context map, dependências, adr/
├── 04-frontend/      estrutura de templates, design
├── 05-database/      modelo físico, índices
├── 06-security/      threat model, isolamento de tenant
├── 07-infrastructure/ VPS, deploy, backup e restore
└── 08-engineering/   estratégia de testes, guia de desenvolvimento
```

## Dois backlogs, de propósito

- `docs/01-product/backlog.md` — o que o negócio quer. Escrito por pessoas.
- `sdd/backlog.md` — dívida técnica, TODO e ideias que surgem durante a implementação.

`/sdd.start --from-backlog` faz a ponte quando um item de produto vira feature.
