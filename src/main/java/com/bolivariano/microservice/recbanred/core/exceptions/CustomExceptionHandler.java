package com.bolivariano.microservice.recbanred.core.exceptions;

import com.bolivariano.microservice.recbanred.core.constants.Labels;
import com.bolivariano.microservice.recbanred.core.enums.TipoFlujo;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaConsultarDeuda;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaEjecutarPago;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaProcesar;
import com.bolivariano.microservice.recbanred.util.LoggingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestControllerAdvice
public class CustomExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomExceptionHandler.class);

    private static final String UNEXPECTED_ERROR_CODE = "-500";
    private static final String UNEXPECTED_ERROR_MESSAGE = "ERROR INTERNO INESPERADO: ";
    private static final String BAD_REQUEST_ERROR_CODE = "-400";
    private static final String BAD_REQUEST_ERROR_MESSAGE = "ERROR AL PROCESAR ENTRADA: ";
    private static final String BAD_PARAMETER_ERROR_MESSAGE = "ERROR DE PARAMETROS INVOCADOS AL INVOCAR SERVICIO";

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Mono<ResponseEntity<MensajeSalidaProcesar>> handleResponseStatusException(ResponseStatusException exception, ServerWebExchange exchange) {
        log.error("Error 400: {}", exception.getMessage());
        TipoFlujo fluxType = exchange.getAttribute(Labels.TIPO_FLUJO);
        
        return Mono.just(getGenericResponse(HttpStatus.BAD_REQUEST, 
                BAD_REQUEST_ERROR_CODE,
                BAD_REQUEST_ERROR_MESSAGE,
                Objects.requireNonNull(fluxType)));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Mono<ResponseEntity<MensajeSalidaProcesar>> handleValidationException(WebExchangeBindException exception, ServerWebExchange exchange) {
        String reason = Objects.requireNonNull(exception.getBindingResult().getFieldError()).getDefaultMessage();
        log.error("Error de validación: {}", reason);
        TipoFlujo fluxType = exchange.getAttribute(Labels.TIPO_FLUJO);
        return Mono.just(getGenericResponse(HttpStatus.BAD_REQUEST,
                BAD_REQUEST_ERROR_CODE,
                BAD_PARAMETER_ERROR_MESSAGE,
                Objects.requireNonNull(fluxType)
        ));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Mono<ResponseEntity<MensajeSalidaProcesar>> handleException(Exception e, ServerWebExchange exchange) {
        log.error("Error interno en {}", exchange.getRequest().getPath(), e);
        TipoFlujo fluxType = exchange.getAttribute(Labels.TIPO_FLUJO);

        return Mono.just(getGenericResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                UNEXPECTED_ERROR_CODE,
                UNEXPECTED_ERROR_MESSAGE + e.getMessage(),
                Objects.requireNonNull(fluxType)
        ));
    }

    @ExceptionHandler(CustomException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Mono<ResponseEntity<MensajeSalidaProcesar>> handleCustomException(CustomException e, ServerWebExchange exchange) {
        String sanitized = LoggingContext.sanitize(e.getLocalizedMessage());
        log.error("Error personalizado: {}", sanitized);
        TipoFlujo fluxType = exchange.getAttribute(Labels.TIPO_FLUJO);

        return Mono.just(getGenericResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                e.getCode(),
                sanitized,
                Objects.requireNonNull(fluxType)
        ));
    }

    @ExceptionHandler(BadFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Mono<ResponseEntity<MensajeSalidaProcesar>> handleBadFormatException(BadFormatException e, ServerWebExchange exchange) {
        log.error("Error en validacion de formato de entrada: {}", e.getMessage());
        TipoFlujo fluxType = exchange.getAttribute(Labels.TIPO_FLUJO);
        return Mono.just(getGenericResponse(HttpStatus.BAD_REQUEST,
                e.getCode(),
                e.getMessage(),
                Objects.requireNonNull(fluxType)
        ));
    }

    private ResponseEntity<MensajeSalidaProcesar> getGenericResponse(HttpStatus httpStatus, String code, String message, TipoFlujo fluxType) {
        MensajeSalidaProcesar response = new MensajeSalidaProcesar();
        response.errorGeneric(code, message);
        if (fluxType.equals(TipoFlujo.CONSULTA)) {
            MensajeSalidaConsultarDeuda consultarDeuda = new MensajeSalidaConsultarDeuda();
            consultarDeuda.setCodigoError(code);
            consultarDeuda.setMensajeUsuario(message);
            response.setMensajeSalidaConsultarDeuda(consultarDeuda);
        }
        if (fluxType.equals(TipoFlujo.PAGO) || fluxType.equals(TipoFlujo.REVERSO)){
            MensajeSalidaEjecutarPago ejecutarPago = new MensajeSalidaEjecutarPago();
            ejecutarPago.setCodigoError(code);
            ejecutarPago.setMensajeUsuario(message);
            response.setMensajeSalidaEjecutarPago(ejecutarPago);
        }
        return new ResponseEntity<>(response, httpStatus);
    }
}
