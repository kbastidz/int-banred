package com.bolivariano.microservice.recbanred.service.banred;

import com.bolivariano.microservice.recbanred.core.constants.Labels;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;
import com.bolivariano.microservice.recbanred.core.payloads.output.AdditionalDataReversal;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.TokenData;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.*;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.*;
import com.bolivariano.microservice.recbanred.core.enums.banred.SubServicioMungye;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoBiller;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoOperacionToken;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.BillPaymentResponseV3;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.BillPaymentRsV3;
import com.bolivariano.microservice.recbanred.service.banred.v3.TokenDataV3Utils;
import com.bolivariano.microservice.recbanred.service.banred.v4.IsoMunQuitoParser;
import com.bolivariano.microservice.recbanred.util.banred.v3.BillerResolver;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaEjecutarPago;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaEjecutarPago;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaProcesar;
import com.bolivariano.microservice.recbanred.service.BanredService;
import com.bolivariano.microservice.recbanred.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;

import static com.bolivariano.microservice.recbanred.core.constants.Defaults.*;

@Service
public class PaymentBanred {

    private static final Logger log = LoggerFactory.getLogger(PaymentBanred.class);

    private final BanredService banredService;
    private final BusinessUtils businessUtils;
    private final TokenDataUtils tokenDataUtils;
    private final AdditionalDataUtils addDataUtils;
    private final TokenDataV3Utils tokenDataV3Utils;
    private final BillerResolver billerResolver;

    public PaymentBanred(BanredService banredService,
                         BusinessUtils businessUtils,
                         TokenDataUtils tokenDataUtils,
                         AdditionalDataUtils addDataUtils,
                         TokenDataV3Utils tokenDataV3Utils,
                         BillerResolver billerResolver) {
        this.banredService = banredService;
        this.businessUtils = businessUtils;
        this.tokenDataUtils = tokenDataUtils;
        this.addDataUtils = addDataUtils;
        this.tokenDataV3Utils = tokenDataV3Utils;
        this.billerResolver = billerResolver;
    }

    private static final Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

    /**
     * Procesa el pago y mapea la respuesta a un DTO de salida de pago de forma asincrona
     *
     * @param paymentRq - objeto de entrada de consulta para construccion XML
     * @return Mono<MensajeSalidaProcesar> - Objeto Salida asincrona del DTO Generico para el pago
     * */
    public Mono<MensajeSalidaProcesar> processPayment(MensajeEntradaEjecutarPago paymentRq) {
        return this.banredService.executePayment(paymentRq)
                .map(paymentRs -> {
                    MensajeSalidaEjecutarPago outputPayment = this.getOutputPaymentForVersioning(paymentRs, paymentRq);
                    log.info("Salida PAGO: {}", LoggingContext.writeJsonLogAndSanitize(outputPayment));
                    return new MensajeSalidaProcesar().successPayment(outputPayment);
                });
    }

    public MensajeSalidaEjecutarPago getOutputPaymentForVersioning(Object paymentRs, MensajeEntradaEjecutarPago paymentRq) {

        if (paymentRs instanceof BillPaymentRsV2 rsV2) {
            BillPaymentResponseV2 response = rsV2.getBillPaymentResponse();
            AdditionalDataReversal responseData = gson.fromJson(response.getResponseData(), AdditionalDataReversal.class);
            String txCode = this.businessUtils.validateAndReturnTxCode(paymentRq.getCuenta(), paymentRq.getTipoCuenta());
            AdditionalDataUtils.injectTxCode(paymentRq.getServicio().getDatosAdicionales(), txCode);

            return MensajeSalidaEjecutarPago.builder()
                    .codigoError(this.businessUtils.getErrorCode(response.getResultCode()))
                    .fechaDebito(CommonUtils.formatDate(new Date(), FULLDATE_FORMAT))
                    .fechaPago(CommonUtils.formatDate(new Date(), FULLDATE_FORMAT))
                    .banderaOffline(this.businessUtils.returnOfflineFlag(response.getStandIN()))
                    .mensajeUsuario(response.getErrorMessage())
                    .mensajeSistema(null)
                    .montoTotal(null)
                    .referencia(AdditionalDataUtils.getValueResponseDataPayment(responseData, Labels.BILLER_AUTH_CODE))
                    .datosAdicionales(this.addDataUtils.getAdditionalDataResponse(responseData, response, paymentRq))
                    .build();

        }
        if (paymentRs instanceof BillPaymentRsV3 rsV3) {
            BillPaymentResponseV3 response = rsV3.getBillPaymentResponse();
            DatosAdicionales datosAdicionales = this.parseResponseDataV3Safe(response.getResponseData(), paymentRq);

            return MensajeSalidaEjecutarPago.builder()
                    .codigoError(this.businessUtils.getErrorCode(response.getResultCode()))
                    .fechaDebito(CommonUtils.formatDate(new Date(), FULLDATE_FORMAT))
                    .fechaPago(CommonUtils.formatDate(new Date(), FULLDATE_FORMAT))
                    .banderaOffline(null)
                    .mensajeUsuario(response.getErrorMessage())
                    .mensajeSistema(null)
                    .montoTotal(null)
                    .referencia(TokenDataV3Utils.getValor(datosAdicionales, "COD_AUTORIZACION"))
                    .datosAdicionales(datosAdicionales)
                    .build();
        }
        if (paymentRs instanceof BillPaymentRsV1 rsV1) {
            BillPaymentResponseV1 response = rsV1.getBillPaymentResponse();
            TokenData tokenData = gson.fromJson(response.getTokenData(), TokenData.class);

            return MensajeSalidaEjecutarPago.builder()
                    .codigoError(this.businessUtils.getErrorCode(response.getResultCode()))
                    .fechaDebito(CommonUtils.formatDate(new Date(), FULLDATE_FORMAT))
                    .fechaPago(CommonUtils.formatDate(new Date(), FULLDATE_FORMAT))
                    .banderaOffline(null)
                    .mensajeUsuario(response.getErrorMessage())
                    .mensajeSistema(null)
                    .montoTotal(null)
                    .referencia(AdditionalDataUtils.getValueTokenDataGeneric(tokenData, Labels.NUM_DOC_IDENTIF))
                    .datosAdicionales(this.tokenDataUtils.getValueTokenDataResponse(tokenData, paymentRq, response))
                    .build();

        }
        // V4 – Municipio de Quito: ISO 8583 TCP
        if (paymentRs instanceof IsoMunQuitoParser.RespuestaIso rsV4) {
            return mapV4Pago(rsV4);
        } else {
            throw new IllegalArgumentException("Tipo de respuesta desconocido: " + paymentRs.getClass());
        }
    }

