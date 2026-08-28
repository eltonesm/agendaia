# ADR 0005 — Exclusion constraint do Postgres como barreira final contra overbooking

- **Status:** Aceito
- **Data:** 2026-08-28

## Contexto

Marcar dois clientes no mesmo horário com o mesmo profissional é o pior defeito
possível neste produto: destrói a confiança do estabelecimento no sistema e
manda ele de volta para o caderno.

A sequência ingênua — consultar disponibilidade, verificar se está livre,
inserir — **não** protege sob concorrência. Duas requisições simultâneas leem o
mesmo estado, ambas concluem que o horário está livre, ambas gravam. Nenhuma
das duas transações enxerga a outra, em nenhum nível de isolamento que faça
sentido usar aqui.

Lock pessimista sobre a agenda do profissional resolveria, ao custo de
serializar reservas e de um ponto de contenção difícil de acertar.

## Decisão

Nós vamos defender em três camadas, sendo a última a única que é verdade:

1. **Regra no domínio** — dá feedback rápido e é testável sem banco.
2. **`EXCLUDE USING gist` no Postgres** — barreira intransponível:

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE appointment ADD CONSTRAINT appointment_no_overlap
  EXCLUDE USING gist (
    tenant_id       WITH =,
    professional_id WITH =,
    tstzrange(starts_at, ends_at, '[)') WITH &&
  ) WHERE (status IN ('SCHEDULED', 'CONFIRMED'));
```

3. **Tradução no adapter** — a violação vira `SlotUnavailableException`, e o
   usuário lê "esse horário acabou de ser reservado", não um erro 500.

Os limites `[)` são escritos explicitamente, ainda que sejam o padrão de
`tstzrange`, para que ninguém os "conserte" para `[]` e passe a rejeitar
atendimentos consecutivos legítimos.

## Consequências

O overbooking passa a ser fisicamente impossível, independente de bug na
aplicação, de corrida entre requisições ou de acesso direto ao banco.

Ficamos amarrados ao PostgreSQL: `EXCLUDE USING gist` não é portável. Dado o
resto das decisões, isso não é custo real.

**H2 deixa de servir para testes** que toquem agendamento — não implementa a
constraint. Testcontainers com Postgres real passa a ser obrigatório, e o teste
que dispara duas reservas simultâneas exigindo que exatamente uma vença é parte
da definição de pronto do core.

Cancelamento e falta precisam sair da cláusula `WHERE` para que o horário seja
liberado — se um status ocupante for esquecido ali, o slot morre para sempre.

## Gatilho de reavaliação

Se aparecer recurso compartilhado (a única cadeira de lavagem, a sala de
sobrancelha), acrescentar uma segunda constraint sobre `resource_id`. A
extensão é aditiva e não exige reescrita.
