package com.aalvarenga.sales.purchase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aalvarenga.sales.catalog.CatalogService;
import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;
import com.aalvarenga.sales.catalog.entity.Product;
import com.aalvarenga.sales.catalog.entity.ProductPlan;
import com.aalvarenga.sales.feature.FeatureToggleService;
import com.aalvarenga.sales.feature.Features;
import com.aalvarenga.sales.pricing.PriceQuote;
import com.aalvarenga.sales.pricing.PricingInput;
import com.aalvarenga.sales.pricing.PricingService;
import com.aalvarenga.sales.purchase.dto.CartResponse;
import com.aalvarenga.sales.purchase.dto.StartCartRequest;
import com.aalvarenga.sales.purchase.dto.UpdateCartSelectionRequest;
import com.aalvarenga.sales.purchase.entity.Purchase;
import com.aalvarenga.sales.purchase.repository.PurchaseRepository;
import com.aalvarenga.sales.shared.error.BusinessException;
import com.aalvarenga.sales.shared.time.BusinessTime;
import com.aalvarenga.sales.subscriber.entity.Subscriber;

import lombok.RequiredArgsConstructor;

/**
 * Carrinho: abrir (clique no produto), atualizar a seleção (plano, método,
 * carteira, bandeira, parcelas) e consultar.
 *
 * <p>Cada alteração de seleção é gravada: além de alimentar a tela, isso registra
 * até onde o assinante chegou (lastStep) - base para medir carrinho abandonado.</p>
 */
@Service
@RequiredArgsConstructor
public class CartService {

    /** Compras que "contam" como já compradas para a regra de compra exclusiva. */
    private static final EnumSet<PurchaseStatus> PURCHASED = EnumSet.of(PurchaseStatus.PROCESSING, PurchaseStatus.COMPLETED);

    private final PurchaseRepository purchaseRepository;
    private final CatalogService catalogService;
    private final PricingService pricingService;
    private final FeatureToggleService featureToggleService;
    private final Clock clock;

    @Transactional
    public CartResponse start(Subscriber subscriber, StartCartRequest request) {
        Product product = catalogService.findActive(request.productCode());
        if (product.isExclusivePurchase()
                && purchaseRepository.existsBySubscriberIdAndProductIdAndStatusIn(subscriber.getId(), product.getId(), PURCHASED)) {
            throw BusinessException.conflict("PRODUCT_ALREADY_PURCHASED",
                    "Product " + product.getCodeId() + " allows only one purchase per subscriber");
        }

        Instant now = clock.instant();
        // Regra: no máximo UM carrinho aberto por assinante. Mesmo produto = retoma o
        // carrinho existente; outro produto = o anterior é encerrado como abandonado.
        for (Purchase open : purchaseRepository.findBySubscriberIdAndStatus(subscriber.getId(), PurchaseStatus.CART)) {
            if (open.getProduct().getId().equals(product.getId())) {
                if (request.recurrenceFrequency() != null && request.recurrenceFrequency() != open.getPlan().getRecurrenceFrequency()) {
                    applySelection(open, new UpdateCartSelectionRequest(request.recurrenceFrequency(), null, null, null, null));
                }
                open.touch(now);
                return toResponse(open, quoteFor(open.getPlan(), open.getPaymentMethod()));
            }
            open.abandon(AbandonReason.REPLACED_BY_NEW_CART, now);
        }

        ProductPlan plan = request.recurrenceFrequency() == null
                ? product.defaultPlan()
                : requirePlan(product, request.recurrenceFrequency());
        Purchase purchase = Purchase.start(subscriber, product, plan, now);
        if (request.recurrenceFrequency() != null) {
            purchase.setLastStep(PurchaseStep.PLAN_SELECTED);
        }
        PriceQuote quote = quoteFor(plan, null);
        purchase.applyQuote(quote);
        purchaseRepository.save(purchase);
        return toResponse(purchase, quote);
    }

    @Transactional
    public CartResponse updateSelection(Subscriber subscriber, UUID purchaseId, UpdateCartSelectionRequest request) {
        Purchase purchase = requireOpenCart(subscriber, purchaseId);
        PriceQuote quote = applySelection(purchase, request);
        purchase.touch(clock.instant());
        return toResponse(purchase, quote);
    }

    @Transactional(readOnly = true)
    public CartResponse get(Subscriber subscriber, UUID purchaseId) {
        Purchase purchase = requireOwned(subscriber, purchaseId);
        return toResponse(purchase, quoteFor(purchase.getPlan(), purchase.getPaymentMethod()));
    }

