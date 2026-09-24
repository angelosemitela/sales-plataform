package com.aalvarenga.sales.catalog;

/** Métodos de pagamento (mesmos valores do billing). */
public enum PaymentMethod {
    CREDIT,
    DEBIT,
    PIX,
    WALLET;

    /** Regra do billing: só crédito e carteira aceitam parcelamento; débito e PIX são sempre 1x. */
    public boolean allowsInstallments() {
        return this == CREDIT || this == WALLET;
    }

    public boolean isCard() {
        return this == CREDIT || this == DEBIT;
    }
}
