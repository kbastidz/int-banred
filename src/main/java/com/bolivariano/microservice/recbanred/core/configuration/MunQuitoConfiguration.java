package com.bolivariano.microservice.recbanred.core.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para la integración V4 con el Municipio del Distrito
 * Metropolitano de Quito mediante trama ISO 8583 sobre TCP directo.
 *
 * Ejemplo application.yml:
 *
 * mun-quito:
 *   host: ""
 *   port: 0
 *   companies: "2300"
 *   acquirer-aba: "00073"
 *   product-code: "0010031004"
 *   header-prefix: "ISO01400007"
 *   header-institution: "3"
 */
@Configuration
@ConfigurationProperties(prefix = "mun-quito")
@Data
public class MunQuitoConfiguration {

    /** IP o hostname del servidor TCP del Municipio de Quito (dejar vacío hasta asignación). */
    private String host;

    /** Puerto TCP (dejar 0 hasta asignación). */
    private int port;

    /** BillCompanyCodes separados por coma que mapean a este integrador. */
    private String companies;

    /** Código ABA de la institución adquirente (Bit 32). Extraído de tramas: "06597777". */
    private String acquirerAba;

    /** Código de producto Municipio para Bit 52. Fijo según ficha: "0010031004". */
    private String productCode;

    /** Prefijo fijo del header de la trama: "ISO01400007". */
    private String headerPrefix;

    /** Dígito final del header (identificador de institución en header). Valor: "3". */
    private String headerInstitution;
}
