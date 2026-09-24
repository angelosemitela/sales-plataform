package com.aalvarenga.sales.catalog;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aalvarenga.sales.catalog.dto.CatalogPlanResponse;
import com.aalvarenga.sales.catalog.dto.CatalogProductResponse;
import com.aalvarenga.sales.catalog.entity.Product;
import com.aalvarenga.sales.catalog.entity.ProductPlan;
import com.aalvarenga.sales.catalog.repository.ProductRepository;
import com.aalvarenga.sales.feature.FeatureToggleService;
import com.aalvarenga.sales.feature.Features;
import com.aalvarenga.sales.pricing.PriceQuote;
import com.aalvarenga.sales.pricing.PricingInput;
import com.aalvarenga.sales.pricing.PricingService;
import com.aalvarenga.sales.shared.error.BusinessException;
import com.aalvarenga.sales.shared.time.BusinessTime;

import lombok.RequiredArgsConstructor;

/** Vitrine: lista os produtos ativos com o preço de cada plano já calculado. */
@Service
@RequiredArgsConstructor
public class CatalogService {

    public static final BigDecimal DEFAULT_MIN_INSTALLMENT = new BigDecimal("5.00");

    private final ProductRepository productRepository;
    private final PricingService pricingService;
    private final FeatureToggleService featureToggleService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<CatalogProductResponse> list() {
        BigDecimal minInstallment = minInstallmentValue();
        LocalDate today = BusinessTime.today(clock);
        return productRepository.findAllByActiveTrueOrderByIdAsc().stream()
                .map(product -> toResponse(product, minInstallment, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogProductResponse get(String codeId) {
        Product product = findActive(codeId);
        return toResponse(product, minInstallmentValue(), BusinessTime.today(clock));
    }

    public Product findActive(String codeId) {
        return productRepository.findByCodeIdAndActiveTrue(codeId)
                .orElseThrow(() -> BusinessException.notFound("PRODUCT_NOT_FOUND", "Product " + codeId + " not found"));
    }

    public BigDecimal minInstallmentValue() {
        return featureToggleService.decimalParameter(Features.MIN_INSTALLMENT_VALUE, DEFAULT_MIN_INSTALLMENT);
    }

    public CatalogPlanResponse toPlanResponse(ProductPlan plan, BigDecimal minInstallment, LocalDate today) {
        PriceQuote quote = pricingService.quote(PricingInput.of(plan, null, minInstallment, today));
        return new CatalogPlanResponse(
                plan.getRecurrenceFrequency(),
                quote.productValue(),
                quote.taxValue(),
                quote.grossValue(),
                quote.hasDiscount(),
                quote.discountValue(),
                quote.discountCycles(),
                quote.netValue(),
                quote.maxInstallments(),
                plan.getPaymentMethods().stream().sorted().toList());
    }

    private CatalogProductResponse toResponse(Product product, BigDecimal minInstallment, LocalDate today) {
        return new CatalogProductResponse(
                product.getCodeId(),
                product.getName(),
                product.isExpirationService(),
                product.isExclusivePurchase(),
                product.isTrial(),
                product.getTrialDays(),
                product.getTaxModel().getCode(),
                product.getPlans().stream().map(plan -> toPlanResponse(plan, minInstallment, today)).toList());
    }
}
