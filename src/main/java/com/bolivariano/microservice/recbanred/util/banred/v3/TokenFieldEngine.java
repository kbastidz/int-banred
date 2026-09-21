package com.bolivariano.microservice.recbanred.util.banred.v3;

import com.bolivariano.microservice.recbanred.core.enums.banred.TipoCampoToken;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.token.CampoToken;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.token.EspecificacionToken;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.TRANSFORMATION_ERROR;

/**
 * Motor generico de construccion (Q0) y parseo (Q1) de tokens de posicion
 * fija, segun lo documentado en los Anexos "Estructura Estandar Token Trama
 * Generica Q0/Q1" de CNEL, MEER y MUNGYE.
 *
 * Reglas generales aplicadas (ver documento de contexto entregado por Anexo):
 *  - Cabecera "! Q0"/"! Q1" (4) + longitud de contenido (5) + espacio (1).
 *  - Numericos: padleft con ceros. Alfa: padright con espacios.
 *  - Variables: precedidas por 2 digitos con la longitud del valor.
 *  - Montos: redondeo a 2 decimales, se remueve el separador decimal y se
 *    aplica padleft con ceros.
 *  - Grupos repetidos (ej. AÑO/VALOR de MUNGYE-Predios): se repiten tantas
 *    veces como quepan en la longitud restante, hasta un maximo configurado.
 */
@Component
public class TokenFieldEngine {

    private static final Logger log = LoggerFactory.getLogger(TokenFieldEngine.class);

    private static final String HEADER_Q0 = "! Q0";
    private static final String HEADER_Q1 = "! Q1";

    // ==================================================================
    // CONSTRUCCION (Q0)
    // ==================================================================

    /**
     * Construye un token Q0 completo (con cabecera "! Q0LLLLL ") a partir de
     * una especificacion y un mapa de valores {nombreCampo -> valor}.
     *
     * @param spec    especificacion de campos (ver TokenSpecRegistry)
     * @param valores mapa con los valores de negocio a insertar; los campos
     *                ausentes toman su valorPorDefecto
     * @return el token Q0 completo, listo para InputData
     */
    public String buildQ0(EspecificacionToken spec, Map<String, String> valores) throws CustomException {
        return build(HEADER_Q0, spec, valores);
    }

    private String build(String header, EspecificacionToken spec, Map<String, String> valores) throws CustomException {
        try {
            StringBuilder contenido = new StringBuilder();
            for (CampoToken campo : spec.getCampos()) {
                contenido.append(renderCampo(campo, valores));
            }
            String cuerpo = contenido.toString();
            String longitud = StringUtils.leftPad(String.valueOf(cuerpo.length()), 5, '0');
            return header + longitud + StringUtils.SPACE + cuerpo;
        } catch (Exception ex) {
            throw new CustomException("Error construyendo token [" + spec.getEtiqueta() + "]: " + ex.getMessage(),
                    ex, TRANSFORMATION_ERROR);
        }
    }

    private String renderCampo(CampoToken campo, Map<String, String> valores) {
        String valor = valores.getOrDefault(campo.getNombre(), campo.getValorPorDefecto());

        return switch (campo.getTipo()) {
            case NUMERICO -> StringUtils.leftPad(StringUtils.defaultIfEmpty(valor, "0"), campo.getLongitud(), '0');
            case ALFA -> StringUtils.rightPad(StringUtils.defaultString(valor), campo.getLongitud(), ' ');
            case MONTO -> StringUtils.leftPad(formatearMonto(valor), campo.getLongitud(), '0');
            case VARIABLE -> {
                String texto = StringUtils.defaultString(valor);
                yield StringUtils.leftPad(String.valueOf(texto.length()), 2, '0') + texto;
            }
            case GRUPO_REPETIDO -> renderGrupoRepetido(campo, valores);
        };
    }

