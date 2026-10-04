package com.bolivariano.microservice.recbanred.util;

import com.bolivariano.microservice.recbanred.core.constants.Labels;
import com.bolivariano.microservice.recbanred.core.constants.Defaults;
import com.bolivariano.microservice.recbanred.core.enums.TipoFlujo;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import com.bolivariano.microservice.recbanred.core.payloads.AdditionalDataPayment;
import com.bolivariano.microservice.recbanred.core.payloads.input.*;
import com.bolivariano.microservice.recbanred.core.payloads.output.AdditionalDataReversal;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.*;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1.BillPaymentResponseV1;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2.BillPaymentResponseV2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.*;
import static com.bolivariano.microservice.recbanred.core.constants.Labels.*;

@Component
public class AdditionalDataUtils {

    private final CompressorUtils compressorUtils;

    public AdditionalDataUtils(CompressorUtils compressorUtils) {
        this.compressorUtils = compressorUtils;
    }


    /**
     * Inyecta el txCode en los DatosAdicionales del request para que esté disponible
     * al construir la respuesta de salida. Solo agrega la entrada si aún no existe.
     */
    public static void injectTxCode(DatosAdicionales additionalData, String txCode) {
        if (additionalData == null || additionalData.getDatoAdicional() == null) return;
        boolean exists = additionalData.getDatoAdicional().stream()
                .anyMatch(d -> Labels.TX_CODE.equals(d.getCodigo()));
        if (!exists) {
            additionalData.getDatoAdicional().add(new DatoAdicional(Labels.TX_CODE, padTxCode(txCode)));
        }
    }

    /**
     * Obtiene el valor del dato adicional de acuerdo al nombre del mismo
     * V2: Se valida si existe el nombre dentro del codigo, caso contrario envia VACIO (VALIDA DATOS ADICIONALES)
     *
     * @param additionalData - El objeto de los datos adicionales
     * @param name - El nombre de la etiqueta a la cual sacar el valor
     * @return String - El valor buscado
     */
    public static String getValueAdditionalData(DatosAdicionales additionalData, String name) {
        return additionalData.getDatoAdicional().stream()
                .filter(data -> StringUtils.isNotEmpty(data.getCodigo())
                        && data.getValor() != null //13052025 - LL: Se aplica esta validacion para evitar NullPointerExceptions
                        && data.getCodigo().equals(name))
                .map(DatoAdicional::getValor)
                .findFirst()
                .orElse(Defaults.EMPTY);
    }

    public static String getValueResponseDataInquiry(AdditionalDataPayment responseData, String key) {
        if (responseData == null || StringUtils.isEmpty(key)) {
            return Defaults.EMPTY;
        }

        AdditionalDataPayment.AdditionalData additionalData = responseData.getAdditionalData();
        AdditionalDataPayment.AdditionalData.Detail detail = additionalData != null ? additionalData.getDetail() : null;
        AdditionalDataPayment.AdditionalData.Detail.PaymentObligation paymentObligation = detail != null ? detail.getPaymentObligation() : null;

        if (paymentObligation == null) {
            return Defaults.EMPTY;
        }

        Map<String, Supplier<String>> valueMap = new HashMap<>();

        AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.Header header = paymentObligation.getHeader();
        if (header != null) {
            valueMap.put("category", header::getCategory);
            valueMap.put("idRubroBCE", header::getIdRubroBCE);
            valueMap.put("nameRubroBCE", header::getNameRubroBCE);
        }

        AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.InfoPerson infoPerson = paymentObligation.getInfoPerson();
        if (infoPerson != null) {
            valueMap.put(Labels.DOCUMENTTYPE, infoPerson::getDocumentType);
            valueMap.put("documentID", infoPerson::getDocumentID);
            valueMap.put("fullName", infoPerson::getFullName);
            valueMap.put("telephone", infoPerson::getTelephone);
            valueMap.put("email", infoPerson::getEmail);
        }

        if (paymentObligation.getDocuments() != null) {
            for (AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.Document doc : paymentObligation.getDocuments()) {
                valueMap.putIfAbsent(Labels.DOCUMENTTYPE, doc::getDocumentType);
                valueMap.putIfAbsent("number", doc::getNumber);
                valueMap.putIfAbsent("documentDate", doc::getDocumentDate);
                valueMap.putIfAbsent("amount", doc::getAmount);
            }
        }
        return valueMap.getOrDefault(key, () -> Defaults.EMPTY).get();
    }

    /**
     * Obtiene los datos adicionales para el mapeo de la data adicional
     * APLICA PARA LA RECURSIVIDAD Y MANEJO GENERICO POR CADA EMPRESA DE RECAUDOS PUBLICOS
     *
     * @param additionalData - La etiqueta de los datos adicionales para consulta pago o reverso
     * @return ArrayList<KeyValue> - La coleccion para la trama de entrada de datos adicionales de banred
     */
    public static List<KeyValue> getBanredAdditionalDataRequest(DatosAdicionales additionalData) {
        //Se separan por coma los pares de valores
        //El primer valor antes del /, indica el dato adicional que hay que enviar a banred en datos adicionales
        //El valor que va despues del /, indica que valor tomar del dato adicional del mensaje de la cola
        //Ex. "codLocalidad/e_localidad,codUbicacion/e_ubi,tipoSuministro/tipoSuministro"

        String stringValues = getValueOrDefault(additionalData, "banred_datoadicional");
        if (StringUtils.isEmpty(stringValues))
            return new ArrayList<>();

        List<KeyValue> keyValue = new ArrayList<>();

        Arrays.stream(stringValues.split(","))
                .map(pair -> pair.split("/"))
                .map(internalPair -> new KeyValue(internalPair[0], getValueAdditionalData(additionalData, internalPair[1])))
                .forEach(keyValue::add);

        return keyValue;
    }

