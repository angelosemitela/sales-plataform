package com.aalvarenga.sales.purchase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CardExpirationTest {

    private static final YearMonth NOW = YearMonth.of(2026, 9);

    @ParameterizedTest
    @CsvSource({
        "09/26, true",   // vence no mês corrente: ainda vale
        "03/31, true",
        "08/26, false",  // mês passado
        "12/24, false",  // exemplo do enunciado: já expirado
        "13/29, false",  // exemplo do enunciado: mês 13 não existe
        "00/30, false",
        "3/31, false",   // formato MM/YY obrigatório
        "03/2031, false"
    })
    void validatesFormatAndExpiration(String expiration, boolean expected) {
        assertThat(CardExpiration.isValid(expiration, NOW)).isEqualTo(expected);
    }
}
