package com.bolivariano.microservice.recbanred.core.enums.banred;

/**
 * Identifica el biller (empresa/institucion) que utiliza el formato de
 * trama fija Q0/Q1 (V3), documentado en los Anexos "Estructura Estandar
 * Token Trama Generica Q0/Q1".
 *
 * NOTA: MUNGYE (Municipio de Guayaquil) agrupa 4 sub-servicios independientes,
 * ver {@link SubServicioMungye}.
 */
public enum TipoBiller {

    CNEL,
    MEER,
    MUNGYE
}
