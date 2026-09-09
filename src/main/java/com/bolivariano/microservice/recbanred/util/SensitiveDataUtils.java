package com.bolivariano.microservice.recbanred.util;

import com.bolivariano.microservice.recbanred.core.constants.Labels;
import com.bolivariano.microservice.recbanred.core.exceptions.BadFormatException;
import com.bolivariano.microservice.recbanred.core.payloads.input.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.*;

@Component
public class SensitiveDataUtils {

    private SensitiveDataUtils() {}
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+");
    private static final Pattern ISO_DATE_TIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern ALPHA_PATTERN = Pattern.compile("^[A-Za-z_]+$");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String REASON_XSS = "Campo contiene caracteres potencialmente maliciosos (XSS)";
    private static final String THE_FIELD = "El campo '";

    public static void validateSensitiveData(MensajeEntradaProcesar inputMessage) throws BadFormatException {
        if (Objects.isNull(inputMessage))
            throw handleBadFormatExceptionValidation("La informacion de entrada esta vacía o nula");

        MensajeEntradaConsultarDeuda inputMessageInquiry = inputMessage.getMensajeEntradaConsultarDeuda();
        if (!Objects.isNull(inputMessageInquiry))
            validateSensitiveDataInquiry(inputMessageInquiry);

        MensajeEntradaEjecutarPago inputMessagePayment = inputMessage.getMensajeEntradaEjecutarPago();
        if (!Objects.isNull(inputMessagePayment))
            validateSensitiveDataPayment(inputMessagePayment);

        MensajeEntradaEjecutarReverso inputMessageReversal = inputMessage.getMensajeEntradaEjecutarReverso();
        if (!Objects.isNull(inputMessageReversal))
            validateSensitiveDataReversal(inputMessageReversal);

    }

    private static void validateSensitiveDataInquiry(MensajeEntradaConsultarDeuda inputMessageInquiry) throws BadFormatException {

        validateValueForXss(inputMessageInquiry);

        requireNonNull(inputMessageInquiry.getServicio());
        validateNumeric(inputMessageInquiry.getSecuencial(), Labels.SECUENCIAL);

        validateDateGeneric(inputMessageInquiry.getFecha(), Labels.FECHA);

        validateAlpha(inputMessageInquiry.getServicio().getCodTipoServicio());
        validateNumeric(inputMessageInquiry.getServicio().getCodigoEmpresa(), Labels.COD_TIPO_IDENTIFICADOR);
        validateNumeric(inputMessageInquiry.getServicio().getCodigoEmpresa(), Labels.CODIGO_EMPRESA);

    }

    private static void validateSensitiveDataPayment(MensajeEntradaEjecutarPago inputMessagePayment) throws BadFormatException {

        validateValueForXss(inputMessagePayment);

        requireNonNull(inputMessagePayment.getServicio());
        validateNumeric(inputMessagePayment.getSecuencial(), Labels.SECUENCIAL);

        validateDateGeneric(inputMessagePayment.getFecha(), Labels.FECHA);
        validateDateGeneric(inputMessagePayment.getFechaPago(), Labels.FECHA_PAGO);

        validateAccount(inputMessagePayment.getCuenta());
        validateNumeric(AdditionalDataUtils.getValueAdditionalData(inputMessagePayment.getServicio().getDatosAdicionales(), Labels.E_BAND_AUTORIZADOR), Labels.E_BAND_AUTORIZADOR);
        validateAlpha(inputMessagePayment.getServicio().getCodTipoServicio());
        validateNumeric(inputMessagePayment.getServicio().getCodigoEmpresa(), Labels.COD_TIPO_IDENTIFICADOR);
        validateNumeric(inputMessagePayment.getServicio().getCodigoEmpresa(), Labels.CODIGO_EMPRESA);
        //20251007 - llascanj: se agrega validacion que no permita pagar con decimales (valida e_decimal)
        validateOnlyDecimals(inputMessagePayment.getValorPago(), AdditionalDataUtils.getValueAdditionalData(inputMessagePayment.getServicio().getDatosAdicionales(), Labels.E_DECIMAL));

    }

    private static void validateSensitiveDataReversal(MensajeEntradaEjecutarReverso inputMessageReversal) throws BadFormatException {

        validateValueForXss(inputMessageReversal);

        requireNonNull(inputMessageReversal.getServicio());
        validateNumeric(inputMessageReversal.getSecuencial(), Labels.SECUENCIAL);

        validateDateGeneric(inputMessageReversal.getFechaPago(), Labels.FECHA_PAGO);

        validateAccount(inputMessageReversal.getCuenta());
        validateNumeric(inputMessageReversal.getMoneda(), Labels.MONEDA);
        validateNumeric(AdditionalDataUtils.getValueAdditionalData(inputMessageReversal.getServicio().getDatosAdicionales(), Labels.E_BAND_AUTORIZADOR), Labels.E_BAND_AUTORIZADOR);
        validateAlpha(inputMessageReversal.getServicio().getCodTipoServicio());
        validateNumeric(inputMessageReversal.getServicio().getCodigoEmpresa(), Labels.COD_TIPO_IDENTIFICADOR);
        validateNumeric(inputMessageReversal.getServicio().getCodigoEmpresa(), Labels.CODIGO_EMPRESA);
    }

