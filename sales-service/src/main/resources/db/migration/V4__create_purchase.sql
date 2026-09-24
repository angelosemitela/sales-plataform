-- =============================================================================
-- V4 - Compras (carrinho -> compra), taxas por compra, pagamento e outbox.
--
-- Ciclo de vida de uma compra (coluna status):
--
--   CART ──(inatividade / novo carrinho)──> ABANDONED
--    │
--    └──(clique em "Comprar" + tokenização)──> PROCESSING ──> COMPLETED
--                                                  └────────> FAILED
--
-- O CARRINHO É a própria compra em status CART: não existe tabela separada.
-- Assim, o "carrinho abandonado" é só um status + motivo, e todo o histórico
-- de preço/seleção fica numa linha só, pronta para relatórios de conversão.
--
-- Mapeamento para o payload "billing" do billing-registration-service:
--   purchase.product_value / discount_value / tax_value / charged_value
--   purchase.installments / payment_method / currency ('BRL')
--   purchase_tax (name, value)        -> billing[].tax[]
--   purchase.protocol                 -> protocol (idempotência no billing)
-- =============================================================================

CREATE TABLE purchase (
    id                   BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id            UUID           NOT NULL,
    -- Enviado como "protocol" ao billing: o billing bloqueia protocolo repetido
    -- que já teve sucesso, então reenvios (retry) são seguros.
    protocol             VARCHAR(40)    NOT NULL,
    subscriber_id        BIGINT         NOT NULL REFERENCES subscriber (id),
    product_id           BIGINT         NOT NULL REFERENCES product (id),
    product_plan_id      BIGINT         NOT NULL REFERENCES product_plan (id),
    channel              VARCHAR(20)    NOT NULL DEFAULT 'WEB',
    currency             VARCHAR(3)        NOT NULL DEFAULT 'BRL',
    status               VARCHAR(20)    NOT NULL,
    -- Último passo alcançado no funil (útil para analisar onde o carrinho foi abandonado).
    last_step            VARCHAR(30)    NOT NULL,

    -- Seleções do assinante na tela de compra (nada sensível: nunca número de cartão/CVV).
    payment_method       VARCHAR(10),
    wallet_provider      VARCHAR(20),
    card_brand           VARCHAR(20),
    installments         INTEGER,

    -- "Foto" (snapshot) da cotação: se o catálogo mudar de preço depois, a compra
    -- continua com o valor que o assinante viu.
    product_value        NUMERIC(12, 2) NOT NULL,
    discount_value       NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount_cycles      INTEGER,
    tax_value            NUMERIC(12, 2) NOT NULL,
    charged_value        NUMERIC(12, 2) NOT NULL,   -- valor cobrado na compra (0 em trial)
    trial_days           INTEGER,
    first_charge_date    DATE,                       -- 1ª cobrança (trial)

    started_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    last_activity_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    abandoned_at         TIMESTAMPTZ,
    abandon_reason       VARCHAR(30),
    submitted_at         TIMESTAMPTZ,
    completed_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version              BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT uk_purchase_public_id UNIQUE (public_id),
    CONSTRAINT uk_purchase_protocol UNIQUE (protocol),
    CONSTRAINT ck_purchase_currency CHECK (currency = 'BRL'),   -- regra geral 7: só BRL por ora
    CONSTRAINT ck_purchase_status CHECK (status IN ('CART', 'ABANDONED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_purchase_last_step CHECK (last_step IN
        ('PRODUCT_SELECTED', 'PLAN_SELECTED', 'PAYMENT_METHOD_SELECTED', 'PAYMENT_DETAILS_FILLED', 'SUBMITTED')),
    CONSTRAINT ck_purchase_payment_method CHECK (payment_method IS NULL OR payment_method IN ('CREDIT', 'DEBIT', 'PIX', 'WALLET')),
    CONSTRAINT ck_purchase_wallet CHECK (
        (payment_method = 'WALLET' AND wallet_provider IN ('PICPAY', 'MERCADO_PAGO'))
        OR (wallet_provider IS NULL)
    ),
    CONSTRAINT ck_purchase_card_brand CHECK (
        card_brand IS NULL OR (payment_method IN ('CREDIT', 'DEBIT') AND card_brand IN ('AMEX', 'ELO', 'MASTERCARD', 'VISA'))
    ),
    CONSTRAINT ck_purchase_installments CHECK (installments IS NULL OR installments BETWEEN 1 AND 12),
    CONSTRAINT ck_purchase_abandon_reason CHECK (abandon_reason IS NULL OR abandon_reason IN ('INACTIVITY', 'REPLACED_BY_NEW_CART')),
    CONSTRAINT ck_purchase_values CHECK (
        product_value > 0 AND discount_value >= 0 AND tax_value >= 0 AND charged_value >= 0
    )
);

CREATE INDEX ix_purchase_subscriber_status ON purchase (subscriber_id, status);
-- Índice PARCIAL: só as linhas em CART entram, deixando o job de abandono rápido
-- mesmo com milhões de compras concluídas na tabela (recurso típico do PostgreSQL).
CREATE INDEX ix_purchase_open_carts ON purchase (last_activity_at) WHERE status = 'CART';

-- Regra geral 9: as taxas de cada compra ficam registradas em linhas próprias,
-- com a alíquota usada no momento (snapshot), separadas das taxas do catálogo.
CREATE TABLE purchase_tax (
    id          BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    purchase_id BIGINT         NOT NULL REFERENCES purchase (id) ON DELETE CASCADE,
    name        VARCHAR(20)    NOT NULL,
    rate        NUMERIC(7, 4)  NOT NULL,
    value       NUMERIC(12, 2) NOT NULL CHECK (value >= 0),
    CONSTRAINT uk_purchase_tax UNIQUE (purchase_id, name)
);

-- -----------------------------------------------------------------------------
-- Estruturas preparadas para a finalização da compra (botão "Comprar" ainda
-- desligado nesta entrega). Recebem os dados de TOKENIZAÇÃO, nunca o cartão:
--   CREDIT/DEBIT -> brand, last_four, expiration, issuer, multiple + tokens
--   PIX          -> issuer chega depois, no retorno (callback) do pagamento
--   WALLET       -> issuer fixo conforme wallet_provider (PicPay / Mercado Pago)
-- -----------------------------------------------------------------------------
CREATE TABLE purchase_payment (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    purchase_id     BIGINT       NOT NULL REFERENCES purchase (id),
    method          VARCHAR(10)  NOT NULL,
    issuer          VARCHAR(60),
    brand           VARCHAR(20),
    last_four       VARCHAR(4),
    expiration      VARCHAR(5),               -- MM/YY
    multiple        BOOLEAN,
    wallet_provider VARCHAR(20),
    installments    INTEGER     NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_purchase_payment UNIQUE (purchase_id),
    CONSTRAINT ck_purchase_payment_method CHECK (method IN ('CREDIT', 'DEBIT', 'PIX', 'WALLET'))
);

CREATE TABLE purchase_payment_token (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    purchase_payment_id BIGINT       NOT NULL REFERENCES purchase_payment (id),
    name                VARCHAR(60)  NOT NULL,
    token               VARCHAR(120) NOT NULL,
    gateway             VARCHAR(60)  NOT NULL,
    expiration_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_purchase_payment_token UNIQUE (token)
);

-- -----------------------------------------------------------------------------
-- Transactional Outbox: o evento para o orquestrador é gravado NA MESMA
-- transação que muda a compra para PROCESSING. Um "relay" (job) publica depois.
-- Evita o problema do "dual write" (gravar no banco e falhar ao publicar, ou o
-- contrário). Orquestração desligada nesta entrega (feature toggle).
-- -----------------------------------------------------------------------------
CREATE TABLE outbox_event (
    id             UUID         PRIMARY KEY,
    aggregate_type VARCHAR(40)  NOT NULL,
    aggregate_id   VARCHAR(60)  NOT NULL,
    event_type     VARCHAR(60)  NOT NULL,
    payload        JSONB        NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts       INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ,
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX ix_outbox_pending ON outbox_event (created_at) WHERE status = 'PENDING';

-- -----------------------------------------------------------------------------
-- Auditoria genérica: UMA tabela + UMA função de trigger para todas as tabelas,
-- guardando a linha antiga em JSONB. No billing (MySQL) cada tabela tinha sua
-- cópia _AU com a mesma estrutura - aqui o JSONB evita manter N tabelas espelho
-- sincronizadas a cada ALTER TABLE (problema real vivido no billing).
-- -----------------------------------------------------------------------------
CREATE TABLE audit_log (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_name VARCHAR(60) NOT NULL,
    row_id     TEXT        NOT NULL,
    command    VARCHAR(10) NOT NULL,
    old_row    JSONB       NOT NULL,
    audit_dt   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_log_table_row ON audit_log (table_name, row_id);

CREATE OR REPLACE FUNCTION fn_audit_row() RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_log (table_name, row_id, command, old_row)
    VALUES (TG_TABLE_NAME, to_jsonb(OLD) ->> TG_ARGV[0], TG_OP, to_jsonb(OLD));
    -- Trigger AFTER: o retorno é ignorado pelo Postgres. (Cuidado: num trigger
    -- BEFORE UPDATE, retornar OLD faria o UPDATE gravar os valores ANTIGOS!)
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- TG_ARGV[0] = nome da coluna de chave da linha auditada.
CREATE TRIGGER tg_audit_subscriber          AFTER UPDATE OR DELETE ON subscriber          FOR EACH ROW EXECUTE FUNCTION fn_audit_row('id');
CREATE TRIGGER tg_audit_subscriber_document AFTER UPDATE OR DELETE ON subscriber_document FOR EACH ROW EXECUTE FUNCTION fn_audit_row('id');
CREATE TRIGGER tg_audit_subscriber_address  AFTER UPDATE OR DELETE ON subscriber_address  FOR EACH ROW EXECUTE FUNCTION fn_audit_row('id');
CREATE TRIGGER tg_audit_subscriber_phone    AFTER UPDATE OR DELETE ON subscriber_phone    FOR EACH ROW EXECUTE FUNCTION fn_audit_row('id');
CREATE TRIGGER tg_audit_product             AFTER UPDATE OR DELETE ON product             FOR EACH ROW EXECUTE FUNCTION fn_audit_row('id');
CREATE TRIGGER tg_audit_product_plan        AFTER UPDATE OR DELETE ON product_plan        FOR EACH ROW EXECUTE FUNCTION fn_audit_row('id');
CREATE TRIGGER tg_audit_tax_model_item      AFTER UPDATE OR DELETE ON tax_model_item      FOR EACH ROW EXECUTE FUNCTION fn_audit_row('id');
CREATE TRIGGER tg_audit_purchase            AFTER UPDATE OR DELETE ON purchase            FOR EACH ROW EXECUTE FUNCTION fn_audit_row('id');
CREATE TRIGGER tg_audit_purchase_payment    AFTER UPDATE OR DELETE ON purchase_payment    FOR EACH ROW EXECUTE FUNCTION fn_audit_row('id');
CREATE TRIGGER tg_audit_feature_toggle      AFTER UPDATE OR DELETE ON feature_toggle      FOR EACH ROW EXECUTE FUNCTION fn_audit_row('name');
CREATE TRIGGER tg_audit_config_parameter    AFTER UPDATE OR DELETE ON config_parameter    FOR EACH ROW EXECUTE FUNCTION fn_audit_row('name');
