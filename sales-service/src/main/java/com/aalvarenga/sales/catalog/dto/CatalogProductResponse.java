package com.aalvarenga.sales.catalog.dto;

import java.util.List;

public record CatalogProductResponse(
        String codeId,
        String name,
        boolean expirationService,
        boolean exclusivePurchase,
        boolean trial,
        Integer trialDays,
        String taxModel,
        List<CatalogPlanResponse> plans) {
}
