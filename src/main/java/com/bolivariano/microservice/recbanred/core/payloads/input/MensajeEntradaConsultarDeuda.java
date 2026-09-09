package com.bolivariano.microservice.recbanred.core.payloads.input;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
public class MensajeEntradaConsultarDeuda implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String canal;
    private String depuracion;
    private String fecha;
    private String oficina;
    private String secuencial;
    private Servicio servicio;
    private String transaccion;
    private String usuario;

}
