package com.bolivariano.microservice.recbanred.core.enums.banred;

/**
 * Tipo de operacion de la trama Q0/Q1, equivalente a {@code TipoFlujo}
 * pero usado exclusivamente por el motor de tokens de trama fija (V3).
 */
public enum TipoOperacionToken {

    INQUIRY,
    PAYMENT,
    REVERSAL
}
