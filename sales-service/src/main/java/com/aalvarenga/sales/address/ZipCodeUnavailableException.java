package com.aalvarenga.sales.address;

/** O provedor de CEP não respondeu (timeout, erro 5xx, rede). */
public class ZipCodeUnavailableException extends RuntimeException {

    public ZipCodeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
