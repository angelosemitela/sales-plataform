package com.aalvarenga.sales.subscriber.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** @param country país do DDI (ex: BR -> +55); number só dígitos, até 20 posições. */
public record PhoneInput(
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotBlank @Pattern(regexp = "^[0-9]{1,20}$", message = "must contain only digits (max 20)") String number) {
}