    /**
     * Mapea la respuesta ISO 8583 de pago (V4 – Municipio de Quito) a MensajeSalidaEjecutarPago.
     * El campo referencia lleva el número de autorización del Municipio (Bit 119).
     */
    private MensajeSalidaEjecutarPago mapV4Pago(IsoMunQuitoParser.RespuestaIso rsV4) {
        DatosAdicionales da = buildDatosAdicionalesV4Pago(rsV4);
        return MensajeSalidaEjecutarPago.builder()
                .codigoError(rsV4.codigoResultado != null ? rsV4.codigoResultado : "")
                .fechaDebito(CommonUtils.formatDate(new Date(), FULLDATE_FORMAT))
                .fechaPago(CommonUtils.formatDate(new Date(), FULLDATE_FORMAT))
                .banderaOffline(null)
                .mensajeUsuario(null)
                .mensajeSistema(null)
                .montoTotal(rsV4.getMontoTotal())
                .referencia(rsV4.numeroAutorizacion)
                .datosAdicionales(da)
                .build();
    }

    private DatosAdicionales buildDatosAdicionalesV4Pago(IsoMunQuitoParser.RespuestaIso rs) {
        java.util.List<com.bolivariano.microservice.recbanred.core.payloads.input.DatoAdicional> lista = new java.util.ArrayList<>();
        addDa(lista, "numero_autorizacion",    rs.numeroAutorizacion);
        addDa(lista, "nombre_contribuyente",   rs.nombreContribuyente);
        addDa(lista, "valor_total",            rs.valorTotal);
        addDa(lista, "valor_rubro",            rs.valorRubro);
        addDa(lista, "intereses",              rs.intereses);
        addDa(lista, "descuento",              rs.descuento);
        addDa(lista, "recargo",                rs.recargo);
        addDa(lista, "costas",                 rs.costas);
        addDa(lista, "fecha_emision",          rs.fechaEmision);
        addDa(lista, "titulo_credito",         rs.tituloCreditoBit113);
        addDa(lista, "avaluo_comercial",       rs.avaluoComercial);
        addDa(lista, "secuencial_banred",      rs.secuencialBanred);
        addDa(lista, "fecha_local",            rs.fechaLocal);
        addDa(lista, "hora_local",             rs.horaLocal);
        addDa(lista, "detalle_rubros",         rs.detalleRubros);
        var da = new DatosAdicionales();
        da.setDatoAdicional(lista);
        return da;
    }

    private void addDa(java.util.List<com.bolivariano.microservice.recbanred.core.payloads.input.DatoAdicional> lista,
                        String codigo, String valor) {
        if (valor == null) return;
        var d = new com.bolivariano.microservice.recbanred.core.payloads.input.DatoAdicional();
        d.setCodigo(codigo);
        d.setValor(valor);
        lista.add(d);
    }

    private DatosAdicionales parseResponseDataV3Safe(String responseData, MensajeEntradaEjecutarPago paymentRq) {
        try {
            String companyCode = AdditionalDataUtils.getValueAdditionalData(paymentRq.getServicio().getDatosAdicionales(), Labels.E_BAND_AUTORIZADOR);
            TipoBiller biller = this.billerResolver.resolverBiller(companyCode);
            int billServiceCode = CommonUtils.convertToInteger(paymentRq.getServicio().getCodigoConvenio());
            SubServicioMungye sub = biller == TipoBiller.MUNGYE ? this.billerResolver.resolverSubServicioMungye(billServiceCode) : null;
            return this.tokenDataV3Utils.parseResponseData(responseData, biller, sub, TipoOperacionToken.PAYMENT);
        } catch (Exception ex) {
            log.error("No se pudo decodificar ResponseData V3: {}", ex.getMessage());
            return null;
        }
    }

}


