package com.aalvarenga.sales.integration.billing;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Rascunho do payload de {@code POST /api/v1/purchases} do billing-registration-service,
 * com os MESMOS nomes de campo do contrato de lá.
 *
 * <p>É um RASCUNHO porque dois campos de {@code billing[]} só existem depois da
 * captura do pagamento no gateway: {@code transactionId} e {@code provider}. O
 * orquestrador (passo seguinte do fluxo) captura o pagamento, completa esses dois
 * campos e então chama o billing.</p>
 *
 * <p>{@code @JsonProperty} explícito nos campos {@code isXxx}: algumas versões do
 * Jackson removem o prefixo "is" de propriedades booleanas; aqui o nome precisa
 * bater exatamente com o billing.</p>
 */
public record BillingPurchaseDraft(
        String channel,
        String transactionDt,
        String protocol,
        List<Account> account,
        List<Product> product,
        List<Payment> payment,
        List<Billing> billing) {

    public record Account(
            String id,
            String name,
            String externalId,
            String email,
            @JsonProperty("isAuthorizedFallback") Boolean isAuthorizedFallback,
            List<Document> document,
            List<Address> address,
            List<Phone> phone) {
    }

    public record Document(String type, String description, String value, String country) {
    }

    public record Address(String type, String description, String addressName, String number,
                          String complement, String zipCode, String country) {
    }

    public record Phone(String number) {
    }

    public record Product(
            String codeId,
            String name,
            String type,
            @JsonProperty("isExpiriationService") Boolean isExpiriationService,
            String recurrenceFrequency,
            BigDecimal productValue,
            BigDecimal discountValue,
            Integer discountCycles,
            String currency,
            @JsonProperty("isTrial") Boolean isTrial,
            Integer trialDays) {
    }

    public record Payment(
            String method,
            String issuer,
            String cardNumber,
            String expiration,
            @JsonProperty("isMultiple") Boolean isMultiple,
            @JsonProperty("isDefault") Boolean isDefault,
            Integer installments,
            String brand,
            List<Token> token) {
    }

    public record Token(String name, String id, String gateway, String expirationDt) {
    }

    public record Billing(
            String codeId,
            BigDecimal productValue,
            BigDecimal discountValue,
            BigDecimal taxValue,
            BigDecimal chargedValue,
            String currency,
            String transactionId,
            Integer installments,
            String provider,
            String paymentMethod,
            List<Tax> tax) {
    }

    public record Tax(String name, BigDecimal value) {
    }
}
