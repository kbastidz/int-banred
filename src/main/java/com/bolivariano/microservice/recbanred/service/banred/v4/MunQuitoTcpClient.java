package com.bolivariano.microservice.recbanred.service.banred.v4;

import com.bolivariano.microservice.recbanred.core.configuration.BanredConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.MunQuitoConfiguration;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import io.netty.channel.ChannelOption;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.*;

/**
 * Cliente TCP reactivo para comunicación con el servidor ISO 8583 del
 * Municipio del Distrito Metropolitano de Quito (V4).
 *
 * Ciclo de vida por transacción:
 *   1. Abre conexión TCP al host:puerto configurado.
 *   2. Envía la trama ISO en texto plano (UTF-8).
 *   3. Lee la respuesta completa.
 *   4. Cierra la conexión.
 *   5. Retorna la trama de respuesta parseada.
 *
 * Se usa una conexión por transacción (stateless) en lugar de un pool
 * persistente, dado el modelo request/response del protocolo ISO 8583.
 */
@Component
public class MunQuitoTcpClient {

    private static final Logger log = LoggerFactory.getLogger(MunQuitoTcpClient.class);

    private final MunQuitoConfiguration munQuitoConfig;
    private final BanredConfiguration banredConfig;
    private final IsoMunQuitoParser isoParser;

    public MunQuitoTcpClient(MunQuitoConfiguration munQuitoConfig,
                              BanredConfiguration banredConfig,
                              IsoMunQuitoParser isoParser) {
        this.munQuitoConfig = munQuitoConfig;
        this.banredConfig   = banredConfig;
        this.isoParser      = isoParser;
    }

    /**
     * Envía una trama ISO y retorna la respuesta parseada de forma reactiva.
     *
     * @param trama Trama ISO 8583 en texto plano construida por {@link IsoMunQuitoBuilder}
     * @return {@link Mono} con el resultado parseado {@link IsoMunQuitoParser.RespuestaIso}
     */
    public Mono<IsoMunQuitoParser.RespuestaIso> enviar(String trama) {
        log.info("MunQuito TCP >> host={}:{} trama=[{}]",
                munQuitoConfig.getHost(), munQuitoConfig.getPort(), trama);

        return TcpClient.create()
                .host(munQuitoConfig.getHost())
                .port(munQuitoConfig.getPort())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, banredConfig.getConnectionTimeout())
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(
                                banredConfig.getReadTimeout(), TimeUnit.MILLISECONDS)))
                .connect()
                .flatMap(conn ->
                        conn.outbound()
                                .sendString(Mono.just(trama), StandardCharsets.UTF_8)
                                .then()
                                .thenMany(conn.inbound().receive().asString(StandardCharsets.UTF_8))
                                .reduce("", String::concat)
                                .doFinally(signal -> conn.dispose())
                )
                .timeout(Duration.ofMillis(banredConfig.getReadTimeout()))
                .map(rawResponse -> {
                    log.info("MunQuito TCP << respuesta=[{}]", rawResponse);
                    return isoParser.parsear(rawResponse);
                })
                .onErrorMap(ReadTimeoutException.class, ex -> {
                    log.error("Timeout de lectura con Municipio Quito TCP: {}", ex.getMessage());
                    return new CustomException(
                            "Tiempo de espera agotado comunicándose con Municipio de Quito",
                            ex, READ_SOCKET_TIMEOUT);
                })
                .onErrorMap(ConnectTimeoutException.class, ex -> {
                    log.error("Timeout de conexión con Municipio Quito TCP: {}", ex.getMessage());
                    return new CustomException(
                            "No se pudo conectar con Municipio de Quito",
                            ex, CONNECT_TIMEOUT_ERROR);
                })
                .onErrorMap(UnknownHostException.class, ex -> {
                    log.error("Host de Municipio Quito no encontrado: {}", ex.getMessage());
                    return new CustomException(
                            "Host de Municipio de Quito no disponible",
                            ex, UNKNOWN_HOST_ERROR);
                })
                .onErrorMap(ex -> !(ex instanceof CustomException), ex -> {
                    log.error("Error inesperado en TCP con Municipio Quito: {}", ex.getMessage());
                    return new CustomException(
                            "Error de comunicación con Municipio de Quito",
                            ex, BANRED_ERROR);
                });
    }
}
