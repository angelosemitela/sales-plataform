package com.aalvarenga.sales.pricing;

import java.math.BigDecimal;

/** Alíquota percentual de uma taxa (ex: CBS 8.8 = 8,8%). */
public record TaxRate(String name, BigDecimal rate) {
}
