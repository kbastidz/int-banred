package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.token;

import com.bolivariano.microservice.recbanred.core.enums.banred.TipoCampoToken;
import lombok.Getter;

import java.util.List;

/**
 * Especificacion completa y ordenada de campos de un token Q0 o Q1, para una
 * combinacion (biller, sub-servicio si aplica, operacion). Equivale a una
 * "hoja" de las fichas tecnicas de Anexo A.0.
 */
@Getter
public class EspecificacionToken {

    /** Etiqueta descriptiva, solo para logs/errores (ej. "CNEL - INQUIRY - Q1"). */
    private final String etiqueta;

    private final List<CampoToken> campos;

    /**
     * Si es true, el token es en si mismo un arreglo de bloques repetidos
     * separados por pipe ("|"), como el Q1 de MASS INQUIRY de MEER. En ese
     * caso, {@code campos} describe UN bloque, y el parseo/armado se hace
     * bloque a bloque (ver {@code TokenFieldEngine#parseArregloPipe}).
     */
    private final boolean arregloPipe;

    public EspecificacionToken(String etiqueta, List<CampoToken> campos) {
        this(etiqueta, campos, false);
    }

    public EspecificacionToken(String etiqueta, List<CampoToken> campos, boolean arregloPipe) {
        this.etiqueta = etiqueta;
        this.campos = campos;
        this.arregloPipe = arregloPipe;
    }

    /**
     * Longitud fija total de los campos que NO son VARIABLE ni GRUPO_REPETIDO.
     * Se usa para: (a) validar specs sin campos dinamicos, y (b) calcular
     * cuanto espacio hay que reservar para los campos que van DESPUES de un
     * GRUPO_REPETIDO al parsear (ver TokenFieldEngine).
     */
    public int longitudFijaDesde(int indiceInicial) {
        int total = 0;
        for (int i = indiceInicial; i < campos.size(); i++) {
            CampoToken campo = campos.get(i);
            if (campo.getTipo() == TipoCampoToken.VARIABLE || campo.getTipo() == TipoCampoToken.GRUPO_REPETIDO)
                continue; // no se contabilizan, se resuelven en runtime
            total += campo.getLongitud();
        }
        return total;
    }
}
