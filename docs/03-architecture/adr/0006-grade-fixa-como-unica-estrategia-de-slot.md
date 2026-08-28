# ADR 0006 — Grade fixa de 10 minutos como única estratégia de slot no MVP

- **Status:** Aceito
- **Data:** 2026-08-28

## Contexto

O documento de arquitetura previa duas estratégias de geração de horários,
configuráveis pelo estabelecimento:

- `FIXED_GRID` — horários em múltiplos de um intervalo fixo.
- `DYNAMIC_DURATION` — encaixe colado no fim do atendimento anterior.

Os dois **não são dois algoritmos**. O cálculo é um pipeline só — empresa ∩
profissional − bloqueios − agendamentos → janelas livres → gerar starts
candidatos → filtrar quem comporta duração + intervalo. Noventa por cento do
código, incluindo toda a parte difícil, é compartilhado. A diferença é como
listar os starts candidatos: umas dez linhas.

O custo real, portanto, não é código. É **uma pergunta de configuração que o
barbeiro não sabe responder**, num produto cuja promessa é simplicidade.

A conta com os serviços reais do piloto (30, 20, 10 e 45 minutos, com intervalo
de 10 para um profissional e 0 para outro) produz blocos ocupados de 40, 30, 20
e 55 minutos. Numa grade de 15 minutos, o desperdício por atendimento é de
0 a 10 minutos. Numa grade de **10 minutos, é de 0 a 5** — praticamente nada,
o que remove o argumento a favor do encaixe dinâmico neste caso.

## Decisão

Nós vamos implementar **apenas `FIXED_GRID`**, com grade padrão de **10
minutos**.

A coluna `booking_strategy` existe no banco com um único valor legal. A geração
de starts candidatos é um **método privado** com nome próprio dentro do
calculador de disponibilidade — não uma interface com uma implementação.

Não haverá tela de configuração de estratégia. O que o estabelecimento
configura é o intervalo entre clientes, que ele entende.

## Consequências

Os horários oferecidos ficam previsíveis e estáveis: duas pessoas com a página
aberta ao mesmo tempo veem a mesma lista, e o segundo a reservar recebe
"acabou de ser reservado" em vez de "esse horário não existe mais" — falha que
o encaixe dinâmico produziria naturalmente.

A coluna no banco custa quase nada agora e evita uma migration com backfill em
dados de clientes reais depois. Acrescentar um valor de enum é gratuito;
acrescentar uma coluna, não.

Aceitamos algum desperdício de agenda — de 0 a 5 minutos por atendimento com as
durações atuais. Se as durações mudarem para números que não casam com a grade
de 10, o desperdício cresce.

Extrair o método privado para uma interface, quando houver segunda estratégia,
é refactor mecânico com os testes já no lugar.

## Gatilho de reavaliação

Instrumentar a ociosidade: somar diariamente os intervalos livres invendáveis
entre atendimentos. Se o piloto perder mais de meia hora por dia em janelas
mortas, `DYNAMIC_DURATION` ganhou o direito de existir. Se o relatório mostrar
poucos minutos, remover a coluna e a decisão.
