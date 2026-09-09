package com.bolivariano.microservice.recbanred.core.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "compressor")
@Data
public class CompressorConfiguration {

    private String baseChar;
    private int number;
    private BigInteger divisor;
    private int padLength;
    private String separator;
    private char filler;

    private Map<String, String> pid;
    private Map<String, String> date;

    public String getPidPrefix(String value) {
        return pid.entrySet().stream()
                .filter(e -> e.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    public String getFullPrefix(String alias) {
        return pid.get(alias);
    }

}
