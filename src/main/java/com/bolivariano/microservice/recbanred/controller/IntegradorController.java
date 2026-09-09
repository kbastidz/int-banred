package com.bolivariano.microservice.recbanred.controller;

import com.bolivariano.microservice.recbanred.core.exceptions.BadFormatException;
import com.bolivariano.microservice.recbanred.core.enums.TipoFlujo;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaProcesar;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaProcesar;
import com.bolivariano.microservice.recbanred.service.banred.InquiryBanred;
import com.bolivariano.microservice.recbanred.service.banred.PaymentBanred;
import com.bolivariano.microservice.recbanred.service.banred.ReversalBanred;
import com.bolivariano.microservice.recbanred.util.BusinessUtils;
import com.bolivariano.microservice.recbanred.util.DataMaskingUtils;
import com.bolivariano.microservice.recbanred.util.LoggingContext;
import com.bolivariano.microservice.recbanred.util.SensitiveDataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/banred")
public class IntegradorController {

    private static final Logger log = LoggerFactory.getLogger(IntegradorController.class);
    private final InquiryBanred inquiryBanred;
    private final PaymentBanred paymentBanred;
    private final ReversalBanred reversalBanred;
    private final BusinessUtils businessUtils;
    private final DataMaskingUtils dataMaskingUtils;

    public IntegradorController(InquiryBanred inquiryBanred,
                                PaymentBanred paymentBanred,
                                ReversalBanred reversalBanred,
                                BusinessUtils businessUtils,
                                DataMaskingUtils dataMaskingUtils) {
        this.inquiryBanred = inquiryBanred;
        this.paymentBanred = paymentBanred;
        this.reversalBanred = reversalBanred;
        this.businessUtils = businessUtils;
        this.dataMaskingUtils = dataMaskingUtils;
    }

    @PostMapping(path = "/procesar",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<MensajeSalidaProcesar>> executeProcessBanred(@RequestBody MensajeEntradaProcesar inputMessage, ServerWebExchange exchange) throws BadFormatException {
        exchange.getAttributes().put("tipoFlujo", inputMessage.getTipoFlujo());

        var maskedInquiry = inputMessage.getTipoFlujo() == TipoFlujo.CONSULTA
                ? dataMaskingUtils.applyMasking(inputMessage.getMensajeEntradaConsultarDeuda())
                : null;

        String[] inputLogData = getInputLogData(inputMessage, maskedInquiry);
        String uid = UUID.randomUUID().toString();
        String dataLogs = this.businessUtils.getDataLogs(inputMessage);
        String[] dataLogsParts = dataLogs.split(",", 2);
        String codigoEmpresa = dataLogsParts.length > 0 ? dataLogsParts[0] : inputLogData[2];
        String secuencialLogs = dataLogsParts.length > 1 ? dataLogsParts[1] : "";
        LoggingContext.init(uid, getTipoFlujoShort(inputMessage.getTipoFlujo()), codigoEmpresa, secuencialLogs);
        LoggingContext.putRequestContext(inputLogData[0], inputLogData[1], inputLogData[3]);

        String sanitizedInput = inputMessage.getTipoFlujo() == TipoFlujo.CONSULTA
                ? LoggingContext.writeJsonLogAndSanitize(maskedInquiry)
                : LoggingContext.writeJsonLogAndSanitize(inputMessage);
        log.info("Entrada recibida: {}", sanitizedInput);
        log.info("Inicia proceso de validación de informacion de entrada");
        SensitiveDataUtils.validateSensitiveData(inputMessage);
        log.info("Finaliza proceso de validación de informacion de entrada");

        return switch (inputMessage.getTipoFlujo()) {
            case CONSULTA -> {
                log.info("Entrada CONSULTA: {}", LoggingContext.writeJsonLogAndSanitize(maskedInquiry));
                yield inquiryBanred
                        .processInquiry(inputMessage.getMensajeEntradaConsultarDeuda())
                        .map(ResponseEntity::ok);
            }
            case PAGO -> paymentBanred
                    .processPayment(inputMessage.getMensajeEntradaEjecutarPago())
                    .map(ResponseEntity::ok);
            case REVERSO -> reversalBanred
                    .processReversal(inputMessage.getMensajeEntradaEjecutarReverso())
                    .map(ResponseEntity::ok);
        };
    }

    private static String getTipoFlujoShort(TipoFlujo tipoFlujo) {
        if (tipoFlujo == null) return "";
        return switch (tipoFlujo) {
            case CONSULTA -> "C";
            case PAGO -> "P";
            case REVERSO -> "R";
        };
    }

    private static String[] getInputLogData(MensajeEntradaProcesar inputMessage, Object maskedInquiry) {
        if (inputMessage == null || inputMessage.getTipoFlujo() == null) return new String[]{"", "", "", ""};

        String canal = "";
        String codTipoServicio = "";
        String codigoEmpresa = "";
        String identificador = "";

        switch (inputMessage.getTipoFlujo()) {
            case CONSULTA -> {
                var msg = maskedInquiry instanceof com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaConsultarDeuda m
                        ? m
                        : inputMessage.getMensajeEntradaConsultarDeuda();
                if (msg == null || msg.getServicio() == null) return new String[]{"", "", "", ""};
                canal = msg.getCanal();
                codTipoServicio = msg.getServicio().getCodTipoServicio();
                codigoEmpresa = msg.getServicio().getCodigoEmpresa();
                identificador = msg.getServicio().getIdentificador();
            }
            case PAGO -> {
                var msg = inputMessage.getMensajeEntradaEjecutarPago();
                if (msg == null || msg.getServicio() == null) return new String[]{"", "", "", ""};
                canal = msg.getCanal();
                codTipoServicio = msg.getServicio().getCodTipoServicio();
                codigoEmpresa = msg.getServicio().getCodigoEmpresa();
                identificador = msg.getServicio().getIdentificador();
            }
            case REVERSO -> {
                var msg = inputMessage.getMensajeEntradaEjecutarReverso();
                if (msg == null || msg.getServicio() == null) return new String[]{"", "", "", ""};
                canal = msg.getCanal();
                codTipoServicio = msg.getServicio().getCodTipoServicio();
                codigoEmpresa = msg.getServicio().getCodigoEmpresa();
                identificador = msg.getServicio().getIdentificador();
            }
        }

        return new String[]{canal, codTipoServicio, codigoEmpresa, identificador};
    }

}
