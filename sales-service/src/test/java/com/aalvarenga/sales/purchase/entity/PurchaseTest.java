package com.aalvarenga.sales.purchase.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;
import com.aalvarenga.sales.catalog.entity.Product;
import com.aalvarenga.sales.pricing.PriceQuote;
import com.aalvarenga.sales.pricing.PricingInput;
import com.aalvarenga.sales.pricing.PricingService;
import com.aalvarenga.sales.support.Fixtures;

/**
 * Regressão do bug "409 DATA_CONFLICT a cada clique na tela de compra": recalcular a
 * cotação NÃO pode recriar as linhas de taxa (o Hibernate inseriria a nova "CBS"
 * antes de apagar a antiga, violando uk_purchase_tax). As mesmas instâncias precisam
 * ser reaproveitadas - no banco isso vira UPDATE.
 */
class PurchaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);

    private final PricingService pricing = new PricingService();

    @Test
    void applyQuoteTwice_reusesTheSameTaxRowsInsteadOfRecreatingThem() {
        Product cap = Fixtures.cap();
        Purchase purchase = Purchase.start(Fixtures.subscriber(), cap, cap.defaultPlan(), Instant.now());
        purchase.applyQuote(quote(cap, null));
        List<PurchaseTax> before = List.copyOf(purchase.getTaxes());

        purchase.applyQuote(quote(cap, PaymentMethod.CREDIT));

        assertThat(purchase.getTaxes()).hasSize(3);
        for (int i = 0; i < before.size(); i++) {
            assertThat(purchase.getTaxes().get(i)).isSameAs(before.get(i));
        }
    }

    @Test
    void applyQuote_updatesValuesAndDropsTaxesThatNoLongerApply() {
        Product streaming = Fixtures.streaming1();
        Purchase purchase = Purchase.start(Fixtures.subscriber(), streaming, streaming.defaultPlan(), Instant.now());
        purchase.applyQuote(quote(streaming, null)); // mensal: CBS 2,28 + IBS 0,78

        PriceQuote annual = pricing.quote(PricingInput.of(streaming.findPlan(RecurrenceFrequency.ANNUAL).orElseThrow(),
                null, new BigDecimal("5.00"), TODAY));
        PriceQuote onlyCbs = new PriceQuote(annual.productValue(), List.of(annual.taxes().getFirst()),
                annual.taxes().getFirst().value(), annual.grossValue(), annual.discountValue(), annual.discountCycles(),
                annual.netValue(), annual.chargedValue(), null, null, TODAY, 12, List.of());
        purchase.applyQuote(onlyCbs);

        assertThat(purchase.getTaxes()).singleElement().satisfies(tax -> {
            assertThat(tax.getName()).isEqualTo("CBS");
            assertThat(tax.getValue()).isEqualByComparingTo("23.65");
        });
    }

    private PriceQuote quote(Product product, PaymentMethod method) {
        return pricing.quote(PricingInput.of(product.defaultPlan(), method, new BigDecimal("5.00"), TODAY));
    }
}
