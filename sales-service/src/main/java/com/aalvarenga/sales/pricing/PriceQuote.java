package com.aalvarenga.sales.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado do cálculo de preço - a "cotação" que a tela exibe e que a compra grava.
 *
 * <p>Fórmula (a mesma que o billing valida: {@code productValue - discountValue +
 * taxValue = chargedValue}):</p>
 * <pre>
 *   taxValue    = soma( productValue x alíquota / 100 )   (cada taxa arredondada)
 *   grossValue  = productValue + taxValue                 (valor "cheio", tachado se há desconto)
 *   netValue    = grossValue - discountValue              (valor com desconto)
 *   chargedValue= netValue, ou 0 se for trial             (o que é cobrado AGORA)
 * </pre>
 *
 * @param firstChargeDate      data da 1ª cobrança em produto trial (senão {@code null})
 * @param recurrenceAnchorDate data que define o dia da recorrência (dia do mês / dia e mês),
 *                             só para produto recorrente SEM trial
 * @param maxInstallments      máximo de parcelas permitido (plano x método x parcela mínima)
 */
public record PriceQuote(
        BigDecimal productValue,
        List<TaxLine> taxes,
        BigDecimal taxValue,
        BigDecimal grossValue,
        BigDecimal discountValue,
        Integer discountCycles,
        BigDecimal netValue,
        BigDecimal chargedValue,
        Integer trialDays,
        LocalDate firstChargeDate,
        LocalDate recurrenceAnchorDate,
        int maxInstallments,
        List<InstallmentOption> installmentOptions) {

    public boolean hasDiscount() {
        return discountValue.signum() > 0;
    }
}
