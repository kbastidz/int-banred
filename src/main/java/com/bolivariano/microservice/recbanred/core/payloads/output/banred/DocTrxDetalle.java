package com.bolivariano.microservice.recbanred.core.payloads.output.banred;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocTrxDetalle extends DocRespuestaBase {
    private String numeroContrato; //comprobante
    private String nombreCliente;
    private String totalPendientePago; //total a Pagar
    private String interesAcumulado;
    private String infraccion;
    private String transferencia;
    private String secuenciaAut;
    private String retencion;
    private String baseImponible;
}