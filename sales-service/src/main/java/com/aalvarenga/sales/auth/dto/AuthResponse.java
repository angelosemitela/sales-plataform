package com.aalvarenga.sales.auth.dto;

import com.aalvarenga.sales.subscriber.dto.SubscriberResponse;

/** @param expiresIn validade do token em segundos */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, SubscriberResponse subscriber) {
}
