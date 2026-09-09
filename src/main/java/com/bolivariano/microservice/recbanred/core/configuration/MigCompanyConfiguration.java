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

@Configuration
@Component
@Data
@ConfigurationProperties(prefix = "mig-company")
@AllArgsConstructor
@NoArgsConstructor
public class MigCompanyConfiguration {

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
