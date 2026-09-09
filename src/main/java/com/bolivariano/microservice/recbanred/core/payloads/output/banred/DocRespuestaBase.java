package com.bolivariano.microservice.recbanred.core.payloads.output.banred;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocRespuestaBase {

    private String tipoReparto;
    private String direccionServicio;
    private String numDocIdentif; //DocTupe (concepto e iodentificador)
    private String fechaLecturaInicial;
    private String fechaLecturaFinal;
    private String fechaEmision;
    private String facturasPendientes;
    private String fechaVencimiento;
    private String consumoServicio;
    private String numeroFactura;
    private String totalMes;
    private String deudaAnterior;
}
