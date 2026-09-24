package com.aalvarenga.sales.pricing;

import java.math.BigDecimal;

/**
 * Uma opção do combo de parcelas.
 *
 * @param installmentValue valor de cada parcela, truncado em centavos. Mesma regra do
 *                         billing (T_BILL_INSTALLMENT): a sobra dos centavos vai para a
 *                         1ª parcela, então as demais nunca passam deste valor.
 */
public record InstallmentOption(int installments, BigDecimal installmentValue) {
}
