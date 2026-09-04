-- Conta de cobrança por estabelecimento (TODO-009, back-office-operador).
--
-- Sem FK para business: billing é outro contexto, mesmo padrão de
-- catalog.service_offering.professional_id — UUID solto, garantia de
-- aplicação, não de banco (ver ADR 0002, PATTERNS.md).
CREATE TABLE billing_account (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE,
    trial_ends_at DATE NOT NULL,
    access_valid_until DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX billing_account_tenant_idx ON billing_account (tenant_id);
