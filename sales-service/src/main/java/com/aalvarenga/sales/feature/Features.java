package com.aalvarenga.sales.feature;

/** Nomes das regras de feature toggle e dos parâmetros (evita "strings mágicas" espalhadas). */
public final class Features {

    public static final String CHECKOUT_ENABLED = "CHECKOUT_ENABLED";
    public static final String ORCHESTRATION_ENABLED = "ORCHESTRATION_ENABLED";

    public static final String MIN_INSTALLMENT_VALUE = "MIN_INSTALLMENT_VALUE";
    public static final String CART_ABANDON_MINUTES = "CART_ABANDON_MINUTES";

    private Features() {
    }
}
