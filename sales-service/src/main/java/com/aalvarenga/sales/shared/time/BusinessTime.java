package com.aalvarenga.sales.shared.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Fuso de negócio único do serviço (mesmo do billing: America/Sao_Paulo).
 *
 * <p>O banco guarda instantes em UTC ({@code TIMESTAMPTZ}); o fuso só importa
 * quando uma regra depende do DIA do calendário (dia da recorrência, data da
 * 1ª cobrança do trial).</p>
 */
public final class BusinessTime {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private BusinessTime() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }
}
