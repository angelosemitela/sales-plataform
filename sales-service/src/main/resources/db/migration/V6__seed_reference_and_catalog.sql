-- =============================================================================
-- V6 - Carga inicial: documentos x países, modelos de taxa, toggles, parâmetros
-- e o catálogo de exemplo.
-- =============================================================================

-- Tipos de documento ----------------------------------------------------------
INSERT INTO document_type (code, description, country_selectable, all_countries, sort_order) VALUES
    ('CPF',      'CPF',                     FALSE, FALSE, 1),
    ('SSN',      'Social Security Number',  FALSE, FALSE, 2),
    ('UE',       'Documento União Europeia', TRUE, FALSE, 3),
    ('PASSPORT', 'Passaporte',               TRUE, TRUE,  4),
    ('OTHER',    'Outro',                    TRUE, TRUE,  5);

-- PASSPORT/OTHER aceitam qualquer país (all_countries = TRUE), então não precisam
-- de linhas aqui - evita 2 x 249 linhas redundantes.
INSERT INTO document_type_country (document_type_code, country_code) VALUES
    ('CPF', 'BR'),
    ('SSN', 'US');

INSERT INTO document_type_country (document_type_code, country_code)
SELECT 'UE', code FROM country
WHERE code IN ('DE','AT','BE','BG','CZ','CY','HR','DK','SK','SI','ES','EE','FI','FR',
               'GR','HU','IE','IT','LV','LT','LU','MT','NL','PL','PT','RO','SE');

-- Modelos de taxa (alíquotas em %) ------------------------------------------------
INSERT INTO tax_model (code, description) VALUES
    ('SERVICE', 'Prestação de serviço'),
    ('PRODUCT', 'Venda de produto');

INSERT INTO tax_model_item (tax_model_code, name, rate) VALUES
    ('SERVICE', 'CBS', 8.8),
    ('SERVICE', 'IBS', 3),
    ('PRODUCT', 'CBS', 7.6),
    ('PRODUCT', 'IBS', 1),
    ('PRODUCT', 'ISS', 2.3);

-- Toggles e parâmetros ---------------------------------------------------------
INSERT INTO feature_toggle (name, enabled, description) VALUES
    ('CHECKOUT_ENABLED',      FALSE, 'Libera o botão Comprar e o endpoint de finalização da compra'),
    ('ORCHESTRATION_ENABLED', FALSE, 'Libera a publicação dos eventos do outbox para o orquestrador');

INSERT INTO config_parameter (name, value, description) VALUES
    ('MIN_INSTALLMENT_VALUE', '5.00', 'Valor mínimo de cada parcela em BRL (regra geral 8)'),
    ('CART_ABANDON_MINUTES',  '30',   'Minutos sem atividade para um carrinho ser considerado abandonado');

-- Catálogo de exemplo ---------------------------------------------------------
INSERT INTO product (code_id, name, expiration_service, exclusive_purchase, trial, trial_days, tax_model_code) VALUES
    ('TS1', 'Teste Streaming 1', TRUE,  TRUE,  FALSE, NULL, 'SERVICE'),
    ('TS2', 'Teste Streaming 2', TRUE,  FALSE, FALSE, NULL, 'SERVICE'),
    ('VR1', 'Bone',              FALSE, FALSE, FALSE, NULL, 'PRODUCT');

-- discount_cycles = 1 nos planos com desconto: o catálogo de exemplo não trazia
-- a duração; 1 ciclo = "por 1 ano" no plano anual.
INSERT INTO product_plan (product_id, recurrence_frequency, product_value, max_installments,
                          has_discount, discount_value, discount_cycles, sort_order)
SELECT p.id, v.freq, v.value, v.max_inst, v.has_disc, v.disc, v.cycles, v.ord
FROM (VALUES
    ('TS1', 'MONTH',   25.90,  1, FALSE, NULL::NUMERIC,   NULL::INTEGER, 1),
    ('TS1', 'ANNUAL', 268.80, 12, TRUE,  30.00,           1,              2),
    ('TS2', 'MONTH',   75.90,  1, FALSE, NULL,            NULL,           1),
    ('TS2', 'ANNUAL', 826.80, 12, TRUE,  120.00,          1,              2),
    ('VR1', 'ONESHOT', 10.90, 12, FALSE, NULL,            NULL,           1)
) AS v(code, freq, value, max_inst, has_disc, disc, cycles, ord)
JOIN product p ON p.code_id = v.code;

INSERT INTO product_plan_payment_method (product_plan_id, method)
SELECT pp.id, m.method
FROM (VALUES
    ('TS1', 'MONTH',   'CREDIT'), ('TS1', 'MONTH',   'DEBIT'), ('TS1', 'MONTH',   'PIX'),
    ('TS1', 'ANNUAL',  'CREDIT'), ('TS1', 'ANNUAL',  'DEBIT'), ('TS1', 'ANNUAL',  'PIX'), ('TS1', 'ANNUAL', 'WALLET'),
    ('TS2', 'MONTH',   'CREDIT'), ('TS2', 'MONTH',   'DEBIT'), ('TS2', 'MONTH',   'PIX'),
    ('TS2', 'ANNUAL',  'CREDIT'), ('TS2', 'ANNUAL',  'DEBIT'), ('TS2', 'ANNUAL',  'PIX'), ('TS2', 'ANNUAL', 'WALLET'),
    ('VR1', 'ONESHOT', 'CREDIT'), ('VR1', 'ONESHOT', 'DEBIT'), ('VR1', 'ONESHOT', 'PIX')
) AS m(code, freq, method)
JOIN product p       ON p.code_id = m.code
JOIN product_plan pp ON pp.product_id = p.id AND pp.recurrence_frequency = m.freq;
