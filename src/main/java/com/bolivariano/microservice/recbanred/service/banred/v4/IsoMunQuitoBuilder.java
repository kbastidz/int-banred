package com.bolivariano.microservice.recbanred.service.banred.v4;

import com.bolivariano.microservice.recbanred.core.configuration.BanredConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.MunQuitoConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Construye las tramas ISO 8583 en texto plano para el Municipio del
 * Distrito Metropolitano de Quito (V4 – TCP).
 *
 * Estructura general de la trama (basada en ficha técnica y tramas de ejemplo):
 *
 * [HEADER][MTI][BITMAP_PRIM][BITMAP_SEC][BIT2][BIT3][BIT7][BIT11][BIT12]
 * [BIT13][BIT15][BIT32][BIT33][BIT37][BIT41][BIT49][BIT52][BIT108][BIT109]
 *
 * Header = "ISO01400007" + dígito institución (fijo "3" = "ISO014000073")
 *
 * MTI:
 *   0200 = consulta (tipoTx 310004) / pago (tipoTx 010005)
 *   0420 = reverso
 *
 * Bitmaps: se calculan dinámicamente según bits activos.
 *
 * Longitudes de campos (según Anexo 1 de la ficha técnica):
 *   Bit  2: llvar N 19  → Llave Municipal
 *   Bit  3: 6N          → Tipo transacción
 *   Bit  4: 12N         → Valor total (solo pago/reverso)
 *   Bit  7: 14N         → Fecha/hora Banred  YYYYMMDDHHMMSS
 *   Bit 11: 6N          → Secuencial Banred
 *   Bit 12: 6N          → Hora local HHMMSS
 *   Bit 13: 8N          → Fecha local YYYYMMDD
 *   Bit 15: 8N          → Fecha compensación YYYYMMDD
 *   Bit 25: 2N          → Indicador reverso (solo 0420)
 *   Bit 32: llvar N 6   → Institución adquirente
 *   Bit 33: 6AN         → Código operador
 *   Bit 37: 6N          → Secuencial adquirente
 *   Bit 41: 16AN        → Número de terminal
 *   Bit 49: 3N          → Tipo moneda (840)
 *   Bit 52: 10N         → Producto (0010031004)
 *   Bit 56: 40AN        → Datos tx original (solo reverso)
 *   Bit 67: llvar N     → Detalle valores a pagar arreglo 1 (pago)
 *   Bit 68: llvar N     → Detalle valores a pagar arreglo 2 (pago)
 *   Bit108: llvar N 3   → Código canal
 *   Bit109: llvar N 3   → Grupo de impuesto
 */
@Component
public class IsoMunQuitoBuilder {

    /* Tipos de transacción según ficha técnica */
    public static final String TX_CONSULTA = "310004";
    public static final String TX_PAGO_REVERSO = "010005";

    /* Grupos de impuesto (Bit 109) */
    public static final String GRUPO_PREDIOS  = "001";
    public static final String GRUPO_PATENTES = "002";
    public static final String GRUPO_VARIOS   = "003";

    /* Indicadores de reverso (Bit 25) */
    public static final String REV_ERROR_COMUNICACION = "01";
    public static final String REV_ANULACION_OPERADOR = "02";
    public static final String REV_TIMEOUT_DISPOSITIVO = "03";
    public static final String REV_TIMEOUT_AUTORIZADOR = "04";

    private static final DateTimeFormatter FMT_BANRED = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter FMT_DATE   = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FMT_TIME   = DateTimeFormatter.ofPattern("HHmmss");

    private final MunQuitoConfiguration munQuitoConfig;
    private final BanredConfiguration banredConfig;

    public IsoMunQuitoBuilder(MunQuitoConfiguration munQuitoConfig,
                               BanredConfiguration banredConfig) {
        this.munQuitoConfig = munQuitoConfig;
        this.banredConfig   = banredConfig;
    }

    // =========================================================================
    // API pública
    // =========================================================================