    public static DatosAdicionales getBanredAdditionalResponse(DatosAdicionales additionalData, AdditionalDataPayment responseData, AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.Document document) {
        //Se separan por coma los pares de valores
        //El primer valor antes del /, indica el dato adicional que hay que recibir de banred en datos adicionales
        //El valor que va despues del /, indica como se va a enviar el valor del dato adicional del mensaje de la cola
        //EX. "RUBROS_PENDIENTES/e_RUBROS_PENDIENTES,TOTAL_DEUDA/e_TOTAL_DEUDA,typeInformation/e_typeInformation,reference/e_reference"

        String stringValues = getValueOrDefault(additionalData, "banred_responsedata");
        if (StringUtils.isEmpty(stringValues))
            return null;

        List<DatoAdicional> lAdditionalData = new ArrayList<>();

        lAdditionalData.add(new DatoAdicional(Labels.ORDEN, document.getOrder().toString()));

        Arrays.stream(stringValues.split(","))
                .map(pair -> pair.split("/"))
                .map(internalPair -> new DatoAdicional(internalPair[0], getValueFromResponseData(responseData, internalPair[1])))
                .forEach(lAdditionalData::add);

        return new DatosAdicionales(lAdditionalData);
    }

    public static Map<String, String> getBanredTokenDataRequest(DatosAdicionales additionalData) {
        //Se separan por coma los pares de valores
        //El primer valor antes del /, indica el dato adicional que hay que enviar a banred en datos adicionales
        //El valor que va despues del /, indica que valor tomar del dato adicional del mensaje de la cola
        //Ex. "codLocalidad/e_localidad,codUbicacion/e_ubi,tipoSuministro/tipoSuministro"

        String stringValues = getValueOrDefault(additionalData, "banred_datoadicional");
        if (StringUtils.isEmpty(stringValues))
            return new LinkedHashMap<>();

        return Arrays.stream(stringValues.split(","))
                .map(pair -> pair.split("/"))
                .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> getValueAdditionalData(additionalData, parts[1]),
                        (oldValue, newValue) -> newValue,
                        LinkedHashMap::new
                ));
    }

    public static List<DatoAdicional> getBanredTokenDataResponse(DatosAdicionales additionalData, TokenData tokenData) {
        //Se separan por coma los pares de valores
        //El primer valor antes del /, indica el dato adicional que hay que recibir de banred en datos adicionales
        //El valor que va despues del /, indica como se va a enviar el valor del dato adicional del mensaje de la cola
        //EX. "RUBROS_PENDIENTES/e_RUBROS_PENDIENTES,TOTAL_DEUDA/e_TOTAL_DEUDA,typeInformation/e_typeInformation,reference/e_reference"

        String stringValues = getValueOrDefault(additionalData, "banred_responsedata");
        if (StringUtils.isEmpty(stringValues))
            return new ArrayList<>();

        return Arrays.stream(stringValues.split(","))
                .map(pair -> pair.split("/"))
                .map(internalPair -> new DatoAdicional(internalPair[0], getValueTokenDataGeneric(tokenData, internalPair[1])))
                .toList();
    }

    /**
     * Obtiene datos adicionales para el JsonObject de DatosCabecera
     * APLIOCA SOLO PARA COMERCIOS QUE MANEJEN VERSIONADO PAYLOAD VERSION 1 DE BANRED
     *
     * @param additionalData - La etiqueta de los datos adicionales para consulta pago o reverso
     * @return ArrayList<KeyValue> - La coleccion para la trama de entrada de datos adicionales de banred
     */
    public Map<String, String> getBanredHeaderDataV1(DatosAdicionales additionalData, TipoFlujo fluxType) {
        LinkedHashMap<String, String> lhmHeaderData = new LinkedHashMap<>();
        lhmHeaderData.put("codAgencia", getValueAdditionalData(additionalData, Labels.E_AGENCIA));
        lhmHeaderData.put("codLocalidad", getValueAdditionalData(additionalData, Labels.E_LOCALIDAD));
        lhmHeaderData.put("codigoInstitucion", getValueAdditionalData(additionalData, Labels.E_BAND_INSTITUCION));
        lhmHeaderData.put("codigoOperador", getValueAdditionalData(additionalData, Labels.E_COD_OPERADOR));
        if (TipoFlujo.REVERSO.equals(fluxType) || TipoFlujo.PAGO.equals(fluxType)) {
            if (TipoFlujo.REVERSO.equals(fluxType))
                lhmHeaderData.put(FECHA, this.retrieveSeqRetBaseValuesFromBillKey(additionalData, FECHA.toLowerCase()));
            else
                lhmHeaderData.put(Labels.FECHA, LocalDate.now().format(DateTimeFormatter.ofPattern(Defaults.DATE_FORMAT_V1)));

            lhmHeaderData.put(Labels.HORA, getDateAndTimePayment(additionalData, Labels.E_HORA));
        } else {
            lhmHeaderData.put(Labels.FECHA, LocalDate.now().format(DateTimeFormatter.ofPattern(Defaults.DATE_FORMAT_V1)));
            lhmHeaderData.put(Labels.HORA, LocalTime.now().format(DateTimeFormatter.ofPattern(Defaults.TIME_FORMAT)));
        }
        lhmHeaderData.entrySet().removeIf(entry -> entry.getValue() == null);
        return lhmHeaderData;
    }

    /**
     * RETORNA UNA VALOR DE ACUERDO AL TIPO DE OBJETO QUE RECIBE COMO PARAMETRO Y A LA ETIQUETA A BUSCAR DENTRO DEL MISMO
     * SE VALIDA POR TIPO DE DTO DADO A QUE EN EL VERSIONAMIENTO DE BANRED:
     * V2 -> USA EL DTO DatosAdicionales
     * V1 -> USA EL DTO TokenData
     *
     * @param object - Objeto a recibir de datos complementarios
     * @param key - la etiqueta a buscar dentro de los datos complementarios
     * @return String - el valor de la etiqueta encontrada o vacio si no existe valor
     * */
    public static String getValueOrDefault(Object object, String key) {
        if (object instanceof DatosAdicionales additionalData)
            return StringUtils.defaultIfEmpty(getValueAdditionalData(additionalData, key), Defaults.EMPTY);
        if (object instanceof TokenData tokenData)
            return StringUtils.defaultIfEmpty(getValueTokenDataGeneric(tokenData, key), Defaults.EMPTY);
        return Defaults.EMPTY;
    }

    public static String getValueFromTaxes(List<AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.Document.Tax> taxes) {
        if (taxes == null || taxes.isEmpty())
            return "000";

        return taxes.stream()
                .map(tax -> tax.getValue() != null
                        ? new BigDecimal(tax.getValue()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add).toString();
    }

    public static String getValueResponseDataPayment(AdditionalDataReversal responseData, String key) {
        if (responseData == null || StringUtils.isEmpty(key)) {
            return Defaults.EMPTY;
        }

        AdditionalDataReversal.Detail detail = responseData.getAdditionalData().getDetail();
        if (detail == null) {
            return Defaults.EMPTY;
        }

        AdditionalDataReversal.Detail.ReversalObligation reversalObligation = detail.getReversalObligation();
        if (reversalObligation == null) {
            return Defaults.EMPTY;
        }

        AdditionalDataReversal.Detail.ReversalObligation.Header header = reversalObligation.getHeader();
        if (header == null) {
            return Defaults.EMPTY;
        }

        AdditionalDataReversal.Detail.ReversalObligation.Header.Documents documents = header.getDocuments();
        if (documents == null) {
            return Defaults.EMPTY;
        }

        return Optional.ofNullable(documents.getBillerAuthorizationCode()).orElse(Defaults.EMPTY);
    }

    public static List<Payments> getDataPayments(DatosAdicionales additionalData) {
        List<Payments> payments = new ArrayList<>();
        payments.add(new Payments(getValueOrDefault(additionalData, Labels.TITULO),
                getValueOrDefault(additionalData, Labels.ANIO),
                getValueOrDefault(additionalData, Labels.RUBRO)));
        return payments;
    }

    public static InfoPerson getDataInfoPerson(DatosAdicionales additionalData) {
        //20251030 - llascanj: Se ajusta una validacion para obtener id y nombre del pagador
        String originDataAux = AdditionalDataUtils.getValueAdditionalData(additionalData, E_DATOS_ORIGEN);
        String identification =  Defaults.EMPTY;
        String name = Defaults.EMPTY;
        if(StringUtils.isNotEmpty(originDataAux) && originDataAux.equalsIgnoreCase("S")) {
            identification = getValueAdditionalData(additionalData, E_IDENTIFICACION_PAGADOR);
            name = getValueAdditionalData(additionalData, E_NOMBRE_PAGADOR);
        }

        return new InfoPerson(getValueOrDefault(additionalData, Labels.CATEGORY),
                getValueOrDefault(additionalData, DOCUMENTTYPE),
                //20250923 - LL: Validar los campos de identificacion y nombre para EPMAPS QUITO
                getAdditionalDataOrDefault(additionalData, DOCUMENTID, identification),
                getAdditionalDataOrDefault(additionalData, FULLNAME, name));
    }

    public DatosAdicionales getAdditionalDataResponse(Object outputAdditionalData, Object banredResponse, Object objectRq) {
        if (Objects.isNull(outputAdditionalData))
            return null;

        List<DatoAdicional> listAdditionalData = new ArrayList<>();

        try {
            //Enviar SOLO en consulta, el canal en formato String como llega al integrador
            if (outputAdditionalData instanceof AdditionalDataPayment addPayment
                    && (!Objects.isNull(objectRq) && objectRq instanceof MensajeEntradaConsultarDeuda inquiryRq)) //20251007 - llascanj: se dinamiza el parametro de entrada ObjectRq
                returnAdditionalDataPayment(addPayment.getAdditionalData().getDetail().getPaymentObligation(), listAdditionalData, inquiryRq);

            if (outputAdditionalData instanceof AdditionalDataReversal addReversal)
                returnAdditionalDataReversal(addReversal.getAdditionalData().getDetail().getReversalObligation(), listAdditionalData);

            if (banredResponse instanceof BillPaymentResponseV2 billResponse) {
                mapCommonFields(listAdditionalData, billResponse, null, null); //20250812 - LL: SE MAPEA COMO NULL DADO A QUE SOLO SIRVE EN V1
                listAdditionalData.add(new DatoAdicional(Labels.E_SEQUENCE_ACQUIRE, billResponse.getSequenceAcquire())); // solo en V2
            }

            // Agregar TransactionTime, TxCode y FormaPago para PAGO y REVERSO
            if (objectRq instanceof MensajeEntradaEjecutarPago paymentRq) {
                DatosAdicionales inputData = paymentRq.getServicio().getDatosAdicionales();
                listAdditionalData.add(new DatoAdicional(Labels.TRX_TIME,
                        CommonUtils.formatDate(new Date(), Defaults.TIME_FORMAT)));
                listAdditionalData.add(new DatoAdicional(Labels.TX_CODE,
                        padTxCode(getAdditionalDataOrDefault(inputData, Labels.TX_CODE, Defaults.EMPTY))));
                listAdditionalData.add(new DatoAdicional(Labels.FRM_PAGO,
                        padFormaPago(getAdditionalDataOrDefault(inputData, Labels.P_FORMA_PAGO, Defaults.FORMA_PAGO))));
            }

            if (objectRq instanceof MensajeEntradaEjecutarReverso reversalRq) {
                DatosAdicionales inputData = reversalRq.getServicio().getDatosAdicionales();
                listAdditionalData.add(new DatoAdicional(Labels.TRX_TIME,
                        CommonUtils.formatDate(new Date(), Defaults.TIME_FORMAT)));
                listAdditionalData.add(new DatoAdicional(Labels.TX_CODE,
                        padTxCode(getAdditionalDataOrDefault(inputData, Labels.TX_CODE, Defaults.EMPTY))));
                listAdditionalData.add(new DatoAdicional(Labels.FRM_PAGO,
                        padFormaPago(getAdditionalDataOrDefault(inputData, Labels.P_FORMA_PAGO, Defaults.FORMA_PAGO))));
            }

            if (objectRq instanceof MensajeEntradaEjecutarPago paymentRq
                    && isValidExtendedReference(AdditionalDataUtils.getValueAdditionalData(paymentRq.getServicio().getDatosAdicionales(), Labels.E_GUARDA_DATO))) {
                separateAndAddReference(listAdditionalData, AdditionalDataUtils.getValueAdditionalData(paymentRq.getServicio().getDatosAdicionales(), Labels.E_CONCEPTO));
            }

        } catch (Exception e) {
            return null;
        }
        return new DatosAdicionales(listAdditionalData);
    }



    private static void returnAdditionalDataPayment(AdditionalDataPayment.AdditionalData.Detail.PaymentObligation paymentObligation, List<DatoAdicional> additionalData, MensajeEntradaConsultarDeuda inquiryRq) {
        //Agregar Channel de entrada como respuesta
        additionalData.add(new DatoAdicional(Labels.CANAL, inquiryRq.getCanal()));

        if (paymentObligation != null) {
            var header = paymentObligation.getHeader();
            var infoPerson = paymentObligation.getInfoPerson();
            var additionalBiller = paymentObligation.getAdditionalBiller();

            if (header != null) {
                additionalData.add(new DatoAdicional(Labels.CATEGORY, header.getCategory()));
                additionalData.add(new DatoAdicional(Labels.IDRUBROBCE, header.getIdRubroBCE()));
                additionalData.add(new DatoAdicional(Labels.NAMERUBROBCE, header.getNameRubroBCE()));
            }

            if (infoPerson != null) {
                additionalData.add(new DatoAdicional(Labels.DOCUMENTTYPE, infoPerson.getDocumentType()));
                additionalData.add(new DatoAdicional(Labels.DOCUMENTID, infoPerson.getDocumentID()));
                additionalData.add(new DatoAdicional(Labels.FULLNAME, infoPerson.getFullName()));
                additionalData.add(new DatoAdicional(Labels.TELEPHONE, infoPerson.getTelephone()));
                additionalData.add(new DatoAdicional(Labels.EMAIL, infoPerson.getEmail()));
            }

            if((!additionalBiller.isEmpty()) && Labels.COD_SENAE.equals(inquiryRq.getServicio().getCodigoEmpresa())){
                String valor = buscarPorCodigo(inquiryRq.getServicio().getDatosAdicionales().getDatoAdicional(), "vp_s_referencia1")
                        .map(DatoAdicional::getValor)
                        .orElse("");
                additionalData.add(new DatoAdicional(Labels.TOKEN1, buildTokenT1(paymentObligation)));
                additionalData.add(new DatoAdicional(Labels.VP_REFERENCE_1, valor));
            }
        }
    }

    private static void returnAdditionalDataReversal(AdditionalDataReversal.Detail.ReversalObligation reversalObligation, List<DatoAdicional> additionalData) {
        if (Objects.isNull(reversalObligation))
            return;

        var header = reversalObligation.getHeader();
        if (header != null) {
            var documents = header.getDocuments();
            if (documents != null) {
                additionalData.add(new DatoAdicional(Labels.BILLER_AUTH_CODE, documents.getBillerAuthorizationCode()));
                additionalData.add(new DatoAdicional(Labels.E_COD_RESPUESTA, documents.getBillerAuthorizationCode()));
            }
        }
    }

    private static String getValueFromResponseData(Object responseData, String label) {
        String result = Defaults.EMPTY;
        if (Objects.isNull(responseData))
            return null;

        if (StringUtils.isEmpty(label))
            return Defaults.EMPTY;

        try {
            if (responseData instanceof AdditionalDataPayment addPayment)
                result = getValueFromAdditionalDataPayment(addPayment.getAdditionalData().getDetail().getPaymentObligation(), label);

            if (responseData instanceof AdditionalDataReversal addReversal)
                result = getValueFromAdditionalDataReversal(addReversal.getAdditionalData().getDetail().getReversalObligation(), label);


        } catch (Exception e) {
            return null;
        }
        return result;
    }

    private static String getValueFromAdditionalDataPayment(AdditionalDataPayment.AdditionalData.Detail.PaymentObligation paymentObligation, String label) {
        String stringValue = getFromHeader(paymentObligation, label);

        if (Defaults.EMPTY.equals(stringValue)) {
            stringValue = getFromAdditionalBiller(paymentObligation, label);
        }

        if (Defaults.EMPTY.equals(stringValue)) {
            stringValue = getFromInfoPerson(paymentObligation, label);
        }

        if (Defaults.EMPTY.equals(stringValue)) {
            stringValue = getFromDocuments(paymentObligation, label);
        }

        return stringValue;
    }

    private static String getFromHeader(AdditionalDataPayment.AdditionalData.Detail.PaymentObligation po, String label) {
        var header = po.getHeader();
        if (header == null) return Defaults.EMPTY;

        return switch (label) {
            case Labels.NAMERUBROBCE -> header.getNameRubroBCE();
            case Labels.IDRUBROBCE -> header.getIdRubroBCE();
            case Labels.CATEGORY -> header.getCategory();
            default -> Defaults.EMPTY;
        };
    }

    private static String getFromAdditionalBiller(AdditionalDataPayment.AdditionalData.Detail.PaymentObligation po, String label) {
        var additionalBiller = po.getAdditionalBiller();
        if (additionalBiller == null || additionalBiller.isEmpty()) return Defaults.EMPTY;

        return additionalBiller.stream()
                .filter(b -> b.getName().equals(label))
                .map(AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.AdditionalBiller::getValue)
                .findFirst()
                .orElse(Defaults.EMPTY);
    }

    private static String getFromInfoPerson(AdditionalDataPayment.AdditionalData.Detail.PaymentObligation po, String label) {
        var infoPerson = po.getInfoPerson();
        if (infoPerson == null) return Defaults.EMPTY;

        return switch (label) {
            case Labels.DOCUMENTTYPE -> infoPerson.getDocumentType();
            case Labels.DOCUMENTID -> infoPerson.getDocumentID();
            case Labels.FULLNAME -> infoPerson.getFullName();
            case Labels.LEGAL_TYPE_PERSON, Labels.LEGAL_NAME_OWNER, Labels.LEGAL_DOCUMENTID_OWNER ->
                    infoPerson.getLegalPerson().stream()
                            .map(person -> switch (label) {
                                case Labels.LEGAL_TYPE_PERSON -> person.getLegalTypePerson();
                                case Labels.LEGAL_NAME_OWNER -> person.getLegalNameOwner();
                                case Labels.LEGAL_DOCUMENTID_OWNER -> person.getLegalDocumentIDOwner();
                                default -> null;
                            })
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(Defaults.EMPTY);
            default -> Defaults.EMPTY;
        };
    }

    private static String getFromDocuments(AdditionalDataPayment.AdditionalData.Detail.PaymentObligation po, String label) {
        var documents = po.getDocuments();
        if (documents == null || documents.isEmpty()) return Defaults.EMPTY;

        return documents.stream()
                .map(doc -> label.equals(Labels.ORDER) ? doc.getOrder().toString() : Defaults.EMPTY)
                .findFirst()
                .orElse(BigDecimal.ZERO.toString());
    }

    private static String getValueFromAdditionalDataReversal(AdditionalDataReversal.Detail.ReversalObligation reversalObligation, String label) throws CustomException {
        var documents = reversalObligation.getHeader().getDocuments();

        // Handle the BILLER_AUTH_CODE case early
        if (documents != null && label.equals(Labels.BILLER_AUTH_CODE)) {
            return documents.getBillerAuthorizationCode();
        }

        var detailData = reversalObligation.getHeader().getDetailData();
        if (detailData != null) {
            return switch (label) {
                case Labels.REVERSALS -> detailData.getReversals().stream()
                        .map(reversal -> {
                            switch (label) {
                                case Labels.TITULO:
                                    return reversal.getTitulo();
                                case Labels.ANIO:
                                    return reversal.getAnio();
                                case Labels.RUBRO:
                                    return reversal.getRubro();
                                default:
                                    return Defaults.EMPTY; // return an empty string for unrecognized labels
                            }
                        })
                        .filter(response -> !Defaults.EMPTY.equals(response)) // Remove empty responses
                        .findFirst()
                        .orElse(Defaults.EMPTY); // If no match, return an empty string

                case Labels.INFO_REVERSAL -> detailData.getInfoReversal().stream()
                        .filter(infoReversal -> infoReversal.getName().equals(label)) // Filter by name match
                        .map(AdditionalDataReversal.Detail.ReversalObligation.Header.DetailData.InfoReversal::getValue) // Get the value if the name matches
                        .findFirst()
                        .orElse(Defaults.EMPTY); // If no match, return an empty string

                default ->
                        throw new CustomException("Etiqueta no reconocida: " + label, null, INTERNAL_ERROR);
            };
        }
        return Defaults.EMPTY; // Return empty if detailData is null
    }

    public static List<AdditionalDataReversal.Detail.ReversalObligation.Header.DetailData.Reversals> getDataReversals(DatosAdicionales additionalData) {
        List<AdditionalDataReversal.Detail.ReversalObligation.Header.DetailData.Reversals> reversals = new ArrayList<>();
        reversals.add(new AdditionalDataReversal.Detail.ReversalObligation.Header.DetailData.Reversals(getValueOrDefault(additionalData, Labels.TITULO),
                getValueOrDefault(additionalData, Labels.ANIO),
                getValueOrDefault(additionalData, Labels.RUBRO)));
        return reversals;
    }

    public static String getAdditionalDataOrDefault(DatosAdicionales additionalData, String label, String defaultValue) {
        if (StringUtils.isEmpty(label))
            return defaultValue;

        //Asi no envia a buscar mas de una vez, ni se pierde memoria en el proceso.
        String value = getValueAdditionalData(additionalData, label);

        return StringUtils.isNotEmpty(value) ? value : defaultValue;
    }

    public static String getValueTokenDataGeneric(TokenData tokenData, String key) {
        if (tokenData == null || StringUtils.isEmpty(key)) {
            return Defaults.EMPTY;
        }

        Map<String, Supplier<String>> valueMap = new HashMap<>();
        addDatosCabecera(valueMap, key, tokenData.getDatosCabecera());

        addDocTrxDetalle(valueMap, key, tokenData.getDocTrxDetalle());
        addDocPagoRespuesta(valueMap, key, tokenData.getDocPagoRespuesta());
        addDocReversoRespuesta(valueMap, key, tokenData.getDocReversoRespuesta());

        return valueMap.getOrDefault(key, () -> Defaults.EMPTY).get();
    }

    private static void addDatosCabecera(Map<String, Supplier<String>> map, String key, DatosCabecera cabecera) {
        if (cabecera == null) return;
        addIfAbsent(map, key, "codAgencia", cabecera::getCodAgencia);
        addIfAbsent(map, key, "codLocalidad", cabecera::getCodLocalidad);
        addIfAbsent(map, key, "codigoInstitucion", cabecera::getCodigoInstitucion);
        addIfAbsent(map, key, "codigoOperador", cabecera::getCodigoOperador);
        addIfAbsent(map, key, Labels.FECHA, cabecera::getFecha);
        addIfAbsent(map, key, Labels.HORA, cabecera::getHora);
    }

    private static void addDocTrxDetalle(Map<String, Supplier<String>> map, String key, List<DocTrxDetalle> lista) {
        if (lista == null) return;
        for (DocTrxDetalle doc : lista) {
            if (doc == null) continue;
            addIfAbsent(map, key, Labels.NUMERO_CONTRATO, doc::getNumeroContrato);
            addIfAbsent(map, key, Labels.NOMBRE_CLIENTE, doc::getNombreCliente);
            addIfAbsent(map, key, "totalPendientePago", doc::getTotalPendientePago);
            addIfAbsent(map, key, "interesAcumulado", doc::getInteresAcumulado);
            addIfAbsent(map, key, "infraccion", doc::getInfraccion);
            addIfAbsent(map, key, "transferencia", doc::getTransferencia);
            addIfAbsent(map, key, Labels.SECUENCIA_AUT, doc::getSecuenciaAut);
            addIfAbsent(map, key, Labels.RETENCION, doc::getRetencion);
            addIfAbsent(map, key, "baseImponible", doc::getBaseImponible);
            addIfAbsent(map, key, "tipoReparto", doc::getTipoReparto);
            addIfAbsent(map, key, "direccionServicio", doc::getDireccionServicio);
            addIfAbsent(map, key, Labels.NUM_DOC_IDENTIF, doc::getNumDocIdentif);
            addIfAbsent(map, key, "fechaLecturaInicial", doc::getFechaLecturaInicial);
            addIfAbsent(map, key, "fechaLecturaFinal", doc::getFechaLecturaFinal);
            addIfAbsent(map, key, Labels.FECHA_EMISION, doc::getFechaEmision);
            addIfAbsent(map, key, "facturasPendientes", doc::getFacturasPendientes);
            addIfAbsent(map, key, Labels.FECHA_VENCIMIENTO, doc::getFechaVencimiento);
            addIfAbsent(map, key, "consumoServicio", doc::getConsumoServicio);
            addIfAbsent(map, key, Labels.NUMERO_FACTURA, doc::getNumeroFactura);
            addIfAbsent(map, key, "totalMes", doc::getTotalMes);
            addIfAbsent(map, key, Labels.DEUDA_ANTERIOR, doc::getDeudaAnterior);
        }
    }

    private static void addDocPagoRespuesta(Map<String, Supplier<String>> map, String key, DocPagoRespuesta docPago) {
            if (docPago != null) {
                addIfAbsent(map, key, "codigoAutorizacion", docPago::getCodigoAutorizacion);
                addIfAbsent(map, key, "totalPendientePago", docPago::getTotalPendientePago);
                addIfAbsent(map, key, "interesAcumulado", docPago::getInteresAcumulado);
                addIfAbsent(map, key, "infraccion", docPago::getInfraccion);
                addIfAbsent(map, key, "transferencia", docPago::getTransferencia);
                addIfAbsent(map, key, Labels.SECUENCIA_AUT, docPago::getSecuenciaAut);
                addIfAbsent(map, key, Labels.RETENCION, docPago::getRetencion);
                addIfAbsent(map, key, Labels.BASE, docPago::getBase);
                addIfAbsent(map, key, Labels.NOMBRE_CLIENTE, docPago::getNombreCliente);
                addIfAbsent(map, key, "tipoReparto", docPago::getTipoReparto);
                addIfAbsent(map, key, "direccionServicio", docPago::getDireccionServicio);
                addIfAbsent(map, key, Labels.NUM_DOC_IDENTIF, docPago::getNumDocIdentif);
                addIfAbsent(map, key, "fechaLecturaInicial", docPago::getFechaLecturaInicial);
                addIfAbsent(map, key, "fechaLecturaFinal", docPago::getFechaLecturaFinal);
                addIfAbsent(map, key, Labels.FECHA_EMISION, docPago::getFechaEmision);
                addIfAbsent(map, key, "facturasPendientes", docPago::getFacturasPendientes);
                addIfAbsent(map, key, Labels.FECHA_VENCIMIENTO, docPago::getFechaVencimiento);
                addIfAbsent(map, key, "consumoServicio", docPago::getConsumoServicio);
                addIfAbsent(map, key, Labels.NUMERO_FACTURA, docPago::getNumeroFactura);
                addIfAbsent(map, key, "totalMes", docPago::getTotalMes);
                addIfAbsent(map, key, Labels.DEUDA_ANTERIOR, docPago::getDeudaAnterior);
                addIfAbsent(map, key, Labels.VALOR_PAGADO, docPago::getValorPagado);
                addIfAbsent(map, key, Labels.NUM_DOC_IDENTIF2, docPago::getNumDocIdentif2);
                addIfAbsent(map, key, Labels.NOMBRE_CLIENTE2, docPago::getNombreCliente2);
            }
    }

    private static void addDocReversoRespuesta(Map<String, Supplier<String>> map, String key, DocReversoRespuesta docReverso) {
        if (docReverso != null) {
            addIfAbsent(map, key, Labels.SECUENCIAL_AUT, docReverso::getSecuencialAut);
            addIfAbsent(map, key, Labels.RETENCION, docReverso::getRetencion);
            addIfAbsent(map, key, Labels.BASE, docReverso::getBase);
            addIfAbsent(map, key, Labels.NUM_DOC_IDENTIF, docReverso::getNumDocIdentif);
            addIfAbsent(map, key, Labels.FECHA_EMISION, docReverso::getFechaEmision);
            addIfAbsent(map, key, Labels.NUMERO_FACTURA, docReverso::getNumeroFactura);
            addIfAbsent(map, key, Labels.VALOR_PAGADO, docReverso::getValorPagado);
            addIfAbsent(map, key, Labels.NUM_DOC_IDENTIF2, docReverso::getNumDocIdentif2);
            addIfAbsent(map, key, Labels.NOMBRE_CLIENTE2, docReverso::getNombreCliente2);
        }
    }

    /**
     * METODO QUE EVITA LA DUPLICACION DEL CODIGO EN CASO QUE EXISTA MAS DE UNA VEZ
     *
     * */
    private static void addIfAbsent(Map<String, Supplier<String>> map, String key, String field, Supplier<String> getter) {
        if (field.equals(key)) {
            map.computeIfAbsent(key, k -> getter);
        }
    }

    public static String getCutoverDateAuditNumber(DatosAdicionales additionalData, String outputLabel) {
        return getSplitValueFromAdditionalData(additionalData, outputLabel, SWITCH_AUDIT_NUMBER, CUTOVER_DATE);
    }

    public String getDateAndTimePayment(DatosAdicionales additionalData, String outputLabel) {
        String valueRetrieved = getSplitValueFromAdditionalData(additionalData, outputLabel, Labels.FECHA, Labels.HORA);
        if (StringUtils.isEmpty(valueRetrieved))
            valueRetrieved = this.retrieveSeqRetBaseValuesFromBillKey(additionalData, outputLabel);
        return valueRetrieved;
    }

    public String takeDateTimeFromReference(DatosAdicionales additionalData, String label) {
        return this.retrieveSeqRetBaseValuesFromBillKey(additionalData, label);
    }

    /**
     * OBTIENE LOS VALORES ESTATICOS QUE SE GUARDAN EN E_FACTURA (EN BASE DE DATOS, EL CAMPO_ALTERNO_2 EN LA CC_TRAN_SERVICIO)
     *
     * @param additionalData - Datos adicionales de entrada (request del consumidor)
     * @param label - la etiqueta a buscar
     * @param firstLabel - Primera etiqueta por defecto dependiendo de la version
     * @param secondLabel - Segunda etiqueta por defecto dependiendo de la version
     * */
    private static String getSplitValueFromAdditionalData(DatosAdicionales additionalData, String label, String firstLabel, String secondLabel) {
        if (StringUtils.isEmpty(label)) return Defaults.EMPTY;

        String rawValue = AdditionalDataUtils.getValueAdditionalData(additionalData, Labels.VPS_REFERENCIA2);
        if (StringUtils.isEmpty(rawValue))
            rawValue = AdditionalDataUtils.getValueAdditionalData(additionalData, Labels.E_FACTURA);

        if (StringUtils.isNotEmpty(rawValue)) {
            String[] values = rawValue.split("/");
            if (values.length < 2) return Defaults.EMPTY;

            if (label.equals(firstLabel)) return values[0];
            if (label.equals(secondLabel)) return values[1];
            //20250808 - LL: SI NO SON IGUALES A LAS ETIQUETAS ESTIPULADAS, SE ENVIA EL VALOR ENCONTRADO
        }
        return AdditionalDataUtils.getValueAdditionalData(additionalData, label);
    }

    public void mapCommonFields(List<DatoAdicional> additionalData, Object response, TokenData tokenData, String trxTime) {
        String switchAuditNumber = Defaults.EMPTY;
        String billerCutoverDate = Defaults.EMPTY;
        String invoice = Defaults.EMPTY;

        if (response instanceof BillPaymentResponseV2 responseV2) {
            switchAuditNumber = responseV2.getSwitchAuditNumber();
            billerCutoverDate = responseV2.getBillerCutoverDate();
            invoice = switchAuditNumber.concat("/").concat(billerCutoverDate);
        }

        // 08042025 - LL: Guardar el fecha y hora del payload de pago para reversos
        if (response instanceof BillPaymentResponseV1 responseV1
                && tokenData != null) {
            String trxDate = getValueTokenDataGeneric(tokenData, Labels.FECHA);
            switchAuditNumber = responseV1.getSwitchAuditNumber();
            billerCutoverDate = responseV1.getBillerCutoverDate();
            invoice = trxDate.concat("/").concat(trxTime); //20250812 - LL: Se parametriza la hora para el pago y reverso
            invoice = this.compressorUtils.encryptInvoiceValuesforV1(setSeqBaseRetValuesToInvoiceKey(tokenData, invoice));
        }

        additionalData.add(new DatoAdicional(Labels.SWITCH_AUDIT_NUMBER, switchAuditNumber));
        additionalData.add(new DatoAdicional(Labels.CUTOVER_DATE, billerCutoverDate));
        additionalData.add(new DatoAdicional(Labels.E_FACTURA, invoice));
    }

    public String setSeqBaseRetValuesToInvoiceKey(TokenData tokenData, String invoice) {
        String sequenceAut = getValueOrDefault(tokenData, Labels.SECUENCIA_AUT);
        String base = getValueOrDefault(tokenData, Labels.BASE);
        String retention = getValueOrDefault(tokenData, Labels.RETENCION);
        String paidValue = getValueOrDefault(tokenData, Labels.VALOR_PAGADO);

        return new StringBuilder()
                .append(invoice).append("/")
                .append(sequenceAut).append("/")
                .append(base).append("/")
                .append(retention).append("/")
                .append(paidValue)
                .toString();
    }

    public String retrieveSeqRetBaseValuesFromBillKey(DatosAdicionales additionalData, String label) {
        String rawValue = getValueAdditionalData(additionalData, Labels.VPS_REFERENCIA2);
        if (StringUtils.isEmpty(rawValue)) {
            rawValue = getValueAdditionalData(additionalData, Labels.E_FACTURA);
        }
        //DEBERIA DEVOLVER 6 VALORES
        if (StringUtils.isNotEmpty(rawValue)) {
            return this.compressorUtils.decryptSpecificValueForInvoice(rawValue, label);
        }
        return Defaults.EMPTY;
    }

    private boolean isValidExtendedReference(String value) {
        return Defaults.REFERENCIA.equalsIgnoreCase(value);
    }

    private void separateAndAddReference(List<DatoAdicional> additionalData, String identifier) {
        if(StringUtils.isEmpty(identifier)) {
            return;
        }

        int length = identifier.length();
        //si es mayor a 60 no se podra insertar en los campos alternos
        if (length > 60) {
            return;
        }

        //se elimina el dato adicional guardado anteriormente
        additionalData.removeIf(data ->
                E_COD_RESPUESTA.equalsIgnoreCase(data.getCodigo()) ||
                E_FACTURA.equalsIgnoreCase(data.getCodigo())
        );

        int splitIndex = (length + 1) / 2;
        String firstPart = identifier.substring(0, splitIndex);
        String secondPart = identifier.substring(splitIndex);

        additionalData.add(new DatoAdicional(E_COD_RESPUESTA, firstPart));
        additionalData.add(new DatoAdicional(E_FACTURA, secondPart));
    }

    private static String padFormaPago(String formaPago) {
        if (StringUtils.isEmpty(formaPago)) {
            return formaPago;
        }
        try {
            int valor = Integer.parseInt(formaPago.trim());
            return String.format("%02d", valor);
        } catch (NumberFormatException e) {
            return formaPago;
        }
    }

    private static String padTxCode(String txCode) {
        if (StringUtils.isEmpty(txCode)) {
            return txCode;
        }
        return txCode.trim();
    }

    private static String buildTokenT1(AdditionalDataPayment.AdditionalData.Detail.PaymentObligation paymentObligation) {
        AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.InfoPerson infoPerson =
                (paymentObligation != null) ? paymentObligation.getInfoPerson() : null;

        List<AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.AdditionalBiller> billers =
                (paymentObligation != null) ? paymentObligation.getAdditionalBiller() : null;

        String documentID = (infoPerson != null) ? infoPerson.getDocumentID() : null;
        String fullName = (infoPerson != null) ? infoPerson.getFullName() : null;

        String ruc = CommonUtils.safePadLeft(documentID, 13, '0');
        String nombre = CommonUtils.safePadRight(fullName, 40, ' ');

        String codigoRaw = CommonUtils.extractByName(billers, Defaults.CUSTOMOFICCE);
        String referenciaRaw = CommonUtils.extractByName(billers, Defaults.REFERENCE);

        String codigo = CommonUtils.safePadLeft(codigoRaw, 3, '0');
        String referencia = CommonUtils.safePadRight(referenciaRaw, 30, ' ');
        String valorNc = CommonUtils.safePadLeft("", 12, '0'); // placeholder hasta definir fuente real

        String body = ruc + nombre + codigo + referencia + valorNc;

        String header = String.format("! T1%05d", body.length());
        return header + " " + body;
    }

    private static Optional<DatoAdicional> buscarPorCodigo(List<DatoAdicional> lista, String codigo) {
        if (lista == null || codigo == null) {
            return Optional.empty();
        }
        return lista.stream()
                .filter(Objects::nonNull)
                .filter(dato -> codigo.equals(dato.getCodigo()))
                .findFirst();
    }


}