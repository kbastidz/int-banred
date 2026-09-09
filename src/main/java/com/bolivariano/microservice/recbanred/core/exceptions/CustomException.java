package com.bolivariano.microservice.recbanred.core.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = false)
public class CustomException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String code;
    private final String message;

    public CustomException(String message, Throwable cause, String code) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}

