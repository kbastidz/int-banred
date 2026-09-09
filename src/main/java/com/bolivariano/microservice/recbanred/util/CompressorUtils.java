package com.bolivariano.microservice.recbanred.util;

import com.bolivariano.microservice.recbanred.core.configuration.CompressorConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;


@Component
public class CompressorUtils {

    private static final Logger log = LoggerFactory.getLogger(CompressorUtils.class);
    private static final String CODE_AUTH_WARN = "Código de autorización vacío o nulo";
    private static final Pattern PATTERN_AUTHCODE_ENCRYPT =
            Pattern.compile("^[A-Z0-9]+-[A-Z0-9]+-\\d{8}-\\d+$");

    private static final Pattern PATERN_AUTHCODE_DECRYPT =
            Pattern.compile("^[^-]+-[^-]+-[^-]+-[^-]+$");

    private static final Pattern PATTERN_INVOICE_COMPILED =
            Pattern.compile("^[^/]+/[^/]+/[^/]+/[^/]+/[^/]+/[^/]+$");

    private final CompressorConfiguration compressorConfiguration;
    public CompressorUtils(CompressorConfiguration compressConfig) {
        this.compressorConfiguration = compressConfig;
    }

    /**
     * Metodo que entrega un tipo de texto encriptado/comprimido para realizar insercion de codigos de autorizacion
     * a la base de datos (VALIDO SOLO PARA PAYLOADS DE VERSION 1 DE BANRED)
     *
     * @param input - El texto del codigo de autorizacion
     * @return String - el codigo de autorizacion encriptado y comprimido hasta 30 caracteres maximo
     * */
    public String encryptAuthCodeForV1(String input) {
        if (StringUtils.isEmpty(input)) {
            log.warn(CODE_AUTH_WARN);
            return input;
        }

        if (!PATTERN_AUTHCODE_ENCRYPT.matcher(input).matches()) {
            log.warn("Formato no reconocido, se retorna sin compresión");
            return input;
        }

        String[] parts = input.split(this.compressorConfiguration.getSeparator());
        if (parts.length != 4) {
            log.error("Formato inválido para compresión");
            return input;
        }

        String pid = parts[0];
        String codes = parts[1];
        String date = parts[2];
        String number = parts[3];

        Map<String, String> originalToCompressed = new LinkedHashMap<>();
        originalToCompressed.put(pid, compressorConfiguration.getPidPrefix(pid));
        originalToCompressed.put(codes, this.compressGeneric(codes));
        originalToCompressed.put(date, this.compressGeneric(date));
        originalToCompressed.put(number, this.compressGeneric(number));

        // Construir resultado eligiendo el comprimido si existe y no está vacío, sino el original
        List<String> resultCompress = new ArrayList<>();
        originalToCompressed.forEach((original, compressed) -> resultCompress.add(compressed));
        return String.join(this.compressorConfiguration.getSeparator(), resultCompress);
    }

    /**
     * Metodo que entrega un tipo de texto desencriptado/descomprimido para realizar insercion de codigos de autorizacion
     * a la base de datos (VALIDO SOLO PARA PAYLOADS DE VERSION 1 DE BANRED)
     *
     * @param input - el texto encriptado/comprimido
     * @return String - el codigo de autorizacion en su forma base
     * */
    public String decryptAuthCodeForV1(String input) {
        if (StringUtils.isEmpty(input)) {
            log.warn(CODE_AUTH_WARN);
            return input;
        }

        if (PATERN_AUTHCODE_DECRYPT.matcher(input).matches()) {
            String[] parts = input.split(this.compressorConfiguration.getSeparator());
            if (parts.length != 4) return input;

            String pidCompressed = parts[0];
            String codesCompressed = parts[1];
            String dateCompressed = parts[2];
            String numberCompressed = parts[3];

            Map<String, String> compressToOriginal = new LinkedHashMap<>();
            compressToOriginal.put(pidCompressed, this.compressorConfiguration.getFullPrefix(pidCompressed));
            compressToOriginal.put(codesCompressed, this.decompressGeneric(codesCompressed, Boolean.FALSE, 0));
            compressToOriginal.put(dateCompressed, this.decompressGeneric(dateCompressed, Boolean.TRUE, 0));
            compressToOriginal.put(numberCompressed, this.decompressGeneric(numberCompressed, Boolean.TRUE, this.compressorConfiguration.getPadLength()));

            List<String> resultOriginals = new ArrayList<>();
            compressToOriginal.forEach((compressed, original) -> resultOriginals.add(original));

            return String.join(this.compressorConfiguration.getSeparator(), resultOriginals);
        }

        return input;
    }

