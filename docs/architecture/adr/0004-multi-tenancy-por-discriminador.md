# ADR 0004 — Multi-tenancy por discriminador, com duas rotas de resolução

- **Status:** Aceito
- **Data:** 2026-08-28

## Contexto

Cada estabelecimento é um tenant e não pode, em nenhuma hipótese, enxergar dado
de outro. Enfiar `tenant_id` num sistema que já existe é uma das migrações mais
caras que há — toca toda tabela, toda query e todo teste. Então entra agora.

As opções eram banco por tenant, schema por tenant ou coluna discriminadora.
Com uma VPS e mensalidade de R$ 50–70, banco ou schema por tenant é
economicamente inviável.

Há uma particularidade que a maior parte dos SaaS não tem: **existem duas
rotas de entrada**. A área administrativa resolve o tenant pela sessão
autenticada. A página pública resolve pelo `slug` da URL — uma string que o
visitante controla.

## Decisão

Nós vamos usar **banco único com coluna `tenant_id`** em toda tabela de
negócio, e um `TenantContext` no módulo `platform` alimentado por **duas rotas
de resolução**:

- `/admin/**` — tenant vem do usuário autenticado.
- `/b/{slug}/**` — tenant vem da resolução do slug.

O filtro do Hibernate (`@TenantId`) aplica o recorte automaticamente em toda
consulta, para que ninguém precise lembrar de escrever `where tenant_id = ?`.

**`tenant_id` nunca é aceito do corpo, da query string ou de header.** E todo
id recebido do formulário público — `professionalId`, `serviceOfferingId` — é
revalidado contra o tenant resolvido pelo slug antes de qualquer escrita.

## Consequências

O isolamento passa a ser propriedade da infraestrutura de acesso a dados, não
da disciplina de quem escreve query.

A rota pública continua sendo a superfície de risco: é onde um id forjado
poderia cruzar tenants. Por isso a revalidação é regra explícita e vigiada,
não uma consequência esperada do filtro.

Migrar para isolamento físico (schema ou banco por tenant) continua possível,
porque a fronteira do tenant está explícita em todo lugar.

Um bug de aplicação ainda pode, em tese, vazar dado — o filtro é aplicação, não
banco. Row Level Security no Postgres é a rede de segurança planejada para
depois da validação do MVP.

## Gatilho de reavaliação

Ativar RLS quando houver mais de um punhado de clientes pagantes. Considerar
isolamento físico se aparecer cliente com exigência contratual de segregação.
