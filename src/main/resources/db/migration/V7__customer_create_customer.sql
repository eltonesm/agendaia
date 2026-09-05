-- Cliente atendido pelo estabelecimento (TODO-006, pagina-publica-agendamento).
--
-- Telefone normalizado (E.164) é a chave natural dentro do tenant (glossário)
-- — a UNIQUE garante que o get-or-create nunca duplica sob concorrência.
CREATE TABLE customer (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    anonymized_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, phone)
);
