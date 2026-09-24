package com.aalvarenga.sales.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expõe um {@link Clock} como bean.
 *
 * <p>Todo código que precisa de "agora" recebe o Clock injetado em vez de chamar
 * {@code Instant.now()} direto. Nos testes, basta um {@code Clock.fixed(...)} para
 * tornar datas determinísticas (ex: "cobrança em 02/10/2026" sempre bate).</p>
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
