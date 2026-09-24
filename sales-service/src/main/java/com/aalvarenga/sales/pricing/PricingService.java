package com.aalvarenga.sales.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.aalvarenga.sales.catalog.PaymentMethod;

/**
 * Cálculo de preço, taxas, trial e parcelamento. <b>Fonte única da verdade</b>:
 * o front nunca calcula preço sozinho, só exibe a cotação devolvida pela API
 * (assim uma regra nunca fica duplicada - e divergente - em duas linguagens).
 *
 * <p>Classe PURA: sem banco, sem relógio, sem estado. Toda informação chega
 * pelo {@link PricingInput}, inclusive a data da compra.</p>
 */
@Service
public class PricingService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public PriceQuote quote(PricingInput input) {
        BigDecimal productValue = money(input.productValue());

        // 1) Taxas: percentual sobre o valor do produto, cada uma arredondada em
        //    centavos (HALF_UP). O total é a SOMA das linhas já arredondadas, para
        //    que "soma de billing.tax[].value == billing.taxValue" feche exatamente no billing.
        List<TaxLine> taxes = input.taxRates().stream()
                .map(rate -> new TaxLine(rate.name(), rate.rate(),
                        money(productValue.multiply(rate.rate()).divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP))))
                .toList();
        BigDecimal taxValue = taxes.stream().map(TaxLine::value).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2) Valores cheio / com desconto / cobrado agora.
        BigDecimal discountValue = money(input.discountValue() == null ? BigDecimal.ZERO : input.discountValue());
        BigDecimal grossValue = productValue.add(taxValue);
        BigDecimal netValue = grossValue.subtract(discountValue);
        BigDecimal chargedValue = input.trial() ? money(BigDecimal.ZERO) : netValue;

        // 3) Datas de exibição: trial -> 1ª cobrança; recorrente sem trial -> dia da recorrência.
        //    "hoje + trialDays + 1": mesma conta do billing (sempre meia-noite do dia seguinte
        //    ao fim do trial). Ex: compra 24/09, 7 dias -> cobrança em 02/10.
        var firstChargeDate = input.trial() ? input.purchaseDate().plusDays(input.trialDays() + 1L) : null;
        var recurrenceAnchor = !input.trial() && input.frequency().isRecurring() ? input.purchaseDate() : null;

        // 4) Parcelamento.
        int maxInstallments = maxInstallments(input.planMaxInstallments(), input.paymentMethod(),
                chargedValue, input.minInstallmentValue());
        List<InstallmentOption> options = new ArrayList<>();
        for (int n = 1; n <= maxInstallments; n++) {
            options.add(new InstallmentOption(n, chargedValue.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN)));
        }

        return new PriceQuote(productValue, taxes, taxValue, grossValue, discountValue,
                discountValue.signum() > 0 ? input.discountCycles() : null,
                netValue, chargedValue,
                input.trial() ? input.trialDays() : null,
                firstChargeDate, recurrenceAnchor, maxInstallments, List.copyOf(options));
    }

    /**
     * Máximo de parcelas = o MENOR entre:
     * <ul>
     *   <li>o máximo do plano no catálogo;</li>
     *   <li>1, se o método não parcela (DEBIT/PIX - regra do billing);</li>
     *   <li>quantas parcelas cabem sem nenhuma ficar abaixo do valor mínimo
     *       (regra geral 8: R$ 15,00 com mínimo de R$ 5,00 -> no máximo 3x).</li>
     * </ul>
     * Sem método escolhido ainda, considera o plano (para exibir "em até Nx").
     * Nunca devolve menos que 1.
     */
    static int maxInstallments(int planMax, PaymentMethod method, BigDecimal chargedValue, BigDecimal minInstallmentValue) {
        int byMethod = method == null || method.allowsInstallments() ? planMax : 1;
        int byMinimumValue;
        if (chargedValue.signum() <= 0) {
            byMinimumValue = 1;                 // nada a cobrar agora (ex: trial): 1x
        } else if (minInstallmentValue == null || minInstallmentValue.signum() <= 0) {
            byMinimumValue = Integer.MAX_VALUE; // sem valor mínimo configurado: não limita
        } else {
            byMinimumValue = chargedValue.divide(minInstallmentValue, 0, RoundingMode.DOWN).intValue();
        }
        return Math.max(1, Math.min(byMethod, byMinimumValue));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
