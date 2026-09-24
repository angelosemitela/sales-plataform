package com.aalvarenga.sales.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aalvarenga.sales.feature.entity.ConfigParameter;
import com.aalvarenga.sales.feature.entity.FeatureToggle;
import com.aalvarenga.sales.feature.repository.ConfigParameterRepository;
import com.aalvarenga.sales.feature.repository.FeatureToggleRepository;

@ExtendWith(MockitoExtension.class)
class FeatureToggleServiceTest {

    @Mock
    private FeatureToggleRepository toggleRepository;
    @Mock
    private ConfigParameterRepository parameterRepository;
    @InjectMocks
    private FeatureToggleService service;

    @Test
    void unknownToggle_isTreatedAsDisabled() {
        when(toggleRepository.findById("X")).thenReturn(Optional.empty());
        assertThat(service.isEnabled("X")).isFalse();
    }

    @Test
    void existingToggle_returnsItsValue() {
        when(toggleRepository.findById("CHECKOUT_ENABLED")).thenReturn(Optional.of(new FeatureToggle("CHECKOUT_ENABLED", true)));
        assertThat(service.isEnabled("CHECKOUT_ENABLED")).isTrue();
    }

    @Test
    void nonNumericParameter_fallsBackToDefault() {
        when(parameterRepository.findById("MIN")).thenReturn(Optional.of(new ConfigParameter("MIN", "abc")));
        assertThat(service.decimalParameter("MIN", new BigDecimal("5.00"))).isEqualByComparingTo("5.00");
    }

    @Test
    void numericParameter_isParsed() {
        when(parameterRepository.findById("CART_ABANDON_MINUTES")).thenReturn(Optional.of(new ConfigParameter("CART_ABANDON_MINUTES", " 45 ")));
        assertThat(service.longParameter("CART_ABANDON_MINUTES", 30)).isEqualTo(45);
    }
}
