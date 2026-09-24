package com.aalvarenga.sales.purchase;

import java.time.YearMonth;

/**
 * Validade de cartão no formato MM/YY (anos 2000-2099), mesma regra do billing:
 * mês entre 01 e 12 e não vencido (um cartão "12/26" vale até o fim de dez/2026).
 */
public final class CardExpiration {

    private CardExpiration() {
    }

    public static boolean isValid(String expiration, YearMonth currentMonth) {
        if (expiration == null || !expiration.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return false;
        }
        int month = Integer.parseInt(expiration.substring(0, 2));
        int year = 2000 + Integer.parseInt(expiration.substring(3));
        return !YearMonth.of(year, month).isBefore(currentMonth);
    }
}
