package com.aalvarenga.sales.purchase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.aalvarenga.sales.feature.FeatureToggleService;
import com.aalvarenga.sales.feature.Features;
import com.aalvarenga.sales.purchase.repository.PurchaseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Job que marca como ABANDONED os carrinhos sem atividade há mais de
 * {@code CART_ABANDON_MINUTES} (parâmetro no banco, padrão 30).
 *
 * <p>Com várias instâncias do serviço, todas rodariam o job - aqui isso é seguro
 * porque o UPDATE é idempotente. Para jobs que NÃO podem rodar em paralelo, o
 * padrão de mercado é o ShedLock (trava distribuída no próprio banco) ou um
 * agendador externo (Kubernetes CronJob, Quartz em cluster).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartAbandonmentJob {

    static final long DEFAULT_ABANDON_MINUTES = 30;

    private final PurchaseRepository purchaseRepository;
    private final FeatureToggleService featureToggleService;
    private final Clock clock;

    /**
     * Métodos {@code @Scheduled} devem ser {@code void}. O {@code @Transactional} fica
     * AQUI (chamado pelo agendador através do proxy do Spring); a chamada interna a
     * {@link #abandonInactiveCarts()} já roda dentro desta transação.
     */
    @Scheduled(fixedDelayString = "${sales.cart.abandon-check-interval:PT1M}")
    @Transactional
    public void run() {
        abandonInactiveCarts();
    }

    /** @return quantidade de carrinhos marcados como abandonados */
    public int abandonInactiveCarts() {
        long minutes = featureToggleService.longParameter(Features.CART_ABANDON_MINUTES, DEFAULT_ABANDON_MINUTES);
        Instant now = clock.instant();
        Instant cutoff = now.minus(Duration.ofMinutes(minutes));
        int abandoned = purchaseRepository.abandonInactiveCarts(cutoff, now,
                PurchaseStatus.CART, PurchaseStatus.ABANDONED, AbandonReason.INACTIVITY);
        if (abandoned > 0) {
            log.info("{} cart(s) marked as ABANDONED (inactive since before {})", abandoned, cutoff);
        }
        return abandoned;
    }
}
