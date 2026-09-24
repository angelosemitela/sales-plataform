package com.aalvarenga.sales.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.aalvarenga.sales.auth.dto.AuthResponse;
import com.aalvarenga.sales.auth.dto.LoginRequest;
import com.aalvarenga.sales.shared.error.BusinessException;
import com.aalvarenga.sales.subscriber.entity.Subscriber;
import com.aalvarenga.sales.subscriber.repository.SubscriberRepository;
import com.aalvarenga.sales.support.Fixtures;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private SubscriberRepository subscriberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService service;
    private Subscriber subscriber;

    @BeforeEach
    void setUp() {
        service = new AuthService(subscriberRepository, passwordEncoder, jwtTokenService);
        subscriber = Fixtures.subscriber();
        subscriber.setPasswordHash("{bcrypt}real");
        when(passwordEncoder.encode(anyString())).thenReturn("{bcrypt}dummy");
    }

    @Test
    void login_correctPassword_issuesToken() {
        AuthResponse expected = new AuthResponse("jwt", "Bearer", 7200, null);
        when(subscriberRepository.findByEmailIgnoreCase("angelo@example.com")).thenReturn(Optional.of(subscriber));
        when(passwordEncoder.matches("secret123", "{bcrypt}real")).thenReturn(true);
        when(jwtTokenService.issue(subscriber)).thenReturn(expected);

        assertThat(service.login(new LoginRequest(" angelo@example.com ", "secret123"))).isSameAs(expected);
    }

    @Test
    void login_wrongPassword_isUnauthorized() {
        when(subscriberRepository.findByEmailIgnoreCase("angelo@example.com")).thenReturn(Optional.of(subscriber));
        when(passwordEncoder.matches("wrong", "{bcrypt}real")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("angelo@example.com", "wrong")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
    }

    @Test
    void login_unknownEmail_sameErrorAndStillChecksAPassword() {
        when(subscriberRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@example.com", "x")))
                .hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
        // Proteção contra timing attack: o BCrypt roda mesmo sem usuário.
        verify(passwordEncoder).matches(eq("x"), eq("{bcrypt}dummy"));
        verify(jwtTokenService, never()).issue(subscriber);
    }
}
