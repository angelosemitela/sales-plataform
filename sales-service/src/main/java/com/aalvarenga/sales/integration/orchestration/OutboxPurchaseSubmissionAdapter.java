package com.aalvarenga.sales.integration.orchestration;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.aalvarenga.sales.integration.billing.BillingPurchaseDraft;
import com.aalvarenga.sales.integration.orchestration.entity.OutboxEvent;
import com.aalvarenga.sales.integration.orchestration.repository.OutboxEventRepository;
import com.aalvarenga.sales.purchase.entity.Purchase;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adapter "Transactional Outbox": em vez de chamar o orquestrador direto (e correr
 * o risco de a compra ser gravada e a mensagem se perder, ou o contrário), grava o
 * evento numa tabela NA MESMA TRANSAÇÃO da compra. O {@link OutboxRelayJob} publica
 * depois, com retentativas.
 *
 * <p>{@code Propagation.MANDATORY}: falha se alguém chamar fora de uma transação -
 * garante, em código, a premissa do padrão.</p>
 */
@Component
@RequiredArgsConstructor
public class OutboxPurchaseSubmissionAdapter implements PurchaseSubmissionPort {

    public static final String EVENT_TYPE = "PurchaseSubmitted";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void submit(Purchase purchase, BillingPurchaseDraft draft) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("Purchase");
        event.setAggregateId(purchase.getPublicId().toString());
        event.setEventType(EVENT_TYPE);
        event.setPayload(jsonMapper.writeValueAsString(draft));
        event.setCreatedAt(clock.instant());
        outboxEventRepository.save(event);
    }
}
