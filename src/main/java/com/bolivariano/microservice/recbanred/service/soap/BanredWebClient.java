package com.bolivariano.microservice.recbanred.service.soap;

import com.bolivariano.microservice.recbanred.core.configuration.BanredConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.CompanyConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.MigCompanyConfiguration;
import com.bolivariano.microservice.recbanred.core.enums.TipoVersion;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import com.bolivariano.microservice.recbanred.core.exceptions.ExecutionException;
import com.bolivariano.microservice.recbanred.service.MarshalService;
import com.bolivariano.microservice.recbanred.util.BusinessUtils;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.*;

@Service
@Data
public class BanredWebClient {

    private static final Logger log = LoggerFactory.getLogger(BanredWebClient.class);

    private final WebClient.Builder webCLientBuilder;
    private final MarshalService marshalService;
    private final BusinessUtils businessUtils;
    private final BanredConfiguration banredConfiguration;
    private final CompanyConfiguration companyConfiguration;
    private final MigCompanyConfiguration migCompanyConfiguration;

    public BanredWebClient(WebClient.Builder webClientBuilder,
                           MarshalService marshalService,
                           BusinessUtils businessUtils,
                           BanredConfiguration banredConfiguration,
                           CompanyConfiguration companyConfiguration,
                           MigCompanyConfiguration migCompanyConfiguration
                           ) {
        this.webCLientBuilder = webClientBuilder;
        this.marshalService = marshalService;
        this.businessUtils = businessUtils;
        this.banredConfiguration = banredConfiguration;
        this.companyConfiguration = companyConfiguration;
        this.migCompanyConfiguration = migCompanyConfiguration;
    }

    /**
     * METODO ASINCRONO Y GENERICO QUE REALIZA LA PETICION HACIA BANRED
     *
     * @param soapRequest - El payload de la peticion
     * @return Mono<Object> - Objeto de respuesta de banred de Via Rapida (NO BLOQUEANTE)
     */
    public Mono<Object> invokeSOAP(Object soapRequest, TipoVersion versionType, String companyCode) {
        try {
            String soapXML = this.marshalService.marshallRequest(soapRequest, versionType);
            return sendSOAPRequest(soapXML, soapRequest, versionType, companyCode);
        } catch (Exception e) {
            log.error("Error al invocar el servicio SOAP de Banred", e);
            return Mono.error(new ExecutionException("Ocurrió un error al invocar el servicio SOAP de Banred", e, BANRED_ERROR, e.getMessage()));
        }
    }

    private Mono<Object> sendSOAPRequest(String soapXML, Object soapRequest, TipoVersion versionType, String companyCode) {
        //28072025 - LL: Se dinamiza la url para versionamiento de BANRED
        String uri = this.companyConfiguration.getVersions().getVersionForType(versionType).getUrl();

        WebClient webClient = this.webCLientBuilder.clone().baseUrl(uri).build();

        return webClient
                .post()
                .bodyValue(soapXML)
                .exchangeToMono(response -> handleResponse(response, soapRequest, versionType))
                .onErrorMap(WebClientRequestException.class, ex -> handleWebClientException(ex, companyCode));
    }

    private Mono<Object> handleResponse(ClientResponse response, Object soapRequest, TipoVersion versionType) {
        HttpStatusCode status = response.statusCode();

        if (isTimeoutStatus(status)) {
            log.error("Se replicó un error de timeout HTTP desde BANRED: {}", status);
            return Mono.error(new CustomException(this.banredConfiguration.getGenericErrorResponse(), null,
                    status.toString()));
        }
        return response.bodyToMono(String.class)
                .flatMap(body -> processBody(body, status, soapRequest, versionType));
    }

