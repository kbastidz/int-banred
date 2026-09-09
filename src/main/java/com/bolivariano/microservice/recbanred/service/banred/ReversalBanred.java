package com.bolivariano.microservice.recbanred.service.banred;

import com.bolivariano.microservice.recbanred.core.constants.Defaults;
import com.bolivariano.microservice.recbanred.core.constants.Labels;
import com.bolivariano.microservice.recbanred.core.enums.TipoReverso;
import com.bolivariano.microservice.recbanred.core.payloads.output.AdditionalDataReversal;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaEjecutarReverso;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaEjecutarPago;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaProcesar;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.TokenData;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillPaymentReversalResponseV1;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillPaymentReversalRsV1;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.BillPaymentReversalResponseV2;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.BillPaymentReversalRsV2;
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

    public ReversalBanred(BusinessUtils businessUtils,
                          BanredService banredService,
                          TokenDataUtils tokenDataUtils,
                          AdditionalDataUtils addDataUtils) {
        this.businessUtils = businessUtils;
        this.banredService = banredService;
        this.tokenDataUtils = tokenDataUtils;
        this.addDataUtils = addDataUtils;
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
        } else {
            throw new IllegalArgumentException("Tipo de respuesta desconocido: " + paymentRs.getClass());
        }
    }
}
