package com.aalvarenga.sales.address.viacep;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.aalvarenga.sales.address.ZipCodeLookupPort;
import com.aalvarenga.sales.address.ZipCodeUnavailableException;
import com.aalvarenga.sales.address.dto.ZipCodeAddress;
import com.aalvarenga.sales.config.SalesProperties;

/**
 * ADAPTER da porta de CEP usando a API pública ViaCEP, via {@link RestClient}
 * (cliente HTTP síncrono moderno do Spring, sucessor do RestTemplate).
 *
 * <p>Timeouts curtos e explícitos: uma API de terceiro lenta não pode travar a
 * thread da nossa requisição. Estudo futuro: Resilience4j (circuit breaker + retry)
 * e cache dos CEPs já consultados.</p>
 */
@Component
public class ViaCepZipCodeAdapter implements ZipCodeLookupPort {

    private final RestClient restClient;

    /** Com dois construtores, o {@code @Autowired} indica qual o Spring deve usar. */
    @Autowired
    public ViaCepZipCodeAdapter(SalesProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.zipCode().timeout());
        requestFactory.setReadTimeout(properties.zipCode().timeout());
        this.restClient = RestClient.builder()
                .baseUrl(properties.zipCode().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /** Usado nos testes, com um RestClient apontando para um servidor simulado. */
    ViaCepZipCodeAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<ZipCodeAddress> lookup(String zipCodeDigits) {
        ViaCepResponse response;
        try {
            response = restClient.get()
                    .uri("/{cep}/json/", zipCodeDigits)
                    .retrieve()
                    .body(ViaCepResponse.class);
        } catch (RestClientException ex) {
            throw new ZipCodeUnavailableException("ViaCEP unavailable", ex);
        }
        if (response == null || response.notFound()) {
            return Optional.empty();
        }
        return Optional.of(new ZipCodeAddress(
                zipCodeDigits,
                blankToNull(response.logradouro()),
                blankToNull(response.bairro()),
                blankToNull(response.localidade()),
                blankToNull(response.uf()),
                "BR"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