    private static void requireNonNull(Object value) throws BadFormatException {
        if (value == null) {
            throw handleBadFormatExceptionValidation("El 'servicio' no puede ser nulo");
        }
    }

    private static void validateNumeric(String value, String fieldName) throws BadFormatException {
        if (StringUtils.isEmpty(value) || !NUMERIC_PATTERN.matcher(value).matches()) {
            throw handleBadFormatExceptionValidation(THE_FIELD + fieldName + "' debe ser numérico");
        }
    }

    //Similar al metodo de validar numerico, pero al ser generico, los pagos por ventanilla son en efectivo, asi que no se debe validar la cuenta vacia o nula
    private static void validateAccount(String value) throws BadFormatException {
        if (StringUtils.isEmpty(value))
            return;

        if (!NUMERIC_PATTERN.matcher(value).matches()) {
            throw handleBadFormatExceptionValidation(THE_FIELD + Labels.CUENTA + "' debe ser numérico");
        }
    }

    private static void validateDateGeneric(String date, String fieldName) throws BadFormatException {
        if(StringUtils.isEmpty(date))
            throw handleBadFormatExceptionValidation("Campo '" + fieldName + "' no puede estar vacio o nulo");

        switch(fieldName) {
            case Labels.FECHA -> {
                if(!ISO_DATE_TIME_PATTERN.matcher(date).matches())
                    throw handleBadFormatExceptionValidation(THE_FIELD + Labels.FECHA + "' no tiene el formato adecuado");
            }
            case Labels.FECHA_PAGO -> {
                if(!DATE_PATTERN.matcher(date).matches())
                    throw handleBadFormatExceptionValidation(THE_FIELD + Labels.FECHA_PAGO + "' no tiene el formato adecuado");
            }
            default -> throw handleBadFormatExceptionValidation("Tipo de campo no reconocido para las fechas: " + fieldName);
        }
    }

    private static void validateAlpha(String value) throws BadFormatException {
        if (StringUtils.isEmpty(value) || !ALPHA_PATTERN.matcher(value).matches()) {
            throw handleBadFormatExceptionValidation("Imposible procesar, '" + Labels.COD_TIPO_SERVICIO + "' debe contener solo letras");
        }
    }

    private static BadFormatException handleBadFormatExceptionValidation(String reason) {
        return new BadFormatException(reason, null, VALIDATION_ERROR);
    }

    private static void validateValueForXss(Object object) throws BadFormatException {
        Map<?, ?> mapperValid = MAPPER.convertValue(object, Map.class);
        validateMap(mapperValid);
    }

    private static void validateMap(Map<?, ?> map) throws BadFormatException {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;

            validateEntry(entry.getKey().toString(), value);
        }
    }

    private static void validateEntry(String key, Object value) throws BadFormatException {
        if (value instanceof String valueAux) {
            validateField(valueAux);
        } else if (value instanceof List<?> list) {
            validateList(key, list);
        } else if (value instanceof Map<?, ?> nestedMap) {
            validateMap(nestedMap);
        }
    }

    private static void validateList(String key, List<?> list) throws BadFormatException {
        for (Object item : list) {
            if (item instanceof Map<?, ?> nestedMap) {
                if ("datoAdicional".equalsIgnoreCase(key)) {
                    validateField(nestedMap.get("codigo"));
                    validateField(nestedMap.get("etiqueta"));
                    validateField(nestedMap.get("editable"));
                    validateField(nestedMap.get("formato"));
                    validateField(nestedMap.get("listasSeleccion"));
                    validateField(nestedMap.get("longitud"));
                    validateField(nestedMap.get("mascara"));
                    validateField(nestedMap.get("regexp"));
                    validateField(nestedMap.get("tipo"));
                    validateField(nestedMap.get("valor"));
                    validateField(nestedMap.get("visible"));
                } else {
                    validateMap(nestedMap);
                }
            }
        }
    }

    private static void validateField(Object value) throws BadFormatException {
        if (value instanceof String str && isValidXss(str)) {
            throw handleBadFormatExceptionValidation(
                    REASON_XSS
            );
        }
    }

    private static boolean isValidXss(String value) {
        if (StringUtils.isEmpty(value)) return false;
        String input = value.toLowerCase();

        return input.contains("<script") ||
                input.contains("</script>") ||
                input.contains("javascript:") ||
                input.contains("<") || input.contains(">") ||
                input.contains("</") || input.contains("/>");
    }

    //20251007 - llascanj: Se valida si permite decimales
    private static void validateOnlyDecimals(BigDecimal amount, String value) throws BadFormatException {
        if ((StringUtils.isNotEmpty(value) && value.equalsIgnoreCase("N"))
                && (amount.stripTrailingZeros().scale() > 0)) {
            throw handleBadFormatExceptionValidation("LA EMPRESA SOLO PERMITE PAGOS CON VALORES ENTEROS");
        }
    }

}
