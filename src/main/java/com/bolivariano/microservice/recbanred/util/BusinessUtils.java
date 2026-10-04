package com.bolivariano.microservice.recbanred.util;

import com.bolivariano.microservice.recbanred.core.configuration.AutoReversalConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.BanredConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.CompanyConfiguration;
import com.bolivariano.microservice.recbanred.core.constants.Defaults;
import com.bolivariano.microservice.recbanred.core.enums.TipoFlujo;
import com.bolivariano.microservice.recbanred.core.enums.TipoReverso;
import com.bolivariano.microservice.recbanred.core.enums.TipoVersion;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import com.bolivariano.microservice.recbanred.core.payloads.input.*;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillInquiryRsV1;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillPaymentReversalRsV1;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillPaymentRsV1;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.BillInquiryRsV2;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.BillPaymentReversalRsV2;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.BillPaymentRsV2;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.BillInquiryRsV3;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.BillPaymentReversalRsV3;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.BillPaymentRsV3;
import com.bolivariano.microservice.recbanred.util.banred.v3.BillerResolver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.bolivariano.microservice.recbanred.core.constants.Labels.*;
import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.*;

@Component
public class BusinessUtils {

    private static final Logger log = LoggerFactory.getLogger(BusinessUtils.class);
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final Map<TipoVersion, Map<String, Class<?>>> responseClassMap;
    private final BanredConfiguration banredConfig;
    private final CompanyConfiguration companyConfig;
    private final CompressorUtils compressorUtils;
    private final AdditionalDataUtils addDataUtils;
    private final AutoReversalConfiguration autoReversalConfig;
    private final BillerResolver billerResolver;

    public BusinessUtils(BanredConfiguration banredConfig,
                         CompanyConfiguration companyConfig,
                         CompressorUtils compressorUtils,
                         AdditionalDataUtils addDataUtils,
                         AutoReversalConfiguration autoReversalConfig,
                         BillerResolver billerResolver) {
        this.banredConfig = banredConfig;
        this.companyConfig = companyConfig;
        this.responseClassMap = this.createResponseClassMap();
        this.compressorUtils = compressorUtils;
        this.addDataUtils = addDataUtils;
        this.autoReversalConfig = autoReversalConfig;
        this.billerResolver = billerResolver;
    }

    public Long formatPaidAmountToBanred(BigDecimal amount) {
        if (Objects.isNull(amount))
            return BigDecimal.ZERO.longValue();

        return amount.multiply(new BigDecimal(100)).longValue();
    }

    public Object formatPaidAmountFromBanred(Object amount) {
        if (Objects.isNull(amount))
            return BigDecimal.ZERO;

        if (amount instanceof String valueString) {
            boolean isNegative = valueString.contains("-");
            valueString = valueString.replace("-", "");

            BigDecimal decimal = new BigDecimal(valueString).divide(new BigDecimal(100));
            return isNegative ? decimal.negate() : decimal;
        }

        if (amount instanceof BigDecimal amountBD)
            return amountBD.divide(new BigDecimal(100));

        return BigDecimal.ZERO;

    }

    public String returnIndicatorReversal(Boolean reversar) {
        return Boolean.TRUE.equals(reversar) ? "1" : "0";
    }

    public Boolean returnOfflineFlag(String standin) {
        if (StringUtils.isEmpty(standin))
            return Boolean.FALSE;

        return BooleanUtils.toBoolean(Integer.parseInt(standin));
    }

    public String transformCutoverDate(String cutoverDate) {
        if (StringUtils.isEmpty(cutoverDate)) {
            log.warn("No se obtuvo información sobre la fecha de vencimiento");
            return Defaults.EMPTY;
        }

        try {
            LocalDateTime cutoverDateTime = this.parseCutoverDate(cutoverDate);
            return this.formatFullDate(cutoverDateTime);
        } catch (DateTimeParseException e) {
            log.error("Error al parsear la fecha de vencimiento (SE TOMARA LA DEL SISTEMA) [{}]: {}", cutoverDate, e.getMessage());
            return this.formatFullDate(LocalDateTime.now());
        }
    }

