package com.aalvarenga.sales.feature;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** Toggles que o FRONT precisa conhecer (ex: habilitar o botão "Comprar"). */
@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureToggleService featureToggleService;

    public record FeaturesResponse(boolean checkoutEnabled) {
    }

    @GetMapping
    public FeaturesResponse features() {
        return new FeaturesResponse(featureToggleService.isEnabled(Features.CHECKOUT_ENABLED));
    }
}
