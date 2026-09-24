package com.aalvarenga.sales.subscriber.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "11144477735", "39053344705"})
    void validCpfs(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "52998224724",     // 2º dígito verificador errado
        "52998224715",     // 1º dígito verificador errado
        "11111111111",     // todos iguais (passa na conta, mas é inválido)
        "529.982.247-25",  // só dígitos são aceitos
        "5299822472",      // 10 dígitos
        "529982247255"     // 12 dígitos
    })
    void invalidCpfs(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isFalse();
    }
}
