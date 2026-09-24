package com.aalvarenga.sales.purchase;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Carteiras aceitas. O {@code issuer} enviado ao billing é FIXO por carteira
 * (regra geral 5), por isso fica no próprio enum.
 */
@Getter
@RequiredArgsConstructor
public enum WalletProvider {
    PICPAY("PicPay"),
    MERCADO_PAGO("Mercado Pago");

    private final String issuer;
}
