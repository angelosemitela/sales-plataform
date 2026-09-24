package com.aalvarenga.sales.shared.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Erro de regra de negócio, com status HTTP e um CÓDIGO estável.
 *
 * <p>O {@code code} (ex: {@code EMAIL_ALREADY_REGISTERED}) é o que o front usa
 * para decidir o que fazer - nunca o texto da mensagem, que pode mudar ou ser
 * traduzido.</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BusinessException badRequest(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static BusinessException notFound(String code, String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, code, message);
    }

    public static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    public static BusinessException unauthorized(String code, String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static BusinessException unavailable(String code, String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
}
