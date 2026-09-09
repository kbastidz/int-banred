package com.bolivariano.microservice.recbanred.core.payloads.output;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Recibos implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<Recibo> recibo;

}
