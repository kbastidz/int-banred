package com.bolivariano.microservice.recbanred.service.banred.v3;

import com.bolivariano.microservice.recbanred.core.configuration.BanredConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.BillerV3Configuration;
import com.bolivariano.microservice.recbanred.core.configuration.CompanyConfiguration;

import com.bolivariano.microservice.recbanred.core.enums.banred.SubServicioMungye;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoBiller;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoOperacionToken;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatoAdicional;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaConsultarDeuda;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaEjecutarPago;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaEjecutarReverso;
import com.bolivariano.microservice.recbanred.core.payloads.input.banred.v3.BillInquiryRequestV3;
import com.bolivariano.microservice.recbanred.core.payloads.input.banred.v3.BillPaymentReversalRequestV3;
import com.bolivariano.microservice.recbanred.core.payloads.input.banred.v3.BillPaymentRequestV3;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.token.EspecificacionToken;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.helper.DatosComplementarios;
import com.bolivariano.microservice.recbanred.util.AdditionalDataUtils;
import com.bolivariano.microservice.recbanred.util.BusinessUtils;
import com.bolivariano.microservice.recbanred.util.CommonUtils;
import com.bolivariano.microservice.recbanred.util.banred.v3.BillerResolver;
import com.bolivariano.microservice.recbanred.util.banred.v3.TokenFieldEngine;
import com.bolivariano.microservice.recbanred.util.banred.v3.TokenSpecRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.bolivariano.microservice.recbanred.core.constants.Labels.E_BAND_AUTORIZADOR;
import static com.bolivariano.microservice.recbanred.core.constants.Labels.E_COD_OPERADOR;


/**
 * Construye los requests SOAP V3 (Consulta, Pago, Reverso) para billers de
 * trama fija (CNEL, MEER, MUNGYE), delegando el armado del InputData al
 * {@link TokenFieldEngine} + {@link TokenSpecRegistry}.
 *
 * CONVENCION para pasar valores especificos del negocio al token (ej. DNI,
 * AÑO, SEMESTRE, PROCESS_TYPE en MUNGYE): se leen tal cual desde
 * datosAdicionales del request de entrada, donde el "codigo" de cada
 * DatoAdicional debe coincidir EXACTAMENTE con el nombre de campo declarado
 * en TokenSpecRegistry (ej. enviar un DatoAdicional con codigo="DNI" para
 * que se inserte en el campo "DNI" del token de MUNGYE-Mercados/Milote).
 * Esto evita tener que crear un mapeador distinto por cada uno de los 3
 * billers x 4 sub-servicios x 3 operaciones.
 */
@Service
public class BanredServiceV3 {

    private final BanredConfiguration banredConfig;
    private final CompanyConfiguration companyConfig;
    private final BillerV3Configuration billerV3Config;
    private final BusinessUtils businessUtils;
    private final BillerResolver billerResolver;
    private final TokenSpecRegistry tokenSpecRegistry;
    private final TokenFieldEngine tokenFieldEngine;
    private final DatosComplementarios datosComplementarios;

    public BanredServiceV3(BanredConfiguration banredConfig,
                            CompanyConfiguration companyConfig,
                            BillerV3Configuration billerV3Config,
                            BusinessUtils businessUtils,
                            BillerResolver billerResolver,
                            TokenSpecRegistry tokenSpecRegistry,
                            TokenFieldEngine tokenFieldEngine,
                            DatosComplementarios datosComplementarios) {
        this.banredConfig = banredConfig;
        this.companyConfig = companyConfig;
        this.billerV3Config = billerV3Config;
        this.businessUtils = businessUtils;
        this.billerResolver = billerResolver;
        this.tokenSpecRegistry = tokenSpecRegistry;
        this.tokenFieldEngine = tokenFieldEngine;
        this.datosComplementarios = datosComplementarios;
    }

    // ==================================================================
    // INQUIRY
    // ==================================================================

