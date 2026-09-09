package com.bolivariano.microservice.recbanred.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.bolivariano.microservice.recbanred.service.BanredClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
class IntegradorControllerTests {

    private WireMockServer mockServer;
    private BanredClient banredClient;

    @BeforeAll
    void setup() {
        mockServer = new WireMockServer(8099); // Iniciar WireMock en puerto 8081
        mockServer.start();
        banredClient = new BanredClient(WebClient.builder());
    }

    @AfterAll
    void teardown() {
        mockServer.stop();
    }

    @Test
    @DisplayName("Success Operation")
    void testSuccessfulRequestRECOPUB115() {
        mockServer.stubFor(post(urlEqualTo("/banred/procesar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"message\":\"Success\"}")));

        StepVerifier.create(banredClient.sendRequestToBanred())
                .expectNext("{\"message\":\"Success\"}")
                .verifyComplete();
    }

    @Test
    @DisplayName("Error Operation")
    void testTimeoutError408RECOUPB115() {
        mockServer.stubFor(post(urlEqualTo("/banred/procesar"))
                .willReturn(aResponse().withStatus(408))); // Simular HTTP 408

        StepVerifier.create(banredClient.sendRequestToBanred())
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Timeout error from Banred"))
                .verify();
    }
}
