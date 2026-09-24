package com.aalvarenga.sales.integration.orchestration;

import java.time.Clock;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.aalvarenga.sales.feature.FeatureToggleService;
import com.aalvarenga.sales.feature.Features;
import com.aalvarenga.sales.integration.orchestration.entity.OutboxEvent;
import com.aalvarenga.sales.integration.orchestration.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * "Relay" do outbox: lê eventos PENDING e publica no orquestrador. Só roda com o
 * toggle {@code ORCHESTRATION_ENABLED} ligado (desligado nesta entrega).
 *
 * <p>Garantia "at-least-once": se a publicação funcionar mas a atualização para
 * PUBLISHED falhar, o evento é reenviado - por isso o consumidor precisa ser
 * idempotente (o billing já é, pelo {@code protocol}). Estudo futuro: Debezium
 * (CDC) lê o outbox direto do log do Postgres, sem polling.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayJob {

    private final OutboxEventRepository outboxEventRepository;
    private final OrchestratorPublisher orchestratorPublisher;
    private final FeatureToggleService featureToggleService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${sales.outbox.relay-interval:PT10S}")
    @Transactional
    public void relay() {
        if (!featureToggleService.isEnabled(Features.ORCHESTRATION_ENABLED)) {
            return;
        }
        for (OutboxEvent event : outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.PENDING)) {
            event.setAttempts(event.getAttempts() + 1);
            try {
                orchestratorPublisher.publish(event);
                event.setStatus(OutboxEvent.PUBLISHED);
                event.setPublishedAt(clock.instant());
            } catch (RuntimeException ex) {
                log.warn("Failed to publish outbox event {} (attempt {}): {}", event.getId(), event.getAttempts(), ex.getMessage());
            }
        }
    }
}
