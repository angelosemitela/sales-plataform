package com.aalvarenga.sales.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;
import com.aalvarenga.sales.catalog.entity.Product;
import com.aalvarenga.sales.catalog.entity.ProductPlan;

/**
 * Tudo o que o cálculo de preço precisa, em tipos simples (sem entidade JPA).
 *
 * <p>Separar o "input" das entidades deixa o {@link PricingService} puro: testável
 * com valores literais, sem banco e sem mocks.</p>
 *
 * @param paymentMethod método escolhido, ou {@code null} se o assinante ainda não escolheu
 */
public record PricingInput(
        BigDecimal productValue,
        BigDecimal discountValue,
        Integer discountCycles,
        RecurrenceFrequency frequency,
        boolean trial,
        Integer trialDays,
        List<TaxRate> taxRates,
        int planMaxInstallments,
        PaymentMethod paymentMethod,
        BigDecimal minInstallmentValue,
        LocalDate purchaseDate) {

    public static PricingInput of(ProductPlan plan, PaymentMethod paymentMethod,
                                  BigDecimal minInstallmentValue, LocalDate purchaseDate) {
        Product product = plan.getProduct();
        List<TaxRate> rates = product.getTaxModel().getItems().stream()
                .map(item -> new TaxRate(item.getName(), item.getRate()))
                .toList();
        return new PricingInput(
                plan.getProductValue(),
                plan.isHasDiscount() ? plan.getDiscountValue() : BigDecimal.ZERO,
                plan.isHasDiscount() ? plan.getDiscountCycles() : null,
                plan.getRecurrenceFrequency(),
                product.isTrial(),
                product.getTrialDays(),
                rates,
                plan.getMaxInstallments(),
                paymentMethod,
                minInstallmentValue,
                purchaseDate);
    }
}
