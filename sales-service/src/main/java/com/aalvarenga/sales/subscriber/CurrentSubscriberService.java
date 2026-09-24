package com.aalvarenga.sales.subscriber;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.aalvarenga.sales.shared.error.BusinessException;
import com.aalvarenga.sales.subscriber.entity.Subscriber;
import com.aalvarenga.sales.subscriber.repository.SubscriberRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolve o assinante logado a partir do token. O claim {@code sub} carrega o
 * externalId (UUID) - nunca o ID sequencial do banco.
 */
@Service
@RequiredArgsConstructor
public class CurrentSubscriberService {

    private final SubscriberRepository subscriberRepository;

    public Subscriber require(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw BusinessException.unauthorized("INVALID_TOKEN", "Token without subject");
        }
        UUID externalId;
        try {
            externalId = UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.unauthorized("INVALID_TOKEN", "Invalid token subject");
        }
        return subscriberRepository.findByExternalId(externalId)
                .orElseThrow(() -> BusinessException.unauthorized("INVALID_TOKEN", "Subscriber not found"));
    }
}
