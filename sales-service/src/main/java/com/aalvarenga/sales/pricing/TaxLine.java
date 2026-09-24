package com.aalvarenga.sales.pricing;

import java.math.BigDecimal;

/** Taxa calculada: alíquota usada + valor em reais (já arredondado em centavos). */
public record TaxLine(String name, BigDecimal rate, BigDecimal value) {
}
