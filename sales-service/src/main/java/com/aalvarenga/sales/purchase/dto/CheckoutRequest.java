package com.aalvarenga.sales.purchase.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import com.aalvarenga.sales.purchase.CardBrand;

/**
 * Finalização da compra (botão "Comprar" - desligado nesta entrega).
 *
 * <p>Para CREDIT/DEBIT, o front primeiro envia o cartão DIRETO ao gateway de
 * pagamento (tokenização) e só repassa aqui o resultado: emissor, se é múltiplo,
 * bandeira, últimos 4 dígitos, validade e os tokens. Assim o número do cartão
 * nunca passa pelo nosso backend (reduz o escopo de PCI-DSS). PIX e WALLET não
 * precisam deste bloco.</p>
 */
public record CheckoutRequest(@Valid Tokenization tokenization) {

    public record Tokenization(
            @NotBlank String issuer,
            Boolean isMultiple,
            CardBrand brand,
            @Pattern(regexp = "\\d{4}") String lastFour,
            @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}") String expiration,
            @Valid List<Token> tokens) {
    }

    public record Token(@NotBlank String name, @NotBlank String id, @NotBlank String gateway, Instant expirationAt) {
    }
}
