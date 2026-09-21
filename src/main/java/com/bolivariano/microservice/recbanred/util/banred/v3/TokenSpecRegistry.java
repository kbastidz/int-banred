package com.bolivariano.microservice.recbanred.util.banred.v3;

import com.bolivariano.microservice.recbanred.core.enums.banred.SubServicioMungye;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoBiller;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoOperacionToken;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.token.CampoToken;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.token.EspecificacionToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.VALIDATION_ERROR;
import static com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.token.CampoToken.*;

/**
 * Registro central, en memoria, de las especificaciones de token Q0/Q1 de
 * cada biller soportado por la V3, segun los Anexos "Estructura Estandar
 * Token Trama Generica Q0/Q1" de CNEL, MEER y MUNGYE.
 *
 * IMPORTANTE: Estas especificaciones fueron construidas y VALIDADAS contra
 * ejemplos reales de SOAP (BillInquiryRq/Rs) para CNEL y MEER (coincidencia
 * exacta de longitudes y valores). Para MUNGYE-PREDIOS existe una
 * observacion pendiente de confirmar con el biller: en produccion, la
 * cantidad de pares AÑO/VALOR del bloque repetido parece ser variable
 * (no siempre 12), por lo que el motor de parseo calcula dinamicamente
 * cuantas repeticiones caben en la longitud real recibida.
 */
@Component
public class TokenSpecRegistry {

    private final Map<String, EspecificacionToken> specsQ0 = new java.util.HashMap<>();
    private final Map<String, EspecificacionToken> specsQ1 = new java.util.HashMap<>();

    public TokenSpecRegistry() {
        registrarCnel();
        registrarMeer();
        registrarMungye();
    }

    // ==================================================================
    // API PUBLICA
    // ==================================================================

    public EspecificacionToken getQ0(TipoBiller biller, SubServicioMungye subServicio, TipoOperacionToken operacion) throws CustomException {
        return obtener(specsQ0, biller, subServicio, operacion, "Q0");
    }

    public EspecificacionToken getQ1(TipoBiller biller, SubServicioMungye subServicio, TipoOperacionToken operacion) throws CustomException {
        return obtener(specsQ1, biller, subServicio, operacion, "Q1");
    }

    private EspecificacionToken obtener(Map<String, EspecificacionToken> mapa, TipoBiller biller,
                                         SubServicioMungye subServicio, TipoOperacionToken operacion, String qtype) throws CustomException {
        String key = clave(biller, subServicio, operacion);
        EspecificacionToken spec = mapa.get(key);
        if (spec == null)
            throw new CustomException("No existe especificacion " + qtype + " registrada para: " + key,
                    null, VALIDATION_ERROR);
        return spec;
    }

    private static String clave(TipoBiller biller, SubServicioMungye subServicio, TipoOperacionToken operacion) {
        return biller.name() + "|" + (subServicio == null ? "-" : subServicio.name()) + "|" + operacion.name();
    }

    private void put(TipoBiller biller, SubServicioMungye sub, TipoOperacionToken op, EspecificacionToken q0, EspecificacionToken q1) {
        specsQ0.put(clave(biller, sub, op), q0);
        specsQ1.put(clave(biller, sub, op), q1);
    }

    // ==================================================================
    // CNEL
    // ==================================================================

