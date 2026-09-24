package com.aalvarenga.sales.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações tipadas do serviço (prefixo {@code sales} no application.yml).
 *
 * <p>Usar um {@code record} com {@code @ConfigurationProperties} em vez de vários
 * {@code @Value} espalhados deixa todas as configurações num lugar só, validadas
 * na subida e fáceis de mockar em testes.</p>
 */
@ConfigurationProperties(prefix = "sales")
public record SalesProperties(Jwt jwt, Cors cors, ZipCode zipCode) {

    /**
     * @param secret     chave HMAC (mínimo 32 bytes para HS256) - em produção,
     *                   vem de variável de ambiente/cofre de segredos, nunca do repositório
     * @param expiration validade do token de acesso
     * @param issuer     emissor gravado no claim "iss"
     */
    public record Jwt(String secret, Duration expiration, String issuer) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /**
     * @param baseUrl URL base da API de CEP (ViaCEP)
     * @param timeout tempo máximo de conexão/leitura - API de terceiro nunca pode
     *                "pendurar" a nossa requisição indefinidamente
     */
    public record ZipCode(String baseUrl, Duration timeout) {
    }
}
