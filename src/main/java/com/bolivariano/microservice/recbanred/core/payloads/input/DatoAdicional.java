package com.bolivariano.microservice.recbanred.core.payloads.input;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DatoAdicional implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String codigo;
    private String etiqueta;
    private Boolean editable;
    private String formato;
    private ListasSeleccion listasSeleccion;
    private String longitud;
    private String mascara;
    private String regexp;
    private String tipo;
    private String valor;
    private Boolean visible;

    public DatoAdicional(String codigo, String valor) {
        this.codigo = codigo;
        this.valor = valor;
    }
}