    public BillInquiryRequestV3 prepareInquiryRequestV3(MensajeEntradaConsultarDeuda inquiryRq,
                                                          DatosAdicionales additionalData,
                                                          CompanyConfiguration.VersionConfig versionConfig) throws CustomException {
        String companyCode = AdditionalDataUtils.getValueAdditionalData(additionalData, E_BAND_AUTORIZADOR);
        TipoBiller biller = billerResolver.resolverBiller(companyCode);
        int billServiceCode = Integer.parseInt(CommonUtils.getValueOrDefault(
                inquiryRq.getServicio().getCodigoConvenio(), BigDecimal.ZERO.toString()));
        SubServicioMungye sub = biller == TipoBiller.MUNGYE ? billerResolver.resolverSubServicioMungye(billServiceCode) : null;

        EspecificacionToken specQ0 = tokenSpecRegistry.getQ0(biller, sub, TipoOperacionToken.INQUIRY);
        Map<String, String> valores = construirValoresBase(additionalData, biller, sub);
        String inputData = tokenFieldEngine.buildQ0(specQ0, valores);

        return BillInquiryRequestV3.builder()
                .channel(this.businessUtils.getChannel(inquiryRq.getCanal()))
                .primaryAcctNumber(datosComplementarios.getPrimaryAcctNumber(companyCode))
                .txCode(this.banredConfig.getTxCodeConsulta())
                .acquirerAuditNumber(inquiryRq.getSecuencial())
                .transactionTime(CommonUtils.formatDate(new Date(), versionConfig.getFormatTime()))
                .transactionDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .businessDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .captureDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .posEntryMode(datosComplementarios.getPosEntryMode(companyCode))
                .cardSequenceNumber(this.businessUtils.getCardSequenceNumberByChannel(inquiryRq.getCanal()))
                .acquirerInstitutionID(this.banredConfig.getAcquirerInstitutionID())
                .track2(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, "track2", StringUtils.EMPTY))
                .retrievalReferenceNumber(inquiryRq.getSecuencial())
                .terminalNumber(datosComplementarios.getAccpIdTellerCode(companyCode))
                .cardAcceptorIdCode(this.banredConfig.getAccpIdTellerCode())
                .cardAcceptorNameLoc(datosComplementarios.getCardAcceptorNameLoc(companyCode))
                .currencyCode(this.banredConfig.getCurrencyCode())
                .terminalData(datosComplementarios.getAccpIdTellerCode(companyCode))
                .receivingInstitutionIdCode(datosComplementarios.getReceivingInstitutionIdCode(companyCode))
                .accountId1(this.banredConfig.getAccountId1())
                .tellerCode(this.banredConfig.getAccpIdTellerCode())
                .billReference(inquiryRq.getServicio().getIdentificador())
                .billServiceCode(billServiceCode)
                .billCompanyCode(Integer.parseInt(companyCode))
                .routingData(this.banredConfig.getRoutingData())
                .inputData(inputData)
                .branchCode(datosComplementarios.getBranchId(companyCode))
                .build();
    }

    // ==================================================================
    // PAYMENT
    // ==================================================================

    public BillPaymentRequestV3 preparePaymentRequestV3(MensajeEntradaEjecutarPago paymentRq,
                                                          DatosAdicionales additionalData,
                                                          CompanyConfiguration.VersionConfig versionConfig) throws CustomException {
        String companyCode = AdditionalDataUtils.getValueAdditionalData(additionalData, E_BAND_AUTORIZADOR);
        TipoBiller biller = billerResolver.resolverBiller(companyCode);
        int billServiceCode = Integer.parseInt(CommonUtils.getValueOrDefault(
                paymentRq.getServicio().getCodigoConvenio(), BigDecimal.ZERO.toString()));
        SubServicioMungye sub = biller == TipoBiller.MUNGYE ? billerResolver.resolverSubServicioMungye(billServiceCode) : null;

        EspecificacionToken specQ0 = tokenSpecRegistry.getQ0(biller, sub, TipoOperacionToken.PAYMENT);
        Map<String, String> valores = construirValoresBase(additionalData, biller, sub);
        valores.put("VALOR_PAGADO", paymentRq.getValorPago() == null ? "0" : paymentRq.getValorPago().toPlainString());
        String inputData = tokenFieldEngine.buildQ0(specQ0, valores);

        String formatDate = datosComplementarios.getFormatDatePago(companyCode) != null
                ? datosComplementarios.getFormatDatePago(companyCode)
                : versionConfig.getFormatDate();
        String date = CommonUtils.formatDate(new Date(), formatDate);

        return BillPaymentRequestV3.builder()
                .channel(this.businessUtils.getChannel(paymentRq.getCanal()))
                .primaryAcctNumber(datosComplementarios.getPrimaryAcctNumberPago(companyCode))
                .txCode(this.businessUtils.validateAndReturnTxCode(paymentRq.getCuenta(), paymentRq.getTipoCuenta()))
                .amount(this.businessUtils.formatPaidAmountToBanred(paymentRq.getValorPago()))
                .acquirerAuditNumber(paymentRq.getSecuencial())
                .transactionTime(CommonUtils.formatDate(new Date(), versionConfig.getFormatTime()))
                .transactionDate(date)
                .businessDate(date)
                .captureDate(date)
                .posEntryMode(this.banredConfig.getPosEntryMode())
                .cardSequenceNumber(this.businessUtils.getCardSequenceNumberByChannel(paymentRq.getCanal()))
                .acquirerInstitutionID(this.banredConfig.getAcquirerInstitutionID())
                .track2(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, "track2", StringUtils.EMPTY))
                .retrievalReferenceNumber(paymentRq.getSecuencial())
                .terminalNumber(datosComplementarios.getAccpIdTellerCode(companyCode))
                .cardAcceptorIdCode(this.banredConfig.getAccpIdTellerCode())
                .cardAcceptorNameLoc(datosComplementarios.getCardAcceptorNameLoc(companyCode))
                .currencyCode(this.banredConfig.getCurrencyCode())
                .terminalData(datosComplementarios.getAccpIdTellerCode(companyCode))
                .receivingInstitutionIdCode(datosComplementarios.getReceivingInstitutionIdCode(companyCode))
                .accountId1(StringUtils.isNotEmpty(paymentRq.getCuenta()) ? paymentRq.getCuenta() : this.banredConfig.getAccountId1())
                .financialAccount(datosComplementarios.getAccountId1Gye(companyCode))
                .billReference(paymentRq.getServicio().getIdentificador())
                .billServiceCode(billServiceCode)
                .billCompanyCode(Integer.parseInt(companyCode))
                .routingData(this.banredConfig.getRoutingData())
                .inputData(inputData)
                .branchCode(datosComplementarios.getBranchId(companyCode))
                .build();
    }

    // ==================================================================
    // REVERSAL
    // ==================================================================

    public BillPaymentReversalRequestV3 prepareReversalRequestV3(MensajeEntradaEjecutarReverso reversalRq,
                                                                   DatosAdicionales additionalData,
                                                                   CompanyConfiguration.VersionConfig versionConfig) throws CustomException {
        String companyCode = AdditionalDataUtils.getValueAdditionalData(additionalData, E_BAND_AUTORIZADOR);
        TipoBiller biller = billerResolver.resolverBiller(companyCode);
        int billServiceCode = Integer.parseInt(CommonUtils.getValueOrDefault(
                reversalRq.getServicio().getCodigoConvenio(), BigDecimal.ZERO.toString()));
        SubServicioMungye sub = biller == TipoBiller.MUNGYE ? billerResolver.resolverSubServicioMungye(billServiceCode) : null;

        EspecificacionToken specQ0 = tokenSpecRegistry.getQ0(biller, sub, TipoOperacionToken.REVERSAL);
        Map<String, String> valores = construirValoresBase(additionalData, biller, sub);
        valores.put("VALOR_PAGADO", reversalRq.getValorPago() == null ? "0" : reversalRq.getValorPago().toPlainString());
        String inputData = tokenFieldEngine.buildQ0(specQ0, valores);

        return BillPaymentReversalRequestV3.builder()
                .channel(this.businessUtils.getChannel(reversalRq.getCanal()))
                .primaryAcctNumber(datosComplementarios.getPrimaryAcctNumber(companyCode))
                .txCode(this.businessUtils.validateAndReturnTxCode(reversalRq.getCuenta(), reversalRq.getTipoCuenta()))
                .amount(this.businessUtils.formatPaidAmountToBanred(reversalRq.getValorPago()))
                .acquirerAuditNumber(reversalRq.getSecuencial())
                .transactionTime(CommonUtils.formatDate(new Date(), versionConfig.getFormatTime()))
                .transactionDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .businessDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .captureDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .posEntryMode(datosComplementarios.getPosEntryMode(companyCode))
                .cardSequenceNumber(this.businessUtils.getCardSequenceNumberByChannel(reversalRq.getCanal()))
                .acquirerInstitutionID(this.banredConfig.getAcquirerInstitutionID())
                .track2(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, "track2", StringUtils.EMPTY))
                .retrievalReferenceNumber(reversalRq.getSecuencial())
                .terminalNumber(datosComplementarios.getAccpIdTellerCode(companyCode))
                .cardAcceptorIdCode(this.banredConfig.getAccpIdTellerCode())
                .cardAcceptorNameLoc(datosComplementarios.getCardAcceptorNameLoc(companyCode))
                .currencyCode(this.banredConfig.getCurrencyCode())
                .terminalData(datosComplementarios.getAccpIdTellerCode(companyCode))
                .receivingInstitutionIdCode(datosComplementarios.getReceivingInstitutionIdCode(companyCode))
                .accountId1(StringUtils.isNotEmpty(reversalRq.getCuenta()) ? reversalRq.getCuenta() : this.banredConfig.getAccountId1())
                .financialAccount(this.banredConfig.getAccountId1())
                .billReference(reversalRq.getServicio().getIdentificador())
                .billServiceCode(billServiceCode)
                .billCompanyCode(Integer.parseInt(companyCode))
                .routingData(this.banredConfig.getRoutingData())
                .inputData(inputData)
                .branchCode(datosComplementarios.getBranchId(companyCode))
                .reversalIndicator(this.businessUtils.returnIndicatorReversal(true))
                .originalData(reversalRq.getSecuencial())
                .build();
    }


    private Map<String, String> construirValoresBase(DatosAdicionales additionalData, TipoBiller biller, SubServicioMungye sub) {
        Map<String, String> valores = new HashMap<>();

        BillerV3Configuration.InstitucionConfig institucion = switch (biller) {
            case CNEL -> billerV3Config.getCnel();
            case MEER -> billerV3Config.getMeer();
            case MUNGYE -> billerV3Config.getMungye();
        };
        String codOperador = AdditionalDataUtils.getValueAdditionalData(additionalData, E_COD_OPERADOR);
        if (institucion != null) {
            valores.put("COD_INSTITUCION_FINANCIERA", institucion.getCodInstitucionFinanciera());
            valores.put("ABA_INSTITUCION", institucion.getCodInstitucionFinanciera());
            valores.put("COD_OPERADOR", codOperador);
        }

        // Convencion: cualquier DatoAdicional cuyo "codigo" coincida con el nombre
        // de un campo del token (ej. DNI, ANIO, SEMESTRE, COLLECTION_ID...) se
        // inserta directamente. Ver javadoc de la clase.
        if (additionalData != null && additionalData.getDatoAdicional() != null) {
            for (DatoAdicional dato : additionalData.getDatoAdicional()) {
                if (StringUtils.isNotEmpty(dato.getCodigo()))
                    valores.put(dato.getCodigo(), dato.getValor());
            }
        }

        return valores;
    }
}
