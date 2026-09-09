package com.bolivariano.microservice.recbanred.util;

import com.bolivariano.microservice.recbanred.core.constants.Defaults;
import com.bolivariano.microservice.recbanred.core.constants.Labels;
import com.bolivariano.microservice.recbanred.core.payloads.input.*;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.*;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillPaymentResponseV1;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class TokenDataUtils {

    private final CompressorUtils compressorUtils;
    private final AdditionalDataUtils addDataUtils;

    public TokenDataUtils(CompressorUtils compressorUtils,
                          AdditionalDataUtils addDataUtils) {
        this.compressorUtils = compressorUtils;
        this.addDataUtils = addDataUtils;
    }

    public DatosAdicionales getValueTokenDataResponse(TokenData outputTokenData, Object inputMessageDetail, BillPaymentResponseV1 billPaymentResponseV1) {
        if (Objects.isNull(outputTokenData))
            return null;

        List<DatoAdicional> listTokenData = new ArrayList<>();
        //20250807 - LL: SE MODIFICA PARA OBTENER LOS VALORES DE RETENCION, BASE Y SECUENCIAL AUT
        if (inputMessageDetail instanceof MensajeEntradaConsultarDeuda inquiry) {
            listTokenData.add(new DatoAdicional(Labels.CANAL, inquiry.getCanal()));
            listTokenData.addAll(AdditionalDataUtils.getBanredTokenDataResponse(inquiry.getServicio().getDatosAdicionales(), outputTokenData));
        }
        //20250812 - LL: Se mapea el e_hora para guardar en el pago y reverso
        String timeAux = Defaults.EMPTY;
        if (inputMessageDetail instanceof MensajeEntradaEjecutarPago payment) {
            listTokenData.add(new DatoAdicional(Labels.CANAL, payment.getCanal()));
            timeAux = AdditionalDataUtils.getValueAdditionalData(payment.getServicio().getDatosAdicionales(), Labels.E_HORA);
        }
        if (inputMessageDetail instanceof MensajeEntradaEjecutarReverso reversal)
            listTokenData.add(new DatoAdicional(Labels.CANAL, reversal.getCanal()));

        try {
            //agrega los datos de la cabecera
            retrieveTokenDataHeader(outputTokenData.getDatosCabecera(), listTokenData);

            if (outputTokenData.getDocTrxDetalle() != null)
                tokenDataInquiry(outputTokenData.getDocTrxDetalle(), listTokenData);
            if (outputTokenData.getDocPagoRespuesta() != null)
                tokenDataPayment(outputTokenData, listTokenData, billPaymentResponseV1, timeAux); //08022025 - LL: Se cambia logica para obtener fecha y hora de PAGO
            if (outputTokenData.getDocReversoRespuesta() != null)
                tokenDataReversal(outputTokenData.getDocReversoRespuesta(), listTokenData);

        } catch (Exception e) {
            return null;
        }
        return new DatosAdicionales(listTokenData);
    }

    private static void retrieveTokenDataHeader(DatosCabecera headerData, List<DatoAdicional> lTokenData) {
        if(headerData == null)
            return;
        lTokenData.add(new DatoAdicional(Labels.E_AGENCIA, headerData.getCodAgencia()));
        lTokenData.add(new DatoAdicional(Labels.E_LOCALIDAD, headerData.getCodLocalidad()));
        lTokenData.add(new DatoAdicional(Labels.E_COD_INSTITUCION, headerData.getCodigoInstitucion()));
        lTokenData.add(new DatoAdicional(Labels.E_COD_OPERADOR, headerData.getCodigoOperador()));
        lTokenData.add(new DatoAdicional(Labels.FECHA, headerData.getFecha()));
        lTokenData.add(new DatoAdicional(Labels.HORA, headerData.getHora()));
    }

    private static void tokenDataInquiry(List<DocTrxDetalle> lDocTrxDetail, List<DatoAdicional> lTokenData) {
        for (DocTrxDetalle docDetail : lDocTrxDetail) {
            lTokenData.add(new DatoAdicional(Labels.NUMERO_CONTRATO, docDetail.getNumeroContrato()));
            lTokenData.add(new DatoAdicional(Labels.TOTAL_PENDIENTE_PAGO, docDetail.getTotalPendientePago()));
            lTokenData.add(new DatoAdicional(Labels.FECHA_EMISION, docDetail.getFechaEmision()));
            lTokenData.add(new DatoAdicional(Labels.FECHA_VENCIMIENTO, docDetail.getFechaVencimiento()));
            lTokenData.add(new DatoAdicional(Labels.DEUDA_ANTERIOR, docDetail.getDeudaAnterior()));
            lTokenData.add(new DatoAdicional(Labels.NUM_DOC_IDENTIF, docDetail.getNumDocIdentif()));
        }
    }

    private void tokenDataPayment(TokenData tokenData, List<DatoAdicional> ltokenData, BillPaymentResponseV1 responseV1, String trxTime) {
        DocPagoRespuesta docPago = tokenData.getDocPagoRespuesta();
        //3006025 - LL: Se encripta el codigo de autorizacion para version 1 BANRED
        ltokenData.add(new DatoAdicional(Labels.BILLER_AUTH_CODE, this.compressorUtils.encryptAuthCodeForV1(docPago.getCodigoAutorizacion())));
        ltokenData.add(new DatoAdicional(Labels.E_COD_RESPUESTA, this.compressorUtils.encryptAuthCodeForV1(docPago.getCodigoAutorizacion())));
        ltokenData.add(new DatoAdicional(Labels.TOTAL_PENDIENTE_PAGO, docPago.getTotalPendientePago()));
        ltokenData.add(new DatoAdicional(Labels.FECHA_EMISION, docPago.getFechaEmision()));
        ltokenData.add(new DatoAdicional(Labels.FECHA_VENCIMIENTO, docPago.getFechaVencimiento()));
        ltokenData.add(new DatoAdicional(Labels.DEUDA_ANTERIOR, docPago.getDeudaAnterior()));
        ltokenData.add(new DatoAdicional(Labels.VALOR_PAGADO, docPago.getValorPagado()));
        ltokenData.add(new DatoAdicional(Labels.NUM_DOC_IDENTIF, docPago.getNumDocIdentif()));
        ltokenData.add(new DatoAdicional(Labels.NOMBRE_CLIENTE, docPago.getNombreCliente()));
        ltokenData.add(new DatoAdicional(Labels.NUM_DOC_IDENTIF2, docPago.getNumDocIdentif2()));
        ltokenData.add(new DatoAdicional(Labels.NOMBRE_CLIENTE2, docPago.getNombreCliente2()));
        ltokenData.add(new DatoAdicional(Labels.NUMERO_FACTURA, docPago.getNumeroFactura()));
        //08072025 - LL: Agregar los campos de e_factura
        this.addDataUtils.mapCommonFields(ltokenData, responseV1, tokenData, trxTime);
    }

    private void tokenDataReversal(DocReversoRespuesta docReversal, List<DatoAdicional> lTokenData) {
        lTokenData.add(new DatoAdicional(Labels.NUMERO_FACTURA, docReversal.getNumeroFactura()));
        lTokenData.add(new DatoAdicional(Labels.FECHA_EMISION, docReversal.getFechaEmision()));
        lTokenData.add(new DatoAdicional(Labels.VALOR_PAGADO, docReversal.getValorPagado()));
        lTokenData.add(new DatoAdicional(Labels.NUM_DOC_IDENTIF, docReversal.getNumDocIdentif()));
        lTokenData.add(new DatoAdicional(Labels.NUM_DOC_IDENTIF2, docReversal.getNumDocIdentif2()));
        lTokenData.add(new DatoAdicional(Labels.NOMBRE_CLIENTE2, docReversal.getNombreCliente2()));
        lTokenData.add(new DatoAdicional(Labels.SECUENCIAL_AUT, docReversal.getSecuencialAut()));
        lTokenData.add(new DatoAdicional(Labels.BASE, docReversal.getBase()));
    }

}
