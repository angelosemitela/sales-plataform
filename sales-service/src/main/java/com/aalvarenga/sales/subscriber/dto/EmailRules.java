package com.aalvarenga.sales.subscriber.dto;

/** Regra única de formato de e-mail (o front usa a mesma expressão). */
public final class EmailRules {

    public static final String REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$";

    private EmailRules() {
    }
}
