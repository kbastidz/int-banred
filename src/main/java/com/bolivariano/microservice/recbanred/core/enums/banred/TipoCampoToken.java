package com.bolivariano.microservice.recbanred.core.enums.banred;

/**
 * Tipo de dato de un campo dentro de una especificacion de token Q0/Q1 de
 * posicion fija. Define como se rellena (build) y como se interpreta (parse).
 */
public enum TipoCampoToken {

    /** Numerico, padleft con ceros a la longitud declarada. */
    NUMERICO,

    /** Texto, padright con espacios a la longitud declarada. */
    ALFA,

    /**
     * Monto: se redondea a 2 decimales, se elimina el separador decimal
     * (los ultimos 2 digitos quedan como parte decimal) y luego se aplica
     * padleft con ceros a la longitud declarada. Ej: 35.36 con longitud 11
     * -> "00000003536".
     */
    MONTO,

    /**
     * Campo de longitud variable: precedido por un sub-campo de 2 digitos
     * que indica la longitud del valor que sigue. Ej: "23JOHANA LINDA..."
     */
    VARIABLE,

    /**
     * Grupo repetitivo de sub-campos (ej. pares AÑO/VALOR de MUNGYE-Predios).
     * Ver {@code CampoToken#getSubCampos()} y {@code CampoToken#getRepeticiones()}.
     */
    GRUPO_REPETIDO
}
