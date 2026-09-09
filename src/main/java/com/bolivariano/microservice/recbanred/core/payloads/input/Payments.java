package com.bolivariano.microservice.recbanred.core.payloads.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payments {

    private String titulo;
    private String anio;
    private String rubro;
}
