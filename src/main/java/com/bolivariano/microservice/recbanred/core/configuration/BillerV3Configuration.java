package com.bolivariano.microservice.recbanred.core.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Configuracion de la V3 (tramas de posicion fija Q0/Q1). Define:
 *  - Que codigos de empresa (BillCompanyCode) corresponden a cada biller.
 *  - Los valores estaticos por institucion (ABA/institucion financiera y
 *    operador) que se insertan en el Q0, ya que son fijos por comercio y
 *    no vienen en el request de entrada (igual que "inputDataInqPayment"
 *    en BanredConfiguration para V1).
 *
 * Ejemplo application.yml:
 *
 * company:
 *   transformation:
 *     versions:
 *       v3:
 *         companies-cnel: "2254"
 *         companies-meer: "2261"
 *         companies-mungye: "2245"
 *         url: "https://.../BanredV3Endpoint"
 *         format-date: "ddMMyyyy"
 *         format-time: "HHmmss"
 * biller:
 *   v3:
 *     cnel:
 *       cod-institucion-financiera: "03033"
 *       cod-operador: "BBL001"
 *     meer:
 *       cod-institucion-financiera: "00000"
 *       cod-operador: "0000000000"
 *     mungye:
 *       aba-institucion: "00597777"
 *       cod-operador: "000000998"
 */
@Configuration
@Component
@Data
@ConfigurationProperties(prefix = "biller.v3")
@AllArgsConstructor
@NoArgsConstructor
public class BillerV3Configuration {

    private String companiesCnel;
    private String companiesMeer;
    private String companiesMungye;

    private InstitucionConfig cnel;
    private InstitucionConfig meer;
    private InstitucionConfig mungye;

    public List<String> getCompaniesCnel() {
        return toList(companiesCnel);
    }

    public List<String> getCompaniesMeer() {
        return toList(companiesMeer);
    }

    public List<String> getCompaniesMungye() {
        return toList(companiesMungye);
    }

    private List<String> toList(String csv) {
        if (StringUtils.isEmpty(csv))
            return Collections.emptyList();
        return Stream.of(csv.split(",")).map(String::trim).toList();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InstitucionConfig {
        /** Codigo institucion financiera (CNEL/MEER) o ABA (MUNGYE). */
        private String codInstitucionFinanciera;
        /** Codigo de operador asignado por el biller. */
        private String codOperador;
    }
}
