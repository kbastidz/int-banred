package com.bolivariano.microservice.recbanred.core.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String codigo;
    private final String razon;

    public NotFoundException(String mensajeUsuario, Throwable causal, String codigo, String razon) {
        super(mensajeUsuario, causal);
        this.codigo = codigo;
        this.razon = razon;
    }
}