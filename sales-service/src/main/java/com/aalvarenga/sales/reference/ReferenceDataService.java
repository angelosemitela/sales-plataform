package com.aalvarenga.sales.reference;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aalvarenga.sales.reference.dto.CountryResponse;
import com.aalvarenga.sales.reference.dto.DocumentTypeResponse;
import com.aalvarenga.sales.reference.entity.Country;
import com.aalvarenga.sales.reference.repository.CountryRepository;
import com.aalvarenga.sales.reference.repository.DocumentTypeRepository;

import lombok.RequiredArgsConstructor;

/**
 * Dados de referência para as telas (países, tipos de documento).
 *
 * <p>Estudo futuro: são dados que quase nunca mudam - candidatos perfeitos para
 * cache ({@code spring-boot-starter-cache} + Caffeine, ou Redis quando houver várias
 * instâncias) e para cabeçalhos HTTP de cache ({@code Cache-Control}/ETag).</p>
 */
@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final CountryRepository countryRepository;
    private final DocumentTypeRepository documentTypeRepository;

    @Transactional(readOnly = true)
    public List<CountryResponse> countries() {
        return countryRepository.findAllByOrderByNameAsc().stream()
                .map(c -> new CountryResponse(c.getCode(), c.getName(), c.getDialCode()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentTypeResponse> documentTypes() {
        return documentTypeRepository.findAllByOrderBySortOrderAsc().stream()
                .map(t -> new DocumentTypeResponse(
                        t.getCode(),
                        t.getDescription(),
                        t.isCountrySelectable(),
                        t.isAllCountries(),
                        t.getAllowedCountries().stream().map(Country::getCode).sorted().toList()))
                .toList();
    }
}
