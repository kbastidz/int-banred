package com.bolivariano.microservice.recbanred.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;
import org.slf4j.MDC;

public class LoggingContext {

    private LoggingContext() {}

    public static final String UID = "uid";
    public static final String COMPANY_CODE = "codEmpresa";
    public static final String SEQUENTIAL_BANK = "secuencial";
    public static final String FLUX_TYPE = "tipoFlujo";
    public static final String CHANNEL = "canal";
    public static final String SERVICE_TYPE = "codTipoServicio";
    public static final String IDENTIFIER = "identificador";

    private static final ObjectMapper LOG_OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            //  No revienta con beans vacíos (muy común en proxies)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            //Fechas en ISO-8601, no timestamps
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            //No escribe zonas raras
            .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false)
            //Evita excepciones por propiedades problemáticas
            .configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
            //Manejo correcto de Java Time
            .registerModule(new JavaTimeModule());

    public static void init(String uid, String tipoFlujo, String codigoEmpresa, String secuencialBanco) {
        MDC.put(UID, uid);
        MDC.put(COMPANY_CODE, codigoEmpresa);
        MDC.put(FLUX_TYPE, tipoFlujo);
        MDC.put(SEQUENTIAL_BANK, secuencialBanco);
    }

    public static void putRequestContext(String canal, String codTipoServicio, String identificador) {
        MDC.put(CHANNEL, StringUtils.defaultString(canal));
        MDC.put(SERVICE_TYPE, StringUtils.defaultString(codTipoServicio));
        MDC.put(IDENTIFIER, StringUtils.defaultString(identificador));
    }

    public static void clear() {
        MDC.clear();
    }

    public static String sanitize(String input) {
        if (input == null) return null;
        return Encode.forJava(input)
                .replace("\\\"", "\"")
                .replace("\\n", "")
                .replace("\\r", "")
                .replace("\\t", "")
                .replaceAll("[\\x00-\\x1F]", "");
    }

    public static String writeJsonLogAndSanitize(Object input) {
        try {
            return sanitize(LOG_OBJECT_MAPPER.writeValueAsString(input));
        } catch (JsonProcessingException e) {
            return StringUtils.EMPTY;
        }
    }}
