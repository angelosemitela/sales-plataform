package com.aalvarenga.sales.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.aalvarenga.sales.catalog.CatalogService;
import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;
import com.aalvarenga.sales.catalog.entity.Product;
import com.aalvarenga.sales.feature.FeatureToggleService;
import com.aalvarenga.sales.feature.Features;
import com.aalvarenga.sales.integration.billing.BillingPurchaseDraft;
import com.aalvarenga.sales.integration.billing.BillingPurchaseDraftMapper;
import com.aalvarenga.sales.integration.orchestration.PurchaseSubmissionPort;
import com.aalvarenga.sales.pricing.PricingService;
import com.aalvarenga.sales.purchase.dto.CheckoutRequest;
import com.aalvarenga.sales.purchase.dto.CheckoutResponse;
import com.aalvarenga.sales.purchase.entity.Purchase;
import com.aalvarenga.sales.purchase.entity.PurchasePayment;
import com.aalvarenga.sales.purchase.repository.PurchasePaymentRepository;
import com.aalvarenga.sales.purchase.repository.PurchaseRepository;
import com.aalvarenga.sales.shared.error.BusinessException;
import com.aalvarenga.sales.subscriber.entity.Subscriber;
import com.aalvarenga.sales.subscriber.repository.SubscriberRepository;
import com.aalvarenga.sales.support.Fixtures;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckoutServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T15:00:00Z");

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private PurchasePaymentRepository paymentRepository;
    @Mock
    private SubscriberRepository subscriberRepository;
    @Mock
    private PurchaseSubmissionPort submissionPort;

    private CheckoutService service;
    private final Subscriber subscriber = Fixtures.subscriber();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        CartService cartService = new CartService(purchaseRepository, catalogService, new PricingService(), featureToggleService, clock);
        service = new CheckoutService(cartService, paymentRepository, subscriberRepository, featureToggleService,
                new BillingPurchaseDraftMapper(), submissionPort, clock);
        when(catalogService.minInstallmentValue()).thenReturn(new BigDecimal("5.00"));
        when(featureToggleService.isEnabled(anyString())).thenReturn(false);
        when(featureToggleService.isEnabled(Features.CHECKOUT_ENABLED)).thenReturn(true);
        when(subscriberRepository.findById(10L)).thenReturn(Optional.of(subscriber));
    }

    @Test
    void checkout_disabledByToggle_isConflictAndNothingIsSubmitted() {
        when(featureToggleService.isEnabled(Features.CHECKOUT_ENABLED)).thenReturn(false);
        Purchase cart = cart(PaymentMethod.PIX, null, null);

        assertThatThrownBy(() -> service.checkout(subscriber, cart.getPublicId(), new CheckoutRequest(null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "CHECKOUT_DISABLED");
        verify(submissionPort, never()).submit(any(), any());
    }

    @Test
    void checkout_creditCard_storesTokenizationAndSubmitsTheBillingDraft() {
        Purchase cart = cart(PaymentMethod.CREDIT, CardBrand.VISA, null);

        CheckoutResponse response = service.checkout(subscriber, cart.getPublicId(), new CheckoutRequest(tokenization("12/30")));

        assertThat(response.status()).isEqualTo(PurchaseStatus.PROCESSING);
        assertThat(cart.getLastStep()).isEqualTo(PurchaseStep.SUBMITTED);
        assertThat(cart.getSubmittedAt()).isEqualTo(NOW);

        ArgumentCaptor<PurchasePayment> payment = ArgumentCaptor.forClass(PurchasePayment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getIssuer()).isEqualTo("Santander");
        assertThat(payment.getValue().getLastFour()).isEqualTo("1234");
        assertThat(payment.getValue().getTokens()).hasSize(1);

        ArgumentCaptor<BillingPurchaseDraft> draft = ArgumentCaptor.forClass(BillingPurchaseDraft.class);
        verify(submissionPort).submit(any(Purchase.class), draft.capture());
        assertThat(draft.getValue().protocol()).isEqualTo(cart.getProtocol());
        assertThat(draft.getValue().payment().getFirst().cardNumber()).isEqualTo("************1234");
        assertThat(draft.getValue().billing().getFirst().chargedValue()).isEqualByComparingTo("270.51");
    }

    @Test
    void checkout_creditCardWithoutTokenization_isBadRequest() {
        Purchase cart = cart(PaymentMethod.CREDIT, CardBrand.VISA, null);

        assertThatThrownBy(() -> service.checkout(subscriber, cart.getPublicId(), new CheckoutRequest(null)))
                .hasFieldOrPropertyWithValue("code", "TOKENIZATION_REQUIRED");
    }

    @Test
    void checkout_expiredCard_isBadRequest() {
        Purchase cart = cart(PaymentMethod.CREDIT, CardBrand.VISA, null);

        assertThatThrownBy(() -> service.checkout(subscriber, cart.getPublicId(), new CheckoutRequest(tokenization("08/26"))))
                .hasFieldOrPropertyWithValue("code", "INVALID_CARD_EXPIRATION");
    }

    @Test
    void checkout_wallet_issuerIsFixedByTheWalletProvider() {
        Purchase cart = cart(PaymentMethod.WALLET, null, WalletProvider.MERCADO_PAGO);

        service.checkout(subscriber, cart.getPublicId(), new CheckoutRequest(null));

        ArgumentCaptor<PurchasePayment> payment = ArgumentCaptor.forClass(PurchasePayment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getIssuer()).isEqualTo("Mercado Pago");
    }

    @Test
    void checkout_pix_issuerArrivesLaterOnThePaymentCallback() {
        Purchase cart = cart(PaymentMethod.PIX, null, null);

        service.checkout(subscriber, cart.getPublicId(), new CheckoutRequest(null));

        ArgumentCaptor<PurchasePayment> payment = ArgumentCaptor.forClass(PurchasePayment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getIssuer()).isNull();
    }

    /** Carrinho aberto no plano ANUAL do TS1 (aceita os 4 métodos). */
    private Purchase cart(PaymentMethod method, CardBrand brand, WalletProvider wallet) {
        Product product = Fixtures.streaming1();
        Purchase purchase = Purchase.start(subscriber, product, product.findPlan(RecurrenceFrequency.ANNUAL).orElseThrow(), NOW);
        purchase.setPaymentMethod(method);
        purchase.setCardBrand(brand);
        purchase.setWalletProvider(wallet);
        purchase.setInstallments(1);
        when(purchaseRepository.findByPublicIdAndSubscriberId(purchase.getPublicId(), 10L)).thenReturn(Optional.of(purchase));
        return purchase;
    }

    private static CheckoutRequest.Tokenization tokenization(String expiration) {
        return new CheckoutRequest.Tokenization("Santander", true, CardBrand.VISA, "1234", expiration,
                List.of(new CheckoutRequest.Token("ACCESS_TOKEN", "TOK-123", "INTERNAL", null)));
    }
}