    private void registrarCnel() {
        // ---- INQUIRY ----
        EspecificacionToken inquiryQ0 = new EspecificacionToken("CNEL-INQUIRY-Q0", List.of(
                numerico("COD_INSTITUCION_FINANCIERA", 5),
                alfa("COD_OPERADOR", 6)
        ));
        EspecificacionToken inquiryQ1 = new EspecificacionToken("CNEL-INQUIRY-Q1", List.of(
                monto("TOTAL_PENDIENTE_PAGO", 12),
                monto("INTERES", 12),
                monto("INFRACCION", 12),
                alfa("FECHA_HORA_LOCAL", 14),     // YYYYMMDDHHmmss
                monto("TOTAL_AMOUNT", 12),
                alfa("SWITCH_TRACE_AUDIT_NUMBER", 6),
                monto("RETENCION", 12),
                monto("BASE", 12),
                numerico("COD_INSTITUCION_FINANCIERA", 5),
                alfa("COD_OPERADOR", 6),
                variable("NOMBRE_CLIENTE"),
                variable("TIPO_DISTRIBUCION"),
                variable("DIRECCION_SERVICIO"),
                alfa("REFERENCIA_CLIENTE", 13),
                alfa("FECHA_INICIAL_LECTURA", 8),
                alfa("FECHA_FINAL_LECTURA", 8),
                alfa("FECHA_EMISION", 8),
                numerico("FACTURAS_PENDIENTES", 2),
                alfa("FECHA_VENCIMIENTO", 8),
                numerico("CONSUMO_KWH", 8),
                alfa("NUMERO_FACTURA", 20),
                monto("DEUDA_TOTAL", 12),
                monto("DEUDA_ANTERIOR", 12)
        ));
        put(TipoBiller.CNEL, null, TipoOperacionToken.INQUIRY, inquiryQ0, inquiryQ1);

        // ---- PAYMENT ----
        EspecificacionToken paymentQ0 = new EspecificacionToken("CNEL-PAYMENT-Q0", List.of(
                alfa("SWITCH_TRACE_AUDIT_NUMBER", 6),
                monto("BASE", 12),
                numerico("COD_INSTITUCION_FINANCIERA", 5),
                alfa("COD_OPERADOR", 6),
                alfa("REFERENCIA_CLIENTE", 13),
                alfa("FECHA_EMISION", 8),
                alfa("FECHA_VENCIMIENTO", 8),
                alfa("NUMERO_FACTURA", 20),
                monto("TOTAL_PENDIENTE_PAGO", 12)
        ));
        EspecificacionToken paymentQ1 = new EspecificacionToken("CNEL-PAYMENT-Q1", List.of(
                alfa("SWITCH_TRACE_AUDIT_NUMBER", 6),
                monto("BASE", 12),
                numerico("COD_INSTITUCION_FINANCIERA", 5),
                alfa("COD_OPERADOR", 6),
                alfa("COD_AUTORIZACION", 6),
                variable("NOMBRE_CLIENTE"),
                variable("TIPO_DISTRIBUCION"),
                variable("DIRECCION_SERVICIO"),
                alfa("REFERENCIA_CLIENTE", 13),
                alfa("FECHA_INICIAL_LECTURA", 8),
                alfa("FECHA_FINAL_LECTURA", 8),
                alfa("FECHA_EMISION", 8),
                numerico("FACTURAS_PENDIENTES", 2),
                alfa("FECHA_VENCIMIENTO", 8),
                numerico("CONSUMO_KWH", 8),
                alfa("NUMERO_FACTURA", 20),
                monto("DEUDA_TOTAL", 12),
                monto("DEUDA_ANTERIOR", 12),
                monto("VALOR_PAGADO", 12)
        ));
        put(TipoBiller.CNEL, null, TipoOperacionToken.PAYMENT, paymentQ0, paymentQ1);

        // ---- REVERSAL ----
        EspecificacionToken reversalQ0 = new EspecificacionToken("CNEL-REVERSAL-Q0", List.of(
                alfa("FECHA_HORA_LOCAL", 14),
                alfa("SWITCH_TRACE_AUDIT_NUMBER", 6),
                monto("RETENCION", 12),
                monto("BASE", 12),
                numerico("COD_INSTITUCION_FINANCIERA", 5),
                alfa("COD_OPERADOR", 6),
                alfa("REFERENCIA_CLIENTE", 13),
                alfa("FECHA_EMISION", 8),
                alfa("NUMERO_FACTURA", 20),
                monto("VALOR_PAGADO", 12),
                alfa("COD_AUTORIZACION", 6)
        ));
        EspecificacionToken reversalQ1 = new EspecificacionToken("CNEL-REVERSAL-Q1", List.of(
                alfa("FECHA_HORA_LOCAL", 14),
                alfa("SWITCH_TRACE_AUDIT_NUMBER", 6),
                monto("RETENCION", 12),
                monto("BASE", 12),
                numerico("COD_INSTITUCION_FINANCIERA", 5),
                alfa("COD_OPERADOR", 6),
                alfa("REFERENCIA_CLIENTE", 13),
                alfa("FECHA_EMISION", 8),
                alfa("NUMERO_FACTURA", 20),
                monto("VALOR_PAGADO", 12)
        ));
        put(TipoBiller.CNEL, null, TipoOperacionToken.REVERSAL, reversalQ0, reversalQ1);
    }

