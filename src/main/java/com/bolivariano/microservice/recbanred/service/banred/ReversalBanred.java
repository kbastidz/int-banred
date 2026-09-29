package com.bolivariano.microservice.recbanred.service.banred;

import com.bolivariano.microservice.recbanred.core.constants.Defaults;
import com.bolivariano.microservice.recbanred.core.constants.Labels;
import com.bolivariano.microservice.recbanred.core.enums.TipoReverso;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;
import com.bolivariano.microservice.recbanred.core.payloads.output.AdditionalDataReversal;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaEjecutarReverso;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaEjecutarPago;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaProcesar;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.TokenData;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillPaymentReversalResponseV1;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillPaymentReversalRsV1;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.BillPaymentReversalResponseV2;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.BillPaymentReversalRsV2;
import com.bolivariano.microservice.recbanred.core.enums.banred.SubServicioMungye;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoBiller;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoOperacionToken;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.BillPaymentReversalResponseV3;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.BillPaymentReversalRsV3;
import com.bolivariano.microservice.recbanred.service.banred.v3.TokenDataV3Utils;
import com.bolivariano.microservice.recbanred.service.banred.v4.IsoMunQuitoParser;
import com.bolivariano.microservice.recbanred.util.banred.v3.BillerResolver;
import com.bolivariano.microservice.recbanred.service.BanredService;
import com.bolivariano.microservice.recbanred.util.AdditionalDataUtils;
import com.bolivariano.microservice.recbanred.util.BusinessUtils;
import com.bolivariano.microservice.recbanred.util.CommonUtils;
import com.bolivariano.microservice.recbanred.util.TokenDataUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;

@Service
public class ReversalBanred {

    private static final Logger log = LoggerFactory.getLogger(ReversalBanred.class);

    private final BusinessUtils businessUtils;
    private final TokenDataUtils tokenDataUtils;
    private final BanredService banredService;
    private final AdditionalDataUtils addDataUtils;
    private final TokenDataV3Utils tokenDataV3Utils;
    private final BillerResolver billerResolver;

    public ReversalBanred(BusinessUtils businessUtils,
                          BanredService banredService,
                          TokenDataUtils tokenDataUtils,
                          AdditionalDataUtils addDataUtils,
                          TokenDataV3Utils tokenDataV3Utils,
                          BillerResolver billerResolver) {
        this.businessUtils = businessUtils;
        this.banredService = banredService;
        this.tokenDataUtils = tokenDataUtils;
        this.addDataUtils = addDataUtils;
        this.tokenDataV3Utils = tokenDataV3Utils;
        this.billerResolver = billerResolver;
    }

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    /**
     * Procesa el reverso del pago y mapea la respuesta a un DTO de salida de pago de forma asincrona
     *
     * @param reversalRq - objeto de entrada de reverso para construccion XML
     * @return Mono<MensajeSalidaProcesar> - Objeto Salida asincrona del DTO Generico para el pago
     * */
    public Mono<MensajeSalidaProcesar> processReversal(MensajeEntradaEjecutarReverso reversalRq) {

        return banredService.executeReversal(reversalRq)
                .map(reversalRs -> {
                    MensajeSalidaEjecutarPago outputReversal = this.getOutputReversalForVersioning(reversalRs, reversalRq);
                    log.info("Salida REVERSO: {}", outputReversal);
                    if (AdditionalDataUtils.getValueAdditionalData(reversalRq.getServicio().getDatosAdicionales(), Labels.E_REVERSO).equals(TipoReverso.A.name()))
                        return new MensajeSalidaProcesar().successAutomaticReversal("REVERSO AUTOMATICO REALIZADO", outputReversal);

                    return new MensajeSalidaProcesar().successReversal(outputReversal);
                });
    }