    private String renderGrupoRepetido(CampoToken grupo, Map<String, String> valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= grupo.getRepeticiones(); i++) {
            for (CampoToken sub : grupo.getSubCampos()) {
                String key = sub.getNombre() + "_" + i;
                Map<String, String> scoped = Map.of(sub.getNombre(), valores.getOrDefault(key, sub.getValorPorDefecto()));
                sb.append(renderCampo(sub, scoped));
            }
        }
        return sb.toString();
    }

    /**
     * Aplica la regla de formato de montos documentada en los Anexos:
     * 1) redondear a 2 decimales, 2) remover separador decimal dejando los
     *    2 ultimos digitos como parte decimal.
     * Ej: 35.354 -> "3535" ; 35 -> "3500"
     */
    private String formatearMonto(String valor) {
        if (StringUtils.isEmpty(valor))
            return "0";
        BigDecimal monto = new BigDecimal(valor).setScale(2, RoundingMode.HALF_UP);
        return monto.movePointRight(2).toBigInteger().toString();
    }

    // ==================================================================
    // PARSEO (Q1)
    // ==================================================================

    /**
     * Parsea un token Q1 completo (incluyendo cabecera "! Q1LLLLL ") y
     * retorna un mapa ordenado {nombreCampo -> valor decodificado}.
     * Los campos de un GRUPO_REPETIDO se exponen como "nombreSubCampo_1",
     * "nombreSubCampo_2", etc.
     */
    public LinkedHashMap<String, String> parseQ1(EspecificacionToken spec, String tokenCompleto) throws CustomException {
        try {
            String cuerpo = removerCabecera(tokenCompleto);
            return parseCuerpo(spec, cuerpo);
        } catch (Exception ex) {
            throw new CustomException("Error parseando token [" + spec.getEtiqueta() + "]: " + ex.getMessage(),
                    ex, TRANSFORMATION_ERROR);
        }
    }

    /**
     * Para tokens Q1 que son un arreglo de bloques separados por pipe "|"
     * (ej. MEER Mass Inquiry). Devuelve una lista de mapas, uno por bloque.
     */
    public List<LinkedHashMap<String, String>> parseQ1ArregloPipe(EspecificacionToken spec, String tokenCompleto) throws CustomException {
        try {
            String cuerpo = removerCabecera(tokenCompleto);
            List<LinkedHashMap<String, String>> resultado = new ArrayList<>();
            for (String bloque : cuerpo.split("\\|", -1)) {
                if (StringUtils.isBlank(bloque))
                    continue;
                resultado.add(parseCuerpo(spec, bloque));
            }
            return resultado;
        } catch (Exception ex) {
            throw new CustomException("Error parseando arreglo pipe [" + spec.getEtiqueta() + "]: " + ex.getMessage(),
                    ex, TRANSFORMATION_ERROR);
        }
    }

    private String removerCabecera(String tokenCompleto) {
        // Formato esperado: "! Q1LLLLL <contenido>" o "! Q1 LLLLL <contenido>"
        // Se tolera espacio opcional entre "! Q1"/"! Q0" y la longitud.
        String sinPrefijo = tokenCompleto.replaceFirst("^!\\s*Q[01]\\s*", "");
        // sinPrefijo ahora empieza con los 5 digitos de longitud + 1 espacio + contenido
        if (sinPrefijo.length() < 6)
            return StringUtils.EMPTY;
        return sinPrefijo.substring(6); // 5 digitos de longitud + 1 espacio
    }

    private LinkedHashMap<String, String> parseCuerpo(EspecificacionToken spec, String cuerpo) {
        LinkedHashMap<String, String> resultado = new LinkedHashMap<>();
        int pos = 0;
        List<CampoToken> campos = spec.getCampos();

        for (int i = 0; i < campos.size(); i++) {
            CampoToken campo = campos.get(i);

            switch (campo.getTipo()) {
                case VARIABLE -> {
                    int longitudCampo = Integer.parseInt(cuerpo.substring(pos, pos + 2));
                    pos += 2;
                    String valor = cuerpo.substring(pos, Math.min(pos + longitudCampo, cuerpo.length()));
                    pos += longitudCampo;
                    resultado.put(campo.getNombre(), valor.trim());
                }
                case GRUPO_REPETIDO -> {
                    int anchoGrupo = campo.anchoGrupo();
                    int longitudFinalReservada = spec.longitudFijaDesde(i + 1);
                    int restante = cuerpo.length() - pos - longitudFinalReservada;
                    int repesDisponibles = anchoGrupo == 0 ? 0 : Math.max(0, restante / anchoGrupo);
                    int repes = Math.min(campo.getRepeticiones(), repesDisponibles);

                    if (repesDisponibles < campo.getRepeticiones())
                        log.warn("GRUPO_REPETIDO [{}]: se esperaban hasta {} repeticiones pero solo caben {} " +
                                        "en la longitud recibida (posible trama variable, revisar con el biller)",
                                campo.getNombre(), campo.getRepeticiones(), repesDisponibles);

                    for (int r = 1; r <= repes; r++) {
                        for (CampoToken sub : campo.getSubCampos()) {
                            String valor = cuerpo.substring(pos, pos + sub.getLongitud());
                            pos += sub.getLongitud();
                            resultado.put(sub.getNombre() + "_" + r, valor.trim());
                        }
                    }
                }
                case ALFA -> {
                    String valor = cuerpo.substring(pos, Math.min(pos + campo.getLongitud(), cuerpo.length()));
                    pos += campo.getLongitud();
                    resultado.put(campo.getNombre(), valor.trim());
                }
                default -> { // NUMERICO, MONTO
                    String valor = cuerpo.substring(pos, Math.min(pos + campo.getLongitud(), cuerpo.length()));
                    pos += campo.getLongitud();
                    resultado.put(campo.getNombre(), valor);
                }
            }
        }
        return resultado;
    }
}
