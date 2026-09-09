package com.bolivariano.microservice.recbanred.core.payloads.output;

import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MensajeSalidaConsultarDeuda {

    private String codigoError;
    private DatosAdicionales datosAdicionales;
    private String fechaVencimiento;
    private String formaPago;
    private String formaPagoRecibos;
    private String identificadorDeuda;
    private BigDecimal limiteMontoMaximo;
    private BigDecimal limiteMontoMinimo;
    private String mensajeUsuario;
    private String mensajeSistema;
    private BigDecimal montoMinimo;
    private BigDecimal montoTotal;
    private String nombreCliente;
    private Recibos recibos;
    private String textoAyuda;
}
