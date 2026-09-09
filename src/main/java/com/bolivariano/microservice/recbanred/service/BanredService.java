package com.bolivariano.microservice.recbanred.service;

import com.bolivariano.microservice.recbanred.core.configuration.BanredConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.CompanyConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.MigCompanyConfiguration;
import com.bolivariano.microservice.recbanred.core.constants.Defaults;
import com.bolivariano.microservice.recbanred.core.enums.TipoFlujo;
import com.bolivariano.microservice.recbanred.core.enums.TipoReverso;
import com.bolivariano.microservice.recbanred.core.enums.TipoVersion;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;
import com.bolivariano.microservice.recbanred.core.payloads.input.banred.v1.*;
import com.bolivariano.microservice.recbanred.core.payloads.input.banred.v2.BillPaymentReversalRqV2;
import com.bolivariano.microservice.recbanred.core.payloads.input.banred.v2.BillPaymentRqV2;
import com.bolivariano.microservice.recbanred.core.payloads.input.banred.v2.*;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaConsultarDeuda;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaEjecutarPago;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaEjecutarReverso;
import com.bolivariano.microservice.recbanred.service.soap.BanredWebClient;
import com.bolivariano.microservice.recbanred.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;

import static com.bolivariano.microservice.recbanred.core.constants.Labels.*;
import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.*;

@Service
public class BanredService {

    private static final Logger log = LoggerFactory.getLogger(BanredService.class);

    private final BanredConfiguration banredConfig;
    private final CompanyConfiguration companyConfig;
    private final MigCompanyConfiguration migCompanyConfig;
    private final BusinessUtils businessUtils;
    private final BanredWebClient webClient;
    private final CompressorUtils compressorUtils;
    private final AdditionalDataUtils addDataUtils;

    public BanredService(BanredConfiguration banredConfig,
                         CompanyConfiguration companyConfig,
                         MigCompanyConfiguration migCompanyConfig,
                         BusinessUtils businessUtils,
                         BanredWebClient webClient,
                         CompressorUtils compressorUtils,
                         AdditionalDataUtils addDataUtils) {
        this.banredConfig = banredConfig;
        this.companyConfig = companyConfig;
        this.migCompanyConfig = migCompanyConfig;
        this.businessUtils = businessUtils;
        this.webClient = webClient;
        this.compressorUtils = compressorUtils;
        this.addDataUtils = addDataUtils;
    }

    /**
     * REALIZA LA CONSULTA DE LA DEUDA HACIA BANRED, TRANSFORMANDO LA DATA DE ENTRADA
     * EN FORMATO XML Y CONSTRUYENDO LA MISMA
     *
     * @param inquiryRq - El mensaje de entrada para la consulta de la deuda
     * @return Object - Objeto de respuesta de banred
     */
    public Mono<Object> executeInquiry(MensajeEntradaConsultarDeuda inquiryRq) {
        return Mono.defer(() -> {
            try {
                DatosAdicionales additionalData = inquiryRq.getServicio().getDatosAdicionales();
                String companyCode = AdditionalDataUtils.getValueAdditionalData(additionalData, E_BAND_AUTORIZADOR);
                TipoVersion versionType = this.businessUtils.getVersionByEnterprise(companyCode);
                CompanyConfiguration.VersionConfig versionConfig = this.companyConfig.getVersions().getVersionForType(versionType);
                Object request = null;

                if (TipoVersion.V2.equals(versionType))
                    request = BillInquiryRqV2.builder().billInquiryRequest(prepareInquiryRequestV2(inquiryRq, additionalData, versionConfig))
                            .build();

                if (TipoVersion.V1.equals(versionType))
                     request = BillInquiryRqV1.builder().billInquiryRequest(prepareInquiryRequestV1(inquiryRq, additionalData, versionConfig))
                             .build();

                return this.webClient.invokeSOAP(request, versionType, companyCode);
            } catch (Exception ex) {
                log.error("ERROR EJECUTANDO CONSULTA: {}", ex.getMessage());
                return Mono.error(new CustomException("ERROR AL EJECUTAR CONSULTA: " + ex.getMessage(), ex, INQUIRY_ERROR));
            }
        });
    }

