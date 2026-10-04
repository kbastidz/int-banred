package com.bolivariano.microservice.recbanred.service.banred;

import com.bolivariano.microservice.recbanred.core.constants.Labels;
import com.bolivariano.microservice.recbanred.core.constants.Defaults;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;
import com.bolivariano.microservice.recbanred.core.payloads.output.Recibo;
import com.bolivariano.microservice.recbanred.core.payloads.output.Recibos;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.DocTrxDetalle;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.TokenData;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillInquiryResponseV1;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillInquiryRsV1;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.BillInquiryResponseV2;
import com.bolivariano.microservice.recbanred.core.payloads.AdditionalDataPayment;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaConsultarDeuda;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaConsultarDeuda;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaProcesar;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.BillInquiryRsV2;
import com.bolivariano.microservice.recbanred.core.enums.banred.SubServicioMungye;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoBiller;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoOperacionToken;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.BillInquiryResponseV3;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.BillInquiryRsV3;
import com.bolivariano.microservice.recbanred.service.banred.v3.TokenDataV3Utils;
import com.bolivariano.microservice.recbanred.util.banred.v3.BillerResolver;
import com.bolivariano.microservice.recbanred.service.BanredService;
import com.bolivariano.microservice.recbanred.util.*;
import com.google.gson.Gson;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Service
public class InquiryBanred {

    private final BanredService banredService;
    private final BusinessUtils businessUtils;
    private final TokenDataUtils tokenDataUtils;
    private final AdditionalDataUtils addDataUtils;
    private final DataMaskingUtils dataMaskingUtils;
    private final TokenDataV3Utils tokenDataV3Utils;
    private final BillerResolver billerResolver;
    private static final Gson gson = new Gson().newBuilder().disableHtmlEscaping().serializeNulls().create();
    private static final Logger log = LoggerFactory.getLogger(InquiryBanred.class);

    public InquiryBanred(BanredService banredService,
                         BusinessUtils businessUtils,
                         TokenDataUtils tokenDataUtils,
                         AdditionalDataUtils addDataUtils,
                         DataMaskingUtils dataMaskingUtils,
                         TokenDataV3Utils tokenDataV3Utils,
                         BillerResolver billerResolver) {
        this.banredService = banredService;
        this.businessUtils = businessUtils;
        this.tokenDataUtils = tokenDataUtils;
        this.addDataUtils = addDataUtils;
        this.dataMaskingUtils = dataMaskingUtils;
        this.tokenDataV3Utils = tokenDataV3Utils;
        this.billerResolver = billerResolver;
    }

    /**
     * Procesa la consulta y mapea la respuesta a un DTO de salida de consulta de forma asincrona
     *
     * @param inquiryRq - objeto de entrada de consulta para construccion XML
     *
     * @return Mono<MensajeSalidaProcesar> - Objeto Salida asincrona del DTO Generico para consulta
     *
     * */
    public Mono<MensajeSalidaProcesar> processInquiry(MensajeEntradaConsultarDeuda inquiryRq) {
        return banredService.executeInquiry(inquiryRq)
                .map(inquiryRs -> {
                    MensajeSalidaConsultarDeuda outputInquiry = this.getOutputInquiryForVersioning(inquiryRs, inquiryRq);
                    outputInquiry = dataMaskingUtils.applyMaskingToInquiryResponse(outputInquiry);
                    log.info("Salida CONSULTA: {}", LoggingContext.writeJsonLogAndSanitize(outputInquiry));
                    return new MensajeSalidaProcesar().successInquiry(outputInquiry);
                });
    }