    /**
     * Construye la trama de CONSULTA (0200 con tipoTx 310004).
     *
     * @param llaveMunicipal  Nº de Predio / Patente / Título de Crédito
     * @param secuencial      Secuencial único de la transacción (6 dígitos)
     * @param terminal        Número de terminal (16 chars)
     * @param operador        Código operador (6 chars)
     * @param canal           Código de canal 3 dígitos ("001"…"007")
     * @param grupoImpuesto   Grupo de impuesto 3 dígitos ("001" PREDIOS, "002" PATENTES, "003" VARIOS)
     */
    public String buildConsulta(String llaveMunicipal, String secuencial,
                                String terminal, String operador,
                                String canal, String grupoImpuesto) {
        LocalDateTime now = LocalDateTime.now();

        /* Bits activos en consulta: 2,3,7,11,12,13,15,32,33,37,41,49,52,108,109 */
        long[] bitsActivos = {2, 3, 7, 11, 12, 13, 15, 32, 33, 37, 41, 49, 52, 108, 109};
        String[] bitmaps = buildBitmaps(bitsActivos);

        StringBuilder sb = new StringBuilder();
        sb.append(buildHeader("0200"));
        sb.append(bitmaps[0]);  // bitmap primario
        sb.append(bitmaps[1]);  // bitmap secundario
        sb.append(buildLlvar(llaveMunicipal, 19));         // Bit 2
        sb.append(TX_CONSULTA);                            // Bit 3
        sb.append(now.format(FMT_BANRED));                 // Bit 7  (14N)
        sb.append(padLeft(secuencial, 6, '0'));            // Bit 11
        sb.append(now.format(FMT_TIME));                   // Bit 12
        sb.append(now.format(FMT_DATE));                   // Bit 13
        sb.append(now.format(FMT_DATE));                   // Bit 15 compensación
        sb.append(buildLlvar(munQuitoConfig.getAcquirerAba(), 6)); // Bit 32
        sb.append(padRight(operador, 6));                  // Bit 33
        sb.append(padLeft(secuencial, 6, '0'));            // Bit 37
        sb.append(padRight(terminal, 16));                 // Bit 41
        sb.append("840");                                  // Bit 49
        sb.append(munQuitoConfig.getProductCode());        // Bit 52 (10N)
        sb.append(buildLlvar(canal, 3));                   // Bit 108
        sb.append(buildLlvar(grupoImpuesto, 3));           // Bit 109
        return sb.toString();
    }

    /**
     * Construye la trama de PAGO (0200 con tipoTx 010005).
     *
     * @param llaveMunicipal  Nº de Predio / Patente / Título
     * @param secuencial      Secuencial único (6 dígitos)
     * @param valorTotal      Valor total en centavos (Bit 4: 12N)
     * @param terminal        Terminal (16 chars)
     * @param operador        Código operador (6 chars)
     * @param canal           Código canal 3 dígitos
     * @param grupoImpuesto   Grupo impuesto 3 dígitos
     * @param arreglo1        Detalle valores Bit 67 (pre-construido como llvar)
     * @param arreglo2        Detalle títulos  Bit 68 (pre-construido como llvar)
     */
    public String buildPago(String llaveMunicipal, String secuencial, long valorTotal,
                            String terminal, String operador,
                            String canal, String grupoImpuesto,
                            String arreglo1, String arreglo2) {
        LocalDateTime now = LocalDateTime.now();

        /* Bits activos en pago: 2,3,4,7,11,12,13,15,32,33,37,41,49,52,67,68,108,109 */
        long[] bitsActivos = {2, 3, 4, 7, 11, 12, 13, 15, 32, 33, 37, 41, 49, 52, 67, 68, 108, 109};
        String[] bitmaps = buildBitmaps(bitsActivos);

        StringBuilder sb = new StringBuilder();
        sb.append(buildHeader("0200"));
        sb.append(bitmaps[0]);
        sb.append(bitmaps[1]);
        sb.append(buildLlvar(llaveMunicipal, 19));         // Bit 2
        sb.append(TX_PAGO_REVERSO);                        // Bit 3
        sb.append(padLeft(String.valueOf(valorTotal), 12, '0')); // Bit 4
        sb.append(now.format(FMT_BANRED));                 // Bit 7
        sb.append(padLeft(secuencial, 6, '0'));            // Bit 11
        sb.append(now.format(FMT_TIME));                   // Bit 12
        sb.append(now.format(FMT_DATE));                   // Bit 13
        sb.append(now.format(FMT_DATE));                   // Bit 15
        sb.append(buildLlvar(munQuitoConfig.getAcquirerAba(), 6)); // Bit 32
        sb.append(padRight(operador, 6));                  // Bit 33
        sb.append(padLeft(secuencial, 6, '0'));            // Bit 37
        sb.append(padRight(terminal, 16));                 // Bit 41
        sb.append("840");                                  // Bit 49
        sb.append(munQuitoConfig.getProductCode());        // Bit 52
        sb.append(StringUtils.defaultString(arreglo1));    // Bit 67 llvar
        sb.append(StringUtils.defaultString(arreglo2));    // Bit 68 llvar
        sb.append(buildLlvar(canal, 3));                   // Bit 108
        sb.append(buildLlvar(grupoImpuesto, 3));           // Bit 109
        return sb.toString();
    }

