package com.bolivariano.microservice.recbanred.core.payloads.output.banred;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocReversoRespuesta {
    private String secuencialAut;
    private String retencion;
    private String base;
    private String numDocIdentif;
    private String fechaEmision;
    private String numeroFactura;
    private String valorPagado;
    private String numDocIdentif2;
    private String nombreCliente2;
}