    private Mono<Object> processBody(String body, HttpStatusCode status, Object soapRequest, TipoVersion versionType) {
        try {
            if (status.isSameCodeAs(HttpStatusCode.valueOf(200))) {
                String resultCode = this.marshalService.extractResultCodeOrFaultString(body);

                if (this.businessUtils.isTimeoutResultCode(resultCode)) {
                    log.error("Tiempo de Espera agotado, devuelto por Banred: resultCode={}", resultCode);
                    return Mono.error(new CustomException(
                            this.banredConfiguration.getGenericErrorResponse(),
                            null,
                            BANRED_CONCATS.concat(resultCode)
                    ));
                }

                String errorMessage = this.marshalService.extractErrorMessage(body);
                if (this.businessUtils.isCommerceErrorMessage(errorMessage)) {
                    log.error("Inconveniente Ocurrido durante la peticion a Banred: {}", errorMessage);
                    return Mono.error(new CustomException(
                       this.banredConfiguration.getGenericErrorResponse(),
                       null,
                       BANRED_CONCATS.concat(resultCode)
                    ));
                }

                return Mono.just(this.marshalService.unmarshalResponse(body, this.businessUtils.getResponseClass(soapRequest, versionType)));
            }

            if (status.isSameCodeAs(HttpStatusCode.valueOf(500))) {
                String faultString = this.marshalService.extractResultCodeOrFaultString(body);
                log.error("Error SOAP de respuesta de Banred: faultstring={}", faultString);
                return Mono.error(new CustomException(
                        this.banredConfiguration.getGenericErrorResponse(),
                        null,
                        BANRED_ERROR
                ));
            }
            return Mono.just(body);

        } catch (Exception e) {
            log.error("Error al procesar respuesta SOAP: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    private boolean isTimeoutStatus(HttpStatusCode status) {
        return status.isSameCodeAs(HttpStatusCode.valueOf(408)) ||
                status.isSameCodeAs(HttpStatusCode.valueOf(504));
    }

    private Throwable handleWebClientException(WebClientRequestException ex, String companyCode) {
        if (ex.getCause() instanceof ReadTimeoutException || ex.getCause() instanceof SocketTimeoutException) {
            log.error("Tiempo de lectura agotado obtener informacion del servidor BANRED: {}", ex.getMessage());
            boolean isMigratedCompany = this.migCompanyConfiguration.getCompanyMigratesList().contains(companyCode);
            String timeoutCode = isMigratedCompany ? TIMEOUT_ERROR : READ_SOCKET_TIMEOUT;
            return new CustomException(this.banredConfiguration.getGenericErrorResponse(), ex, timeoutCode);
        }
        if (ex.getCause() instanceof ConnectTimeoutException) {
            log.error("Tiempo de conexión agotado al intentar acceder a BANRED: {}", ex.getMessage());
            return new CustomException(banredConfiguration.getGenericErrorResponse(), ex, CONNECT_TIMEOUT_ERROR);
        }

        if (ex.getCause() instanceof UnknownHostException) {
            log.error("Dominio BANRED no reconocido, o IP no registrada. Validar WAF y/o hostAlias del archivo deployment.yml de este artefacto: {}", ex.getMessage());
            return new CustomException(this.banredConfiguration.getGenericErrorResponse(), ex, UNKNOWN_HOST_ERROR);
        }

        if (ex.getCause() instanceof ConnectException) {
            log.error("No se pudo establecer conexión con BANRED (host inaccesible o puerto cerrado): {}", ex.getMessage());
            return new CustomException(banredConfiguration.getGenericErrorResponse(), ex, CONNECT_ERROR);
        }

        if (ex.getCause() instanceof SSLHandshakeException) {
            log.error("Error en la negociación SSL con BANRED (certificacion caducada o invalida): {}", ex.getMessage());
            return new CustomException(banredConfiguration.getGenericErrorResponse(), ex, HANDSHAKE_ERROR);
        }

        if (ex.getCause() instanceof NoRouteToHostException) {
            log.error("No existe redireccion hacia BANRED (validar con infraestructura reglas de Firewall): {}", ex.getMessage());
            return new CustomException(banredConfiguration.getGenericErrorResponse(), ex, "NRH503");
        }

        log.error("Causa no definida del error de conexion a BANRED: {}", ex.getMessage());
        return ex;
    }
}