    /**
     * Construye la trama de REVERSO (0420 con tipoTx 010005).
     *
     * @param llaveMunicipal   Nº de Predio / Patente / Título
     * @param secuencial       Secuencial actual (6 dígitos)
     * @param valorTotal       Valor total en centavos
     * @param indicadorReverso Código del motivo (01-04 según ficha técnica)
     * @param terminal         Terminal (16 chars)
     * @param operador         Código operador (6 chars)
     * @param canal            Código canal 3 dígitos
     * @param grupoImpuesto    Grupo impuesto 3 dígitos
     * @param datosTxOriginal  Bit 56 de la tx original (40AN): MTI+seq+fecha+hora+terminal
     */
    public String buildReverso(String llaveMunicipal, String secuencial, long valorTotal,
                               String indicadorReverso,
                               String terminal, String operador,
                               String canal, String grupoImpuesto,
                               String datosTxOriginal) {
        LocalDateTime now = LocalDateTime.now();

        /* Bits activos en reverso: 2,3,4,7,11,12,13,15,25,32,33,37,41,49,52,56,108,109 */
        long[] bitsActivos = {2, 3, 4, 7, 11, 12, 13, 15, 25, 32, 33, 37, 41, 49, 52, 56, 108, 109};
        String[] bitmaps = buildBitmaps(bitsActivos);

        StringBuilder sb = new StringBuilder();
        sb.append(buildHeader("0420"));
        sb.append(bitmaps[0]);
        sb.append(bitmaps[1]);
        sb.append(buildLlvar(llaveMunicipal, 19));         // Bit 2
        sb.append(TX_PAGO_REVERSO);                        // Bit 3
        sb.append(padLeft(String.valueOf(valorTotal), 12, '0')); // Bit 4
        sb.append(now.format(FMT_BANRED));                 // Bit 7
        sb.append(padLeft(secuencial, 6, '0'));            // Bit 11
        sb.append(now.format(FMT_TIME));                   // Bit 12
        sb.append(now.format(FMT_DATE));                   // Bit 13
        sb.append(now.format(FMT_DATE));                   // Bit 15
        sb.append(padLeft(indicadorReverso, 2, '0'));      // Bit 25
        sb.append(buildLlvar(munQuitoConfig.getAcquirerAba(), 6)); // Bit 32
        sb.append(padRight(operador, 6));                  // Bit 33
        sb.append(padLeft(secuencial, 6, '0'));            // Bit 37 (igual al pago original)
        sb.append(padRight(terminal, 16));                 // Bit 41
        sb.append("840");                                  // Bit 49
        sb.append(munQuitoConfig.getProductCode());        // Bit 52
        sb.append(padRight(StringUtils.defaultString(datosTxOriginal), 40)); // Bit 56
        sb.append(buildLlvar(canal, 3));                   // Bit 108
        sb.append(buildLlvar(grupoImpuesto, 3));           // Bit 109
        return sb.toString();
    }

