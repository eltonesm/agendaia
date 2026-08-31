-- Segundo agregado de organization: quem atende no estabelecimento.
--
-- Contexto: organization. Ver ADR 0002 (subdomínio de suporte: a entidade JPA
-- É o modelo) e ADR 0011 (nada é apagado — deactivate(), nunca DELETE).

-- ---------------------------------------------------------------------------
-- professional — quem executa o atendimento.
--
-- Ao contrário de business.slug e app_user.email, NÃO há restrição UNIQUE em
-- name (DD-3 da spec técnica): nome é rótulo de exibição, não identificador.
-- Dois profissionais do mesmo estabelecimento podem se chamar igual.
-- ---------------------------------------------------------------------------
CREATE TABLE professional (
    id          uuid         PRIMARY KEY,
    tenant_id   uuid         NOT NULL REFERENCES business (id),
    name        varchar(120) NOT NULL,
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),

    -- Repete no banco a regra que Professional.register aplica em memória
    -- (mesma lógica do ADR 0005: validação na aplicação é feedback, garantia
    -- é do banco).
    CONSTRAINT professional_name_not_blank CHECK (length(btrim(name)) >= 2)
);

CREATE INDEX professional_tenant_idx ON professional (tenant_id);

COMMENT ON TABLE  professional IS 'Profissional que atende no estabelecimento. Pode ou não ter um app_user associado (fora de escopo nesta feature).';
COMMENT ON COLUMN professional.name IS 'Rótulo de exibição, não identificador. Duplicata entre profissionais do mesmo tenant é permitida.';