    // ==================================================================
    // MEER
    // ==================================================================

    private void registrarMeer() {
        // ---- INQUIRY (individual) ----
        EspecificacionToken inquiryQ0 = new EspecificacionToken("MEER-INQUIRY-Q0", List.of(
                numerico("PADLEFT_ZEROS", 10)
        ));
        EspecificacionToken inquiryQ1 = new EspecificacionToken("MEER-INQUIRY-Q1", List.of(
                numerico("BILL_SERVICE_CODE", 2),
                alfa("NUMERO_CONTRATO", 12),
                alfa("CODIGO_REFERENCIA", 10),
                variable("NOMBRE_CLIENTE"),
                // Campo NO documentado formalmente por MEER (ver nota de validacion):
                // se infiere como "monto pendiente"; longitud observada 8, confirmar con biller.
                monto("MONTO_PENDIENTE_INFERIDO", 8)
        ));
        put(TipoBiller.MEER, null, TipoOperacionToken.INQUIRY, inquiryQ0, inquiryQ1);

        // ---- PAYMENT ----
        EspecificacionToken paymentQ0 = new EspecificacionToken("MEER-PAYMENT-Q0", List.of(
                numerico("PADLEFT_ZEROS", 10)
        ));
        EspecificacionToken paymentQ1 = new EspecificacionToken("MEER-PAYMENT-Q1", List.of(
                alfa("PAYMENT_REFERENCE_CODE", 25)
        ));
        put(TipoBiller.MEER, null, TipoOperacionToken.PAYMENT, paymentQ0, paymentQ1);

        // ---- REVERSAL (Q1 "NO APLICA" segun ficha: se usa solo ResultCode/ErrorMessage del sobre SOAP) ----
        EspecificacionToken reversalQ0 = new EspecificacionToken("MEER-REVERSAL-Q0", List.of(
                // Tomar los primeros 10 caracteres del PaymentTransactionID (PAYMENT_REFERENCE_CODE del Q1 de pago)
                alfa("REFERENCE_CODE_Q1_PAYMENT", 25)
        ));
        EspecificacionToken reversalQ1 = new EspecificacionToken("MEER-REVERSAL-Q1", List.of()); // NO APLICA
        put(TipoBiller.MEER, null, TipoOperacionToken.REVERSAL, reversalQ0, reversalQ1);
    }

    /** Especificacion de UN bloque del Q1 de MASS INQUIRY (exitoso) de MEER; se parsea con parseQ1ArregloPipe. */
    public EspecificacionToken getMeerMassInquiryQ1Bloque() {
        return new EspecificacionToken("MEER-MASS_INQUIRY-Q1-BLOQUE", List.of(
                numerico("RESPONSE_CODE", 2),
                alfa("MESSAGE", 19),
                numerico("BILL_SERVICE_CODE", 2),
                alfa("NUMERO_CONTRATO", 12),
                alfa("CODIGO_REFERENCIA", 10),
                variable("NOMBRE_CLIENTE"),
                monto("MONTO_PENDIENTE", 12),
                alfa("ESPACIO", 1),
                alfa("FECHA_VENCIMIENTO", 10)
        ), true);
    }

    /** Especificacion del Q0 de MASS INQUIRY de MEER: un solo campo variable con contratos separados por "|". */
    public EspecificacionToken getMeerMassInquiryQ0() {
        return new EspecificacionToken("MEER-MASS_INQUIRY-Q0", List.of(
                variable("CONTRATOS_PIPE") // el valor ya debe venir concatenado con "|" por el llamador
        ));
    }

    // ==================================================================
    // MUNGYE (Municipio de Guayaquil) - 4 sub-servicios
    // ==================================================================

    private void registrarMungye() {
        registrarMungyeCep();
        registrarMungyePredios();
        registrarMungyeMilote();
        registrarMungyeMercados();
        registrarMungyeSadall();
    }

