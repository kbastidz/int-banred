package com.bolivariano.microservice.recbanred.util;

import com.bolivariano.microservice.recbanred.core.configuration.DataMaskingConfiguration;
import com.bolivariano.microservice.recbanred.core.exceptions.BadFormatException;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatoAdicional;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaConsultarDeuda;
import com.bolivariano.microservice.recbanred.core.payloads.input.Servicio;
import com.bolivariano.microservice.recbanred.core.payloads.output.MensajeSalidaConsultarDeuda;
import org.apache.commons.lang3.StringUtils;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.VALIDATION_ERROR;

/**
 * Utilidades de enmascaramiento, sanitización y validación para el flujo de CONSULTA.
 * Solo cuando data-masking.enabled = true:
 *  1. Sanitizar campos clave (secuencial, transaccion, usuario) contra injection (XSS, script,
 *     HTML tags, caracteres de control). Si contienen patrones peligrosos → BadFormatException.
 *  2. Rechazar campos clave que superen maxKeyFieldLength → BadFormatException.
 *  3. Sanear silenciosamente los valores de DatosAdicionales (strip de tags + OWASP encode).
 *  4. Truncar valores de DatosAdicionales que superen maxFieldLength (anti-DoS / buffer-overflow).
 *  5. Devolver una copia con los campos sensitiveFieldCodes enmascarados para uso en logs.
 * Cuando data-masking.enabled = false, el request/respuesta continúa sin sanitización ni enmascaramiento.
 */
@Component
public class DataMaskingUtils {

    private static final Logger log = LoggerFactory.getLogger(DataMaskingUtils.class);

    /** Carácter de relleno. Se dejan visibles los últimos {@value VISIBLE_SUFFIX} caracteres. */
    private static final char MASK_CHAR = '*';
    private static final int VISIBLE_SUFFIX = 4;

    /**
     * Detecta etiquetas HTML/XML, entidades numéricas y manejadores de eventos JS.
     * Ejemplos que captura: <script>, </div>, <img/>, &#x3C;, javascript:, onclick=
     */
    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "<[^>]*>|&#x?[0-9a-f]+;|javascript\\s*:|on\\w+\\s*=",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Patrón permitido para campos clave estructurales (secuencial, transaccion, usuario).
     * Solo alfanuméricos, guión, guion bajo y punto.
     */
    private static final Pattern SAFE_KEY_FIELD_PATTERN = Pattern.compile("^[a-zA-Z0-9_.\\-]*$");

    private final DataMaskingConfiguration maskingConfig;

    public DataMaskingUtils(DataMaskingConfiguration maskingConfig) {
        this.maskingConfig = maskingConfig;
    }

    /**
     * Punto de entrada principal — aplica sobre el objeto de consulta.
     * Si data-masking.enabled = true, ejecuta sanitización/validación y devuelve una copia
     * con campos sensibles enmascarados para log; si está deshabilitado, retorna el mismo objeto.
     *
     * @param inquiryRq objeto de consulta de entrada (se sanea in-place en DatosAdicionales,
     *                  los campos clave solo se validan)
     * @return copia enmascarada si masking está habilitado, o el mismo objeto si no
     * @throws BadFormatException si un campo clave es inválido o supera su longitud máxima
     */
    public MensajeEntradaConsultarDeuda applyMasking(MensajeEntradaConsultarDeuda inquiryRq) throws BadFormatException {
        if (!maskingConfig.isEnabled()) {
            return inquiryRq;
        }

        sanitizeAndValidate(inquiryRq);

        return buildMaskedCopy(inquiryRq);
    }

    /**
     * Enmascara datos sensibles en la respuesta de CONSULTA cuando la bandera esta habilitada.
     */
    public MensajeSalidaConsultarDeuda applyMaskingToInquiryResponse(MensajeSalidaConsultarDeuda inquiryRs) {
        if (inquiryRs == null || !maskingConfig.isEnabled()) {
            return inquiryRs;
        }

        if (inquiryRs.getDatosAdicionales() != null && inquiryRs.getDatosAdicionales().getDatoAdicional() != null) {
            List<DatoAdicional> maskedList = inquiryRs.getDatosAdicionales().getDatoAdicional().stream()
                    .map(this::maskDatoAdicional)
                    .toList();
            DatosAdicionales maskedDatos = new DatosAdicionales();
            maskedDatos.setDatoAdicional(maskedList);
            inquiryRs.setDatosAdicionales(maskedDatos);
        }

        if (isSensitiveCode("documentID")) {
            inquiryRs.setIdentificadorDeuda(mask(inquiryRs.getIdentificadorDeuda()));
        }
        if (isSensitiveCode("fullName")) {
            inquiryRs.setNombreCliente(mask(inquiryRs.getNombreCliente()));
        }

        return inquiryRs;
    }

