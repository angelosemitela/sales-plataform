package com.aalvarenga.sales.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;

/** Plano como a vitrine exibe: preço já com taxas e desconto calculados. */
public record CatalogPlanResponse(
        RecurrenceFrequency recurrenceFrequency,
        BigDecimal productValue,
        BigDecimal taxValue,
        BigDecimal grossValue,
        boolean hasDiscount,
        BigDecimal discountValue,
        Integer discountCycles,
        BigDecimal netValue,
        int maxInstallments,
        List<PaymentMethod> paymentMethods) {
}