    private void registrarMungyeCep() {
        EspecificacionToken inquiryQ0 = new EspecificacionToken("MUNGYE-CEP-INQUIRY-Q0", List.of(
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        // No se documenta un Q1 propio para CEP-Inquiry en la ficha (se resuelve via sobre SOAP)
        EspecificacionToken inquiryQ1 = new EspecificacionToken("MUNGYE-CEP-INQUIRY-Q1", List.of());
        put(TipoBiller.MUNGYE, SubServicioMungye.CEP, TipoOperacionToken.INQUIRY, inquiryQ0, inquiryQ1);

        EspecificacionToken paymentQ0 = new EspecificacionToken("MUNGYE-CEP-PAYMENT-Q0", List.of(
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken paymentQ1 = new EspecificacionToken("MUNGYE-CEP-PAYMENT-Q1", List.of(
                alfa("COD_AUTORIZACION", 10),
                alfa("COLLECTION_ID", 10),
                alfa("FECHA_COBRO", 19), // dd-MM-yyyy HH:mm:ss
                numerico("RESULT_CODE", 6)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.CEP, TipoOperacionToken.PAYMENT, paymentQ0, paymentQ1);

        EspecificacionToken reversalQ0 = new EspecificacionToken("MUNGYE-CEP-REVERSAL-Q0", List.of(
                alfa("COD_AUTORIZACION", 10),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken reversalQ1 = new EspecificacionToken("MUNGYE-CEP-REVERSAL-Q1", List.of(
                alfa("COD_AUTORIZACION", 10),
                numerico("RESULT_CODE", 6)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.CEP, TipoOperacionToken.REVERSAL, reversalQ0, reversalQ1);
    }

    private void registrarMungyePredios() {
        EspecificacionToken inquiryQ0 = new EspecificacionToken("MUNGYE-PREDIOS-INQUIRY-Q0", List.of(
                alfa("PROCESS_TYPE", 1, "A"),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken inquiryQ1 = new EspecificacionToken("MUNGYE-PREDIOS-INQUIRY-Q1", List.of(
                numerico("ANIO_ACTUAL", 4),
                alfa("SEMESTRE", 1),
                monto("DEUDA", 10),
                variable("NOMBRE_CLIENTE"),
                alfa("FECHA_REGISTRO", 8),
                // Bloque repetido AÑO/VALOR: hasta 12 años segun ficha. En produccion puede ser
                // variable (ver nota de cabecera de esta clase) - el motor lo calcula dinamicamente.
                grupoRepetido("ANIO_VALOR", 12,
                        numerico("ANIO", 4),
                        monto("VALOR", 10)),
                numerico("ERROR", 2)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.PREDIOS, TipoOperacionToken.INQUIRY, inquiryQ0, inquiryQ1);

        EspecificacionToken paymentQ0 = new EspecificacionToken("MUNGYE-PREDIOS-PAYMENT-Q0", List.of(
                alfa("PROCESS_TYPE", 1, "P"),
                numerico("ANIO_ACTUAL", 4),
                alfa("SEMESTRE", 1),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken paymentQ1 = new EspecificacionToken("MUNGYE-PREDIOS-PAYMENT-Q1", List.of(
                numerico("ANIO_ACTUAL", 4),
                alfa("SEMESTRE", 1),
                monto("DEUDA", 10),
                variable("NOMBRE_CLIENTE"),
                monto("AV_COMERCIAL", 10),
                monto("AV_CATASTRAL", 10),
                monto("AV_IMPONIBLE", 10),
                alfa("TITULO_CREDITO", 11),
                monto("DESCUENTO", 10),
                monto("RECARGO", 10),
                monto("COACTIVA", 10),
                monto("LIQUIDACION", 10),
                alfa("FECHA_REGISTRO", 8),
                numerico("NUMERO_LIQUIDACION", 10),
                numerico("CUIC", 10),
                numerico("ERROR", 2)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.PREDIOS, TipoOperacionToken.PAYMENT, paymentQ0, paymentQ1);

        EspecificacionToken reversalQ0 = new EspecificacionToken("MUNGYE-PREDIOS-REVERSAL-Q0", List.of(
                alfa("PROCESS_TYPE", 1, "R"),
                numerico("ANIO_ACTUAL", 4),
                alfa("SEMESTRE", 1),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken reversalQ1 = new EspecificacionToken("MUNGYE-PREDIOS-REVERSAL-Q1", List.of(
                numerico("ANIO_ACTUAL", 4),
                alfa("SEMESTRE", 1),
                numerico("ERROR", 2)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.PREDIOS, TipoOperacionToken.REVERSAL, reversalQ0, reversalQ1);
    }

    private void registrarMungyeMilote() {
        EspecificacionToken inquiryQ0 = new EspecificacionToken("MUNGYE-MILOTE-INQUIRY-Q0", List.of(
                alfa("TYPE", 3, "VTA"),
                numerico("SUBTYPE", 5, "00023"),
                alfa("PROCESS_TYPE", 1, "C"),
                numerico("DNI", 10),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken inquiryQ1 = new EspecificacionToken("MUNGYE-MILOTE-INQUIRY-Q1", List.of(
                numerico("ANIO", 4),
                numerico("NUMERO", 10),
                variable("NOMBRE_CLIENTE"),
                alfa("VALORES_QUERY", 336), // arreglo de cuotas, formato interno propio del biller
                numerico("CUOTA_MAXIMA", 2)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.MILOTE, TipoOperacionToken.INQUIRY, inquiryQ0, inquiryQ1);

        EspecificacionToken paymentQ0 = new EspecificacionToken("MUNGYE-MILOTE-PAYMENT-Q0", List.of(
                alfa("TYPE", 3, "VTA"),
                numerico("SUBTYPE", 5, "00023"),
                alfa("PROCESS_TYPE", 1, "P"),
                numerico("DNI", 10),
                numerico("ANIO", 4),
                numerico("NUMERO", 10),
                alfa("VALORES_QUERY", 28),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken paymentQ1 = new EspecificacionToken("MUNGYE-MILOTE-PAYMENT-Q1", List.of(
                alfa("FECHA_REGISTRO", 8),
                numerico("ANIO", 4),
                numerico("NUMERO", 10),
                variable("NOMBRE_CLIENTE"),
                alfa("VALORES_QUERY", 28),
                numerico("CUOTA_MAXIMA", 2)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.MILOTE, TipoOperacionToken.PAYMENT, paymentQ0, paymentQ1);

        EspecificacionToken reversalQ0 = new EspecificacionToken("MUNGYE-MILOTE-REVERSAL-Q0", List.of(
                alfa("TYPE", 3, "VTA"),
                numerico("SUBTYPE", 5, "00023"),
                alfa("PROCESS_TYPE", 1, "R"),
                numerico("DNI", 10),
                numerico("ANIO", 4),
                numerico("NUMERO", 10),
                alfa("VALORES_QUERY", 28),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken reversalQ1 = new EspecificacionToken("MUNGYE-MILOTE-REVERSAL-Q1", List.of(
                alfa("FECHA_REGISTRO", 8),
                numerico("ANIO", 4),
                numerico("NUMERO", 10),
                variable("NOMBRE_CLIENTE"),
                alfa("VALORES_QUERY", 28),
                numerico("CUOTA_MAXIMA", 2)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.MILOTE, TipoOperacionToken.REVERSAL, reversalQ0, reversalQ1);
    }

    // ==================================================================
    // MUNGYE - SADALL (Saneamiento Agua - empresa 2245, BillServiceCode=3)
    // Trama Q1 inferida de respuesta real 20260919 (longitud=224).
    // Estructura confirmada:
    //   numerico(5)  → REFERENCIA_DEUDA
    //   monto(12)    → DEUDA_TOTAL
    //   variable     → NOMBRE_CLIENTE
    //   alfa(8)      → FECHA_EMISION     (yyyyMMdd)
    //   alfa(8)      → FECHA_VENCIMIENTO (yyyyMMdd, ultimos 4 en 0000 = sin venc. exacto)
    //   monto(6)     → MONTO_PARCIAL     (campo adicional inferido)
    //   alfa(154)    → DATOS_ADICIONALES (resto de la trama, pendiente de documentacion oficial)
    // NOTA: los campos a partir de MONTO_PARCIAL deben confirmarse con el biller.
    // ==================================================================

    private void registrarMungyeSadall() {
        EspecificacionToken inquiryQ0 = new EspecificacionToken("MUNGYE-SADALL-INQUIRY-Q0", List.of(
                alfa("PROCESS_TYPE", 1, "A"),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken inquiryQ1 = new EspecificacionToken("MUNGYE-SADALL-INQUIRY-Q1", List.of(
                numerico("REFERENCIA_DEUDA", 5),
                monto("DEUDA_TOTAL", 12),
                variable("NOMBRE_CLIENTE"),
                alfa("FECHA_EMISION", 8),
                alfa("FECHA_VENCIMIENTO", 8),
                monto("MONTO_PARCIAL", 6),
                // TODO: confirmar estructura de los 154 caracteres restantes con el biller SADALL
                alfa("DATOS_ADICIONALES", 154)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.SADALL, TipoOperacionToken.INQUIRY, inquiryQ0, inquiryQ1);

        // Q0/Q1 de PAGO y REVERSO: pendientes de documentacion oficial del biller.
        // Se registran con specs vacias para evitar excepcion en TokenSpecRegistry.getQ1().
        EspecificacionToken paymentQ0 = new EspecificacionToken("MUNGYE-SADALL-PAYMENT-Q0", List.of(
                alfa("PROCESS_TYPE", 1, "P"),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken paymentQ1 = new EspecificacionToken("MUNGYE-SADALL-PAYMENT-Q1", List.of());
        put(TipoBiller.MUNGYE, SubServicioMungye.SADALL, TipoOperacionToken.PAYMENT, paymentQ0, paymentQ1);

        EspecificacionToken reversalQ0 = new EspecificacionToken("MUNGYE-SADALL-REVERSAL-Q0", List.of(
                alfa("PROCESS_TYPE", 1, "R"),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken reversalQ1 = new EspecificacionToken("MUNGYE-SADALL-REVERSAL-Q1", List.of());
        put(TipoBiller.MUNGYE, SubServicioMungye.SADALL, TipoOperacionToken.REVERSAL, reversalQ0, reversalQ1);
    }

    private void registrarMungyeMercados() {
        EspecificacionToken inquiryQ0 = new EspecificacionToken("MUNGYE-MERCADOS-INQUIRY-Q0", List.of(
                alfa("PROCESS_TYPE", 1, "C"),
                alfa("INQUIRY_TYPE", 1, "P"),
                alfa("DNI", 13),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken inquiryQ1 = new EspecificacionToken("MUNGYE-MERCADOS-INQUIRY-Q1", List.of(
                numerico("DEBT_ID", 10),
                alfa("REQUEST_TYPE", 3),
                variable("SOLIDARITY_DISCOUNT"),
                variable("NOMBRE"),
                variable("STORE_CODE"),
                variable("MERCHANT_DESCRIPTION"),
                alfa("VALORES_QUERY", 348)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.MERCADOS, TipoOperacionToken.INQUIRY, inquiryQ0, inquiryQ1);

        EspecificacionToken paymentQ0 = new EspecificacionToken("MUNGYE-MERCADOS-PAYMENT-Q0", List.of(
                alfa("PROCESS_TYPE", 1, "P"),
                alfa("INQUIRY_TYPE", 1, "P"),
                numerico("COLLECTION_ID", 10),
                alfa("DNI", 13),
                alfa("VALORES_QUERY", 28),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken paymentQ1 = new EspecificacionToken("MUNGYE-MERCADOS-PAYMENT-Q1", List.of(
                numerico("DEBT_ID", 10),
                alfa("REQUEST_TYPE", 3),
                alfa("DNI", 13),
                variable("NOMBRE_COMERCIANTE"),
                alfa("VALORES_QUERY", 28),
                numerico("PAYMENT_ID", 10),
                alfa("FECHA_REGISTRO", 8)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.MERCADOS, TipoOperacionToken.PAYMENT, paymentQ0, paymentQ1);

        EspecificacionToken reversalQ0 = new EspecificacionToken("MUNGYE-MERCADOS-REVERSAL-Q0", List.of(
                alfa("PROCESS_TYPE", 1, "R"),
                alfa("INQUIRY_TYPE", 1, "P"),
                numerico("COLLECTION_ID", 10),
                alfa("DNI", 13),
                alfa("VALORES_QUERY", 28),
                numerico("ABA_INSTITUCION", 8),
                alfa("COD_OPERADOR", 9)
        ));
        EspecificacionToken reversalQ1 = new EspecificacionToken("MUNGYE-MERCADOS-REVERSAL-Q1", List.of(
                numerico("DEBT_ID", 10),
                alfa("REQUEST_TYPE", 3),
                variable("NOMBRE_COMERCIANTE"),
                variable("STORE_CODE"),
                variable("MERCHANT_NAME"),
                alfa("VALORES_QUERY", 28)
        ));
        put(TipoBiller.MUNGYE, SubServicioMungye.MERCADOS, TipoOperacionToken.REVERSAL, reversalQ0, reversalQ1);
    }
}
