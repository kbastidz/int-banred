package com.bolivariano.microservice.recbanred.core.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "services")
public class ServicesConfiguration {

    private Truststore truststore;

    @Data
    public static class Truststore {
        private String path;
        private String password;
        private String type;
    }

}
