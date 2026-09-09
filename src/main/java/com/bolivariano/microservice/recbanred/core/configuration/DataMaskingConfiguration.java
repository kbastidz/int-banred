package com.bolivariano.microservice.recbanred.core.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Configuration
@Component
@Data
@ConfigurationProperties(prefix = "data-masking")
@AllArgsConstructor
@NoArgsConstructor
public class DataMaskingConfiguration {

    /** Activa o desactiva sanitización, validación y enmascaramiento del flujo de consulta */
    private boolean enabled;

    /** Códigos de campos adicionales (DatoAdicional.codigo) que deben enmascararse */
    private List<String> sensitiveFieldCodes = Collections.emptyList();

    /**
     * Longitud máxima permitida para cualquier valor de campo genérico.
     * Previene ataques de desbordamiento de buffer / DoS.
     * Default: 500
     */
    private int maxFieldLength = 500;

    /**
     * Longitud máxima permitida para campos clave de cabecera (secuencial, transaccion, usuario).
     * Default: 100
     */
    private int maxKeyFieldLength = 500;
}
