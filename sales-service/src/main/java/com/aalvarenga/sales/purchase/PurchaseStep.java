package com.aalvarenga.sales.purchase;

/**
 * Último passo alcançado no funil de compra. Com ele, um relatório de carrinhos
 * abandonados responde "em que ponto o assinante desistiu?".
 */
public enum PurchaseStep {
    PRODUCT_SELECTED,
    PLAN_SELECTED,
    PAYMENT_METHOD_SELECTED,
    PAYMENT_DETAILS_FILLED,
    SUBMITTED
}
