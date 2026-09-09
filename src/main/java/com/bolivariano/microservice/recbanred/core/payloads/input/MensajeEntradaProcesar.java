package com.bolivariano.microservice.recbanred.core.payloads.input;

import com.bolivariano.microservice.recbanred.core.enums.TipoFlujo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
public class MensajeEntradaProcesar implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private TipoFlujo tipoFlujo;
    private MensajeEntradaEjecutarPago mensajeEntradaEjecutarPago;
    private MensajeEntradaConsultarDeuda mensajeEntradaConsultarDeuda;
    private MensajeEntradaEjecutarReverso mensajeEntradaEjecutarReverso;
}
