package com.aalvarenga.sales.subscriber.validation;

/**
 * Validação LOCAL de CPF (sem consulta à Receita): formato + dígitos verificadores.
 *
 * <p>Algoritmo (módulo 11):</p>
 * <ol>
 *   <li>11 dígitos, e não todos iguais (111.111.111-11 passa na conta, mas é inválido);</li>
 *   <li>1º DV: soma dos 9 primeiros dígitos x pesos 10..2; resto = soma % 11;
 *       DV = resto &lt; 2 ? 0 : 11 - resto;</li>
 *   <li>2º DV: soma dos 10 primeiros dígitos (já com o 1º DV) x pesos 11..2, mesma regra.</li>
 * </ol>
 * O front roda o MESMO algoritmo para dar feedback imediato; o back repete porque
 * o front nunca é confiável (qualquer um chama a API direto).
 */
public final class CpfValidator {

    private CpfValidator() {
    }

    public static boolean isValid(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(cpf, 9) == cpf.charAt(9) - '0'
                && checkDigit(cpf, 10) == cpf.charAt(10) - '0';
    }

    private static int checkDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
