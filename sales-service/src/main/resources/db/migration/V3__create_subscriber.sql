-- =============================================================================
-- V3 - Assinantes (dados de login + dados do "account" do billing).
--
-- Mapeamento para o payload "account" do billing-registration-service:
--   subscriber.external_id          -> account[].externalId (ID gerado AQUI no Sales)
--   subscriber.name / email         -> account[].name / email
--   subscriber.authorized_fallback  -> account[].isAuthorizedFallback
--   subscriber_document             -> account[].document[]
--   subscriber_address              -> account[].address[]
--   subscriber_phone (ddi+número)   -> account[].phone[].number (só dígitos)
-- =============================================================================

CREATE TABLE subscriber (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- ID público: nunca expomos o ID sequencial (evita enumeração de assinantes).
    external_id         UUID         NOT NULL,
    name                VARCHAR(120) NOT NULL,
    email               VARCHAR(254) NOT NULL,
    password_hash       VARCHAR(100) NOT NULL,   -- BCrypt; a senha em texto nunca é gravada
    authorized_fallback BOOLEAN      NOT NULL DEFAULT TRUE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,  -- lock otimista (@Version)
    CONSTRAINT uk_subscriber_external_id UNIQUE (external_id),
    CONSTRAINT ck_subscriber_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

-- E-mail único sem diferenciar maiúsculas: índice único FUNCIONAL sobre lower(email)
-- (recurso que o MySQL só passou a ter de forma parecida bem depois).
CREATE UNIQUE INDEX uk_subscriber_email ON subscriber (lower(email));

CREATE TABLE subscriber_document (
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscriber_id      BIGINT       NOT NULL REFERENCES subscriber (id),
    document_type_code VARCHAR(20)  NOT NULL REFERENCES document_type (code),
    description        VARCHAR(120),
    value              VARCHAR(40)  NOT NULL,
    country_code       VARCHAR(2)      NOT NULL REFERENCES country (code),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Mesma regra do billing: um documento por tipo por assinante.
    CONSTRAINT uk_subscriber_document_type UNIQUE (subscriber_id, document_type_code),
    -- Um mesmo documento (ex: um CPF) não pode pertencer a dois assinantes.
    CONSTRAINT uk_subscriber_document_value UNIQUE (document_type_code, country_code, value)
);

CREATE TABLE subscriber_address (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscriber_id BIGINT       NOT NULL REFERENCES subscriber (id),
    type          VARCHAR(20)  NOT NULL,
    description   VARCHAR(120) NOT NULL,
    international BOOLEAN      NOT NULL DEFAULT FALSE,
    zip_code      VARCHAR(20)  NOT NULL,
    address_name  VARCHAR(200) NOT NULL,
    number        VARCHAR(10),                 -- só dígitos, opcional ("sem número")
    complement    VARCHAR(120),
    district      VARCHAR(120),
    city          VARCHAR(120),
    state         VARCHAR(60),
    country_code  VARCHAR(2)      NOT NULL REFERENCES country (code),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_subscriber_address_type CHECK (type IN ('RESIDENCIAL', 'COMERCIAL', 'OTHER')),
    CONSTRAINT ck_subscriber_address_number CHECK (number IS NULL OR number ~ '^[0-9]+$'),
    -- Endereço nacional é sempre do Brasil.
    CONSTRAINT ck_subscriber_address_national CHECK (international = TRUE OR country_code = 'BR')
);

CREATE INDEX ix_subscriber_address_subscriber ON subscriber_address (subscriber_id);

CREATE TABLE subscriber_phone (
    id            BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subscriber_id BIGINT      NOT NULL REFERENCES subscriber (id),
    country_code  VARCHAR(2)     NOT NULL REFERENCES country (code),
    dial_code     VARCHAR(8)  NOT NULL,   -- cópia do DDI no momento do cadastro
    number        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_subscriber_phone_number CHECK (number ~ '^[0-9]{1,20}$')
);

CREATE INDEX ix_subscriber_phone_subscriber ON subscriber_phone (subscriber_id);
