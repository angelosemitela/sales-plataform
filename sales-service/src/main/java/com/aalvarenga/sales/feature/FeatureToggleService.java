package com.aalvarenga.sales.feature;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aalvarenga.sales.feature.entity.ConfigParameter;
import com.aalvarenga.sales.feature.entity.FeatureToggle;
import com.aalvarenga.sales.feature.repository.ConfigParameterRepository;
import com.aalvarenga.sales.feature.repository.FeatureToggleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Leitura de toggles e parâmetros, sempre "fail-safe" (mesmo racional do billing):
 * toggle inexistente = DESLIGADO; parâmetro inexistente ou inválido = valor padrão
 * informado pelo chamador. Configuração quebrada nunca vira erro 500.
 *
 * <p>Estudo futuro: bibliotecas dedicadas (Unleash, FF4j, OpenFeature) trazem
 * painel, liberação gradual (ex: 10% dos usuários) e segmentação.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureToggleService {

    private final FeatureToggleRepository toggleRepository;
    private final ConfigParameterRepository parameterRepository;

    @Transactional(readOnly = true)
    public boolean isEnabled(String name) {
        return toggleRepository.findById(name).map(FeatureToggle::isEnabled).orElseGet(() -> {
            log.warn("Feature toggle '{}' not found - treated as disabled", name);
            return false;
        });
    }

    @Transactional(readOnly = true)
    public BigDecimal decimalParameter(String name, BigDecimal defaultValue) {
        return parameterRepository.findById(name).map(ConfigParameter::getValue).map(value -> {
            try {
                return new BigDecimal(value.trim());
            } catch (NumberFormatException ex) {
                log.warn("Parameter '{}' has a non numeric value '{}' - using default {}", name, value, defaultValue);
                return defaultValue;
            }
        }).orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public long longParameter(String name, long defaultValue) {
        return decimalParameter(name, BigDecimal.valueOf(defaultValue)).longValue();
    }
}
