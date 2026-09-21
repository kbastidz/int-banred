package com.bolivariano.microservice.recbanred.core.configuration;

import com.bolivariano.microservice.recbanred.core.enums.TipoVersion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@Configuration
@Component
@Data
@ConfigurationProperties(prefix = "company.transformation")
@AllArgsConstructor
@NoArgsConstructor
public class CompanyConfiguration {

    private String validEnterpriseV1;

    private Versions versions;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Versions {
        private VersionConfig v1;
        private VersionConfig v2;
        private VersionConfig v3;

        public VersionConfig getVersionForType(TipoVersion versionType) {
            if (versionType.equals(TipoVersion.V1))
                return v1;
            if (versionType.equals(TipoVersion.V2))
                return v2;
            if (versionType.equals(TipoVersion.V3))
                return v3;
            return null;
        }
    }

   @Data
   @AllArgsConstructor
   @NoArgsConstructor
   public static class VersionConfig {

       private String companies;
       private String formatDate;
       private String formatTime;
       private String url;

       public List<String> getCompaniesList() {
           if (StringUtils.isEmpty(companies)) {
               return Collections.emptyList();
           }
           return Stream.of(companies.split(","))
                   .map(String::trim)
                   .toList();
       }
   }

   @Data
   @AllArgsConstructor
   @NoArgsConstructor
   public static class MigCompany {
       private String companyMigrates;

       public List<String> getCompanyMigratesList() {
           if (StringUtils.isEmpty(companyMigrates)) {
               return Collections.emptyList();
           }
           return Stream.of(companyMigrates.split(","))
                   .map(String::trim)
                   .toList();
       }
   }
}
