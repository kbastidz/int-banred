package com.bolivariano.microservice.recbanred.core.payloads.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListaSeleccion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String codigo;
    private String etiqueta;
}