    // -------------------------------------------------------------------------
    // 1-4. Sanitización + validación (solo cuando masking está habilitado)
    // -------------------------------------------------------------------------

    private void sanitizeAndValidate(MensajeEntradaConsultarDeuda inquiryRq) throws BadFormatException {
        int maxKey   = maskingConfig.getMaxKeyFieldLength();
        int maxField = maskingConfig.getMaxFieldLength();

        validateKeyFields(inquiryRq, maxKey);
        sanitizeAdditionalDataValues(inquiryRq, maxField);
    }

    private void validateKeyFields(MensajeEntradaConsultarDeuda inquiryRq, int maxKey) throws BadFormatException {

        // Campos clave: validación estricta — rechazan si contienen injection o superan longitud
        validateKeyField("secuencial",  inquiryRq.getSecuencial(),  maxKey);
        validateKeyField("transaccion", inquiryRq.getTransaccion(), maxKey);
        validateKeyField("usuario",     inquiryRq.getUsuario(),     maxKey);
        validateKeyField("identificador",inquiryRq.getServicio().getIdentificador(),     maxKey);
    }

    private void sanitizeAdditionalDataValues(MensajeEntradaConsultarDeuda inquiryRq, int maxField) {

        // DatosAdicionales: saneamiento silencioso + truncado anti-DoS
        if (inquiryRq.getServicio() == null) {
            return;
        }

        DatosAdicionales datosAdicionales = inquiryRq.getServicio().getDatosAdicionales();
        if (datosAdicionales == null || datosAdicionales.getDatoAdicional() == null) {
            return;
        }

        for (DatoAdicional dato : datosAdicionales.getDatoAdicional()) {
            String original  = dato.getValor();
            String sanitized = sanitizeFieldValue(original);

            if (!Objects.equals(sanitized, original)) {
                log.warn("Campo adicional [{}] contenía caracteres peligrosos y fue saneado.", Encode.forJava(dato.getCodigo()));
            }

            if (StringUtils.isNotEmpty(sanitized) && sanitized.length() > maxField) {
                log.warn("Campo adicional [{}] supera {} caracteres. Se truncará.", Encode.forJava(dato.getCodigo()), maxField);
                sanitized = sanitized.substring(0, maxField);
            }

            dato.setValor(sanitized);
        }
    }

    /**
     * Valida un campo clave de cabecera:
     *  - Longitud máxima → BadFormatException.
     *  - Patrón de injection → BadFormatException.
     *  - Caracteres fuera del alfabeto permitido → BadFormatException.
     */
    private void validateKeyField(String fieldName, String value, int maxLength) throws BadFormatException {
        if (StringUtils.isEmpty(value)) return;

        if (value.length() > maxLength) {
            log.error("Campo [{}] supera la longitud máxima de {} caracteres.", fieldName, maxLength);
            throw new BadFormatException(
                    "El campo supera la longitud máxima permitida.",
                    null, VALIDATION_ERROR
            );
        }

        if (INJECTION_PATTERN.matcher(value).find()) {
            log.error("Campo [{}] contiene patrones de inyección no permitidos.", fieldName);
            throw new BadFormatException(
                    "El campo contiene caracteres no permitidos.",
                    null, VALIDATION_ERROR
            );
        }

        if (!SAFE_KEY_FIELD_PATTERN.matcher(value).matches()) {
            log.error("Campo [{}] contiene caracteres especiales no permitidos: [{}]",
                    fieldName, Encode.forJava(value));
            throw new BadFormatException(
                    "El campo  contiene caracteres especiales no permitidos.",
                    null, VALIDATION_ERROR
            );
        }
    }

