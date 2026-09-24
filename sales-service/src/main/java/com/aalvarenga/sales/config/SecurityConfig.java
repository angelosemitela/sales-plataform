package com.aalvarenga.sales.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Segurança da API: login próprio (e-mail + senha) emitindo um JWT assinado com HMAC.
 *
 * <p><b>Como funciona:</b> {@code POST /api/v1/auth/login} confere a senha (BCrypt)
 * e devolve um token. Nas chamadas seguintes o front manda
 * {@code Authorization: Bearer <token>} e o <i>resource server</i> do Spring
 * Security valida assinatura e expiração - sem sessão no servidor (STATELESS), o
 * que permite subir N instâncias do serviço atrás de um balanceador sem
 * "sticky session".</p>
 *
 * <p><b>CSRF desligado</b> de propósito: CSRF explora cookies enviados
 * automaticamente pelo navegador; como o token vai num header que o JavaScript
 * precisa colocar explicitamente, esse ataque não se aplica.</p>
 *
 * <p><b>Evolução para estudo:</b> trocar o login próprio por um provedor de
 * identidade (Keycloak, Auth0, Cognito) via OIDC - o serviço deixaria de guardar
 * senhas e passaria a só validar tokens emitidos por ele (bastaria trocar o
 * {@link JwtDecoder} por um que lê as chaves públicas do provedor). Outra
 * variação: padrão BFF com cookie httpOnly, que tira o token do alcance do
 * JavaScript.</p>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rotas públicas: cadastro, login e dados necessários ANTES do login.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/subscribers").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/catalog/**",
                                "/api/v1/reference/**",
                                "/api/v1/features",
                                "/api/v1/addresses/zip-codes/**").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public SecretKey jwtSecretKey(SalesProperties properties) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("sales.jwt.secret must have at least 32 bytes for HS256");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /**
     * Encoder "delegante": grava o hash com o prefixo do algoritmo ({@code {bcrypt}$2a$...}).
     * Se um dia migrarmos para Argon2, senhas antigas continuam válidas e as novas já
     * nascem no algoritmo novo - sem migração em massa.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SalesProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