    /**
     * Se identifica la salida de la consulta y se mapea de acuerdo al tipo de version utilizada para BANRED
     *
     * @param inquiryRs - Objeto salida de la consulta
     * @param inquiryRq - Objeto entrada de la consulta
     *
     * @return MensajeSalidaConsultarDeuda - DTO de salida de consulta
     *
     * */
    public MensajeSalidaConsultarDeuda getOutputInquiryForVersioning(Object inquiryRs, MensajeEntradaConsultarDeuda inquiryRq) {

        if (inquiryRs instanceof BillInquiryRsV2 rsV2) {
            BillInquiryResponseV2 response = rsV2.getBillInquiryResponse();
            AdditionalDataPayment responseData = gson.fromJson(response.getResponseData(), AdditionalDataPayment.class);

            return MensajeSalidaConsultarDeuda.builder()
                    .codigoError(this.businessUtils.getErrorCode(response.getResultCode()))
                    .datosAdicionales(this.addDataUtils.getAdditionalDataResponse(responseData, response, inquiryRq))
                    .fechaVencimiento(this.businessUtils.transformCutoverDate(response.getBillerCutoverDate()))
                    .formaPago(null)
                    .formaPagoRecibos(null)
                    .identificadorDeuda(AdditionalDataUtils.getValueResponseDataInquiry(responseData, Labels.DOCUMENTID))
                    .limiteMontoMaximo(StringUtils.isNotEmpty(response.getAmount())
                            ? (BigDecimal) this.businessUtils.formatPaidAmountFromBanred(response.getAmount()) : BigDecimal.ZERO)
                    .limiteMontoMinimo(BigDecimal.ZERO)
                    .mensajeUsuario(response.getErrorMessage())
                    .mensajeSistema(null)
                    .montoMinimo(BigDecimal.ZERO)
                    .montoTotal(StringUtils.isNotEmpty(response.getAmount())
                            ? (BigDecimal) this.businessUtils.formatPaidAmountFromBanred(response.getAmount()) : BigDecimal.ZERO)
                    .nombreCliente(AdditionalDataUtils.getValueResponseDataInquiry(responseData, Labels.FULLNAME))
                    .recibos(this.getReceiptsV2(response, inquiryRq.getServicio().getDatosAdicionales(), responseData))
                    .textoAyuda(null)
                    .build();

        }
        if (inquiryRs instanceof BillInquiryRsV3 rsV3) {
            BillInquiryResponseV3 response = rsV3.getBillInquiryResponse();
            DatosAdicionales datosAdicionales = this.parseResponseDataV3Safe(response.getResponseData(), inquiryRq, TipoOperacionToken.INQUIRY);

            return MensajeSalidaConsultarDeuda.builder()
                    .codigoError(this.businessUtils.getErrorCode(response.getResultCode()))
                    .datosAdicionales(datosAdicionales)
                    .fechaVencimiento(this.businessUtils.transformCutoverDate(response.getBillerCutoverDate()))
                    .formaPago(null)
                    .formaPagoRecibos(null)
                    .identificadorDeuda(TokenDataV3Utils.getValor(datosAdicionales, "REFERENCIA_CLIENTE"))
                    .limiteMontoMaximo(StringUtils.isNotEmpty(response.getAmount())
                            ? (BigDecimal) this.businessUtils.formatPaidAmountFromBanred(response.getAmount()) : BigDecimal.ZERO)
                    .limiteMontoMinimo(BigDecimal.ZERO)
                    .mensajeUsuario(response.getErrorMessage())
                    .mensajeSistema(null)
                    .montoMinimo(BigDecimal.ZERO)
                    .montoTotal(StringUtils.isNotEmpty(response.getAmount())
                            ? (BigDecimal) this.businessUtils.formatPaidAmountFromBanred(response.getAmount()) : BigDecimal.ZERO)
                    .nombreCliente(TokenDataV3Utils.getValor(datosAdicionales, "NOMBRE_CLIENTE"))
                    // NOTA: el desglose por Recibo (uno por documento/año/cuota) depende de cada
                    // biller (ver TokenDataV3Utils/EspecificacionToken); se deja generico en
                    // "datosAdicionales" y puede mapearse a Recibos puntualmente si se requiere.
                    .recibos(null)
                    .textoAyuda(null)
                    .build();
        }
        if (inquiryRs instanceof BillInquiryRsV1 rsV1) {
            BillInquiryResponseV1 response = rsV1.getBillInquiryResponse();
            TokenData tokenData = gson.fromJson(response.getTokenData(), TokenData.class);

            return MensajeSalidaConsultarDeuda.builder()
                    .codigoError(this.businessUtils.getErrorCode(response.getResultCode()))
                    .datosAdicionales(this.tokenDataUtils.getValueTokenDataResponse(tokenData, inquiryRq, null))
                    .fechaVencimiento(this.businessUtils.transformCutoverDate(response.getBillerCutoverDate()))
                    .formaPago(null)
                    .formaPagoRecibos(null)
                    .identificadorDeuda(AdditionalDataUtils.getValueTokenDataGeneric(tokenData, Labels.NUM_DOC_IDENTIF))
                    .limiteMontoMaximo(StringUtils.isNotEmpty(response.getAmount())
                            ? (BigDecimal) this.businessUtils.formatPaidAmountFromBanred(response.getAmount()) : BigDecimal.ZERO)
                    .limiteMontoMinimo(BigDecimal.ZERO)
                    .mensajeUsuario(response.getErrorMessage())
                    .mensajeSistema(null)
                    .montoMinimo(BigDecimal.ZERO)
                    .montoTotal(StringUtils.isNotEmpty(response.getAmount())
                            ? (BigDecimal) this.businessUtils.formatPaidAmountFromBanred(response.getAmount()) : BigDecimal.ZERO)
                    .nombreCliente(AdditionalDataUtils.getValueTokenDataGeneric(tokenData, Labels.NOMBRE_CLIENTE))
                    .recibos(this.getReceiptsV1(tokenData))
                    .textoAyuda(null)
                    .build();

        } else {
            throw new IllegalArgumentException("Tipo de respuesta desconocido: " + inquiryRs.getClass());
        }
    }

