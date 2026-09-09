package com.bolivariano.microservice.recbanred.core.payloads.output;

import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@XmlRootElement(name = "salidaEjecutarPago")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeSalidaEjecutarPago {

    private Boolean banderaOffline;
    private String codigoError;
    private DatosAdicionales datosAdicionales;
    private String fechaDebito;
    private String fechaPago;
    private String mensajeUsuario;
    private String mensajeSistema;
    private BigDecimal montoTotal;
    private String referencia;
}
