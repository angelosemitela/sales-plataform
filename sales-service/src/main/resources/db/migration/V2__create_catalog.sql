-- =============================================================================
-- V2 - Catálogo de produtos.
--
-- Um PRODUTO (ex: "Teste Streaming 1") tem N PLANOS de cobrança
-- (billingInformation do catálogo de exemplo): um por periodicidade.
-- Cada plano aceita um subconjunto de métodos de pagamento.
--
-- Mapeamento para o payload "product" do billing-registration-service:
--   product.code_id              -> product[].codeId
--   product.name                 -> product[].name
--   product.expiration_service   -> product[].isExpiriationService
--   product.trial / trial_days   -> product[].isTrial / trialDays
--   plan.recurrence_frequency    -> ONESHOT => product[].type = ONESHOT
--                                   MONTH/ANNUAL => type = RECURRENCE + recurrenceFrequency
--   plan.product_value           -> product[].productValue
--   plan.discount_value/_cycles  -> product[].discountValue / discountCycles
-- =============================================================================

CREATE TABLE product (
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code_id            VARCHAR(30)  NOT NULL,
    name               VARCHAR(120) NOT NULL,
    expiration_service BOOLEAN      NOT NULL,
    -- Compra exclusiva: o assinante não pode ter duas compras concluídas do mesmo produto.
    exclusive_purchase BOOLEAN      NOT NULL DEFAULT FALSE,
    trial              BOOLEAN      NOT NULL DEFAULT FALSE,
    trial_days         INTEGER,
    tax_model_code     VARCHAR(20)  NOT NULL REFERENCES tax_model (code),
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_product_code_id UNIQUE (code_id),
    -- Mesma regra do billing: trial exige trialDays > 0; sem trial, trialDays nulo.
    CONSTRAINT ck_product_trial CHECK (
        (trial = TRUE AND trial_days > 0) OR (trial = FALSE AND trial_days IS NULL)
    )
);

CREATE TABLE product_plan (
    id                   BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id           BIGINT        NOT NULL REFERENCES product (id),
    recurrence_frequency VARCHAR(10)   NOT NULL,
    product_value        NUMERIC(12, 2) NOT NULL CHECK (product_value > 0),
    max_installments     INTEGER      NOT NULL CHECK (max_installments BETWEEN 1 AND 12),
    has_discount         BOOLEAN       NOT NULL DEFAULT FALSE,
    discount_value       NUMERIC(12, 2),
    -- Não existia no catálogo de exemplo: quantos ciclos o desconto dura
    -- (necessário para a mensagem "por N meses/anos" e para o discountCycles do billing).
    discount_cycles      INTEGER,
    sort_order           INTEGER      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_product_plan UNIQUE (product_id, recurrence_frequency),
    CONSTRAINT ck_product_plan_frequency CHECK (recurrence_frequency IN ('ONESHOT', 'MONTH', 'ANNUAL')),
    -- Regra do billing: produto RECURRENCE mensal só aceita 1 parcela.
    CONSTRAINT ck_product_plan_month_installments CHECK (recurrence_frequency <> 'MONTH' OR max_installments = 1),
    CONSTRAINT ck_product_plan_discount CHECK (
        (has_discount = FALSE AND discount_value IS NULL AND discount_cycles IS NULL)
        OR (has_discount = TRUE AND discount_value > 0 AND discount_value < product_value
            AND discount_cycles >= 1)
    )
);

CREATE INDEX ix_product_plan_product ON product_plan (product_id);

CREATE TABLE product_plan_payment_method (
    product_plan_id BIGINT      NOT NULL REFERENCES product_plan (id),
    method          VARCHAR(10) NOT NULL,
    PRIMARY KEY (product_plan_id, method),
    CONSTRAINT ck_plan_payment_method CHECK (method IN ('CREDIT', 'DEBIT', 'PIX', 'WALLET'))
);
