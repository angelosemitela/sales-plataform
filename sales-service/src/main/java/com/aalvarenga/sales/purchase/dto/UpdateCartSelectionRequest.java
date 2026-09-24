package com.aalvarenga.sales.purchase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;
import com.aalvarenga.sales.purchase.CardBrand;
import com.aalvarenga.sales.purchase.WalletProvider;

/**
 * Estado COMPLETO da seleção na tela (semântica de PUT: o que não vier, fica nulo).
 *
 * @param cardBrand    bandeira detectada no front pelo número digitado - o número em
 *                     si NUNCA é enviado ao backend
 * @param installments nulo = usar o máximo permitido (padrão pedido para o combo)
 */
public record UpdateCartSelectionRequest(
        @NotNull RecurrenceFrequency recurrenceFrequency,
        PaymentMethod paymentMethod,
        WalletProvider walletProvider,
        CardBrand cardBrand,
        @Min(1) @Max(12) Integer installments) {
}
