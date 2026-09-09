package com.bolivariano.microservice.recbanred.core.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = false)
public class BadFormatException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String code;
    private final String message;

    public BadFormatException(String message, Throwable cause, String codigo) {
        super(message, cause);
        this.code = codigo;
        this.message = message;
    }
}
