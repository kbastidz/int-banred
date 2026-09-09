package com.bolivariano.microservice.recbanred.core.payloads.input;

import com.bolivariano.microservice.recbanred.core.payloads.output.Recibos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MensajeEntradaEjecutarReverso implements Serializable {

    @Serial
    private static final long serialVersionUID = 2405172041950251807L;

    private String canal;
    private String cuenta;
    private DatosAdicionales datosAdicionales;
    private String depuracion;
    private String esquemaFirma;
    private String fechaPago;
    private String moneda;
    private String nombreCliente;
    private String oficina;
    private Recibos recibos;
    private String usuario;
    private String transaccion;
    private String tipoCuenta;
    private BigDecimal valorPago;

    private Servicio servicio;
    private String secuencial;
    private BigDecimal valorComision;
}
