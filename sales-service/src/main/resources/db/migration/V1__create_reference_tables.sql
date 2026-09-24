-- =============================================================================
-- V1 - Tabelas de referência (domínio) e de configuração.
--
-- Convenções deste banco (PostgreSQL), diferentes do billing (MySQL) de propósito,
-- para estudar o que cada banco oferece de melhor:
--   * nomes em snake_case minúsculo (padrão da comunidade PostgreSQL - no Postgres,
--     identificadores sem aspas viram minúsculos automaticamente);
--   * datas em TIMESTAMPTZ (instante absoluto com fuso), em vez de epoch em ms -
--     a conversão para epoch acontece só na borda, ao montar o payload do billing;
--   * dinheiro em NUMERIC(12,2) (exato, nunca DOUBLE/FLOAT);
--   * booleanos em BOOLEAN nativo (o MySQL do billing usava CHAR '0'/'1');
--   * "enums" como VARCHAR + CHECK (mais fácil de evoluir que o CREATE TYPE ... AS
--     ENUM do Postgres, que exige ALTER TYPE para cada valor novo, e mapeia de
--     forma simples com @Enumerated(EnumType.STRING) no JPA).
-- =============================================================================

-- Países (ISO 3166-1 alpha-2). Carga completa em V5.
CREATE TABLE country (
    code       VARCHAR(2)     PRIMARY KEY,
    name       VARCHAR(80) NOT NULL,
    dial_code  VARCHAR(8)  NOT NULL          -- DDI, ex: '+55'
);

-- Tipos de documento aceitos no cadastro do assinante.
--   country_selectable = TRUE  -> a tela abre seleção de país;
--   all_countries      = TRUE  -> qualquer país da tabela country é aceito
--                                 (senão, só os listados em document_type_country).
CREATE TABLE document_type (
    code               VARCHAR(20)  PRIMARY KEY,
    description        VARCHAR(80)  NOT NULL,
    country_selectable BOOLEAN      NOT NULL,
    all_countries      BOOLEAN      NOT NULL,
    sort_order         INTEGER     NOT NULL,
    CONSTRAINT ck_document_type_code CHECK (code IN ('CPF', 'SSN', 'UE', 'PASSPORT', 'OTHER'))
);

CREATE TABLE document_type_country (
    document_type_code VARCHAR(20) NOT NULL REFERENCES document_type (code),
    country_code       VARCHAR(2)     NOT NULL REFERENCES country (code),
    PRIMARY KEY (document_type_code, country_code)
);

-- Modelos de tributação (taxModel do catálogo) e suas taxas.
-- RATE é PERCENTUAL sobre o valor do produto (8.8 = 8,8%).
CREATE TABLE tax_model (
    code        VARCHAR(20) PRIMARY KEY,
    description VARCHAR(80) NOT NULL
);

CREATE TABLE tax_model_item (
    id             BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tax_model_code VARCHAR(20)   NOT NULL REFERENCES tax_model (code),
    name           VARCHAR(20)   NOT NULL,
    rate           NUMERIC(7, 4) NOT NULL CHECK (rate >= 0 AND rate < 100),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_tax_model_item UNIQUE (tax_model_code, name)
);

-- Liga/desliga funcionalidades em runtime (mesmo conceito de T_CONFIG_FEATURE_TOGGLE
-- do billing).
CREATE TABLE feature_toggle (
    name        VARCHAR(60)  PRIMARY KEY,
    enabled     BOOLEAN      NOT NULL,
    description VARCHAR(255) NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Parâmetros de configuração com valor livre (mesmo conceito de T_CONFIG_PARAMETERS
-- do billing).
CREATE TABLE config_parameter (
    name        VARCHAR(60)  PRIMARY KEY,
    value       VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
