package com.bolivariano.microservice.recbanred.service.banred.v4;

import com.bolivariano.microservice.recbanred.core.configuration.BanredConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.MunQuitoConfiguration;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaConsultarDeuda;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaEjecutarPago;
import com.bolivariano.microservice.recbanred.core.payloads.input.MensajeEntradaEjecutarReverso;
import com.bolivariano.microservice.recbanred.util.AdditionalDataUtils;
import com.bolivariano.microservice.recbanred.util.BusinessUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static com.bolivariano.microservice.recbanred.core.constants.Labels.*;

/**
 * Servicio de negocio V4: construye las tramas ISO 8583 para el Municipio
 * del Distrito Metropolitano de Quito y delega el envío/recepción TCP a
 * {@link MunQuitoTcpClient}.
 *
 * Mapeo de datos adicionales del request:
 *   - "e_band_autorizador"  → companyCode (ya usado para determinar versión)
 *   - "canal"               → canal (viene directo del campo canal del mensaje)
 *   - "e_term"              → número de terminal (16 chars)
 *   - "e_operador"          → código operador (6 chars)
 *   - "grupo_impuesto"      → grupo impuesto ("001"=PREDIOS, "002"=PATENTES, "003"=VARIOS)
 *   - "arreglo1"            → Bit 67 pre-armado desde la respuesta de consulta (solo en pago)
 *   - "arreglo2"            → Bit 68 pre-armado desde la respuesta de consulta (solo en pago)
 *   - "e_reverso_ind"       → indicador de reverso ("01"-"04")
 *   - "e_ssn_corr"          → secuencial original del pago (para Bit 56 en reverso)
 *   - "e_fecha_orig"        → fecha original de la tx (YYYYMMDD, para Bit 56 en reverso)
 *   - "e_hora_orig"         → hora original de la tx (HHMMSS, para Bit 56 en reverso)
 */
@Service
public class BanredServiceV4 {

    private static final Logger log = LoggerFactory.getLogger(BanredServiceV4.class);

    private final IsoMunQuitoBuilder isoBuilder;
    private final MunQuitoTcpClient tcpClient;
    private final BanredConfiguration banredConfig;
    private final MunQuitoConfiguration munQuitoConfig;
    private final BusinessUtils businessUtils;

    // Nombres de campos en datosAdicionales para V4
    private static final String DA_GRUPO_IMPUESTO   = "grupo_impuesto";
    private static final String DA_OPERADOR         = "e_operador";
    private static final String DA_REVERSO_IND      = "e_reverso_ind";
    private static final String DA_SSN_CORR         = "e_ssn_corr";
    private static final String DA_FECHA_ORIG       = "e_fecha_orig";
    private static final String DA_HORA_ORIG        = "e_hora_orig";
    private static final String DA_ARREGLO1         = "arreglo1";
    private static final String DA_ARREGLO2         = "arreglo2";

    public BanredServiceV4(IsoMunQuitoBuilder isoBuilder,
                            MunQuitoTcpClient tcpClient,
                            BanredConfiguration banredConfig,
                            MunQuitoConfiguration munQuitoConfig,
                            BusinessUtils businessUtils) {
        this.isoBuilder      = isoBuilder;
        this.tcpClient       = tcpClient;
        this.banredConfig    = banredConfig;
        this.munQuitoConfig  = munQuitoConfig;
        this.businessUtils   = businessUtils;
    }

    // =========================================================================
    // CONSULTA
    // =========================================================================

