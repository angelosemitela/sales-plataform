package com.aalvarenga.sales.address.dto;

/**
 * Endereço devolvido pela consulta de CEP. Campos que a API não souber
 * preencher (ex: CEP geral de cidade pequena, sem logradouro) vêm {@code null} -
 * o front libera esses campos para digitação.
 */
public record ZipCodeAddress(String zipCode, String addressName, String district, String city, String state, String country) {
}
