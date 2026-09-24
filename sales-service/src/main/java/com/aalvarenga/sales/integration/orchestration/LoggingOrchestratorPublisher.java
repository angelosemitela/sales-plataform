package com.aalvarenga.sales.integration.orchestration;

import org.springframework.stereotype.Component;

import com.aalvarenga.sales.integration.orchestration.entity.OutboxEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementação provisória: só registra em log. O orquestrador será implementado
 * em outro momento; quando existir, entra um adapter real (ex: KafkaTemplate) no
 * lugar deste, sem tocar no restante do código.
 */
@Slf4j
@Component
public class LoggingOrchestratorPublisher implements OrchestratorPublisher {

    @Override
    public void publish(OutboxEvent event) {
        log.info("[orchestrator-stub] {} for {} {}", event.getEventType(), event.getAggregateType(), event.getAggregateId());
    }
}
