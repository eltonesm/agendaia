-- Três agregados novos em organization: os dados que alimentam o cálculo de
-- disponibilidade (feature futura, scheduling). Esta feature só declara o
-- dado, não calcula nada com ele.
--
-- Contexto: organization. Ver ADR 0002 (subdomínio de suporte: a entidade JPA
-- É o modelo) e ADR 0011 (nada é apagado — deactivate(), nunca DELETE).
--
-- tenant_id e professional_id têm FK normal para business/professional: são
-- do MESMO contexto (organization), diferente de catalog (TODO-003), que
-- referencia organization por UUID solto por ser outro contexto.

-- ---------------------------------------------------------------------------
-- business_operating_hours — quando o estabelecimento PODE abrir.
--
-- Entidade de Business, sem identidade própria fora dele (glossário). Várias
-- faixas por dia são permitidas; dia sem nenhuma faixa é dia fechado.
-- ---------------------------------------------------------------------------
CREATE TABLE business_operating_hours (
    id          uuid        PRIMARY KEY,
    tenant_id   uuid        NOT NULL REFERENCES business (id),
    day_of_week varchar(9)  NOT NULL,
    opens_at    time        NOT NULL,
    closes_at   time        NOT NULL,
    active      boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT business_operating_hours_range_valid CHECK (closes_at > opens_at)
);

CREATE INDEX business_operating_hours_tenant_idx ON business_operating_hours (tenant_id);

COMMENT ON TABLE  business_operating_hours IS 'Quando o estabelecimento pode abrir. Limite externo da disponibilidade (glossário).';
COMMENT ON COLUMN business_operating_hours.day_of_week IS 'Nome do java.time.DayOfWeek (MONDAY..SUNDAY), armazenado por extenso para legibilidade.';

-- ---------------------------------------------------------------------------
-- work_schedule — quando o profissional DECLARA que trabalha.
--
-- Raiz de agregado. Almoço recorrente é modelado como duas faixas no mesmo
-- dia (o vão entre elas É o almoço), nunca como TimeOff.
-- ---------------------------------------------------------------------------
CREATE TABLE work_schedule (
    id               uuid        PRIMARY KEY,
    tenant_id        uuid        NOT NULL REFERENCES business (id),
    professional_id  uuid        NOT NULL REFERENCES professional (id),
    day_of_week      varchar(9)  NOT NULL,
    starts_at        time        NOT NULL,
    ends_at          time        NOT NULL,
    active           boolean     NOT NULL DEFAULT true,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT work_schedule_range_valid CHECK (ends_at > starts_at)
);

CREATE INDEX work_schedule_tenant_idx ON work_schedule (tenant_id);
CREATE INDEX work_schedule_professional_day_idx ON work_schedule (professional_id, day_of_week);

COMMENT ON TABLE  work_schedule IS 'Jornada recorrente semanal do profissional, em faixas. Dado declarado, não calculado (glossário).';
COMMENT ON COLUMN work_schedule.professional_id IS 'FK normal: Professional é do mesmo contexto (organization), diferente de service_offering.professional_id (TODO-003, outro contexto).';

-- ---------------------------------------------------------------------------
-- time_off — indisponibilidade EXCEPCIONAL e datada.
--
-- professional_id anulável: nulo vale para o estabelecimento inteiro (feriado,
-- fechamento para reforma) -- sem tabela nem coluna de tipo separada (DD-3).
-- ---------------------------------------------------------------------------
CREATE TABLE time_off (
    id               uuid         PRIMARY KEY,
    tenant_id        uuid         NOT NULL REFERENCES business (id),
    professional_id  uuid         REFERENCES professional (id),
    starts_at        timestamptz  NOT NULL,
    ends_at          timestamptz  NOT NULL,
    reason           varchar(500),
    active           boolean      NOT NULL DEFAULT true,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT time_off_range_valid CHECK (ends_at > starts_at)
);

CREATE INDEX time_off_tenant_idx ON time_off (tenant_id);
CREATE INDEX time_off_professional_idx ON time_off (professional_id);

COMMENT ON TABLE  time_off IS 'Indisponibilidade excepcional e datada. Feriado e fechamento do estabelecimento são TimeOff sem profissional (glossário).';
COMMENT ON COLUMN time_off.professional_id IS 'Anulável de propósito: nulo significa que o bloqueio vale para todo o estabelecimento (DD-3).';
