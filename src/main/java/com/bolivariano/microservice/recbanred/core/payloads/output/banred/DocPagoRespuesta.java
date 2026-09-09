package com.bolivariano.microservice.recbanred.core.payloads.output.banred;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocPagoRespuesta extends DocRespuestaBase {
    private String codigoAutorizacion;
    private String totalPendientePago;
    private String interesAcumulado;
    private String infraccion;
    private String transferencia;
    private String secuenciaAut;
    private String retencion;
    private String base;
    private String nombreCliente;
    private String valorPagado;
    private String numDocIdentif2;
    private String nombreCliente2;
}