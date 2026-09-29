package com.bolivariano.microservice.recbanred.service.banred.v4;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parsea las tramas de respuesta ISO 8583 del Municipio del Distrito
 * Metropolitano de Quito (V4 – TCP).
 *
 * Estructura de la respuesta (basada en ficha técnica Anexo 1 y tramas reales):
 *
 * Header(15) + MTI(4) + BitmapPrim(16) + BitmapSec(16) + campos variables
 *
 * Campos en respuesta de CONSULTA (0210 – 310004):
 *   Bit  2: llvar N 19 – Llave Municipal
 *   Bit  3: 6N         – Tipo transacción
 *   Bit 11: 6N         – Secuencial Banred
 *   Bit 12: 6N         – Hora local
 *   Bit 13: 8N         – Fecha local
 *   Bit 15: 8N         – Fecha compensación
 *   Bit 28: 14N        – Documento (cédula/RUC propietario)
 *   Bit 32: llvar N 6  – Institución adquirente
 *   Bit 33: 6AN        – Código operador
 *   Bit 37: 6N         – Secuencial adquirente
 *   Bit 39: 2N         – Código resultado (00 = OK)
 *   Bit 41: 16AN       – Número terminal
 *   Bit 45: 35A        – Nombre/apellidos contribuyente
 *   Bit 57: llvar N 6  – Institución autorizadora
 *   Bit 67: llvar N    – Detalle valores a pagar arreglo 1
 *   Bit 68: llvar N    – Detalle valores a pagar arreglo 2
 *   Bit 69: llvar AN   – Dirección
 *
 * Campos ADICIONALES en respuesta de PAGO (0210 – 010005):
 *   Bit  4: 12N  – Valor total
 *   Bit  6: 12N  – Valor rubro
 *   Bit  8: 12N  – Intereses
 *   Bit 64: llvar N – Datos adicionales (dirección, clave catastral, etc.)
 *   Bit 65: 12N  – Avalúo comercial
 *   Bit104: llvar N – Fecha emisión
 *   Bit113: llvar N – Título crédito
 *   Bit114: llvar N – Descuento
 *   Bit115: llvar N – Recargo
 *   Bit116: llvar N – Costas
 *   Bit119: llvar N – Número autorización
 *   Bit121: llvar N – Detalle de rubros
 */
@Component
public class IsoMunQuitoParser {

    private static final Logger log = LoggerFactory.getLogger(IsoMunQuitoParser.class);

    /** Longitud del header fijo: prefijo (11) + dígito institución (1) = 12 chars, más MTI (4) = 16 total. */
    private static final int HEADER_FULL_LEN = 16;

    // =========================================================================
    // DTO de respuesta
    // =========================================================================

    /** Resultado parseado de cualquier respuesta ISO del Municipio de Quito. */
    public static class RespuestaIso {
        public String mti;
        public String llaveMunicipal;        // Bit 2
        public String tipoTransaccion;       // Bit 3
        public String valorTotal;            // Bit 4  (pago)
        public String valorRubro;            // Bit 6  (pago)
        public String intereses;             // Bit 8  (pago)
        public String secuencialBanred;      // Bit 11
        public String horaLocal;             // Bit 12
        public String fechaLocal;            // Bit 13
        public String fechaCompensacion;     // Bit 15
        public String documento;             // Bit 28
        public String institucionAdquirente; // Bit 32
        public String codigoOperador;        // Bit 33
        public String secuencialAdquirente;  // Bit 37
        public String codigoResultado;       // Bit 39
        public String numeroTerminal;        // Bit 41
        public String nombreContribuyente;   // Bit 45
        public String institucionAutorizadora; // Bit 57
        public List<DetalleDeuda> detallesDeuda;   // Bit 67
        public List<DetallesTitulo> detallesTitulo; // Bit 68
        public String direccion;             // Bit 69
        public String datosAdicionales;      // Bit 64
        public String avaluoComercial;       // Bit 65
        public String fechaEmision;          // Bit 104
        public String tituloCreditoBit113;   // Bit 113
        public String descuento;             // Bit 114
        public String recargo;              // Bit 115
        public String costas;               // Bit 116
        public String numeroAutorizacion;    // Bit 119
        public String detalleRubros;         // Bit 121

        /** true si codigoResultado == "00" */
        public boolean isAprobada() {
            return "00".equals(codigoResultado);
        }

        /** Monto total como BigDecimal (divide entre 100). */
        public BigDecimal getMontoTotal() {
            return parseMonto(valorTotal);
        }

        public BigDecimal getMontoIntereses() {
            return parseMonto(intereses);
        }

        public BigDecimal getMontoDescuento() {
            return parseMonto(descuento);
        }