    private BillInquiryRequestV2 prepareInquiryRequestV2(MensajeEntradaConsultarDeuda inquiryRq, DatosAdicionales additionalData, CompanyConfiguration.VersionConfig versionConfig) throws CustomException {
        return BillInquiryRequestV2.builder()
                .channel(this.businessUtils.getChannel(inquiryRq.getCanal()))
                .primaryAcctNumber(this.banredConfig.getPrimaryAcctNumber())
                .txCode(this.banredConfig.getTxCodeConsulta())
                .acquirerAuditNumber(inquiryRq.getSecuencial())
                .transactionTime(CommonUtils.formatDate(new Date(), versionConfig.getFormatTime()))
                .transactionDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .acquirerInstitutionID(this.banredConfig.getAcquirerInstitutionID())
                .track2(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, TRACK_2, Defaults.EMPTY))
                .terminalID(AdditionalDataUtils.getValueAdditionalData(additionalData, E_TERM))
                .currencyCode(this.banredConfig.getCurrencyCode())
                .billReference(inquiryRq.getServicio().getIdentificador())
                .billServiceCode(Integer.parseInt(CommonUtils.getValueOrDefault(inquiryRq.getServicio().getCodigoConvenio(), BigDecimal.ZERO.toString())))
                .billCompanyCode(Integer.parseInt(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_BAND_AUTORIZADOR, BigDecimal.ZERO.toString())))
                .medio(this.banredConfig.getMedio())
                .branchId(this.banredConfig.getBranchId())
                .aditionalData(this.businessUtils.prepareAdditionalData(additionalData, TipoFlujo.CONSULTA, TipoVersion.V2, null))
                .build();
    }

    private BillInquiryRequestV1 prepareInquiryRequestV1(MensajeEntradaConsultarDeuda inquiryRq, DatosAdicionales additionalData, CompanyConfiguration.VersionConfig versionConfig) throws CustomException {
        return BillInquiryRequestV1.builder()
                .channel(this.businessUtils.getChannel(inquiryRq.getCanal()))
                .primaryAcctNumber(this.banredConfig.getPrimaryAcctNumber())
                .txCode(this.banredConfig.getTxCodeConsulta())
                .acquirerAuditNumber(inquiryRq.getSecuencial())
                .transactionTime(CommonUtils.formatDate(new Date(), versionConfig.getFormatTime()))
                .transactionDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .businessDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .captureDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .posEntryMode(this.banredConfig.getPosEntryMode())
                .cardSequenceNumber(this.businessUtils.getCardSequenceNumberByChannel(inquiryRq.getCanal()))
                .acquirerInstitutionID(this.banredConfig.getAcquirerInstitutionID())
                .track2(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, TRACK_2, Defaults.EMPTY))
                .retrievalReferenceNumber(inquiryRq.getSecuencial())
                .terminalNumber(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_TERM, Defaults.TERMINALID))
                .cardAcceptorIdCode(this.banredConfig.getAccpIdTellerCode())
                .cardAcceptorNameLoc(this.banredConfig.getCardAcceptorNameLoc())
                .currencyCode(this.banredConfig.getCurrencyCode())
                .terminalData(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_TERM, Defaults.TERMINALID))
                .receivingInstitutionIdCode(this.banredConfig.getReceivingInstitutionIdCode())
                .accountId1(this.banredConfig.getAccountId1())
                .tellerCode(this.banredConfig.getAccpIdTellerCode())
                .billReference(inquiryRq.getServicio().getIdentificador())
                .billServiceCode(Integer.parseInt(CommonUtils.getValueOrDefault(inquiryRq.getServicio().getCodigoConvenio(), BigDecimal.ZERO.toString())))
                .billCompanyCode(Integer.parseInt(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_BAND_AUTORIZADOR, BigDecimal.ZERO.toString())))
                .routingData(this.banredConfig.getRoutingData())
                .inputData(this.banredConfig.getInputDataInqPayment().concat(StringUtils.SPACE).concat(this.banredConfig.getAccountId1()))
                .branchCode(this.banredConfig.getBranchId())
                .tokenData(this.businessUtils.prepareAdditionalData(additionalData, TipoFlujo.CONSULTA, TipoVersion.V1, null))
                .build();
    }

    /**
     * REALIZA EL PAGO DE LA DEUDA HACIA BANRED, TRANSFORMANDO LA DATA DE ENTRADA
     * EN FORMATO XML Y CONSTRUYENDO LA MISMA
     *
     * @param paymentRq - El mensaje de entrada para el pago de la deuda
     * @return Object - Objeto de respuesta de banred
     */
    public Mono<Object> executePayment(MensajeEntradaEjecutarPago paymentRq) {
        return Mono.defer(() -> {
            try {
                DatosAdicionales additionalData = paymentRq.getServicio().getDatosAdicionales();
                String companyCode = AdditionalDataUtils.getValueAdditionalData(additionalData, E_BAND_AUTORIZADOR);
                TipoVersion versionType = this.businessUtils.getVersionByEnterprise(companyCode);
                CompanyConfiguration.VersionConfig versionConfig = this.companyConfig.getVersions().getVersionForType(versionType);
                Object request = null;

                if (TipoVersion.V2.equals(versionType))
                    request = BillPaymentRqV2.builder().billPaymentRequest(preparePaymentRequestV2(paymentRq, additionalData, versionConfig))
                            .build();

                if (TipoVersion.V1.equals(versionType))
                    request = BillPaymentRqV1.builder().billPaymentRequest(preparePaymentRequestV1(paymentRq, additionalData, versionConfig))
                            .build();

                return this.webClient.invokeSOAP(request, versionType, companyCode);
            } catch (Exception ex) {
                log.error("ERROR EJECUTANDO PAGO: {}", ex.getMessage());
                return Mono.error(new CustomException("ERROR AL EJECUTAR PAGO " + ex.getMessage(), ex, PAYMENT_ERROR));
            }
        });
    }

    private BillPaymentRequestV2 preparePaymentRequestV2(MensajeEntradaEjecutarPago paymentRq, DatosAdicionales additionalData, CompanyConfiguration.VersionConfig versionConfig) throws CustomException {

        return BillPaymentRequestV2.builder()
                .channel(this.businessUtils.getChannel(paymentRq.getCanal()))
                .primaryAcctNumber(this.banredConfig.getPrimaryAcctNumber())
                .txCode(this.businessUtils.validateAndReturnTxCode(paymentRq.getCuenta(), paymentRq.getTipoCuenta()))
                .acquirerAuditNumber(paymentRq.getSecuencial())
                .transactionTime(CommonUtils.formatDate(new Date(), versionConfig.getFormatTime()))
                .transactionDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .acquirerInstitutionID(this.banredConfig.getAcquirerInstitutionID())
                .track2(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, TRACK_2, Defaults.EMPTY))
                .terminalID(AdditionalDataUtils.getValueAdditionalData(additionalData, E_TERM))
                .currencyCode(this.banredConfig.getCurrencyCode())
                .billReference(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_CONCEPTO, paymentRq.getServicio().getIdentificador()))
                .billServiceCode(Integer.parseInt(CommonUtils.getValueOrDefault(paymentRq.getServicio().getCodigoConvenio(), String.valueOf(Defaults.BILL_SERV_CODE))))
                .billCompanyCode(Integer.parseInt(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_BAND_AUTORIZADOR, BigDecimal.ZERO.toString())))
                .medio(this.banredConfig.getMedio())
                .branchId(this.banredConfig.getBranchId())
                .accountId1(StringUtils.isNotEmpty(paymentRq.getCuenta()) ? paymentRq.getCuenta() : this.banredConfig.getAccountId1())
                .amount(this.businessUtils.formatPaidAmountToBanred(paymentRq.getValorPago()))
                .formaPago(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, P_FORMA_PAGO, Defaults.FORMA_PAGO))
                .indicatorReversal(this.businessUtils.returnIndicatorReversal(!Defaults.REVERSAR))
                .trazabilidad(CommonUtils.getValueOrDefault(paymentRq.getSecuencial(), Defaults.TRAZABILIDAD_UNICIDAD))
                .unicidad(CommonUtils.getValueOrDefault(paymentRq.getSecuencial(), Defaults.TRAZABILIDAD_UNICIDAD))
                .aditionalData(this.businessUtils.prepareAdditionalData(additionalData, TipoFlujo.PAGO, TipoVersion.V2, null))
                .build();
    }

    private BillPaymentRequestV1 preparePaymentRequestV1(MensajeEntradaEjecutarPago paymentRq, DatosAdicionales additionalData, CompanyConfiguration.VersionConfig versionConfig) throws CustomException {

        return BillPaymentRequestV1.builder()
                .channel(this.businessUtils.getChannel(paymentRq.getCanal()))
                .primaryAcctNumber(this.banredConfig.getPrimaryAcctNumber())
                .txCode(this.businessUtils.validateAndReturnTxCode(paymentRq.getCuenta(), paymentRq.getTipoCuenta()))
                .amount(this.businessUtils.formatPaidAmountToBanred(paymentRq.getValorPago()))
                .acquirerAuditNumber(paymentRq.getSecuencial())
                .transactionTime(AdditionalDataUtils.getValueAdditionalData(additionalData, E_HORA))
                .transactionDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .businessDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .captureDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .posEntryMode(this.banredConfig.getPosEntryMode())
                .cardSequenceNumber(this.businessUtils.getCardSequenceNumberByChannel(paymentRq.getCanal()))
                .acquirerInstitutionID(this.banredConfig.getAcquirerInstitutionID())
                .track2(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, TRACK_2, Defaults.EMPTY))
                .retrievalReferenceNumber(paymentRq.getSecuencial())
                .terminalNumber(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_TERM, Defaults.TERMINALID))
                .cardAcceptorIdCode(this.banredConfig.getAccpIdTellerCode())
                .cardAcceptorNameLoc(this.banredConfig.getCardAcceptorNameLoc())
                .currencyCode(this.banredConfig.getCurrencyCode())
                .terminalData(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_TERM, Defaults.TERMINALID))
                .receivingInstitutionIdCode(this.banredConfig.getReceivingInstitutionIdCode())
                .accountId1(StringUtils.isNotEmpty(paymentRq.getCuenta()) ? paymentRq.getCuenta() : this.banredConfig.getAccountId1())
                .financialAccount(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_FINANCIAL_CODE, this.banredConfig.getAccountId1()))
                .billReference(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_CONCEPTO, paymentRq.getServicio().getIdentificador()))
                .billServiceCode(Integer.parseInt(CommonUtils.getValueOrDefault(paymentRq.getServicio().getCodigoConvenio(), String.valueOf(Defaults.BILL_SERV_CODE))))
                .billCompanyCode(Integer.parseInt(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_BAND_AUTORIZADOR, BigDecimal.ZERO.toString())))
                .routingData(this.banredConfig.getRoutingData())
                .inputData(this.banredConfig.getInputDataInqPayment().concat(StringUtils.SPACE).concat(this.banredConfig.getAccountId1()))
                .branchCode(this.banredConfig.getBranchId())
                .tokenData(this.businessUtils.prepareAdditionalData(additionalData, TipoFlujo.PAGO, TipoVersion.V1, paymentRq))
                .build();
    }


    /**
     * REALIZA EL REVERSO (MANUAL Y/O AUTOMATICO) DE LA DEUDA HACIA BANRED, TRANSFORMANDO LA DATA DE ENTRADA
     * EN FORMATO XML Y CONSTRUYENDO LA MISMA
     *
     * @param reversalRq - El mensaje de entrada para el reverso del pago
     * @return Object - Objeto de respuesta de banred
     */
    public Mono<Object> executeReversal(MensajeEntradaEjecutarReverso reversalRq) {
        return Mono.defer(() -> {
            try {
                DatosAdicionales additionalData = reversalRq.getServicio().getDatosAdicionales();
                String companyCode = AdditionalDataUtils.getValueAdditionalData(additionalData, E_BAND_AUTORIZADOR);
                TipoVersion versionType = this.businessUtils.getVersionByEnterprise(companyCode);
                CompanyConfiguration.VersionConfig versionConfig = this.companyConfig.getVersions().getVersionForType(versionType);
                Object request = this.getRequestExecuteReversal(reversalRq, versionType, versionConfig);

                return this.webClient.invokeSOAP(request, versionType, companyCode);
            } catch (Exception ex) {
                log.error("ERROR EJECUTANDO REVERSO: {}", ex.getMessage());
                return Mono.error(new CustomException("ERROR AL EJECUTAR REVERSO: " + ex.getMessage(), ex, REVERSAL_ERROR));
            }
        });
    }

    private Object getRequestExecuteReversal(MensajeEntradaEjecutarReverso reversalRq, TipoVersion versionType, CompanyConfiguration.VersionConfig versionConfig) throws CustomException {
        DatosAdicionales additionalData = reversalRq.getServicio().getDatosAdicionales();

        return switch (TipoReverso.valueOf(AdditionalDataUtils.getValueAdditionalData(additionalData, E_REVERSO))) {
            case M -> {
                log.info("EMPRESA APLICA PARA REVERSO MANUAL");
                yield buildReversalRequest(versionType, reversalRq, additionalData, versionConfig);
            }
            case A -> {
                log.info("EMPRESA APLICA PARA REVERSO AUTOMATICO");
                yield buildReversalRequest(versionType, reversalRq, additionalData, versionConfig);
            }
            default -> {
                log.error("TIPO DE REVERSO NO DEFINIDO O NO SOPORTADO");
                throw new CustomException("TIPO DE REVERSO NO DEFINIDO", null, REVERSAL_ERROR);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T buildReversalRequest(TipoVersion versionType, MensajeEntradaEjecutarReverso reversalRq, DatosAdicionales additionalData, CompanyConfiguration.VersionConfig versionConfig) throws CustomException {
        return switch (versionType) {
            case V2 -> (T) BillPaymentReversalRqV2.builder()
                    .billPaymentReversalRequest(prepareReversalRequestV2(reversalRq, additionalData, versionConfig))
                    .build();
            case V1 -> (T) BillPaymentReversalRqV1.builder()
                    .billPaymentReversalRequest(prepareReversalRequestV1(reversalRq, additionalData))
                    .build();
            default -> {
                log.error("VERSION NO SOPORTADA {}", versionType);
                throw new CustomException("TIPO DE VERSION NO SOPORTADO", null, INTERNAL_ERROR);
            }
        };
    }

    private BillPaymentReversalRequestV2 prepareReversalRequestV2(MensajeEntradaEjecutarReverso reversalRq, DatosAdicionales additionalData, CompanyConfiguration.VersionConfig versionConfig) throws CustomException {
        boolean isAutomaticReversal = this.businessUtils.isAutomaticReversal(additionalData);

        String companyCode = AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_BAND_AUTORIZADOR, Defaults.EMPTY);
        boolean useTransactionTimeFromRequest = this.migCompanyConfig.getCompanyMigratesList().contains(companyCode);
        String transactionTime = useTransactionTimeFromRequest
                ? AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_HORA, CommonUtils.formatDate(new Date(), versionConfig.getFormatTime()))
                : CommonUtils.formatDate(new Date(), versionConfig.getFormatTime());

        return BillPaymentReversalRequestV2.builder()
                .channel(this.businessUtils.getChannel(reversalRq.getCanal()))
                .primaryAcctNumber(this.banredConfig.getPrimaryAcctNumber())
                .txCode(this.businessUtils.validateAndReturnTxCode(reversalRq.getCuenta(), reversalRq.getTipoCuenta()))
                .acquirerAuditNumber(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_SSN_CORR, reversalRq.getSecuencial()))
                .acquirerInstitutionID(this.banredConfig.getAcquirerInstitutionID())
                .transactionTime(transactionTime)
                .transactionDate(CommonUtils.formatDate(new Date(), versionConfig.getFormatDate()))
                .track2(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, TRACK_2, Defaults.EMPTY))
                .terminalID(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_TERM, Defaults.TERMINALID))
                .currencyCode(this.banredConfig.getCurrencyCode())
                .billReference(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_CONCEPTO, reversalRq.getServicio().getIdentificador()))
                .billServiceCode(Integer.parseInt(CommonUtils.getValueOrDefault(reversalRq.getServicio().getCodigoConvenio(), String.valueOf(Defaults.BILL_SERV_CODE))))
                .billCompanyCode(Integer.parseInt(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_BAND_AUTORIZADOR, BigDecimal.ZERO.toString())))
                .switchAuditNumber(returnReference2VpsFromVersioning(isAutomaticReversal, additionalData, SWITCH_AUDIT_NUMBER, TipoVersion.V2))
                .sequenceAcquire(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_SEQUENCE_ACQUIRE, Defaults.EMPTY))
                .billerCutoverDate(returnReference2VpsFromVersioning(isAutomaticReversal, additionalData, CUTOVER_DATE, TipoVersion.V2))
                .medio(this.banredConfig.getMedio())
                .branchId(this.banredConfig.getBranchId())
                .accountId1(StringUtils.isNotEmpty(reversalRq.getCuenta()) ? reversalRq.getCuenta() : this.banredConfig.getAccountId1())
                .amount(this.businessUtils.formatPaidAmountToBanred(reversalRq.getValorPago()))
                .formaPago(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, P_FORMA_PAGO, Defaults.FORMA_PAGO))
                .indicatorReversal(this.businessUtils.returnIndicatorReversal(Defaults.REVERSAR))
                .trazabilidad(CommonUtils.getValueOrDefault(reversalRq.getSecuencial(), Defaults.TRAZABILIDAD_UNICIDAD))
                .unicidad(CommonUtils.getValueOrDefault(reversalRq.getSecuencial(), Defaults.TRAZABILIDAD_UNICIDAD))
                .aditionalData(this.businessUtils.prepareAdditionalData(additionalData, TipoFlujo.REVERSO, TipoVersion.V2, reversalRq))
                .build();
    }

    private BillPaymentReversalRequestV1 prepareReversalRequestV1(MensajeEntradaEjecutarReverso reversalRq, DatosAdicionales additionalData) throws CustomException {
        boolean isAutomaticReversal = this.businessUtils.isAutomaticReversal(additionalData);
        String timePayment = this.returnReference2VpsFromVersioning(isAutomaticReversal, additionalData, E_HORA, TipoVersion.V1);
        String datePayment = this.returnReference2VpsFromVersioning(isAutomaticReversal, additionalData, FECHA, TipoVersion.V1).substring(4); //05082024 - LL: se quita el año con substring

        return BillPaymentReversalRequestV1.builder()
                .channel(this.businessUtils.getChannel(reversalRq.getCanal()))
                .primaryAcctNumber(this.banredConfig.getPrimaryAcctNumber())
                .txCode(this.businessUtils.validateAndReturnTxCode(reversalRq.getCuenta(), reversalRq.getTipoCuenta()))
                .amount(this.businessUtils.formatPaidAmountToBanred(reversalRq.getValorPago()))
                .acquirerAuditNumber(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_SSN_CORR, reversalRq.getSecuencial()))
                .transactionTime(timePayment)
                .transactionDate(datePayment)
                .businessDate(datePayment)
                .captureDate(datePayment)
                .posEntryMode(this.banredConfig.getPosEntryMode())
                .cardSequenceNumber(this.businessUtils.getCardSequenceNumberByChannel(reversalRq.getCanal()))
                .acquirerInstitutionID(this.banredConfig.getAcquirerInstitutionID())
                .track2(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, TRACK_2, Defaults.EMPTY))
                .retrievalReferenceNumber(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_SSN_CORR, reversalRq.getSecuencial()))
                .terminalNumber(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_TERM, Defaults.TERMINALID))
                .cardAcceptorIdCode(this.banredConfig.getAccpIdTellerCode())
                .cardAcceptorNameLoc(this.banredConfig.getCardAcceptorNameLoc())
                .currencyCode(this.banredConfig.getCurrencyCode())
                .terminalData(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_TERM, Defaults.TERMINALID))
                .receivingInstitutionIdCode(this.banredConfig.getReceivingInstitutionIdCode())
                .accountId1(StringUtils.isNotEmpty(reversalRq.getCuenta()) ? reversalRq.getCuenta() : this.banredConfig.getAccountId1())
                .financialAccount(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_FINANCIAL_CODE, this.banredConfig.getAccountId1()))
                .billReference(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_CONCEPTO, reversalRq.getServicio().getIdentificador()))
                .billServiceCode(Integer.parseInt(CommonUtils.getValueOrDefault(reversalRq.getServicio().getCodigoConvenio(), String.valueOf(Defaults.BILL_SERV_CODE))))
                .billCompanyCode(Integer.parseInt(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_BAND_AUTORIZADOR, BigDecimal.ZERO.toString())))
                .routingData(this.banredConfig.getRoutingData())
                .inputData(this.banredConfig.getInputDataReversal().concat(StringUtils.SPACE).concat(this.compressorUtils.decryptAuthCodeForV1(this.businessUtils.resolveAuthCodeForReversal(additionalData))))
                .branchCode(this.banredConfig.getBranchId())
                .reversalIndicator(this.businessUtils.returnIndicatorReversal(Defaults.REVERSAR))
                .originalData(AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, E_SSN_CORR, reversalRq.getSecuencial()))
                .tokenData(this.businessUtils.prepareAdditionalData(additionalData, TipoFlujo.REVERSO, TipoVersion.V1, reversalRq))
                .build();
    }

    private String returnReference2VpsFromVersioning(boolean isAutomatic, DatosAdicionales datosAdicionales, String key, TipoVersion tipoVersion) {
        if (isAutomatic)
            return AdditionalDataUtils.getAdditionalDataOrDefault(datosAdicionales, key, Defaults.EMPTY);

        return switch (tipoVersion) {
            case V1 -> this.addDataUtils.takeDateTimeFromReference(datosAdicionales, key);
            case V2 -> AdditionalDataUtils.getCutoverDateAuditNumber(datosAdicionales, key);
            default -> Defaults.EMPTY;
        };
    }
}