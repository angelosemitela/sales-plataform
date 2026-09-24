package com.aalvarenga.sales.integration.orchestration;

import com.aalvarenga.sales.integration.orchestration.entity.OutboxEvent;

/**
 * Envio efetivo do evento ao orquestrador (Kafka, RabbitMQ, Temporal, Camunda...).
 * Porta separada do outbox: trocar o broker não mexe em nada do fluxo de compra.
 */
public interface OrchestratorPublisher {

    void publish(OutboxEvent event);
}