    public Mono<IsoMunQuitoParser.RespuestaIso> executeConsulta(MensajeEntradaConsultarDeuda rq) {
        DatosAdicionales da = rq.getServicio().getDatosAdicionales();

        String llave        = rq.getServicio().getIdentificador();
        String secuencial   = rq.getSecuencial();
        String terminal     = buildTerminal(da, rq.getCanal());
        String operador     = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_OPERADOR, banredConfig.getAccpIdTellerCode());
        String canal        = resolveCanal(rq.getCanal());
        String grupoImpuesto = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_GRUPO_IMPUESTO, IsoMunQuitoBuilder.GRUPO_PREDIOS);

        String trama = isoBuilder.buildConsulta(llave, secuencial, terminal, operador, canal, grupoImpuesto);
        log.info("V4 CONSULTA – llave={} secuencial={} grupo={}", llave, secuencial, grupoImpuesto);
        return tcpClient.enviar(trama);
    }

    // =========================================================================
    // PAGO
    // =========================================================================

    public Mono<IsoMunQuitoParser.RespuestaIso> executePago(MensajeEntradaEjecutarPago rq) {
        DatosAdicionales da = rq.getServicio().getDatosAdicionales();

        String llave         = rq.getServicio().getIdentificador();
        String secuencial    = rq.getSecuencial();
        long   valorCentavos = businessUtils.formatPaidAmountToBanred(rq.getValorPago());
        String terminal      = buildTerminal(da, rq.getCanal());
        String operador      = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_OPERADOR, banredConfig.getAccpIdTellerCode());
        String canal         = resolveCanal(rq.getCanal());
        String grupoImpuesto = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_GRUPO_IMPUESTO, IsoMunQuitoBuilder.GRUPO_PREDIOS);

        // Bit 67 y 68 vienen pre-armados desde la respuesta de consulta
        String arreglo1 = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_ARREGLO1, "");
        String arreglo2 = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_ARREGLO2, "");

        String trama = isoBuilder.buildPago(llave, secuencial, valorCentavos,
                terminal, operador, canal, grupoImpuesto, arreglo1, arreglo2);
        log.info("V4 PAGO – llave={} secuencial={} valor={}", llave, secuencial, valorCentavos);
        return tcpClient.enviar(trama);
    }

    // =========================================================================
    // REVERSO
    // =========================================================================

    public Mono<IsoMunQuitoParser.RespuestaIso> executeReverso(MensajeEntradaEjecutarReverso rq) {
        DatosAdicionales da = rq.getServicio().getDatosAdicionales();

        String llave            = rq.getServicio().getIdentificador();
        String secuencial       = rq.getSecuencial();
        long   valorCentavos    = businessUtils.formatPaidAmountToBanred(rq.getValorPago());
        String terminal         = buildTerminal(da, rq.getCanal());
        String operador         = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_OPERADOR, banredConfig.getAccpIdTellerCode());
        String canal            = resolveCanal(rq.getCanal());
        String grupoImpuesto    = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_GRUPO_IMPUESTO, IsoMunQuitoBuilder.GRUPO_PREDIOS);
        String indicadorReverso = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_REVERSO_IND, IsoMunQuitoBuilder.REV_ANULACION_OPERADOR);
        String secOrig          = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_SSN_CORR, secuencial);
        String fechaOrig        = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_FECHA_ORIG, "");
        String horaOrig         = AdditionalDataUtils.getAdditionalDataOrDefault(da, DA_HORA_ORIG, "");

        String datosTxOriginal  = isoBuilder.buildDatosTxOriginal(secOrig, fechaOrig, horaOrig, terminal);

        String trama = isoBuilder.buildReverso(llave, secuencial, valorCentavos,
                indicadorReverso, terminal, operador, canal, grupoImpuesto, datosTxOriginal);
        log.info("V4 REVERSO – llave={} secuencial={} indicador={}", llave, secuencial, indicadorReverso);
        return tcpClient.enviar(trama);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Construye el número de terminal de 16 chars.
     * Según ficha: Fi adquirente(4) + Código Agencia(6) + Código terminal(6).
     * En este integrador usamos el ABA de la institución + terminal del request.
     */
    private String buildTerminal(DatosAdicionales da, String canal) {
        String term = AdditionalDataUtils.getAdditionalDataOrDefault(da, E_TERM, banredConfig.getAccpIdTellerCode());
        // Formatear a 16 chars (relleno de espacios a la derecha)
        return StringUtils.rightPad(StringUtils.defaultString(term).trim(), 16, ' ');
    }

    /**
     * Convierte el nombre de canal interno al código de 3 dígitos de la ficha.
     * 001=Ventanilla, 002=Kiosco, 003=ATM, 004=Internet, 005=IVR, 006=Celular, 007=Otros
     */
    private String resolveCanal(String nombreCanal) {
        if (StringUtils.isBlank(nombreCanal)) return "007";
        return switch (nombreCanal.toUpperCase().trim()) {
            case "VEN"  -> "001";
            case "KIO"  -> "002";
            case "ATM"  -> "003";
            case "IBK"  -> "004";
            case "SAT"  -> "004";
            case "IVR"  -> "005";
            case "WAP"  -> "006";
            case "CNB"  -> "007";
            default     -> "007";
        };
    }
}
