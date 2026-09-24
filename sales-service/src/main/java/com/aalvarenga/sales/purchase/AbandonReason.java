package com.aalvarenga.sales.purchase;

public enum AbandonReason {
    /** Sem atividade por mais que CART_ABANDON_MINUTES. */
    INACTIVITY,
    /** O assinante abriu outro produto: o carrinho anterior é fechado. */
    REPLACED_BY_NEW_CART
}
