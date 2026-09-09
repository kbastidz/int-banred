package com.bolivariano.microservice.recbanred.core.payloads.input;


import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatosAdicionales implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private List<DatoAdicional> datoAdicional;

}
