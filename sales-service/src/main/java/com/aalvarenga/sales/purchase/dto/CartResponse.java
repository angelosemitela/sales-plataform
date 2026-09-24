package com.aalvarenga.sales.purchase.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;
import com.aalvarenga.sales.catalog.dto.CatalogPlanResponse;
import com.aalvarenga.sales.pricing.PriceQuote;
import com.aalvarenga.sales.purchase.CardBrand;
import com.aalvarenga.sales.purchase.PurchaseStatus;
import com.aalvarenga.sales.purchase.PurchaseStep;
import com.aalvarenga.sales.purchase.WalletProvider;

/**
 * Tudo o que a tela de compra precisa numa resposta só: produto, planos, seleção
 * atual, cotação e se o botão "Comprar" está liberado.
 *
 * <p>A API devolve DADOS (datas, números); os TEXTOS ("por 1 ano", "todo dia 24")
 * são montados no front - a tradução/formatação é responsabilidade da interface.</p>
 */
public record CartResponse(
        UUID id,
        String protocol,
        PurchaseStatus status,
        PurchaseStep lastStep,
        Instant startedAt,
        Product product,
        List<CatalogPlanResponse> plans,
        Selection selection,
        PriceQuote quote,
        boolean checkoutEnabled) {

    public record Product(String codeId, String name, boolean expirationService, boolean trial, Integer trialDays) {
    }

    public record Selection(RecurrenceFrequency recurrenceFrequency, PaymentMethod paymentMethod,
                            WalletProvider walletProvider, CardBrand cardBrand, Integer installments) {
    }
}