        private BigDecimal parseMonto(String raw) {
            if (StringUtils.isBlank(raw)) return BigDecimal.ZERO;
            try {
                return new BigDecimal(raw.trim()).divide(new BigDecimal(100));
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
    }

    public static class DetalleDeuda {
        public String codigoTipoImpuesto;  // 9(03)
        public String descripcion;          // X(22)
        public String anioDeuda;            // 9(04)
        public String montoDeuda;           // 9(08)V99
    }

    public static class DetallesTitulo {
        public String numeroTitulo;         // 9(12)
        public String prioridad;            // 9(03)
        public String indice;               // 9(02)
        public String numeroObra;           // 9(05)
        public String numeroDividendo;      // 9(02)
        public String codigoEspecial;       // X(01)
    }

    // =========================================================================
    // Parseo principal
    // =========================================================================

    /**
     * Parsea una trama de respuesta ISO completa del Municipio de Quito.
     *
     * @param trama La trama en texto plano tal como llega por TCP.
     * @return {@link RespuestaIso} con todos los campos extraídos.
     */
    public RespuestaIso parsear(String trama) {
        RespuestaIso resp = new RespuestaIso();
        try {
            if (StringUtils.isBlank(trama)) {
                log.error("Trama vacía recibida del Municipio de Quito");
                return resp;
            }

            // El header completo es "ISO014000073" (12 chars) + MTI (4) = posición 0..15
            // Según tramas: "ISO014000073" = 12 chars, luego "0210" = MTI
            int pos = 0;

            // Header completo: prefijo (11) + dígito institución (1) = 12
            pos = 12;
            resp.mti = trama.substring(pos, pos + 4);
            pos += 4;

            // Bitmaps: primario (16) + secundario (16)
            String bitmapPrimHex  = trama.substring(pos, pos + 16); pos += 16;
            String bitmapSecHex   = trama.substring(pos, pos + 16); pos += 16;

            long bitmapPrim = Long.parseUnsignedLong(bitmapPrimHex, 16);
            long bitmapSec  = Long.parseUnsignedLong(bitmapSecHex, 16);

            // Parsear campos según bits activos (bitmap cubre bits 1-128)
            pos = parsearCampos(trama, pos, bitmapPrim, bitmapSec, resp);

            log.debug("Trama Municipio Quito parseada. MTI={}, resultado={}, contribuyente={}",
                    resp.mti, resp.codigoResultado, resp.nombreContribuyente);

        } catch (Exception ex) {
            log.error("Error parseando trama ISO Municipio Quito: {}", ex.getMessage(), ex);
        }
        return resp;
    }

    // =========================================================================
    // Parseo de campos individuales
    // =========================================================================

    private int parsearCampos(String trama, int pos, long bitmapPrim, long bitmapSec, RespuestaIso resp) {
        // Bit 2 – Llave Municipal (llvar N 19)
        if (isBitSet(bitmapPrim, 2)) {
            LlvarResult lv2 = readLlvarResult(trama, pos);
            resp.llaveMunicipal = lv2.value; pos = lv2.nextPos;
        }
        // Bit 3 – Tipo transacción (6N)
        if (isBitSet(bitmapPrim, 3)) {
            FixedResult f3 = readFixed(trama, pos, 6); resp.tipoTransaccion = f3.value; pos = f3.nextPos;
        }
        // Bit 4 – Valor total (12N)
        if (isBitSet(bitmapPrim, 4)) {
            FixedResult f4 = readFixed(trama, pos, 12); resp.valorTotal = f4.value; pos = f4.nextPos;
        }
        // Bit 6 – Valor rubro (12N)
        if (isBitSet(bitmapPrim, 6)) {
            FixedResult f6 = readFixed(trama, pos, 12); resp.valorRubro = f6.value; pos = f6.nextPos;
        }
        // Bit 8 – Intereses (12N)
        if (isBitSet(bitmapPrim, 8)) {
            FixedResult f8 = readFixed(trama, pos, 12); resp.intereses = f8.value; pos = f8.nextPos;
        }
        // Bit 11 – Secuencial Banred (6N)
        if (isBitSet(bitmapPrim, 11)) {
            FixedResult f11 = readFixed(trama, pos, 6); resp.secuencialBanred = f11.value; pos = f11.nextPos;
        }
        // Bit 12 – Hora local (6N)
        if (isBitSet(bitmapPrim, 12)) {
            FixedResult f12 = readFixed(trama, pos, 6); resp.horaLocal = f12.value; pos = f12.nextPos;
        }
        // Bit 13 – Fecha local (8N)
        if (isBitSet(bitmapPrim, 13)) {
            FixedResult f13 = readFixed(trama, pos, 8); resp.fechaLocal = f13.value; pos = f13.nextPos;
        }
        // Bit 15 – Fecha compensación (8N)
        if (isBitSet(bitmapPrim, 15)) {
            FixedResult f15 = readFixed(trama, pos, 8); resp.fechaCompensacion = f15.value; pos = f15.nextPos;
        }
        // Bit 28 – Documento (14N)
        if (isBitSet(bitmapPrim, 28)) {
            FixedResult f28 = readFixed(trama, pos, 14); resp.documento = f28.value; pos = f28.nextPos;
        }
        // Bit 32 – Institución adquirente (llvar N 6)
        if (isBitSet(bitmapPrim, 32)) {
            LlvarResult lv32 = readLlvarResult(trama, pos); resp.institucionAdquirente = lv32.value; pos = lv32.nextPos;
        }
        // Bit 33 – Código operador (6AN)
        if (isBitSet(bitmapPrim, 33)) {
            FixedResult f33 = readFixed(trama, pos, 6); resp.codigoOperador = f33.value; pos = f33.nextPos;
        }
        // Bit 37 – Secuencial adquirente (6N)
        if (isBitSet(bitmapPrim, 37)) {
            FixedResult f37 = readFixed(trama, pos, 6); resp.secuencialAdquirente = f37.value; pos = f37.nextPos;
        }
        // Bit 39 – Código resultado (2N)
        if (isBitSet(bitmapPrim, 39)) {
            FixedResult f39 = readFixed(trama, pos, 2); resp.codigoResultado = f39.value; pos = f39.nextPos;
        }
        // Bit 41 – Número terminal (16AN)
        if (isBitSet(bitmapPrim, 41)) {
            FixedResult f41 = readFixed(trama, pos, 16); resp.numeroTerminal = f41.value; pos = f41.nextPos;
        }
        // Bit 45 – Nombre/apellidos (35A)
        if (isBitSet(bitmapPrim, 45)) {
            FixedResult f45 = readFixed(trama, pos, 35); resp.nombreContribuyente = f45.value.trim(); pos = f45.nextPos;
        }
        // Bit 57 – Institución autorizadora (llvar N 6)
        if (isBitSet(bitmapPrim, 57)) {
            LlvarResult lv57 = readLlvarResult(trama, pos); resp.institucionAutorizadora = lv57.value; pos = lv57.nextPos;
        }
        // Bit 64 – Datos adicionales pago (llvar N)
        if (isBitSet(bitmapPrim, 64)) {
            LlvarResult lv64 = readLlvarVariableResult(trama, pos, 3); resp.datosAdicionales = lv64.value; pos = lv64.nextPos;
        }
        // Bit 65 – Avalúo comercial (12N)
        if (isBitSet(bitmapPrim, 65)) {
            FixedResult f65 = readFixed(trama, pos, 12); resp.avaluoComercial = f65.value; pos = f65.nextPos;
        }
        // Bit 67 – Detalle valores arreglo 1 (llvar N, ocurrencias de 780N)
        if (isBitSet(bitmapPrim, 67)) {
            LlvarResult lv67 = readLlvarVariableResult(trama, pos, 3);
            resp.detallesDeuda = parsearArreglo1(lv67.value); pos = lv67.nextPos;
        }
        // Bit 68 – Detalle títulos arreglo 2 (llvar N, ocurrencias de 520N)
        if (isBitSet(bitmapPrim, 68)) {
            LlvarResult lv68 = readLlvarVariableResult(trama, pos, 3);
            resp.detallesTitulo = parsearArreglo2(lv68.value); pos = lv68.nextPos;
        }
        // Bit 69 – Dirección (llvar AN 20)
        if (isBitSet(bitmapPrim, 69)) {
            LlvarResult lv69 = readLlvarResult(trama, pos); resp.direccion = lv69.value; pos = lv69.nextPos;
        }

        // ---- Bitmap secundario (bits 65-128) ----
        // Bit 104 – Fecha emisión (llvar N 8)
        if (isBitSet(bitmapSec, 104 - 64)) {
            LlvarResult lv104 = readLlvarResult(trama, pos); resp.fechaEmision = lv104.value; pos = lv104.nextPos;
        }
        // Bit 113 – Título crédito (llvar N 12)
        if (isBitSet(bitmapSec, 113 - 64)) {
            LlvarResult lv113 = readLlvarResult(trama, pos); resp.tituloCreditoBit113 = lv113.value; pos = lv113.nextPos;
        }
        // Bit 114 – Descuento (llvar N 12)
        if (isBitSet(bitmapSec, 114 - 64)) {
            LlvarResult lv114 = readLlvarResult(trama, pos); resp.descuento = lv114.value; pos = lv114.nextPos;
        }
        // Bit 115 – Recargo (llvar N 12)
        if (isBitSet(bitmapSec, 115 - 64)) {
            LlvarResult lv115 = readLlvarResult(trama, pos); resp.recargo = lv115.value; pos = lv115.nextPos;
        }
        // Bit 116 – Costas (llvar N 12)
        if (isBitSet(bitmapSec, 116 - 64)) {
            LlvarResult lv116 = readLlvarResult(trama, pos); resp.costas = lv116.value; pos = lv116.nextPos;
        }
        // Bit 119 – Número autorización (llvar N 10)
        if (isBitSet(bitmapSec, 119 - 64)) {
            LlvarResult lv119 = readLlvarResult(trama, pos); resp.numeroAutorizacion = lv119.value; pos = lv119.nextPos;
        }
        // Bit 121 – Detalle rubros (llvar N 310)
        if (isBitSet(bitmapSec, 121 - 64)) {
            LlvarResult lv121 = readLlvarVariableResult(trama, pos, 3); resp.detalleRubros = lv121.value; pos = lv121.nextPos;
        }

        return pos;
    }

    // =========================================================================
    // Parseo de arreglos
    // =========================================================================

    /** Parsea el contenido del Bit 67: ocurrencias de 37 chars (3+22+4+8) */
    private List<DetalleDeuda> parsearArreglo1(String contenido) {
        List<DetalleDeuda> lista = new ArrayList<>();
        if (StringUtils.isBlank(contenido)) return lista;
        int pos = 0;
        // Cada ocurrencia: codTipoImpuesto(3)+descripcion(22)+anio(4)+monto(10) = 39 chars
        int tamOcurrencia = 3 + 22 + 4 + 10;
        while (pos + tamOcurrencia <= contenido.length()) {
            DetalleDeuda d = new DetalleDeuda();
            d.codigoTipoImpuesto = contenido.substring(pos, pos + 3); pos += 3;
            d.descripcion        = contenido.substring(pos, pos + 22).trim(); pos += 22;
            d.anioDeuda          = contenido.substring(pos, pos + 4); pos += 4;
            d.montoDeuda         = contenido.substring(pos, pos + 10); pos += 10;
            lista.add(d);
        }
        return lista;
    }

    /** Parsea el contenido del Bit 68: ocurrencias de 25 chars (12+3+2+5+2+1) */
    private List<DetallesTitulo> parsearArreglo2(String contenido) {
        List<DetallesTitulo> lista = new ArrayList<>();
        if (StringUtils.isBlank(contenido)) return lista;
        int pos = 0;
        int tamOcurrencia = 12 + 3 + 2 + 5 + 2 + 1;
        while (pos + tamOcurrencia <= contenido.length()) {
            DetallesTitulo t = new DetallesTitulo();
            t.numeroTitulo    = contenido.substring(pos, pos + 12); pos += 12;
            t.prioridad       = contenido.substring(pos, pos + 3);  pos += 3;
            t.indice          = contenido.substring(pos, pos + 2);  pos += 2;
            t.numeroObra      = contenido.substring(pos, pos + 5);  pos += 5;
            t.numeroDividendo = contenido.substring(pos, pos + 2);  pos += 2;
            t.codigoEspecial  = contenido.substring(pos, pos + 1);  pos += 1;
            lista.add(t);
        }
        return lista;
    }

    // =========================================================================
    // Utilidades de lectura
    // =========================================================================

    private record FixedResult(String value, int nextPos) {}
    private record LlvarResult(String value, int nextPos) {}

    private FixedResult readFixed(String trama, int pos, int len) {
        if (pos + len > trama.length()) {
            log.warn("readFixed: pos={} len={} excede trama.length={}", pos, len, trama.length());
            return new FixedResult("", trama.length());
        }
        return new FixedResult(trama.substring(pos, pos + len), pos + len);
    }

    /** Lee un campo llvar con longitud codificada en 2 dígitos decimales. */
    private LlvarResult readLlvarResult(String trama, int pos) {
        if (pos + 2 > trama.length()) return new LlvarResult("", trama.length());
        int len = Integer.parseInt(trama.substring(pos, pos + 2));
        pos += 2;
        if (pos + len > trama.length()) {
            log.warn("readLlvar: len={} excede trama en pos={}", len, pos);
            return new LlvarResult(trama.substring(pos), trama.length());
        }
        return new LlvarResult(trama.substring(pos, pos + len), pos + len);
    }

    /** Lee un campo llvar con longitud codificada en N dígitos decimales. */
    private LlvarResult readLlvarVariableResult(String trama, int pos, int lenDigits) {
        if (pos + lenDigits > trama.length()) return new LlvarResult("", trama.length());
        int len = Integer.parseInt(trama.substring(pos, pos + lenDigits));
        pos += lenDigits;
        if (pos + len > trama.length()) {
            return new LlvarResult(trama.substring(pos), trama.length());
        }
        return new LlvarResult(trama.substring(pos, pos + len), pos + len);
    }

    private boolean isBitSet(long bitmap, int bitNumber) {
        // bit 1 = MSB (posición 63 en long de 64 bits)
        return (bitmap & (1L << (64 - bitNumber))) != 0;
    }
}