    /**
     * Resuelve el biller/sub-servicio a partir del request original y decodifica
     * el ResponseData V3 (token Q1). Nunca lanza excepcion: en caso de error,
     * se loguea y se retorna null (igual criterio que getReceiptsV1/getReceiptsV2).
     */
    private DatosAdicionales parseResponseDataV3Safe(String responseData, MensajeEntradaConsultarDeuda inquiryRq, TipoOperacionToken operacion) {
        try {
            String companyCode = AdditionalDataUtils.getValueAdditionalData(inquiryRq.getServicio().getDatosAdicionales(), Labels.E_BAND_AUTORIZADOR);
            String canal = inquiryRq.getCanal();
            TipoBiller biller = this.billerResolver.resolverBiller(companyCode);
            int billServiceCode = CommonUtils.convertToInteger(inquiryRq.getServicio().getCodigoConvenio());
            SubServicioMungye sub = biller == TipoBiller.MUNGYE ? this.billerResolver.resolverSubServicioMungye(billServiceCode) : null;
            return this.tokenDataV3Utils.parseResponseData(responseData, biller, sub, operacion,canal);
        } catch (Exception ex) {
            log.error("No se pudo decodificar ResponseData V3: {}", ex.getMessage());
            return null;
        }
    }

    private Recibos getReceiptsV1(TokenData tokenData) {
        if (!validTokenData(tokenData)) {
            log.error("No existe información de la respuesta o del TokenData");
            return null;
        }

        String sanitizedTokenData = LoggingContext.writeJsonLogAndSanitize(tokenData);
        log.info("TOKEN DATA BANRED -> {}", sanitizedTokenData);

        try {
            List<Recibo> receipts = new ArrayList<>();

            tokenData.getDocTrxDetalle()
                    .forEach(docTrxDetalle -> {
                        Recibo recibo = mapToReceiptV1(docTrxDetalle);
                        receipts.add(recibo);
                    });

            return new Recibos(receipts);
        } catch (Exception ex) {
            log.error("Ocurrió un error al mapear la data hacia el objeto RECIBO: {}", ex.getMessage());
            return null;
        }
    }

    private Recibos getReceiptsV2(BillInquiryResponseV2 response, DatosAdicionales additionalData, AdditionalDataPayment responseData) {
        if (!validResponse(responseData)) {
            log.error("No existe información de la respuesta o del responseData");
            return null;
        }

        String sanitizedResponseData = LoggingContext.writeJsonLogAndSanitize(responseData);
        log.info("RESPONSE DATA BANRED -> {}", sanitizedResponseData);

        try {
            List<Recibo> receipts = new ArrayList<>();

            responseData.getAdditionalData()
                    .getDetail()
                    .getPaymentObligation()
                    .getDocuments()
                    .forEach(document -> {
                        Recibo recibo = mapToReceiptV2(document, response, additionalData, responseData);
                        receipts.add(recibo);
                    });

            return new Recibos(receipts);
        } catch (Exception ex) {
            log.error("Ocurrió un error al mapear la data hacia el objeto RECIBO: {}", ex.getMessage());
            return null;
        }
    }

    private boolean validResponse(AdditionalDataPayment responseData) {
        return responseData != null && responseData.getAdditionalData() != null;
    }

    private boolean validTokenData(TokenData tokenData) {
        return tokenData != null && tokenData.getDocTrxDetalle() != null;
    }

    private Recibo mapToReceiptV1(DocTrxDetalle trxDetail) {
        Recibo receipt = new Recibo();
        receipt.setComprobante(trxDetail.getNumeroContrato());
        receipt.setConcepto(trxDetail.getNumDocIdentif());
        receipt.setTotalAPagar((BigDecimal) this.businessUtils.formatPaidAmountFromBanred(trxDetail.getTotalPendientePago()));
        receipt.setIdentificador(trxDetail.getNumDocIdentif());
        receipt.setFecha(trxDetail.getFechaEmision());
        return receipt;
    }

    private Recibo mapToReceiptV2(AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.Document document, BillInquiryResponseV2 response, DatosAdicionales additionalData, AdditionalDataPayment responseData) {
        Recibo receipt = new Recibo();
        receipt.setComprobante(document.getNumber());
        receipt.setConcepto(document.getDocumentType());
        receipt.setCuota(document.getAmount());
        receipt.setDatosAdicionales(AdditionalDataUtils.getBanredAdditionalResponse(additionalData, responseData, document));
        receipt.setDato1(document.getReference1());
        receipt.setDato2(document.getReference2());
        receipt.setDividendo(response.getPartialPayment());
        receipt.setFecha(document.getDocumentDate());
        receipt.setFormaPago(null);
        receipt.setIdentificador(document.getDocumentType());
        receipt.setImpuesto(AdditionalDataUtils.getValueFromTaxes(document.getTaxes()));
        receipt.setInteres(CommonUtils.toBigDecimal(document.getInterest()));
        receipt.setInteresesPagados(null);
        receipt.setInteresesPendientes(CommonUtils.toBigDecimal(document.getInterest()));
        receipt.setNumeroPredial(document.getNumber());
        receipt.setPago(CommonUtils.toBigDecimal(document.getAmount()));
        receipt.setReferencia(Defaults.EMPTY);
        receipt.setSecuencia(response.getSwitchAuditNumber());
        receipt.setTipoProceso(null);
        receipt.setTotalAPagar(CommonUtils.toBigDecimal(document.getAmount()));
        receipt.setValor(CommonUtils.toBigDecimal(document.getAmount()));

        return receipt;
    }
}
