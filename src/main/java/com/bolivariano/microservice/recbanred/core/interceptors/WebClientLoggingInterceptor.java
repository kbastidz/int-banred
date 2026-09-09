package com.bolivariano.microservice.recbanred.core.interceptors;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import static com.bolivariano.microservice.recbanred.core.constants.Labels.*;

public class WebClientLoggingInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebClientLoggingInterceptor.class);

    private WebClientLoggingInterceptor() {
    }

    /**
     * Interceptor para loggear la petición con su cuerpo en formato String.
     */
    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            // Log de información de la solicitud
            log.info("=== PETICIÓN ===");
            log.info("URL: {}", clientRequest.url());

            log.info("Body: {}", Encode.forJava(oneLine(getBodyLog(clientRequest))));

            return Mono.just(clientRequest);
        });
    }

    /**
     * Interceptor para loggear la respuesta SOAP.
     */
    public static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("=== RESPUESTA SOAP ===");


            return clientResponse.bodyToMono(String.class)
                    .flatMap(body -> {
                        log.info("Cuerpo de la respuesta: {}", oneLine(body));

                        ClientResponse mutatedResponse = ClientResponse
                                .create(clientResponse.statusCode())
                                .body(body)
                                .build();

                        return Mono.just(mutatedResponse);
                    });
        });
    }

    private static String getBodyLog(ClientRequest clientRequest) {
        String bodyAsJson = new Gson().toJson(clientRequest.body());
        JSONObject jsonAux = new JSONObject(bodyAsJson);

        if (jsonAux.has(KEY_BODY_LOG) && !jsonAux.isNull(KEY_BODY_LOG))
            return jsonAux.get(KEY_BODY_LOG).toString();

        return jsonAux.keySet().stream()
                .filter(key -> key.startsWith("arg$") && !jsonAux.isNull(key))
                .findFirst()
                .map(key -> jsonAux.get(key).toString())
                .orElse(bodyAsJson);
    }

    private static String oneLine(String value) {
        if (StringUtils.isEmpty(value)) return StringUtils.EMPTY;
        return value.replaceAll("[\r\n\t]+", " ").replaceAll("\\s{2,}", " ").trim();
    }
}
