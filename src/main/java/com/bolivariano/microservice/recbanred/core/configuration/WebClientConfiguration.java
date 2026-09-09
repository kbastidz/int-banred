package com.bolivariano.microservice.recbanred.core.configuration;

import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import com.bolivariano.microservice.recbanred.service.MarshalService;
import com.bolivariano.microservice.recbanred.core.interceptors.WebClientLoggingInterceptor;
import com.bolivariano.microservice.recbanred.util.BusinessUtils;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfiguration {

    private final BanredConfiguration banredConfiguration;

    public WebClientConfiguration(BanredConfiguration banredConfiguration) {
        this.banredConfiguration = banredConfiguration;
    }

    @Bean
    public WebClient.Builder webClient(MarshalService marshalService, BusinessUtils businessUtils) throws CustomException {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(clientHttp()))
                .defaultHeader("Accept", "text/xml;charset=UTF-8")
                .defaultHeader("Content-Type", "text/xml;charset=UTF-8")
                .filter(WebClientLoggingInterceptor.logRequest())  // Log del request con cuerpo
                .filter(WebClientLoggingInterceptor.logResponse()); // Log del response con cuerpo)
    }

    @Bean
    public HttpClient clientHttp() throws CustomException {

        try {
            // Crear el cliente HTTP reactivo con SSL configurado
            return HttpClient.create(ConnectionProvider.builder("custom")
                            .maxConnections(1000)
                            .pendingAcquireMaxCount(2900)
                            .pendingAcquireTimeout(Duration.ofMillis(banredConfiguration.getReadTimeout()))
                            .build())
                    .secure(SslProvider.builder().sslContext(SslContextBuilder
                                    .forClient()
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                    .build())
                            .build())  // Usar el SslProvider de reactor.netty
                    .responseTimeout(java.time.Duration.ofMillis(banredConfiguration.getReadTimeout()))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, banredConfiguration.getConnectionTimeout())
                    .doOnConnected(conn ->
                            conn.addHandlerLast(new ReadTimeoutHandler(banredConfiguration.getConnectionRequestTimeout(), TimeUnit.MILLISECONDS))
                                    .addHandlerLast(new WriteTimeoutHandler(banredConfiguration.getConnectionRequestTimeout(), TimeUnit.MILLISECONDS))
                    );
        } catch (Exception ex) {
            throw new CustomException("Error configurando el TrustStore: " + ex.getMessage(), ex, "INI001");
        }
    }
}