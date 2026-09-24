package com.aalvarenga.sales.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.aalvarenga.sales.auth.dto.AuthResponse;
import com.aalvarenga.sales.config.SalesProperties;
import com.aalvarenga.sales.subscriber.dto.SubscriberResponse;
import com.aalvarenga.sales.subscriber.entity.Subscriber;

import lombok.RequiredArgsConstructor;

/** Emite o token de acesso (JWT assinado com HS256). */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final SalesProperties properties;
    private final Clock clock;

    public AuthResponse issue(Subscriber subscriber) {
        Instant now = clock.instant();
        long expiresIn = properties.jwt().expiration().toSeconds();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .subject(subscriber.getExternalId().toString())
                .claim("name", subscriber.getName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", expiresIn, toResponse(subscriber));
    }

    public static SubscriberResponse toResponse(Subscriber subscriber) {
        return new SubscriberResponse(subscriber.getExternalId(), subscriber.getName(),
                subscriber.getEmail(), subscriber.isAuthorizedFallback());
    }
}
