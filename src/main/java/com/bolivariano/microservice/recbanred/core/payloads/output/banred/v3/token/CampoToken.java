package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.token;

import com.bolivariano.microservice.recbanred.core.enums.banred.TipoCampoToken;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Descriptor de un campo dentro de una {@link EspecificacionToken}. Es el
 * equivalente programatico de una fila de las fichas tecnicas
 * "Anexo A.0 - Estructura Estandar Token Trama Generica Q0/Q1" (columnas
 * VALUE / LENGTH / DESCRIPTION / COMMENTS).
 */
@Getter
public class CampoToken {

    /** Nombre logico del campo (ej. "TOTAL_PENDIENTE_PAGO"). Usado como key al parsear/construir. */
    private final String nombre;

    /** Longitud fija del campo. Ignorada para VARIABLE y GRUPO_REPETIDO. */
    private final int longitud;

    private final TipoCampoToken tipo;

    /** Valor por defecto a usar en build() si no viene en el mapa de valores. */
    private final String valorPorDefecto;

    /** Solo para GRUPO_REPETIDO: los sub-campos que forman una repeticion. */
    private final List<CampoToken> subCampos;

    /** Solo para GRUPO_REPETIDO: cantidad maxima de repeticiones esperadas (ej. 12 años). */
    private final int repeticiones;

    private CampoToken(String nombre, int longitud, TipoCampoToken tipo, String valorPorDefecto,
                        List<CampoToken> subCampos, int repeticiones) {
        this.nombre = nombre;
        this.longitud = longitud;
        this.tipo = tipo;
        this.valorPorDefecto = valorPorDefecto;
        this.subCampos = subCampos == null ? Collections.emptyList() : subCampos;
        this.repeticiones = repeticiones;
    }

    public static CampoToken numerico(String nombre, int longitud) {
        return new CampoToken(nombre, longitud, TipoCampoToken.NUMERICO, "0", null, 0);
    }

    public static CampoToken numerico(String nombre, int longitud, String valorPorDefecto) {
        return new CampoToken(nombre, longitud, TipoCampoToken.NUMERICO, valorPorDefecto, null, 0);
    }

    public static CampoToken alfa(String nombre, int longitud) {
        return new CampoToken(nombre, longitud, TipoCampoToken.ALFA, "", null, 0);
    }

    public static CampoToken alfa(String nombre, int longitud, String valorPorDefecto) {
        return new CampoToken(nombre, longitud, TipoCampoToken.ALFA, valorPorDefecto, null, 0);
    }

    public static CampoToken monto(String nombre, int longitud) {
        return new CampoToken(nombre, longitud, TipoCampoToken.MONTO, "0", null, 0);
    }

    public static CampoToken variable(String nombre) {
        return new CampoToken(nombre, -1, TipoCampoToken.VARIABLE, "", null, 0);
    }

    public static CampoToken grupoRepetido(String nombre, int repeticiones, CampoToken... subCampos) {
        return new CampoToken(nombre, -1, TipoCampoToken.GRUPO_REPETIDO, null, List.of(subCampos), repeticiones);
    }

    /** Ancho total (en caracteres) de una repeticion del grupo. Solo valido para GRUPO_REPETIDO. */
    public int anchoGrupo() {
        return subCampos.stream().mapToInt(CampoToken::getLongitud).sum();
    }
}
