package com.aalvarenga.sales.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;

/**
 * Testes com os valores REAIS da carga inicial do catálogo - servem também de
 * documentação executável das regras de preço.
 */
class PricingServiceTest {

    private static final List<TaxRate> SERVICE = List.of(
            new TaxRate("CBS", new BigDecimal("8.8")), new TaxRate("IBS", new BigDecimal("3")));
    private static final List<TaxRate> PRODUCT = List.of(
            new TaxRate("CBS", new BigDecimal("7.6")), new TaxRate("IBS", new BigDecimal("1")),
            new TaxRate("ISS", new BigDecimal("2.3")));
    private static final BigDecimal MIN = new BigDecimal("5.00");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

    private final PricingService service = new PricingService();

    @Test
    void monthlyPlanWithoutDiscount_taxesAreAddedToTheProductValue() {
        PriceQuote quote = service.quote(input("25.90", null, null, RecurrenceFrequency.MONTH, false, null, SERVICE, 1));

        // 25,90 x 8,8% = 2,2792 -> 2,28 ; 25,90 x 3% = 0,777 -> 0,78
        assertThat(quote.taxes()).extracting(TaxLine::value).containsExactly(new BigDecimal("2.28"), new BigDecimal("0.78"));
        assertThat(quote.taxValue()).isEqualByComparingTo("3.06");
        assertThat(quote.grossValue()).isEqualByComparingTo("28.96");
        assertThat(quote.netValue()).isEqualByComparingTo("28.96");
        assertThat(quote.chargedValue()).isEqualByComparingTo("28.96");
        assertThat(quote.hasDiscount()).isFalse();
        assertThat(quote.discountCycles()).isNull();
        assertThat(quote.maxInstallments()).isEqualTo(1);
        assertThat(quote.recurrenceAnchorDate()).isEqualTo(TODAY);
        assertThat(quote.firstChargeDate()).isNull();
    }

    @Test
    void annualPlanWithDiscount_discountIsAppliedAfterTaxes() {
        PriceQuote quote = service.quote(input("268.80", "30", 1, RecurrenceFrequency.ANNUAL, false, null, SERVICE, 12));

        // 268,80 x 8,8% = 23,6544 -> 23,65 ; x 3% = 8,064 -> 8,06
        assertThat(quote.taxValue()).isEqualByComparingTo("31.71");
        assertThat(quote.grossValue()).isEqualByComparingTo("300.51");
        assertThat(quote.discountValue()).isEqualByComparingTo("30.00");
        assertThat(quote.discountCycles()).isEqualTo(1);
        assertThat(quote.netValue()).isEqualByComparingTo("270.51");
        assertThat(quote.chargedValue()).isEqualByComparingTo("270.51");
        // Mesma fórmula validada pelo billing: productValue - discountValue + taxValue = chargedValue
        assertThat(quote.productValue().subtract(quote.discountValue()).add(quote.taxValue()))
                .isEqualByComparingTo(quote.chargedValue());
        assertThat(quote.maxInstallments()).isEqualTo(12);
        assertThat(quote.installmentOptions()).hasSize(12);
        assertThat(quote.installmentOptions().getLast().installmentValue()).isEqualByComparingTo("22.54");
    }

    @Test
    void minimumInstallmentValue_limitsTheNumberOfInstallments() {
        // Boné: 10,90 + taxas PRODUCT (0,83 + 0,11 + 0,25 = 1,19) = 12,09 -> 12,09 / 5 = 2 parcelas,
        // mesmo com o plano permitindo 12.
        PriceQuote quote = service.quote(input("10.90", null, null, RecurrenceFrequency.ONESHOT, false, null, PRODUCT, 12));

        assertThat(quote.taxValue()).isEqualByComparingTo("1.19");
        assertThat(quote.chargedValue()).isEqualByComparingTo("12.09");
        assertThat(quote.maxInstallments()).isEqualTo(2);
        assertThat(quote.installmentOptions()).extracting(InstallmentOption::installmentValue)
                .containsExactly(new BigDecimal("12.09"), new BigDecimal("6.04"));
        assertThat(quote.recurrenceAnchorDate()).isNull(); // ONESHOT não tem recorrência
    }

    @Test
    void maxInstallments_followsTheRuleExampleOfFifteenReais() {
        assertThat(PricingService.maxInstallments(12, PaymentMethod.CREDIT, new BigDecimal("15.00"), MIN)).isEqualTo(3);
        assertThat(PricingService.maxInstallments(12, PaymentMethod.CREDIT, new BigDecimal("14.99"), MIN)).isEqualTo(2);
        assertThat(PricingService.maxInstallments(12, PaymentMethod.CREDIT, new BigDecimal("3.00"), MIN)).isEqualTo(1);
    }

    @Test
    void debitAndPix_areAlwaysSingleInstallment() {
        assertThat(PricingService.maxInstallments(12, PaymentMethod.DEBIT, new BigDecimal("500"), MIN)).isEqualTo(1);
        assertThat(PricingService.maxInstallments(12, PaymentMethod.PIX, new BigDecimal("500"), MIN)).isEqualTo(1);
        assertThat(PricingService.maxInstallments(12, PaymentMethod.WALLET, new BigDecimal("500"), MIN)).isEqualTo(12);
    }

    @Test
    void withoutSelectedMethod_usesThePlanLimitForDisplay() {
        assertThat(PricingService.maxInstallments(12, null, new BigDecimal("500"), MIN)).isEqualTo(12);
    }

    @Test
    void missingMinimumParameter_doesNotLimit() {
        assertThat(PricingService.maxInstallments(12, PaymentMethod.CREDIT, new BigDecimal("10"), BigDecimal.ZERO)).isEqualTo(12);
        assertThat(PricingService.maxInstallments(12, PaymentMethod.CREDIT, new BigDecimal("10"), null)).isEqualTo(12);
    }

    @Test
    void trial_chargesZeroNowAndFirstChargeIsTodayPlusTrialDaysPlusOne() {
        PriceQuote quote = service.quote(input("25.90", null, null, RecurrenceFrequency.MONTH, true, 7, SERVICE, 1));

        assertThat(quote.chargedValue()).isEqualByComparingTo("0.00");
        assertThat(quote.netValue()).isEqualByComparingTo("28.96"); // valor que será cobrado depois do trial
        assertThat(quote.trialDays()).isEqualTo(7);
        assertThat(quote.firstChargeDate()).isEqualTo(LocalDate.of(2026, 10, 2)); // 24/09 + 7 + 1
        assertThat(quote.recurrenceAnchorDate()).isNull();
        assertThat(quote.maxInstallments()).isEqualTo(1);
    }

    private static PricingInput input(String value, String discount, Integer cycles, RecurrenceFrequency frequency,
                                      boolean trial, Integer trialDays, List<TaxRate> rates, int planMax) {
        return new PricingInput(new BigDecimal(value), discount == null ? BigDecimal.ZERO : new BigDecimal(discount), cycles,
                frequency, trial, trialDays, rates, planMax, PaymentMethod.CREDIT, MIN, TODAY);
    }
}
