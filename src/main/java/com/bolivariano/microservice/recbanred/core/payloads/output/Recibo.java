package com.bolivariano.microservice.recbanred.core.payloads.output;

import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class Recibo implements Serializable {

    protected String comprobante;//!important
    protected String concepto;
    protected String cuota;
    protected transient DatosAdicionales datosAdicionales;
    protected String dato1;//!important
    protected String dato2;//!important
    protected String dividendo;
    protected String fecha;//!important
    protected String formaPago;
    protected String identificador;
    protected String impuesto;
    protected BigDecimal interes;
    protected BigDecimal interesesPagados;
    protected BigDecimal interesesPendientes;
    protected String numeroPredial;
    protected BigDecimal pago;
    protected String referencia;
    protected String secuencia;
    protected String tipoProceso;
    protected BigDecimal totalAPagar; //!important
    protected BigDecimal valor;
}
