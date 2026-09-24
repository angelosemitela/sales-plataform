package com.aalvarenga.sales.subscriber.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentInput(
        @NotBlank @Size(max = 20) String type,
        @Size(max = 120) String description,
        @NotBlank @Size(max = 40) String value,
        @NotBlank @Size(min = 2, max = 2) String country) {
}
