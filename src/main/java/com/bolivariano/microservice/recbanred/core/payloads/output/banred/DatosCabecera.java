package com.bolivariano.microservice.recbanred.core.payloads.output.banred;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatosCabecera {
    private String codAgencia;
    private String codLocalidad;
    private String codigoInstitucion;
    private String codigoOperador;
    private String fecha;
    private String hora;

}
