package com.bolivariano.microservice.recbanred.util;

import com.bolivariano.microservice.recbanred.core.constants.Defaults;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
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


}
