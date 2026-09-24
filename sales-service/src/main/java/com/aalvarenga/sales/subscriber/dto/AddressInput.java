package com.aalvarenga.sales.subscriber.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.aalvarenga.sales.subscriber.AddressType;

/**
 * @param number só dígitos e OPCIONAL (endereço sem número)
 */
public record AddressInput(
        @NotNull AddressType type,
        @NotBlank @Size(max = 120) String description,
        @NotNull Boolean international,
        @NotBlank @Size(max = 20) String zipCode,
        @NotBlank @Size(max = 200) String addressName,
        @Pattern(regexp = "^[0-9]{0,10}$", message = "must contain only digits") String number,
        @Size(max = 120) String complement,
        @Size(max = 120) String district,
        @Size(max = 120) String city,
        @Size(max = 60) String state,
        @NotBlank @Size(min = 2, max = 2) String country) {
}