    public MensajeSalidaEjecutarPago getOutputReversalForVersioning(Object paymentRs, MensajeEntradaEjecutarReverso reversalRq) {

        if (paymentRs instanceof BillPaymentReversalRsV2 rsV2) {
            BillPaymentReversalResponseV2 response = rsV2.getBillPaymentReversalResponse();
            AdditionalDataReversal responseData = gson.fromJson(response.getResponseData(), AdditionalDataReversal.class);
            String txCode = this.businessUtils.validateAndReturnTxCode(reversalRq.getCuenta(), reversalRq.getTipoCuenta());
            AdditionalDataUtils.injectTxCode(reversalRq.getServicio().getDatosAdicionales(), txCode);

            return MensajeSalidaEjecutarPago.builder()
                    .codigoError(this.businessUtils.getErrorCode(response.getResultCode()))
                    .fechaDebito(CommonUtils.formatDate(new Date(), Defaults.FULLDATE_FORMAT))
                    .fechaPago(CommonUtils.formatDate(new Date(), Defaults.FULLDATE_FORMAT))
                    .banderaOffline(this.businessUtils.returnOfflineFlag(response.getStandIN()))
                    .mensajeUsuario(response.getErrorMessage())
                    .mensajeSistema(null)
                    .montoTotal(null)
                    .referencia(AdditionalDataUtils.getValueResponseDataPayment(responseData, Labels.BILLER_AUTH_CODE))
                    .datosAdicionales(this.addDataUtils.getAdditionalDataResponse(responseData, response, null))
                    .build();

        }
        if (paymentRs instanceof BillPaymentReversalRsV3 rsV3) {
            BillPaymentReversalResponseV3 response = rsV3.getBillPaymentReversalResponse();
            DatosAdicionales datosAdicionales = this.parseResponseDataV3Safe(response.getResponseData(), reversalRq);

            return MensajeSalidaEjecutarPago.builder()
                    .codigoError(this.businessUtils.getErrorCode(response.getResultCode()))
                    .fechaDebito(CommonUtils.formatDate(new Date(), Defaults.FULLDATE_FORMAT))
                    .fechaPago(CommonUtils.formatDate(new Date(), Defaults.FULLDATE_FORMAT))
                    .banderaOffline(null)
                    .mensajeUsuario(response.getErrorMessage())
                    .mensajeSistema(null)
                    .montoTotal(null)
                    .referencia(TokenDataV3Utils.getValor(datosAdicionales, "COD_AUTORIZACION"))
                    .datosAdicionales(datosAdicionales)
                    .build();
        }
        if (paymentRs instanceof BillPaymentReversalRsV1 rsV1) {
            BillPaymentReversalResponseV1 response = rsV1.getBillPaymentReversalResponse();
            TokenData tokenData = gson.fromJson(response.getTokenData(), TokenData.class);

            return MensajeSalidaEjecutarPago.builder()
                    .codigoError(this.businessUtils.getErrorCode(response.getResultCode()))
                    .fechaDebito(CommonUtils.formatDate(new Date(), Defaults.FULLDATE_FORMAT))
                    .fechaPago(CommonUtils.formatDate(new Date(), Defaults.FULLDATE_FORMAT))
                    .banderaOffline(null)
                    .mensajeUsuario(response.getErrorMessage())
                    .mensajeSistema(null)
                    .montoTotal(null)
                    .referencia(AdditionalDataUtils.getValueTokenDataGeneric(tokenData, Labels.NUM_DOC_IDENTIF))
                    .datosAdicionales(this.tokenDataUtils.getValueTokenDataResponse(tokenData, reversalRq, null))
                    .build();
        }
        // V4 – Municipio de Quito: ISO 8583 TCP
        if (paymentRs instanceof IsoMunQuitoParser.RespuestaIso rsV4) {
            return mapV4Reverso(rsV4);
        } else {
            throw new IllegalArgumentException("Tipo de respuesta desconocido: " + paymentRs.getClass());
        }
    }

    /**
     * Mapea la respuesta ISO 8583 de reverso (V4 – Municipio de Quito) a MensajeSalidaEjecutarPago.
     * El reverso aprobado devuelve código resultado "00" y los datos de la tx original en Bit 56.
     */
    private MensajeSalidaEjecutarPago mapV4Reverso(IsoMunQuitoParser.RespuestaIso rsV4) {
        DatosAdicionales da = buildDatosAdicionalesV4Reverso(rsV4);
        return MensajeSalidaEjecutarPago.builder()
                .codigoError(rsV4.codigoResultado != null ? rsV4.codigoResultado : "")
                .fechaDebito(CommonUtils.formatDate(new Date(), Defaults.FULLDATE_FORMAT))
                .fechaPago(CommonUtils.formatDate(new Date(), Defaults.FULLDATE_FORMAT))
                .banderaOffline(null)
                .mensajeUsuario(null)
                .mensajeSistema(null)
                .montoTotal(null)
                .referencia(rsV4.secuencialBanred)
                .datosAdicionales(da)
                .build();
    }

    private DatosAdicionales buildDatosAdicionalesV4Reverso(IsoMunQuitoParser.RespuestaIso rs) {
        java.util.List<com.bolivariano.microservice.recbanred.core.payloads.input.DatoAdicional> lista =
                new java.util.ArrayList<>();
        addDa(lista, "codigo_resultado",       rs.codigoResultado);
        addDa(lista, "secuencial_banred",      rs.secuencialBanred);
        addDa(lista, "secuencial_adquirente",  rs.secuencialAdquirente);
        addDa(lista, "institucion_autorizadora", rs.institucionAutorizadora);
        addDa(lista, "fecha_local",            rs.fechaLocal);
        addDa(lista, "hora_local",             rs.horaLocal);
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

    private DatosAdicionales parseResponseDataV3Safe(String responseData, MensajeEntradaEjecutarReverso reversalRq) {
        try {
            String companyCode = AdditionalDataUtils.getValueAdditionalData(reversalRq.getServicio().getDatosAdicionales(), Labels.E_BAND_AUTORIZADOR);
            TipoBiller biller = this.billerResolver.resolverBiller(companyCode);
            int billServiceCode = CommonUtils.convertToInteger(reversalRq.getServicio().getCodigoConvenio());
            SubServicioMungye sub = biller == TipoBiller.MUNGYE ? this.billerResolver.resolverSubServicioMungye(billServiceCode) : null;
            return this.tokenDataV3Utils.parseResponseData(responseData, biller, sub, TipoOperacionToken.REVERSAL);
        } catch (Exception ex) {
            log.error("No se pudo decodificar ResponseData V3: {}", ex.getMessage());
            return null;
        }
    }
}
