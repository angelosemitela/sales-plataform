package com.aalvarenga.sales.reference.dto;

import java.util.List;

/**
 * @param allowedCountries países aceitos quando {@code allCountries = false};
 *                         vazio quando qualquer país é aceito (o front usa a lista completa)
 */
public record DocumentTypeResponse(String code, String description, boolean countrySelectable,
                                   boolean allCountries, List<String> allowedCountries) {
}
