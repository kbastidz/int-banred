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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Configuración de trama personalizada de infoReversal por empresa para reverso automático V2.
 *
 * Ejemplo en application.yml:
 *
 * auto-reversal-config:
 *   companies:
 *     - code: "2259"
 *       reversal-types: "A,M"
 *       info-reversal-fields: "codAgencia,codLocalidad,billerAuthorizationCode,metodo_pago"
 *     - code: "8453"
 *       reversal-types: "A"
 *       info-reversal-fields: "codAgencia,billerAuthorizationCode,metodo_pago"
 */
@Configuration
@Component
@Data
@ConfigurationProperties(prefix = "auto-reversal-config")
@AllArgsConstructor
@NoArgsConstructor
public class AutoReversalConfiguration {

    private List<CompanyReversalConfig> companies = Collections.emptyList();

    /**
     * Busca la configuración de una empresa por su código.
     * Devuelve empty si la empresa no está configurada.
     */
    public Optional<CompanyReversalConfig> findByCompanyCode(String companyCode) {
        if (StringUtils.isEmpty(companyCode) || companies == null) return Optional.empty();
        return companies.stream()
                .filter(c -> companyCode.equals(c.getCode()))
                .findFirst();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompanyReversalConfig {

        /** Código de empresa (codigoEmpresa del servicio) */
        private String code;

        /**
         * Tipos de reverso que usarán trama personalizada para esta empresa.
         * Valores permitidos: A, M. Separados por coma. Ej: "A,M"
         * Si no se define, se mantiene compatibilidad usando solo "A".
         */
        private String reversalTypes;

        /**
         * Campos que irán en infoReversal, en el orden definido aquí.
         * Separados por coma. Ej: "codAgencia,codLocalidad,billerAuthorizationCode,metodo_pago"
         */
        private String infoReversalFields;

        /**
         * Devuelve la lista de campos en el orden configurado.
         */
        public List<String> getInfoReversalFieldList() {
            if (StringUtils.isEmpty(infoReversalFields)) return Collections.emptyList();
            return Stream.of(infoReversalFields.split(","))
                    .map(String::trim)
                    .toList();
        }

        /**
         * Determina si la empresa tiene habilitado el tipo de reverso para trama personalizada.
         */
        public boolean supportsReversalType(String reversalType) {
            if (StringUtils.isEmpty(reversalType)) return false;

            String configuredTypes = StringUtils.isEmpty(reversalTypes) ? "A" : reversalTypes;
            Set<String> allowedTypes = Stream.of(configuredTypes.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .map(String::toUpperCase)
                    .collect(java.util.stream.Collectors.toSet());

            return allowedTypes.contains(reversalType.trim().toUpperCase());
        }
    }
}