    /**
     * SE REALIZA LA ENCRIPTACION DEL CAMPO E_FACTURA PARA LA VERSION 1 UTILIZADA DE BANRED
     * NECESARIO PARA AJUSTAR LA CANTIDAD DE CARACTERES QUE SE INGRESARAN A LA BASE DE DATOS EN EL CAMPO_ALT_2 DE LA TABLA CC_TRAN_SERVICIO
     * Y RECUPERAR EL VALOR DEL MISMO
     *
     * @param input - EL VALOR CONCATENADO DE E_FACTURA
     * @return String - LA ENCRIPTACION DE E_FACTURA
     * */
    public String encryptInvoiceValuesforV1(String input) {
        if (StringUtils.isEmpty(input)) {
            log.warn(CODE_AUTH_WARN);
            return input;
        }

        log.info("Se encriptan los valores del campo e_factura");
        if (!PATTERN_INVOICE_COMPILED.matcher(input).matches()) {
            log.warn("Formato no reconocido, se retorna sin compresión");
            return input;
        }

        String[] parts = input.split("/", -1);
        if (parts.length != 6) {
            log.error("Formato inválido para compresión");
            return input;
        }

        List<String> originalParts = Arrays.asList(parts);
        List<String> compressedParts = originalParts.stream()
                .map(this::compressGeneric)
                .toList();

        // Construir el resultado final uniendo las partes comprimidas
        return String.join("/", compressedParts);
    }

    /**
     * Metodo que entrega un tipo de texto desencriptado/descomprimido para realizar insercion de codigos de autorizacion
     * a la base de datos (VALIDO SOLO PARA PAYLOADS DE VERSION 1 DE BANRED)
     *
     * @param input - el texto encriptado/comprimido
     * @return String - el codigo de autorizacion en su forma base
     * */
    public String decryptSpecificValueForInvoice(String input, String label) {
        if (StringUtils.isEmpty(input)) {
            log.warn(CODE_AUTH_WARN);
            return input;
        }

        String[] parts = input.split("/", -1);
        if (parts.length != 6) {
            return input;
        }

        // Mapear las partes con etiquetas estándar en minúscula para buscar rápido
        Map<String, String> partsMap = Map.of(
                "fecha", parts[0],
                "hora", parts[1],
                "e_hora", parts[1],
                "secuenciaaut", parts[2],
                "retencion", parts[3],
                "base", parts[4],
                "valorpagado", parts[5]
        );

        String key = label.toLowerCase();
        String compressedValue = partsMap.get(key);
        if (compressedValue == null) {
            log.warn("Etiqueta desconocida: {}", label);
            return input;
        }

        log.info("Se desencriptan los valores del campo e_factura");
        // Verificar si coincide con el patrón para decidir si descomprimir
        if (PATTERN_INVOICE_COMPILED.matcher(input).matches()) {
            return decompressGeneric(compressedValue, Boolean.TRUE, 0);
        } else {
            return compressedValue;
        }
    }

    private String encodeBase62(BigInteger number) {
        if (number.equals(BigInteger.ZERO)) return "0";
        StringBuilder builder = new StringBuilder();
        BigInteger base = BigInteger.valueOf(this.compressorConfiguration.getBaseChar().length());
        while (number.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRemainder = number.divideAndRemainder(base);
            builder.insert(0, this.compressorConfiguration.getBaseChar().charAt(divRemainder[1].intValue()));
            number = divRemainder[0];
        }
        return builder.toString();
    }

    private BigInteger decodeBase62(String input) {
        BigInteger result = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(this.compressorConfiguration.getBaseChar().length());
        for (char auxChar : input.toCharArray()) {
            int index = this.compressorConfiguration.getBaseChar().indexOf(auxChar);
            if (index == -1) log.error("CARACTER NO VALIDO: {}", auxChar);
            result = result.multiply(base).add(BigInteger.valueOf(index));
        }
        return result;
    }

    private String compressGeneric(String input) {
        if (StringUtils.isEmpty(input)) {
            log.error("NO EXISTE INFORMACION PARA COMPRIMIR");
            return input;
        }
        BigInteger value;
        if (input.matches("\\d+")) {
            // Si es solo numérico, parsea directamente
            value = new BigInteger(input);
        } else {
            // Si es alfanumérico, convierte a BigInteger mediante base 128 (ASCII)
            value = BigInteger.ZERO;
            for (char aux : input.toCharArray()) {
                value = value.multiply(this.compressorConfiguration.getDivisor()).add(BigInteger.valueOf(aux));
            }
        }

        return encodeBase62(value);
    }

    private String decompressGeneric(String input, boolean wasNotAlphaNumeric, int padLength) {
        if (StringUtils.isEmpty(input)) {
            log.error("NO EXISTE INFORMACION PARA DESCOMPRIMIR");
            return input;
        }

        BigInteger value = decodeBase62(input);
        if (wasNotAlphaNumeric) {
            // Si era solo numérico, se recupera directamente el número
            String result = value.toString();
            if (padLength != 0)
                return StringUtils.leftPad(result, padLength, this.compressorConfiguration.getFiller());
            return value.toString();
        } else {
            // Si era alfanumérico, reconstruir la cadena desde base128
            StringBuilder builder = new StringBuilder();
            while (value.compareTo(BigInteger.ZERO) > 0) {
                BigInteger[] divRemainder = value.divideAndRemainder(this.compressorConfiguration.getDivisor());
                builder.insert(0, (char) divRemainder[1].intValue());
                value = divRemainder[0];
            }

            return builder.toString();
        }
    }
}
