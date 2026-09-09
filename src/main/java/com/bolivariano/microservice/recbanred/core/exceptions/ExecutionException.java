package com.bolivariano.microservice.recbanred.core.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ExecutionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final String message;
    private final String reason;

    public ExecutionException(String message, Throwable cause, String code, String reason) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.reason = reason;
    }

    public ExecutionException(String code, String message, String reason) {
        this.code = code;
        this.message = message;
        this.reason = reason;
    }
}

