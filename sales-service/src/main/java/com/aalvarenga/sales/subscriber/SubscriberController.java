package com.aalvarenga.sales.subscriber;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aalvarenga.sales.auth.JwtTokenService;
import com.aalvarenga.sales.auth.dto.AuthResponse;
import com.aalvarenga.sales.subscriber.dto.RegisterSubscriberRequest;
import com.aalvarenga.sales.subscriber.dto.SubscriberResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/subscribers")
@RequiredArgsConstructor
public class SubscriberController {

    private final SubscriberRegistrationService registrationService;
    private final CurrentSubscriberService currentSubscriberService;
    private final JwtTokenService jwtTokenService;

    /** Cadastra e já devolve o token: o assinante sai do cadastro logado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterSubscriberRequest request) {
        return jwtTokenService.issue(registrationService.register(request));
    }

    @GetMapping("/me")
    public SubscriberResponse me(@AuthenticationPrincipal Jwt jwt) {
        return JwtTokenService.toResponse(currentSubscriberService.require(jwt));
    }
}
