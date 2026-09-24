package com.aalvarenga.sales.catalog;

/**
 * Periodicidade do plano. {@code ONESHOT} = venda avulsa (no billing vira
 * {@code type=ONESHOT}); {@code MONTH}/{@code ANNUAL} viram {@code type=RECURRENCE}.
 */
public enum RecurrenceFrequency {
    ONESHOT,
    MONTH,
    ANNUAL;

    public boolean isRecurring() {
        return this != ONESHOT;
    }
}
