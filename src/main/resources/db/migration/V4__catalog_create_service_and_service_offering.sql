-- Primeiro código de catalog: o que o estabelecimento vende (service) e o
-- que o cliente de fato agenda (service_offering — serviço por profissional).
--
-- Contexto: catalog. Ver DD-2 da spec técnica: nenhuma coluna aqui aponta
-- para fora de catalog com chave estrangeira, tenant_id incluído — mesma
-- regra que já valia para professional_id, agora generalizada.

CREATE TABLE service (
    id           uuid         PRIMARY KEY,
    tenant_id    uuid         NOT NULL,  -- sem FK: fora do contexto (DD-2)
    name         varchar(120) NOT NULL,
    description  varchar(500),
    active       boolean      NOT NULL DEFAULT true,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT service_name_not_blank CHECK (length(btrim(name)) >= 2),
    CONSTRAINT service_name_unique UNIQUE (tenant_id, name)
);

CREATE INDEX service_tenant_idx ON service (tenant_id);

COMMENT ON TABLE  service IS 'O conceito vendável ("Corte de Cabelo"). Sem preço nem duração — isso é da oferta.';
COMMENT ON COLUMN service.tenant_id IS 'UUID solto, sem FK: catalog não referencia tabela de organization (DD-2).';

CREATE TABLE service_offering (
    id                 uuid         PRIMARY KEY,
    tenant_id          uuid         NOT NULL,  -- sem FK, mesmo motivo
    service_id         uuid         NOT NULL REFERENCES service (id),  -- mesmo contexto: FK normal
    professional_id    uuid         NOT NULL,  -- outro contexto: UUID solto, sem FK
    duration_minutes   integer      NOT NULL,
    price_cents        bigint       NOT NULL,
    buffer_minutes     integer      NOT NULL DEFAULT 0,
    active             boolean      NOT NULL DEFAULT true,
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT service_offering_duration_positive CHECK (duration_minutes > 0),
    CONSTRAINT service_offering_price_not_negative CHECK (price_cents >= 0),
    CONSTRAINT service_offering_buffer_not_negative CHECK (buffer_minutes >= 0),
    -- Único por (tenant, service, professional) — um profissional tem no
    -- máximo uma oferta de cada serviço (BR-7, data-model.md).
    CONSTRAINT service_offering_unique UNIQUE (tenant_id, service_id, professional_id)
);

CREATE INDEX service_offering_tenant_idx ON service_offering (tenant_id);
CREATE INDEX service_offering_service_idx ON service_offering (service_id);

COMMENT ON TABLE  service_offering IS 'O que o cliente de fato agenda: um serviço executado por um profissional específico.';
COMMENT ON COLUMN service_offering.professional_id IS 'UUID solto, sem FK — profissional é de organization, outro contexto. Validado em memória via organization.api (DD-1), não pelo banco.';
COMMENT ON COLUMN service_offering.price_cents IS 'Centavos, inteiro — nunca decimal/double (DD-3 da spec técnica).';
