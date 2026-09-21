package com.bolivariano.microservice.recbanred.util;

import com.bolivariano.microservice.recbanred.core.constants.Defaults;
import com.bolivariano.microservice.recbanred.core.payloads.AdditionalDataPayment;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class CommonUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);

    private CommonUtils() {}

    public static String formatDate(Date date, String format) {
        if (StringUtils.isEmpty(format)) {
            log.warn("Formato de texto vacío, no se realizará la transformación de la fecha");
            return Defaults.EMPTY;
        }
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static int convertToInteger(String value) {
        if (StringUtils.isEmpty(value))
            return 0;

        return Integer.parseInt(value);
    }

    public static BigDecimal toBigDecimal(String value) {
        if(StringUtils.isEmpty(value))
            return BigDecimal.ZERO;

        return Optional.of(value)
                .map(BigDecimal::new)
                .orElse(BigDecimal.ZERO);
    }

    public static String fromBigDecimal(BigDecimal value) {
        if(value.compareTo(BigDecimal.ZERO) <= 0)
            return StringUtils.leftPad("0", 12, '0');

        return StringUtils.leftPad(value.multiply(new BigDecimal(100)).toBigInteger().toString(), 12, '0');
    }

    public static String fillValue(String value) {
        if(StringUtils.isEmpty(value))
            return Defaults.EMPTY;

        return StringUtils.leftPad(value, 12, '0');
    }

    public static String getValueOrDefault(String value, String defaultValue) {
        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(defaultValue))
            return Defaults.EMPTY;

        return StringUtils.isNotEmpty(value) ? value : defaultValue;
    }

    public static String safePadLeft(String value, int length, char padChar) {
        String v = (value == null) ? "" : value;
        if (v.length() >= length) {
            return v.length() == length ? v : v.substring(v.length() - length);
        }
        return CommonUtils.padLeft(v, length, padChar);
    }

    public static String safePadRight(String value, int length, char padChar) {
        String v = (value == null) ? "" : value;
        if (v.length() >= length) {
            return v.length() == length ? v : v.substring(0, length);
        }
        return CommonUtils.padRight(v, length, padChar);
    }

    public static String padLeft(String value, int length, char padChar) {
        String v = value == null ? "" : value;
        if (v.length() > length) return v.substring(v.length() - length); // trunca por la izquierda si se pasa
        StringBuilder sb = new StringBuilder();
        for (int i = v.length(); i < length; i++) sb.append(padChar);
        return sb.append(v).toString();
    }

    public static String padRight(String value, int length, char padChar) {
        String v = value == null ? "" : value;
        if (v.length() > length) return v.substring(0, length); // trunca por la derecha si se pasa
        StringBuilder sb = new StringBuilder(v);
        for (int i = v.length(); i < length; i++) sb.append(padChar);
        return sb.toString();
    }

    public static String extractByName(List<AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.AdditionalBiller> billers, String name) {
        if (billers == null || billers.isEmpty() || name == null) {
            return "";
        }

        return billers.stream()
                .filter(Objects::nonNull)
                .filter(b -> name.equalsIgnoreCase(b.getName())) // equalsIgnoreCase ya maneja b.getName() == null devolviendo false sin NPE, porque el literal "name" nunca es null aquí
                .map(AdditionalDataPayment.AdditionalData.Detail.PaymentObligation.AdditionalBiller::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }


}
