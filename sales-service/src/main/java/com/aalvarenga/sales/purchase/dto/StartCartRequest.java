package com.aalvarenga.sales.purchase.dto;

import jakarta.validation.constraints.NotBlank;

import com.aalvarenga.sales.catalog.RecurrenceFrequency;

/** @param recurrenceFrequency plano desejado; se nulo, usa o primeiro plano do produto */
public record StartCartRequest(@NotBlank String productCode, RecurrenceFrequency recurrenceFrequency) {
}
