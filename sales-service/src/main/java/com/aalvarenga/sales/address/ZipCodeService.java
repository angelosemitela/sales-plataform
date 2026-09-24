package com.aalvarenga.sales.address;

import org.springframework.stereotype.Service;

import com.aalvarenga.sales.address.dto.ZipCodeAddress;
import com.aalvarenga.sales.shared.error.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipCodeService {

    private final ZipCodeLookupPort zipCodeLookupPort;

    public ZipCodeAddress lookup(String zipCode) {
        String digits = zipCode == null ? "" : zipCode.replaceAll("\\D", "");
        if (!digits.matches("\\d{8}")) {
            throw BusinessException.badRequest("INVALID_ZIP_CODE", "Zip code must have 8 digits");
        }
        try {
            return zipCodeLookupPort.lookup(digits)
                    .orElseThrow(() -> BusinessException.notFound("ZIP_CODE_NOT_FOUND", "Zip code " + digits + " not found"));
        } catch (ZipCodeUnavailableException ex) {
            log.warn("Zip code provider unavailable: {}", ex.getMessage());
            // 503: o front mostra aviso e libera o preenchimento manual do endereço.
            throw BusinessException.unavailable("ZIP_CODE_SERVICE_UNAVAILABLE", "Zip code service unavailable, fill in manually");
        }
    }
}
