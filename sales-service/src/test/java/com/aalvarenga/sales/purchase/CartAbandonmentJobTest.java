package com.aalvarenga.sales.purchase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aalvarenga.sales.feature.FeatureToggleService;
import com.aalvarenga.sales.feature.Features;
import com.aalvarenga.sales.purchase.repository.PurchaseRepository;

@ExtendWith(MockitoExtension.class)
class CartAbandonmentJobTest {

    private static final Instant NOW = Instant.parse("2026-09-24T15:00:00Z");

    @Mock
    private PurchaseRepository purchaseRepository;
    @Mock
    private FeatureToggleService featureToggleService;

    @Test
    void abandonsCartsInactiveForLongerThanTheConfiguredMinutes() {
        CartAbandonmentJob job = new CartAbandonmentJob(purchaseRepository, featureToggleService, Clock.fixed(NOW, ZoneOffset.UTC));
        when(featureToggleService.longParameter(Features.CART_ABANDON_MINUTES, 30)).thenReturn(45L);
        when(purchaseRepository.abandonInactiveCarts(NOW.minusSeconds(45 * 60), NOW,
                PurchaseStatus.CART, PurchaseStatus.ABANDONED, AbandonReason.INACTIVITY)).thenReturn(3);

        assertThat(job.abandonInactiveCarts()).isEqualTo(3);
    }
}
