-- Agendamento (TODO-006, pagina-publica-agendamento).
--
-- Sem FK para customer/service_offering/professional: scheduling referencia
-- por UUID solto, mesmo padrão de service_offering.professional_id
-- (ver ADR 0002, PATTERNS.md) — garantia de aplicação, não de banco.
--
-- btree_gist já habilitada na V1.
CREATE TABLE appointment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    professional_id UUID NOT NULL,
    service_offering_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    duration_minutes INT NOT NULL,
    price_cents BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

-- ADR 0005, literal: a barreira real contra overbooking.
ALTER TABLE appointment ADD CONSTRAINT appointment_no_overlap
  EXCLUDE USING gist (
    tenant_id       WITH =,
    professional_id WITH =,
    tstzrange(starts_at, ends_at, '[)') WITH &&
  ) WHERE (status IN ('SCHEDULED', 'CONFIRMED'));

-- BR-9: contagem de agendamentos futuros ativos por telefone (via customer_id).
CREATE INDEX appointment_customer_idx ON appointment (tenant_id, customer_id, status);
