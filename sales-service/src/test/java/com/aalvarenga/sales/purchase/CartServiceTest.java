package com.aalvarenga.sales.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.aalvarenga.sales.catalog.CatalogService;
import com.aalvarenga.sales.catalog.PaymentMethod;
import com.aalvarenga.sales.catalog.RecurrenceFrequency;
import com.aalvarenga.sales.catalog.entity.Product;
import com.aalvarenga.sales.feature.FeatureToggleService;
import com.aalvarenga.sales.pricing.PricingService;
import com.aalvarenga.sales.purchase.dto.CartResponse;
import com.aalvarenga.sales.purchase.dto.StartCartRequest;
import com.aalvarenga.sales.purchase.dto.UpdateCartSelectionRequest;
import com.aalvarenga.sales.purchase.entity.Purchase;
import com.aalvarenga.sales.purchase.repository.PurchaseRepository;
import com.aalvarenga.sales.shared.error.BusinessException;
import com.aalvarenga.sales.subscriber.entity.Subscriber;
import com.aalvarenga.sales.support.Fixtures;

/**
 * Regras do carrinho, com o {@link PricingService} REAL (é puro, não precisa de mock).
 *
 * <p>Modo LENIENT: os stubs do {@code @BeforeEach} (parâmetro de parcela mínima,
 * toggle) servem a quase todos os testes, mas não a todos - no modo estrito
 * padrão isso geraria {@code UnnecessaryStubbingException} (lição aprendida no billing).</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T15:00:00Z");

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private CatalogService catalogService;
    @Mock
    private FeatureToggleService featureToggleService;

    private CartService service;
    private final Subscriber subscriber = Fixtures.subscriber();

    @BeforeEach
    void setUp() {
        service = new CartService(purchaseRepository, catalogService, new PricingService(), featureToggleService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(catalogService.minInstallmentValue()).thenReturn(new BigDecimal("5.00"));
        when(featureToggleService.isEnabled(anyString())).thenReturn(false);
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void start_createsCartWithFirstPlanAndQuote() {
        Product product = Fixtures.streaming1();
        when(catalogService.findActive("TS1")).thenReturn(product);
        when(purchaseRepository.findBySubscriberIdAndStatus(10L, PurchaseStatus.CART)).thenReturn(List.of());

        CartResponse response = service.start(subscriber, new StartCartRequest("TS1", null));

        assertThat(response.status()).isEqualTo(PurchaseStatus.CART);
        assertThat(response.lastStep()).isEqualTo(PurchaseStep.PRODUCT_SELECTED);
        assertThat(response.selection().recurrenceFrequency()).isEqualTo(RecurrenceFrequency.MONTH);
        assertThat(response.quote().chargedValue()).isEqualByComparingTo("28.96");
        assertThat(response.protocol()).startsWith("SLS-");
        assertThat(response.checkoutEnabled()).isFalse();
        verify(purchaseRepository).save(any(Purchase.class));
    }

    @Test
    void start_withRequestedPlan_marksPlanSelected() {
        when(catalogService.findActive("TS1")).thenReturn(Fixtures.streaming1());
        when(purchaseRepository.findBySubscriberIdAndStatus(10L, PurchaseStatus.CART)).thenReturn(List.of());

        CartResponse response = service.start(subscriber, new StartCartRequest("TS1", RecurrenceFrequency.ANNUAL));

        assertThat(response.lastStep()).isEqualTo(PurchaseStep.PLAN_SELECTED);
        assertThat(response.quote().chargedValue()).isEqualByComparingTo("270.51");
    }

    @Test
    void start_sameProductWithOpenCart_reusesIt() {
        Product product = Fixtures.streaming1();
        Purchase open = openCart(product);
        when(catalogService.findActive("TS1")).thenReturn(product);
        when(purchaseRepository.findBySubscriberIdAndStatus(10L, PurchaseStatus.CART)).thenReturn(List.of(open));

        CartResponse response = service.start(subscriber, new StartCartRequest("TS1", null));

        assertThat(response.id()).isEqualTo(open.getPublicId());
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void start_otherProductWithOpenCart_abandonsThePreviousOne() {
        Purchase previous = openCart(Fixtures.streaming1());
        when(catalogService.findActive("VR1")).thenReturn(Fixtures.cap());
        when(purchaseRepository.findBySubscriberIdAndStatus(10L, PurchaseStatus.CART)).thenReturn(List.of(previous));

        service.start(subscriber, new StartCartRequest("VR1", null));

        assertThat(previous.getStatus()).isEqualTo(PurchaseStatus.ABANDONED);
        assertThat(previous.getAbandonReason()).isEqualTo(AbandonReason.REPLACED_BY_NEW_CART);
        assertThat(previous.getAbandonedAt()).isEqualTo(NOW);
    }

    @Test
    void start_exclusiveProductAlreadyPurchased_isConflict() {
        when(catalogService.findActive("TS1")).thenReturn(Fixtures.streaming1());
        when(purchaseRepository.existsBySubscriberIdAndProductIdAndStatusIn(eq(10L), anyLong(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.start(subscriber, new StartCartRequest("TS1", null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PRODUCT_ALREADY_PURCHASED");
    }

    @Test
    void updateSelection_withoutInstallments_defaultsToTheMaximumAllowed() {
        Purchase cart = openCart(Fixtures.streaming1());
        stubOwned(cart);

        CartResponse response = service.updateSelection(subscriber, cart.getPublicId(),
                new UpdateCartSelectionRequest(RecurrenceFrequency.ANNUAL, PaymentMethod.CREDIT, null, null, null));

        assertThat(response.selection().installments()).isEqualTo(12);
        assertThat(response.lastStep()).isEqualTo(PurchaseStep.PAYMENT_METHOD_SELECTED);
        assertThat(cart.getTaxes()).hasSize(2);
        assertThat(cart.getLastActivityAt()).isEqualTo(NOW);
    }

    @Test
    void updateSelection_methodNotAcceptedByPlan_isBadRequest() {
        Purchase cart = openCart(Fixtures.streaming1());
        stubOwned(cart);

        // WALLET só é aceito no plano anual.
        assertThatThrownBy(() -> service.updateSelection(subscriber, cart.getPublicId(),
                new UpdateCartSelectionRequest(RecurrenceFrequency.MONTH, PaymentMethod.WALLET, WalletProvider.PICPAY, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PAYMENT_METHOD_NOT_ACCEPTED");
    }

    @Test
    void updateSelection_installmentsAboveMinimumValueRule_isBadRequest() {
        Purchase cart = openCart(Fixtures.cap());
        stubOwned(cart);

        // 12,09 com parcela mínima de 5,00 -> no máximo 2x.
        assertThatThrownBy(() -> service.updateSelection(subscriber, cart.getPublicId(),
                new UpdateCartSelectionRequest(RecurrenceFrequency.ONESHOT, PaymentMethod.CREDIT, null, CardBrand.VISA, 3)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "INSTALLMENTS_NOT_ALLOWED");
    }

    @Test
    void updateSelection_walletProviderWithoutWalletMethod_isBadRequest() {
        Purchase cart = openCart(Fixtures.streaming1());
        stubOwned(cart);

        assertThatThrownBy(() -> service.updateSelection(subscriber, cart.getPublicId(),
                new UpdateCartSelectionRequest(RecurrenceFrequency.ANNUAL, PaymentMethod.PIX, WalletProvider.PICPAY, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "WALLET_PROVIDER_NOT_ALLOWED");
    }

    @Test
    void updateSelection_cardBrandWithoutCardMethod_isBadRequest() {
        Purchase cart = openCart(Fixtures.streaming1());
        stubOwned(cart);

        assertThatThrownBy(() -> service.updateSelection(subscriber, cart.getPublicId(),
                new UpdateCartSelectionRequest(RecurrenceFrequency.MONTH, PaymentMethod.PIX, null, CardBrand.VISA, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "CARD_BRAND_NOT_ALLOWED");
    }

    @Test
    void updateSelection_onAbandonedCart_isConflict() {
        Purchase cart = openCart(Fixtures.streaming1());
        cart.abandon(AbandonReason.INACTIVITY, NOW);
        stubOwned(cart);

        assertThatThrownBy(() -> service.updateSelection(subscriber, cart.getPublicId(),
                new UpdateCartSelectionRequest(RecurrenceFrequency.MONTH, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "CART_NOT_OPEN");
    }

    @Test
    void get_ofAnotherSubscriber_isNotFound() {
        Purchase cart = openCart(Fixtures.streaming1());
        when(purchaseRepository.findByPublicIdAndSubscriberId(cart.getPublicId(), 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(subscriber, cart.getPublicId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PURCHASE_NOT_FOUND");
    }

    @Test
    void stepFor_reflectsHowFarTheSubscriberWent() {
        assertThat(CartService.stepFor(null, selection(null, null, null))).isEqualTo(PurchaseStep.PLAN_SELECTED);
        assertThat(CartService.stepFor(PaymentMethod.PIX, selection(PaymentMethod.PIX, null, null)))
                .isEqualTo(PurchaseStep.PAYMENT_DETAILS_FILLED);
        assertThat(CartService.stepFor(PaymentMethod.WALLET, selection(PaymentMethod.WALLET, null, null)))
                .isEqualTo(PurchaseStep.PAYMENT_METHOD_SELECTED);
        assertThat(CartService.stepFor(PaymentMethod.WALLET, selection(PaymentMethod.WALLET, WalletProvider.MERCADO_PAGO, null)))
                .isEqualTo(PurchaseStep.PAYMENT_DETAILS_FILLED);
        assertThat(CartService.stepFor(PaymentMethod.CREDIT, selection(PaymentMethod.CREDIT, null, CardBrand.ELO)))
                .isEqualTo(PurchaseStep.PAYMENT_DETAILS_FILLED);
    }

    private static UpdateCartSelectionRequest selection(PaymentMethod method, WalletProvider wallet, CardBrand brand) {
        return new UpdateCartSelectionRequest(RecurrenceFrequency.MONTH, method, wallet, brand, null);
    }

    private Purchase openCart(Product product) {
        Purchase purchase = Purchase.start(subscriber, product, product.defaultPlan(), NOW.minusSeconds(60));
        purchase.applyQuote(service.quoteFor(product.defaultPlan(), null));
        return purchase;
    }

    private void stubOwned(Purchase cart) {
        when(purchaseRepository.findByPublicIdAndSubscriberId(cart.getPublicId(), 10L)).thenReturn(Optional.of(cart));
    }
}
