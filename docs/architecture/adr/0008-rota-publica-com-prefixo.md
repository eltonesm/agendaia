# ADR 0008 — Rota pública com prefixo `/b/{slug}`

- **Status:** Aceito
- **Data:** 2026-08-28

## Contexto

Cada estabelecimento tem um apelido único que forma seu link público. A proposta
original mapeava esse apelido na raiz: `/barbearia-do-joao`.

Isso torna `/login`, `/admin`, `/css`, `/js`, `/actuator` e `/error` apelidos
válidos do ponto de vista do roteador. Um cliente que cadastrasse o
estabelecimento "Admin" derrubaria o painel administrativo — e a ordem de
resolução de rotas passaria a ser uma dependência frágil e não óbvia do
sistema.

A alternativa para preservar a URL limpa seria uma lista de palavras reservadas
validada no cadastro, mais garantia explícita de precedência de mapeamento. Isso
funciona, mas exige que a lista seja mantida para sempre: toda rota nova do
sistema vira uma palavra que alguém pode já ter cadastrado.

## Decisão

Nós vamos prefixar a área pública com **`/b/`**:

```
/b/barbearia-do-joao
/admin/**
/login
```

## Consequências

A classe inteira de colisão entre apelido e rota do sistema desaparece, e
adicionar rotas novas ao sistema deixa de ser uma decisão com efeito
retroativo sobre cadastros existentes.

A URL fica um pouco menos bonita. Para um link compartilhado por WhatsApp e
lido no celular, o custo é desprezível.

Mudar isso depois quebra links já compartilhados por estabelecimentos com seus
clientes — por isso a decisão é tomada agora, e não "quando incomodar".

## Gatilho de reavaliação

Quando houver domínio próprio por estabelecimento
(`barbeariadojoao.agendaia.com` ou domínio do cliente), o prefixo perde o
sentido e a raiz volta a ficar disponível.
