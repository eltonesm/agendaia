-- Primeiras tabelas de negócio do sistema.
--
-- Contexto: organization. Ver ADR 0002 (subdomínio de suporte: a entidade JPA
-- É o modelo) e ADR 0003 (Business e User nascem na mesma transação).

-- ---------------------------------------------------------------------------
-- business — o estabelecimento.
--
-- ATENÇÃO: esta é a única tabela de negócio SEM coluna tenant_id, porque ela
-- É o tenant. Seu id é o tenant_id de todas as demais. A regra do PATTERNS.md
-- ("toda tabela de negócio tem tenant_id") tem exatamente esta exceção.
-- ---------------------------------------------------------------------------
CREATE TABLE business (
    id          uuid         PRIMARY KEY,
    name        varchar(120) NOT NULL,
    slug        varchar(60)  NOT NULL,
    timezone    varchar(64)  NOT NULL DEFAULT 'America/Sao_Paulo',
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT business_slug_unique     UNIQUE (slug),
    -- Repete no banco a regra que SlugGenerator.hasValidFormat aplica em
    -- memória. Deliberado, mesma lógica do ADR 0005: validação na aplicação é
    -- feedback, garantia é do banco.
    CONSTRAINT business_slug_format     CHECK (slug ~ '^[a-z0-9]([a-z0-9-]*[a-z0-9])?$'),
    CONSTRAINT business_slug_min_length CHECK (length(slug) >= 3),
    CONSTRAINT business_name_not_blank  CHECK (length(btrim(name)) >= 2)
);

COMMENT ON TABLE  business IS 'Estabelecimento. É o tenant: seu id é o tenant_id das demais tabelas.';
COMMENT ON COLUMN business.slug IS 'Trecho da URL pública /b/{slug}. Único global.';
COMMENT ON COLUMN business.timezone IS 'Fuso IANA. Cálculo de disponibilidade acontece neste fuso; armazenamento em UTC.';

-- ---------------------------------------------------------------------------
-- app_user — quem autentica no painel.
--
-- Nome com prefixo porque "user" é palavra reservada no PostgreSQL (DD-6).
-- Funciona entre aspas, mas SELECT * FROM user devolve o usuário do banco sem
-- erro nenhum — armadilha permanente para quem depura em produção.
-- ---------------------------------------------------------------------------
CREATE TABLE app_user (
    id             uuid         PRIMARY KEY,
    tenant_id      uuid         NOT NULL REFERENCES business (id),
    email          varchar(254) NOT NULL,
    name           varchar(120) NOT NULL,
    -- BCrypt produz 60 caracteres; a folga cobre variação de prefixo
    -- ($2a$, $2b$, $2y$) sem apertar.
    password_hash  varchar(72)  NOT NULL,
    role           varchar(20)  NOT NULL,
    active         boolean      NOT NULL DEFAULT true,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT app_user_email_unique UNIQUE (email),
    -- Um valor só, por enquanto. A migration que introduzir o segundo papel
    -- altera esta restrição — e assim ela documenta o que existe de fato.
    CONSTRAINT app_user_role_valid    CHECK (role IN ('OWNER'))
);

CREATE INDEX app_user_tenant_idx ON app_user (tenant_id);

COMMENT ON TABLE  app_user IS 'Usuário que autentica no painel. Prefixo evita a palavra reservada "user".';
COMMENT ON COLUMN app_user.email IS 'Credencial de login. Único no sistema inteiro, não por tenant.';

-- ---------------------------------------------------------------------------
-- business_slug_history NÃO é criada aqui (DD-3).
--
-- O slug é imutável nesta feature, e a modelagem prevista tem um problema: a
-- unicidade precisaria valer ENTRE business.slug e o histórico, e não existe
-- UNIQUE que atravesse tabelas. Um estabelecimento novo poderia tomar um slug
-- do histórico de outro e quebrar o redirecionamento.
--
-- Quem implementar a troca de slug: leia o DD-3 antes. A saída provável é uma
-- tabela única de slugs com coluna active, dando um domínio único de unicidade,
-- e não simplesmente acrescentar a tabela de histórico.
-- ---------------------------------------------------------------------------
