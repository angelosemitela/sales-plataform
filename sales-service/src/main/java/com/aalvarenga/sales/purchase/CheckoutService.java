package com.aalvarenga.sales.purchase;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.feature.FeatureToggleService;
import com.aalvarenga.sales.feature.Features;
import com.aalvarenga.sales.integration.billing.BillingPurchaseDraftMapper;
import com.aalvarenga.sales.integration.orchestration.PurchaseSubmissionPort;
import com.aalvarenga.sales.pricing.PriceQuote;
import com.aalvarenga.sales.purchase.dto.CheckoutRequest;
import com.aalvarenga.sales.purchase.dto.CheckoutResponse;
import com.aalvarenga.sales.purchase.entity.Purchase;
import com.aalvarenga.sales.purchase.entity.PurchasePayment;
import com.aalvarenga.sales.purchase.entity.PurchasePaymentToken;
import com.aalvarenga.sales.purchase.repository.PurchasePaymentRepository;
import com.aalvarenga.sales.shared.error.BusinessException;
import com.aalvarenga.sales.shared.time.BusinessTime;
import com.aalvarenga.sales.subscriber.entity.Subscriber;
import com.aalvarenga.sales.subscriber.repository.SubscriberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Clique final em "Comprar". <b>Desligado nesta entrega</b> pelo toggle
 * {@code CHECKOUT_ENABLED}, mas o fluxo já está desenhado de ponta a ponta:
 * <ol>
 *   <li>valida carrinho aberto + método escolhido + dados de tokenização;</li>
 *   <li>recalcula a cotação (preço final na data da compra);</li>
 *   <li>grava o pagamento tokenizado ({@code purchase_payment} + tokens);</li>
 *   <li>muda a compra para PROCESSING;</li>
 *   <li>monta o rascunho do payload do billing e o entrega à porta de submissão
 *       (outbox -> orquestrador) - tudo na MESMA transação.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartService cartService;
    private final PurchasePaymentRepository paymentRepository;
    private final SubscriberRepository subscriberRepository;
    private final FeatureToggleService featureToggleService;
    private final BillingPurchaseDraftMapper billingMapper;
    private final PurchaseSubmissionPort submissionPort;
    private final Clock clock;

    @Transactional
    public CheckoutResponse checkout(Subscriber subscriber, UUID purchaseId, CheckoutRequest request) {
        if (!featureToggleService.isEnabled(Features.CHECKOUT_ENABLED)) {
            throw BusinessException.conflict("CHECKOUT_DISABLED", "Checkout is not available yet");
        }
        Purchase purchase = cartService.requireOpenCart(subscriber, purchaseId);
        PaymentMethod method = purchase.getPaymentMethod();
        if (method == null) {
            throw BusinessException.badRequest("PAYMENT_METHOD_REQUIRED", "Select a payment method before checkout");
        }

        PurchasePayment payment = buildPayment(purchase, method, request.tokenization());

        PriceQuote quote = cartService.quoteFor(purchase.getPlan(), method);
        if (purchase.getInstallments() > quote.maxInstallments()) {
            throw BusinessException.badRequest("INSTALLMENTS_NOT_ALLOWED",
                    "Maximum installments for this selection is " + quote.maxInstallments());
        }
        purchase.applyQuote(quote);

        Instant now = clock.instant();
        purchase.submit(now);
        paymentRepository.save(payment);

        // Recarrega o assinante DENTRO da transação: documentos/endereços/telefones são LAZY.
        Subscriber fullSubscriber = subscriberRepository.findById(subscriber.getId()).orElseThrow();
        submissionPort.submit(purchase, billingMapper.toDraft(purchase, fullSubscriber, payment, now));
        return new CheckoutResponse(purchase.getPublicId(), purchase.getProtocol(), purchase.getStatus());
    }

    /**
     * Regra geral 5 - de onde vem cada dado do pagamento:
     * <ul>
     *   <li>CREDIT/DEBIT: issuer, isMultiple e tokens vêm da TOKENIZAÇÃO;</li>
     *   <li>PIX: issuer só chega no retorno (callback) do pagamento - fica nulo aqui;</li>
     *   <li>WALLET: issuer fixo pela carteira escolhida (PicPay / Mercado Pago).</li>
     * </ul>
     */
    PurchasePayment buildPayment(Purchase purchase, PaymentMethod method, CheckoutRequest.Tokenization tokenization) {
        PurchasePayment payment = new PurchasePayment();
        payment.setPurchase(purchase);
        payment.setMethod(method);
        payment.setInstallments(purchase.getInstallments());

        switch (method) {
            case CREDIT, DEBIT -> {
                if (tokenization == null || tokenization.isMultiple() == null || tokenization.brand() == null
                        || tokenization.lastFour() == null || tokenization.tokens() == null || tokenization.tokens().isEmpty()) {
                    throw BusinessException.badRequest("TOKENIZATION_REQUIRED",
                            "Card payments require issuer, isMultiple, brand, lastFour, expiration and tokens");
                }
                if (purchase.getCardBrand() != null && purchase.getCardBrand() != tokenization.brand()) {
                    throw BusinessException.badRequest("CARD_BRAND_MISMATCH", "Tokenized card brand differs from the selected one");
                }
                if (!CardExpiration.isValid(tokenization.expiration(), YearMonth.from(BusinessTime.today(clock)))) {
                    throw BusinessException.badRequest("INVALID_CARD_EXPIRATION", "Card expiration is invalid or expired");
                }
                payment.setIssuer(tokenization.issuer());
                payment.setMultiple(tokenization.isMultiple());
                payment.setBrand(tokenization.brand());
                payment.setLastFour(tokenization.lastFour());
                payment.setExpiration(tokenization.expiration());
                addTokens(payment, tokenization.tokens());
            }
            case WALLET -> {
                if (purchase.getWalletProvider() == null) {
                    throw BusinessException.badRequest("WALLET_PROVIDER_REQUIRED", "Select PicPay or Mercado Pago");
                }
                payment.setWalletProvider(purchase.getWalletProvider());
                payment.setIssuer(purchase.getWalletProvider().getIssuer());
                if (tokenization != null && tokenization.tokens() != null) {
                    addTokens(payment, tokenization.tokens());
                }
            }
            case PIX -> payment.setIssuer(null);
        }
        return payment;
    }

    private static void addTokens(PurchasePayment payment, List<CheckoutRequest.Token> tokens) {
        for (CheckoutRequest.Token input : tokens) {
            PurchasePaymentToken token = new PurchasePaymentToken();
            token.setName(input.name());
            token.setToken(input.id());
            token.setGateway(input.gateway());
            token.setExpirationAt(input.expirationAt());
            payment.addToken(token);
        }
    }
}
