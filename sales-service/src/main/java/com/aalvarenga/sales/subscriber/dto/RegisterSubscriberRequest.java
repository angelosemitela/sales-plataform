package com.aalvarenga.sales.subscriber.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro simplificado do assinante. Campos baseados no {@code account} do billing
 * (name, email, isAuthorizedFallback, document, address, phone) + a senha de login.
 *
 * <p>Bean Validation cuida do FORMATO de cada campo; regras que dependem do banco
 * (e-mail já usado, país aceito pelo documento, CPF válido) ficam no service.</p>
 */
public record RegisterSubscriberRequest(
        @NotBlank @Size(max = 120) String name,
        // @Email sozinho aceita "a@b" (sem domínio de topo); o regexp exige "algo@dominio.tld".
        @NotBlank @Size(max = 254) @Email(regexp = EmailRules.REGEX, message = "must be a valid e-mail address") String email,
        // Máximo de 72: o BCrypt ignora o que passar de 72 bytes.
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Boolean isAuthorizedFallback,
        @NotNull @Valid DocumentInput document,
        @NotNull @Valid AddressInput address,
        @NotNull @Valid PhoneInput phone) {
}
