package com.aalvarenga.sales.address;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aalvarenga.sales.address.dto.ZipCodeAddress;

import lombok.RequiredArgsConstructor;

/**
 * Proxy de consulta de CEP. O front chama NOSSA API, e não a ViaCEP direto:
 * trocamos de provedor sem publicar front novo, controlamos timeout/cache e o
 * navegador não depende de CORS de terceiros.
 */
@RestController
@RequestMapping("/api/v1/addresses/zip-codes")
@RequiredArgsConstructor
public class ZipCodeController {

    private final ZipCodeService zipCodeService;

    @GetMapping("/{zipCode}")
    public ZipCodeAddress lookup(@PathVariable String zipCode) {
        return zipCodeService.lookup(zipCode);
    }
}
