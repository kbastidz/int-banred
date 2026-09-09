package com.bolivariano.microservice.recbanred.core.payloads.input;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class Servicio implements Serializable {

    @Serial
    private static final long serialVersionUID = 2405172041950251807L;

    @XmlElement(required = true)
    private String codTipoServicio;
    @XmlElement(required = true)
    private String codigoConvenio;
    @XmlElement(required = true)
    private String codigoEmpresa;
    @XmlElement(required = true)
    private String codigoTipoBanca;
    private String codigoTipoIdentificador;
    private DatosAdicionales datosAdicionales;
    private String identificador;


}