    /**
     * Sanea un valor de campo libre:
     *  1. Elimina etiquetas HTML/XML y patrones de script.
     *  2. Codifica caracteres peligrosos restantes con OWASP Encoder (forJava).
     *  3. Elimina caracteres de control ASCII (0x00-0x1F).
     */
    private String sanitizeFieldValue(String value) {
        if (StringUtils.isEmpty(value)) return value;

        // Strip de tags e injection patterns
        String cleaned = INJECTION_PATTERN.matcher(value).replaceAll("");

        // OWASP encode + limpieza de escapes residuales
        cleaned = Encode.forJava(cleaned)
                .replace("\\\"", "\"")
                .replace("\\n", "")
                .replace("\\r", "")
                .replace("\\t", "");

        // Eliminar caracteres de control (0x00-0x1F excepto espacio 0x20)
        return cleaned.replaceAll("[\\x00-\\x1F]", "");
    }

    // -------------------------------------------------------------------------
    // 5. Copia enmascarada para log (solo cuando masking está habilitado)
    // -------------------------------------------------------------------------

    private MensajeEntradaConsultarDeuda buildMaskedCopy(MensajeEntradaConsultarDeuda original) {
        MensajeEntradaConsultarDeuda masked = new MensajeEntradaConsultarDeuda();
        masked.setCanal(original.getCanal());
        masked.setDepuracion(original.getDepuracion());
        masked.setFecha(original.getFecha());
        masked.setOficina(original.getOficina());
        masked.setSecuencial(original.getSecuencial());
        masked.setTransaccion(original.getTransaccion());
        masked.setUsuario(original.getUsuario());

        if (original.getServicio() != null) {
            masked.setServicio(buildMaskedServicio(original.getServicio()));
        }

        return masked;
    }

    private Servicio buildMaskedServicio(Servicio original) {
        Servicio maskedServicio = new Servicio();
        maskedServicio.setCodTipoServicio(original.getCodTipoServicio());
        maskedServicio.setCodigoConvenio(original.getCodigoConvenio());
        maskedServicio.setCodigoEmpresa(original.getCodigoEmpresa());
        maskedServicio.setCodigoTipoBanca(original.getCodigoTipoBanca());
        maskedServicio.setCodigoTipoIdentificador(original.getCodigoTipoIdentificador());
        maskedServicio.setIdentificador(original.getIdentificador());

        if (original.getDatosAdicionales() != null && original.getDatosAdicionales().getDatoAdicional() != null) {
            List<DatoAdicional> maskedList = original.getDatosAdicionales().getDatoAdicional().stream()
                    .map(this::maskDatoAdicional)
                    .toList();
            DatosAdicionales maskedDatos = new DatosAdicionales();
            maskedDatos.setDatoAdicional(maskedList);
            maskedServicio.setDatosAdicionales(maskedDatos);
        }

        return maskedServicio;
    }

    private DatoAdicional maskDatoAdicional(DatoAdicional original) {
        DatoAdicional copy = new DatoAdicional();
        copy.setCodigo(original.getCodigo());
        copy.setEtiqueta(original.getEtiqueta());
        copy.setEditable(original.getEditable());
        copy.setFormato(original.getFormato());
        copy.setListasSeleccion(original.getListasSeleccion());
        copy.setLongitud(original.getLongitud());
        copy.setMascara(original.getMascara());
        copy.setRegexp(original.getRegexp());
        copy.setTipo(original.getTipo());
        copy.setVisible(original.getVisible());

        // La regla de enmascaramiento se evalua por el valor de `codigo`, no por la clave JSON `valor`.
        boolean isSensitive = isSensitiveCode(original.getCodigo());
        copy.setValor(isSensitive ? mask(original.getValor()) : original.getValor());

        return copy;
    }

    private boolean isSensitiveCode(String code) {
        String normalizedCode = StringUtils.trimToEmpty(code).toLowerCase(Locale.ROOT);
        return maskingConfig.getSensitiveFieldCodes() != null
                && maskingConfig.getSensitiveFieldCodes().stream()
                .filter(StringUtils::isNotBlank)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(normalizedCode::equals);
    }

    /**
     * Enmascara un valor dejando visibles solo los últimos {@value VISIBLE_SUFFIX} caracteres.
     * Valores nulos, vacíos o más cortos que el sufijo se enmascaran completamente.
     */
    private String mask(String value) {
        if (StringUtils.isEmpty(value)) return value;
        if (value.length() <= VISIBLE_SUFFIX) return StringUtils.repeat(MASK_CHAR, value.length());
        return StringUtils.repeat(MASK_CHAR, value.length() - VISIBLE_SUFFIX)
                + value.substring(value.length() - VISIBLE_SUFFIX);
    }
}
