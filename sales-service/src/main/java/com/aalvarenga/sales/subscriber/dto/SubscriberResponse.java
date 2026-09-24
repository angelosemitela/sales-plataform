package com.aalvarenga.sales.subscriber.dto;

import java.util.UUID;

public record SubscriberResponse(UUID externalId, String name, String email, boolean authorizedFallback) {
}
