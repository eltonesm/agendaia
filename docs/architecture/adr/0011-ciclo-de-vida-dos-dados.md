# ADR 0011 — Nada de negócio é apagado

- **Status:** Aceito
- **Data:** 2026-08-30

## Contexto

Três perguntas apareceram ao modelar os dados, e à primeira vista não têm
relação entre si:

1. O que acontece quando um profissional sai do estabelecimento? Os
   agendamentos passados dele precisam continuar existindo — são registro do
   negócio, e o relatório do dono depende deles.
2. O cliente final exerce o direito de exclusão previsto na LGPD. Mas os
   agendamentos dele são registro do estabelecimento, que tem obrigação fiscal
   e contratual de manter — e o estabelecimento não consentiu com essa
   exclusão.
3. O dono renomeia a barbearia e quer mudar o slug. Só que ele já compartilhou
   o link antigo por WhatsApp com dezenas de clientes, e não tem como recolhê-lo.

As três são a mesma pergunta: **o que fazer quando um dado precisa sair de
circulação sem deixar de existir?**

Apagar a linha resolve o pedido imediato e destrói o histórico. Impedir a
remoção preserva o histórico e frustra o usuário — na prática, depois da
primeira semana quase nada pode mais ser apagado, e o dono recebe uma mensagem
de erro em vez do que pediu.

## Decisão

Nós vamos adotar um princípio único: **nada de negócio é apagado; o que sai de
circulação é marcado.**

**Desativação lógica** — `Professional`, `Service`, `ServiceOffering`,
`User` e `Business` têm `active`. Desativado desaparece da página pública e das
listas de escolha, mas continua existindo, e os agendamentos que o referenciam
seguem íntegros.

**Anonimização** — o pedido de exclusão do cliente preenche `anonymized_at` e
substitui nome e telefone. O agendamento permanece com horário, serviço e valor.
Atende ao direito do titular sem destruir o registro do estabelecimento.

**Histórico de slug** — trocar o slug não apaga o anterior: ele vai para
`BusinessSlugHistory` e continua resolvendo, com redirecionamento para o atual.

## Consequências

O histórico do negócio fica íntegro por construção. Relatório de faturamento,
agenda passada e histórico de cliente continuam corretos depois de qualquer
saída ou pedido de exclusão.

Links já compartilhados nunca quebram — o que importa num produto cujo canal de
distribuição é o WhatsApp do próprio dono.

**O que fica pior, e é o custo real:**

- **Toda consulta precisa filtrar por `active`.** Esquecer o filtro é um bug
  silencioso: o profissional demitido reaparece na página pública, e ninguém
  percebe até um cliente agendar com ele. A mitigação é convenção explícita no
  `PATTERNS.md` e teste por caso de uso, não vigilância.
- **A resolução de tenant passa a ter duas fontes** — slug atual e histórico.
  Uma consulta a mais no caminho mais quente da página pública.
- **Anonimizar não é apagar.** Se um titular exigir remoção completa e a
  interpretação jurídica não aceitar anonimização, será preciso um procedimento
  manual. Aceitamos esse risco: a alternativa apaga faturamento do
  estabelecimento, que também é titular de direitos sobre aquele registro.
- Unicidade de telefone por tenant convive com anonimização: o registro
  anonimizado libera o telefone, e um retorno futuro da mesma pessoa cria um
  cliente novo — sem histórico. É o comportamento correto.

## Gatilho de reavaliação

Se o volume de registros inativos começar a atrapalhar consulta ou leitura de
tela, introduzir arquivamento — mover para tabela histórica em vez de filtrar.
Com o volume projetado (~900 mil agendamentos para cem estabelecimentos em um
ano) isso está muito longe.

Se assessoria jurídica entender que anonimização não satisfaz o direito de
exclusão, rever a decisão 2 — e aí o problema deixa de ser técnico.