    private LocalDateTime parseCutoverDate(String input) {
        return switch (input.length()) {
            case 8 -> parseV2Date(input);
            case 4 -> parseV1Date(input);
            default -> {
                log.error("FECHA DE VENCIMIENTO NO VÁLIDA. SE TOMARÁ LA FECHA DEL SISTEMA!");
                yield LocalDateTime.now();
            }
        };
    }

    private LocalDateTime parseV2Date(String input) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Defaults.DATE_FORMAT_V2); // "ddMMyyyy"
        return LocalDate.parse(input, formatter).atStartOfDay();
    }

    private LocalDateTime parseV1Date(String input) {
        String fullDate = Year.now().toString().concat(input); // yyyyMMdd
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Defaults.DATE_FORMAT_V1); // "yyyyMMdd"
        return LocalDate.parse(fullDate, formatter).atStartOfDay();
    }

    private String formatFullDate(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern(Defaults.FULLDATE_FORMAT));
    }

    public int getChannel(String channelName) {

        if (channelName.equals(this.banredConfig.getNombreCanalBancaMovil()))
            return this.banredConfig.getCodigoCanalBancaMovil();

        if (channelName.equals(this.banredConfig.getNombreCanalSAT()))
            return this.banredConfig.getCodigoCanalSAT();

        if (channelName.equals(this.banredConfig.getNombreCanalVentanilla()))
            return this.banredConfig.getCodigoCanalVentanilla();

        if (channelName.equals(this.banredConfig.getNombreCanalBancaVirtual()))
            return this.banredConfig.getCodigoCanalBancaVirtual();

        if (channelName.equals(this.banredConfig.getNombreCanalCNB()))
            return this.banredConfig.getCodigoCanalCNB();

        return -1;
    }

    public boolean isAutomaticReversal(DatosAdicionales datosAdicionales) {
        TipoReverso reversalType = TipoReverso.valueOf(AdditionalDataUtils.getValueAdditionalData(datosAdicionales, E_REVERSO));
        return TipoReverso.A.equals(reversalType);
    }

    public String validateAndReturnTxCode(String account, String accountType) {
        if (StringUtils.isEmpty(account) && StringUtils.isEmpty(accountType))
            return this.banredConfig.getTxCodePagoEfectivo();
        return this.banredConfig.getTxCodePagoDebito();
    }

    public String getErrorCode(String resultCode) {
        if (StringUtils.isEmpty(resultCode)) {
            log.error("Codigo de resultado no obtenido, por favor indicar al proveedor.");
            return Defaults.EMPTY;
        }

        try {
            int numericCode = Integer.parseInt(resultCode);
            if (numericCode == 0) {
                resultCode = "0";
            }
        } catch (NumberFormatException e) {
            // Si no es numérico, validamos por texto
            if (resultCode.equals(Defaults.SUCCESS_BANRED)) {
                resultCode = "0";
            }
        }
        return resultCode;
    }

    public String prepareAdditionalData(DatosAdicionales datosAdicionales, TipoFlujo fluxType, TipoVersion versionType, Object objectReq) throws CustomException {

        if (TipoVersion.V2.equals(versionType)) {
            // Trama personalizada de reverso V2 para tipos configurados por empresa (A/M).
            if (TipoFlujo.REVERSO.equals(fluxType)
                    && objectReq instanceof MensajeEntradaEjecutarReverso reversalRq) {
                String reversalType = AdditionalDataUtils.getValueAdditionalData(datosAdicionales, E_REVERSO);
                String codigoEmpresa = reversalRq.getServicio().getCodigoEmpresa();
                Optional<AutoReversalConfiguration.CompanyReversalConfig> companysConfig =
                        autoReversalConfig.findByCompanyCode(codigoEmpresa);

                if (companysConfig.isPresent() && companysConfig.get().supportsReversalType(reversalType)) {
                    log.info("APLICANDO TRAMA PERSONALIZADA REVERSO V2 PARA EMPRESA: {}, TIPO_REVERSO: {}",
                            Encode.forJava(codigoEmpresa), Encode.forJava(reversalType));
                    return prepareAdditionalDataV2AutoReversal(datosAdicionales, companysConfig.get());
                }
            }
            return this.prepareAdditionalDataV2(datosAdicionales, fluxType);
        } else {
            return this.prepareAdditionalDataV1(datosAdicionales, fluxType, objectReq);
        }
    }

    /**
     * Prepara los datos adicionales para la version Payload No. 2 de Banred
     * APLICA PARA EMPRESAS QUE SOLAMENTE MANEJEN ESTA VERSION DE PAYLOAD (LA MAYORIA DE COMERCIOS HASTA EL MOMENTO: 11062025 LL)
     *
     * @param additionalData - Referencia a los datos adicionales dentro del request extraido del microservicio CONSUMIDOR
     * @param fluxType        - Identifica el tipo de flujo del proceso (CONSULTA, PAGO, REVERSO)
     * @return String - Un JsonString definido para la etiqueta AdditionalData
     */
    public String prepareAdditionalDataV2(DatosAdicionales additionalData, TipoFlujo fluxType) throws CustomException {
        try {
            List<KeyValue> keyValues = AdditionalDataUtils.getBanredAdditionalDataRequest(additionalData);
            if (fluxType.equals(TipoFlujo.REVERSO))
                keyValues.add(new KeyValue(BILLER_AUTH_CODE, this.resolveAuthCodeForReversal(additionalData)));

            String[] labelsReq = this.getLabelsForAdditionalData(fluxType).split(",");

            //Usar LinkedHashMap para mapear el orden
            LinkedHashMap<String, Object> detailData = new LinkedHashMap<>();

            if (fluxType.equals(TipoFlujo.PAGO)) {
                detailData.put(labelsReq[1], keyValues);
                detailData.put(PAYMENTS, AdditionalDataUtils.getDataPayments(additionalData));
                detailData.put(INFO_PERSON, AdditionalDataUtils.getDataInfoPerson(additionalData));
            }

            if (fluxType.equals(TipoFlujo.REVERSO)) {
                detailData.put(REVERSALS, AdditionalDataUtils.getDataReversals(additionalData));
                detailData.put(labelsReq[1], keyValues);
            }

            if (fluxType.equals(TipoFlujo.CONSULTA))
                detailData.put(labelsReq[1], keyValues);

            // Armado jerárquico de la estructura con LinkedHashMap
            Map<String, Object> headerMap = Map.of(DETAIL_DATA, detailData);
            Map<String, Object> flujoMap = Map.of(HEADER, headerMap);
            Map<String, Object> detailMap = Map.of(labelsReq[0], flujoMap);
            Map<String, Object> additionalDataMap = Map.of(DETAIL, detailMap);
            Map<String, Object> root = Map.of(ADDITIONAL_DATA, additionalDataMap);

            // Serializar respetando orden

            return gson.toJson(root);

        } catch (Exception ex) {
            throw new CustomException("Error construyendo JSON de data adicional: " + ex.getMessage(), null, "-1");
        }
    }

    /**
     * Prepara la trama personalizada de datos adicionales V2 para empresas en auto-reversal-config
     * cuando el flujo de REVERSO coincide con el tipo configurado por empresa (A/M).
     *
     * Estructura generada:
     * additionalData/detail/reversalObligation/header/detailData/reversals + infoReversal
     *
     * Los campos y su orden en infoReversal se leen desde el properties por empresa.
     * Los valores se resuelven desde banred_datoadicional (mapeo nombreSalida/codigoEntrada),
     * excepto billerAuthorizationCode que usa resolveAuthCodeForReversal (lógica estándar).
     */
    private String prepareAdditionalDataV2AutoReversal(DatosAdicionales additionalData,
            AutoReversalConfiguration.CompanyReversalConfig companyConfig) throws CustomException {
        try {
            // Construir mapa de resolución nombreCampoSalida -> valor
            // a partir del campo banred_datoadicional del request
            Map<String, String> valueMap = new LinkedHashMap<>();
            List<KeyValue> allKeyValues = AdditionalDataUtils.getBanredAdditionalDataRequest(additionalData);
            for (KeyValue kv : allKeyValues) {
                valueMap.put(kv.getName(), kv.getValue());
            }
            // billerAuthorizationCode usa la lógica estándar del código fuente
            valueMap.put(BILLER_AUTH_CODE, this.resolveAuthCodeForReversal(additionalData));

            // Armar infoReversal solo con los campos configurados, en el orden configurado
            List<LinkedHashMap<String, String>> infoReversal = new ArrayList<>();
            for (String fieldName : companyConfig.getInfoReversalFieldList()) {
                LinkedHashMap<String, String> item = new LinkedHashMap<>();
                item.put("name", fieldName);
                item.put("value", valueMap.getOrDefault(fieldName, Defaults.EMPTY));
                infoReversal.add(item);
            }

            // reversals se arma igual que el flujo normal de reverso
            List<Object> reversals = new ArrayList<>(AdditionalDataUtils.getDataReversals(additionalData));

            LinkedHashMap<String, Object> detailData = new LinkedHashMap<>();
            detailData.put(REVERSALS, reversals);
            detailData.put(INFO_REVERSAL, infoReversal);

            LinkedHashMap<String, Object> headerMap = new LinkedHashMap<>();
            headerMap.put(DETAIL_DATA, detailData);

            LinkedHashMap<String, Object> reversalObligationMap = new LinkedHashMap<>();
            reversalObligationMap.put(HEADER, headerMap);

            LinkedHashMap<String, Object> detailMap = new LinkedHashMap<>();
            detailMap.put(REVERSALS_OBLIGATION, reversalObligationMap);

            LinkedHashMap<String, Object> additionalDataMap = new LinkedHashMap<>();
            additionalDataMap.put(DETAIL, detailMap);

            LinkedHashMap<String, Object> root = new LinkedHashMap<>();
            root.put(ADDITIONAL_DATA, additionalDataMap);

            return gson.toJson(root);

        } catch (Exception ex) {
            throw new CustomException("Error construyendo trama personalizada de reverso automático: "
                    + ex.getMessage(), null, "-1");
        }
    }

    /**
     * Prepara los datos adicionales para la version Payload No. 1 de Banred
     * APLICA PARA EMPRESAS QUE SOLAMENTE MANEJEN ESTA VERSION DE PAYLOAD (LOS COMERCIOS DE EMPRESA ELECTRICA: 11062025 - LL)
     *
     * @param additionalData - Referencia a los datos adicionales dentro del request extraido del microservicio CONSUMIDOR
     * @param fluxType       - Identifica el tipo de flujo del proceso (CONSULTA, PAGO, REVERSO)
     * @return String - Un JsonString definido para la etiqueta Tokenmata
     */
    public String prepareAdditionalDataV1(DatosAdicionales additionalData, TipoFlujo fluxType, Object objectReq) throws CustomException {
        try {
            LinkedHashMap<String, Object> root = new LinkedHashMap<>();
            String labelDoc = this.getLabelsForTokenData(fluxType);

            Map<String, String> headerData = this.addDataUtils.getBanredHeaderDataV1(additionalData, fluxType);
            Map<String, String> docsTrx = null;

            //Usar LinkedHashMap para mapear el orden
            root.put(DATOS_CABECERA, headerData);

            if (fluxType.equals(TipoFlujo.PAGO) || fluxType.equals(TipoFlujo.REVERSO))
                docsTrx = this.getBanredDocsTrx(additionalData, fluxType, objectReq);

            root.put(labelDoc, docsTrx);
            root.entrySet().removeIf(entry -> StringUtils.isEmpty(entry.getKey())); //20250813 - LL: LIMPIAR EL LHM PARA QUE NO SALGA EL OBJETO CON EL NOMBRE VACIO EN CONSULTA

            return gson.toJson(root);

        } catch (Exception ex) {
            throw new CustomException("Error construyendo JSON de data adicional: " + ex.getMessage(), null, "-1");
        }
    }

    private String getLabelsForAdditionalData(TipoFlujo fluxType) {
        return switch (fluxType) {
            case CONSULTA -> ADDITIONAL_INQUIRY;
            case PAGO -> ADDITIONAL_PAYMENT;
            case REVERSO -> ADDITIONAL_REVERSAL;
            default ->  {
                log.error("TIPO DE FLUJO NO DEFINIDO: {}", fluxType);
                yield Defaults.EMPTY;
            }
        };
    }

    private String getLabelsForTokenData(TipoFlujo fluxType) {
        return switch (fluxType) {
            case PAGO -> DOCTRX_PAYMENT;
            case REVERSO -> DOCTRX_REVERSAL;
            default -> Defaults.EMPTY;
        };
    }

    public Class<?> getResponseClass(Object requestObject, TipoVersion versionType) throws CustomException {
        if (requestObject == null) {
            log.error("No se obtuvo respuesta del servicio Banred");
            throw new CustomException(
                    "Sin respuesta del servicio Banred: No se recibió respuesta para transformar a la clase destino",
                    null, BANRED_UNAVAIABLE
            );
        }

        String className = requestObject.getClass().getSimpleName();
        Map<String, Class<?>> versionMap = responseClassMap.get(versionType);

        if (versionMap == null || !versionMap.containsKey(className)) {
            throw new CustomException(
                    "Sin clase de objeto respuesta (consulta, pago o reverso) para tipo versión: " + versionType,
                    null, INTERNAL_ERROR
            );
        }

        return versionMap.get(className);
    }

    private Map<TipoVersion, Map<String, Class<?>>> createResponseClassMap() {
        Map<TipoVersion, Map<String, Class<?>>> map = new EnumMap<>(TipoVersion.class);

        Map<String, Class<?>> v1Map = Map.of(
                "BillInquiryRqV1", BillInquiryRsV1.class,
                "BillPaymentRqV1", BillPaymentRsV1.class,
                "BillPaymentReversalRqV1", BillPaymentReversalRsV1.class
        );

        Map<String, Class<?>> v2Map = Map.of(
                "BillInquiryRqV2", BillInquiryRsV2.class,
                "BillPaymentRqV2", BillPaymentRsV2.class,
                "BillPaymentReversalRqV2", BillPaymentReversalRsV2.class
        );

        Map<String, Class<?>> v3Map = Map.of(
                "BillInquiryRqV3", BillInquiryRsV3.class,
                "BillPaymentRqV3", BillPaymentRsV3.class,
                "BillPaymentReversalRqV3", BillPaymentReversalRsV3.class
        );

        map.put(TipoVersion.V1, v1Map);
        map.put(TipoVersion.V2, v2Map);
        map.put(TipoVersion.V3, v3Map);

        return Collections.unmodifiableMap(map);
    }

    public String getDataLogs(MensajeEntradaProcesar inputMessage) {
        String resultado = "";

        if (Objects.isNull(inputMessage)) {
            log.error("No se puede procesar informacion para logs, entrada vacia.");
            return resultado;
        }

        if (inputMessage.getMensajeEntradaConsultarDeuda() != null &&
                StringUtils.isNotEmpty(inputMessage.getMensajeEntradaConsultarDeuda().getServicio().getCodigoEmpresa()) &&
                StringUtils.isNotEmpty(inputMessage.getMensajeEntradaConsultarDeuda().getSecuencial()))
            resultado = inputMessage.getMensajeEntradaConsultarDeuda().getServicio().getCodigoEmpresa().concat(",").concat(inputMessage.getMensajeEntradaConsultarDeuda().getSecuencial());

        if (inputMessage.getMensajeEntradaEjecutarPago() != null &&
                StringUtils.isNotEmpty(inputMessage.getMensajeEntradaEjecutarPago().getServicio().getCodigoEmpresa()) &&
                StringUtils.isNotEmpty(inputMessage.getMensajeEntradaEjecutarPago().getSecuencial()))
            resultado = inputMessage.getMensajeEntradaEjecutarPago().getServicio().getCodigoEmpresa().concat(",").concat(inputMessage.getMensajeEntradaEjecutarPago().getSecuencial());


        if (inputMessage.getMensajeEntradaEjecutarReverso() != null &&
                StringUtils.isNotEmpty(inputMessage.getMensajeEntradaEjecutarReverso().getServicio().getCodigoEmpresa()) &&
                StringUtils.isNotEmpty(inputMessage.getMensajeEntradaEjecutarReverso().getSecuencial()))
            resultado = inputMessage.getMensajeEntradaEjecutarReverso().getServicio().getCodigoEmpresa().concat(",").concat(inputMessage.getMensajeEntradaEjecutarReverso().getSecuencial());

        return resultado;
    }

    public boolean isTimeoutResultCode(String resultCode) {
        if (resultCode == null) return false;
        List<String> timeoutCodes = Arrays.asList(Defaults.TIMEOUT_CODES_BANRED.split(","));
        return timeoutCodes.contains(resultCode);
    }

    public boolean isCommerceErrorMessage(String errorMessage) {
        if (StringUtils.isEmpty(errorMessage)) return false;
        return this.banredConfig.getSystemErrorBanred().equals(errorMessage)
                || this.banredConfig.getNotPermitedHoursError().equals(errorMessage);
    }

    public TipoVersion getVersionByEnterprise(String companyCode) throws CustomException {
        if (StringUtils.isNotEmpty(companyCode)) {
            if (this.companyConfig.getValidEnterpriseV1().equalsIgnoreCase(companyCode))
                return TipoVersion.V1;
            // 09092026 - LL: Empresas de trama fija Q0/Q1 (CNEL, MEER, MUNGYE) usan V3
            if (this.billerResolver.esCompanyV3(companyCode))
                return TipoVersion.V3;
             else
                return TipoVersion.V2;
        } else {
            log.error("VERSION DE PAYLOAD DE BANRED, NO ENCONTRADA O AUN NO CONTEMPLADA PARA EL COMPONENTE INTEGRADOR, POR FAVOR INDICAR AL CUSTODIO DEL PROYECTO.");
            throw new CustomException("VERSION Payload NO SOPORTADA", null, VALIDATION_ERROR);
        }
    }

    public int getCardSequenceNumberByChannel(String channel) {
        if (channel.equals(this.banredConfig.getNombreCanalBancaMovil()))
            return this.banredConfig.getCardSequenceBancaMovil();

        if (channel.equals(this.banredConfig.getNombreCanalSAT()))
            return this.banredConfig.getCardSequenceSAT();

        if (channel.equals(this.banredConfig.getNombreCanalVentanilla()))
            return this.banredConfig.getCardSequenceVentanilla();

        if (channel.equals(this.banredConfig.getNombreCanalBancaVirtual()))
            return this.banredConfig.getCardSequenceBancaVirtual();

        if (channel.equals(this.banredConfig.getNombreCanalCNB()))
            return this.banredConfig.getCardSequenceCNB();

        return -1;
    }

    public Map<String, String> getBanredDocsTrx(DatosAdicionales addtionalData, TipoFlujo fluxType, Object objectReq) {
        LinkedHashMap<String, String> lhmDocsTrx = new LinkedHashMap<>();

        if (fluxType.equals(TipoFlujo.PAGO))
            this.getBanredDocsTrxPayment(addtionalData, lhmDocsTrx, objectReq);

        if (fluxType.equals(TipoFlujo.REVERSO))
            this.getBanredDocsTrxReversal(addtionalData, lhmDocsTrx);

        return lhmDocsTrx;
    }

    private void getBanredDocsTrxPayment(DatosAdicionales additionalData, LinkedHashMap<String, String> lhmDocsTrx, Object objectReq) {

        //20250807 - LL: Se modifica metodo para reutilizar objetos (en este caso siempre llega la Entrada del Pago
        MensajeEntradaEjecutarPago payment = (MensajeEntradaEjecutarPago) objectReq;

        lhmDocsTrx.put("Default", AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, "e_default", Defaults.TRAZABILIDAD_UNICIDAD));
        lhmDocsTrx.putAll(AdditionalDataUtils.getBanredTokenDataRequest(additionalData));
        lhmDocsTrx.put(NUM_DOC_IDENTIF, AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, DOCUMENTID, "1001996219"));
        lhmDocsTrx.put(NOMBRE_CLIENTE, payment.getNombreCliente());
        lhmDocsTrx.put(FECHA_EMISION, AdditionalDataUtils.getValueAdditionalData(additionalData, FECHA_EMISION));
        lhmDocsTrx.put(FECHA_VENCIMIENTO, AdditionalDataUtils.getValueAdditionalData(additionalData, FECHA_VENCIMIENTO));
        lhmDocsTrx.put(NUMERO_FACTURA, AdditionalDataUtils.getValueAdditionalData(additionalData, NUMERO_FACTURA));
        lhmDocsTrx.put(TOTAL_PAGADO, CommonUtils.fromBigDecimal(payment.getValorPago()));
        lhmDocsTrx.put(NUM_DOC_IDENTIF2, Defaults.EMPTY);
        lhmDocsTrx.put(NOMBRE_CLIENTE2, Defaults.EMPTY);

    }

    private void getBanredDocsTrxReversal(DatosAdicionales additionalData, LinkedHashMap<String, String> lhmDocsTrx) {

        lhmDocsTrx.put(CODIGO_AUTORIZACION, this.compressorUtils.decryptAuthCodeForV1(this.resolveAuthCodeForReversal(additionalData)));
        lhmDocsTrx.put(SECUENCIA_AUT, this.addDataUtils.retrieveSeqRetBaseValuesFromBillKey(additionalData, SECUENCIA_AUT.toLowerCase()));
        lhmDocsTrx.put(RETENCION, CommonUtils.fillValue(this.addDataUtils.retrieveSeqRetBaseValuesFromBillKey(additionalData, RETENCION.toLowerCase())));
        lhmDocsTrx.put(BASE, CommonUtils.fillValue(this.addDataUtils.retrieveSeqRetBaseValuesFromBillKey(additionalData, BASE.toLowerCase())));
        lhmDocsTrx.put(NUM_DOC_IDENTIF, AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, DOCUMENTID, Defaults.TRAZABILIDAD_UNICIDAD));
        lhmDocsTrx.put(FECHA_EMISION, AdditionalDataUtils.getValueAdditionalData(additionalData, FECHA_EMISION));
        lhmDocsTrx.put(NUMERO_FACTURA, AdditionalDataUtils.getValueAdditionalData(additionalData, NUMERO_FACTURA));
        //20250807 - LL: SE AGREGA VALOR DE PAGO DEPENDIENDO SI ES UN REVERSO MANUAL O SI ES UN REVERSO AUTOMATICO
        lhmDocsTrx.put(VALOR_PAGADO, CommonUtils.fillValue(this.addDataUtils.retrieveSeqRetBaseValuesFromBillKey(additionalData, VALOR_PAGADO.toLowerCase())));

    }

    public String resolveAuthCodeForReversal(DatosAdicionales additionalData) {
        String reversalType = AdditionalDataUtils.getValueAdditionalData(additionalData, E_REVERSO);
        String responseCode = AdditionalDataUtils.getValueAdditionalData(additionalData, E_COD_RESPUESTA);

        String label = switch(TipoReverso.valueOf(reversalType)) {
            case M -> VPS_REFERENCIA1;
            case A -> BILLER_AUTH_CODE;
            default -> Defaults.EMPTY;
        };

        return AdditionalDataUtils.getAdditionalDataOrDefault(additionalData, label, responseCode);
    }
}