    /**
     * Construye el arreglo 1 (Bit 67) para el request de pago.
     * Estructura por ocurrencia: codigoTipoImpuesto(3) + descripcion(22) + anioDeuda(4) + montoDeuda(10.2)
     */
    public String buildArreglo1(String codigoTipoImpuesto, String descripcion,
                                String anioDeuda, long montoDeuda) {
        String item = padLeft(codigoTipoImpuesto, 3, '0')
                + padRight(descripcion, 22)
                + padLeft(anioDeuda, 4, '0')
                + padLeft(String.valueOf(montoDeuda), 10, '0') + "00"; // 9(08)V99
        return buildLlvarVariable(item);
    }

    /**
     * Construye el arreglo 2 (Bit 68) para el request de pago.
     * Estructura por ocurrencia: numTitulo(12)+prioridad(3)+indice(2)+numObra(5)+numDividendo(2)+codEspecial(1)
     */
    public String buildArreglo2(String numTitulo, String prioridad, String indice,
                                String numObra, String numDividendo, String codEspecial) {
        String item = padLeft(numTitulo, 12, '0')
                + padLeft(prioridad, 3, '0')
                + padLeft(indice, 2, '0')
                + padLeft(numObra, 5, '0')
                + padLeft(numDividendo, 2, '0')
                + padRight(codEspecial, 1);
        return buildLlvarVariable(item);
    }

    /**
     * Construye el Bit 56 (datos de la transacción original) para el reverso.
     * Composición: MTI(4) + secAdquirente(6) + fechaLocal(8) + horaLocal(6) + terminal(16) = 40
     */
    public String buildDatosTxOriginal(String secuencialAdquirente,
                                        String fechaLocal, String horaLocal,
                                        String terminal) {
        return "0200"
                + padLeft(secuencialAdquirente, 6, '0')
                + padLeft(fechaLocal, 8, '0')
                + padLeft(horaLocal, 6, '0')
                + padRight(terminal, 16);
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /** Genera el header: prefijo + dígito institución + MTI. */
    private String buildHeader(String mti) {
        return munQuitoConfig.getHeaderPrefix()
                + munQuitoConfig.getHeaderInstitution()
                + mti;
    }

    /**
     * Calcula los bitmaps primario y secundario (cada uno 16 chars hexadecimales = 64 bits).
     * Bit 1 presente en bitmap primario implica que existe bitmap secundario.
     */
    private String[] buildBitmaps(long[] bits) {
        long primary   = 0L;
        long secondary = 0L;

        for (long bit : bits) {
            if (bit >= 1 && bit <= 64) {
                primary |= (1L << (64 - bit));
            } else if (bit >= 65 && bit <= 128) {
                secondary |= (1L << (128 - bit));
                primary   |= (1L << (64 - 1)); // marca presencia de bitmap secundario
            }
        }

        return new String[]{
                String.format("%016X", primary),
                String.format("%016X", secondary)
        };
    }

    /**
     * Codifica un campo llvar (longitud 2 dígitos + valor) con longitud máxima indicada.
     * Formato: LL + valor (alineado a la izquierda, sin relleno adicional).
     */
    private String buildLlvar(String value, int maxLen) {
        String trimmed = StringUtils.defaultString(value).trim();
        if (trimmed.length() > maxLen) trimmed = trimmed.substring(0, maxLen);
        return String.format("%02d", trimmed.length()) + trimmed;
    }

    /**
     * Codifica un campo llvar de longitud variable (longitud 3 dígitos + valor).
     */
    private String buildLlvarVariable(String value) {
        String v = StringUtils.defaultString(value);
        return String.format("%03d", v.length()) + v;
    }

    private String padLeft(String value, int length, char pad) {
        return StringUtils.leftPad(StringUtils.defaultString(value).trim(), length, pad);
    }

    private String padRight(String value, int length) {
        return StringUtils.rightPad(StringUtils.defaultString(value).trim(), length, ' ');
    }
}
