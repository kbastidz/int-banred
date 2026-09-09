package com.bolivariano.microservice.recbanred.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class BanredClient {

    private final WebClient webClient;

    public BanredClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8099").build();
    }

    public Mono<String> sendRequestToBanred() {
        return webClient.post()
                .uri("/banred/procesar")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == 408) {
                        return Mono.error(new RuntimeException("Timeout error from Banred"));
                    }
                    return Mono.error(ex);
                });
    }
}