    /**
     * Valida e aplica a seleção. Ordem das regras:
     * plano existe -> método aceito pelo plano -> carteira só com WALLET ->
     * bandeira só com cartão -> parcelas dentro do máximo calculado.
     */
    PriceQuote applySelection(Purchase purchase, UpdateCartSelectionRequest request) {
        ProductPlan plan = requirePlan(purchase.getProduct(), request.recurrenceFrequency());
        PaymentMethod method = request.paymentMethod();
        if (method != null && !plan.accepts(method)) {
            throw BusinessException.badRequest("PAYMENT_METHOD_NOT_ACCEPTED",
                    "Payment method " + method + " is not accepted for plan " + plan.getRecurrenceFrequency());
        }
        if (request.walletProvider() != null && method != PaymentMethod.WALLET) {
            throw BusinessException.badRequest("WALLET_PROVIDER_NOT_ALLOWED", "walletProvider is only allowed for WALLET");
        }
        if (request.cardBrand() != null && (method == null || !method.isCard())) {
            throw BusinessException.badRequest("CARD_BRAND_NOT_ALLOWED", "cardBrand is only allowed for CREDIT or DEBIT");
        }

        PriceQuote quote = quoteFor(plan, method);
        Integer installments = null;
        if (method != null) {
            installments = request.installments() == null ? quote.maxInstallments() : request.installments();
            if (installments > quote.maxInstallments()) {
                throw BusinessException.badRequest("INSTALLMENTS_NOT_ALLOWED",
                        "Maximum installments for this selection is " + quote.maxInstallments());
            }
        }

        purchase.setPlan(plan);
        purchase.setPaymentMethod(method);
        purchase.setWalletProvider(request.walletProvider());
        purchase.setCardBrand(request.cardBrand());
        purchase.setInstallments(installments);
        purchase.setLastStep(stepFor(method, request));
        purchase.applyQuote(quote);
        return quote;
    }

    static PurchaseStep stepFor(PaymentMethod method, UpdateCartSelectionRequest request) {
        if (method == null) {
            return PurchaseStep.PLAN_SELECTED;
        }
        boolean detailsFilled = switch (method) {
            case PIX -> true;                                   // PIX não tem campos adicionais
            case WALLET -> request.walletProvider() != null;
            case CREDIT, DEBIT -> request.cardBrand() != null;  // número digitado e bandeira reconhecida
        };
        return detailsFilled ? PurchaseStep.PAYMENT_DETAILS_FILLED : PurchaseStep.PAYMENT_METHOD_SELECTED;
    }

    PriceQuote quoteFor(ProductPlan plan, PaymentMethod method) {
        BigDecimal minInstallment = catalogService.minInstallmentValue();
        LocalDate today = BusinessTime.today(clock);
        return pricingService.quote(PricingInput.of(plan, method, minInstallment, today));
    }

    Purchase requireOwned(Subscriber subscriber, UUID purchaseId) {
        return purchaseRepository.findByPublicIdAndSubscriberId(purchaseId, subscriber.getId())
                .orElseThrow(() -> BusinessException.notFound("PURCHASE_NOT_FOUND", "Purchase not found"));
    }

    Purchase requireOpenCart(Subscriber subscriber, UUID purchaseId) {
        Purchase purchase = requireOwned(subscriber, purchaseId);
        if (!purchase.isOpen()) {
            // Ex: o job marcou como abandonado por inatividade. O front oferece "recomeçar".
            throw BusinessException.conflict("CART_NOT_OPEN", "Cart is " + purchase.getStatus());
        }
        return purchase;
    }

    private static ProductPlan requirePlan(Product product, RecurrenceFrequency frequency) {
        return product.findPlan(frequency).orElseThrow(() -> BusinessException.badRequest("PLAN_NOT_AVAILABLE",
                "Plan " + frequency + " is not available for product " + product.getCodeId()));
    }

    CartResponse toResponse(Purchase purchase, PriceQuote quote) {
        Product product = purchase.getProduct();
        BigDecimal minInstallment = catalogService.minInstallmentValue();
        LocalDate today = BusinessTime.today(clock);
        return new CartResponse(
                purchase.getPublicId(),
                purchase.getProtocol(),
                purchase.getStatus(),
                purchase.getLastStep(),
                purchase.getStartedAt(),
                new CartResponse.Product(product.getCodeId(), product.getName(), product.isExpirationService(),
                        product.isTrial(), product.getTrialDays()),
                product.getPlans().stream().map(p -> catalogService.toPlanResponse(p, minInstallment, today)).toList(),
                new CartResponse.Selection(purchase.getPlan().getRecurrenceFrequency(), purchase.getPaymentMethod(),
                        purchase.getWalletProvider(), purchase.getCardBrand(), purchase.getInstallments()),
                quote,
                featureToggleService.isEnabled(Features.CHECKOUT_ENABLED));
    }
}
