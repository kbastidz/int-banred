package com.bolivariano.microservice.recbanred.service.banred.v3;


import com.bolivariano.microservice.recbanred.core.constants.Labels;
import com.bolivariano.microservice.recbanred.core.enums.banred.SubServicioMungye;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoBiller;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoOperacionToken;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatoAdicional;
import com.bolivariano.microservice.recbanred.core.payloads.input.DatosAdicionales;
import com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.token.EspecificacionToken;
import com.bolivariano.microservice.recbanred.util.banred.v3.TokenFieldEngine;
import com.bolivariano.microservice.recbanred.util.banred.v3.TokenSpecRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Decodifica el campo ResponseData (token Q1 de posicion fija) de una
 * respuesta V3 y lo expone como {@link DatosAdicionales}, en el mismo
 * formato de salida que usan V1 ({@code TokenDataUtils}) y V2
 * ({@code AdditionalDataUtils}), para que el resto del pipeline
 * (InquiryBanred/PaymentBanred/ReversalBanred -> MensajeSalida*) no tenga
 * que conocer los detalles de la trama fija.
 */
@Component
public class TokenDataV3Utils {

    private static final Logger log = LoggerFactory.getLogger(TokenDataV3Utils.class);

    private final TokenSpecRegistry tokenSpecRegistry;
    private final TokenFieldEngine tokenFieldEngine;

    public TokenDataV3Utils(TokenSpecRegistry tokenSpecRegistry, TokenFieldEngine tokenFieldEngine) {
        this.tokenSpecRegistry = tokenSpecRegistry;
        this.tokenFieldEngine = tokenFieldEngine;
    }

    /**
     * Parsea el ResponseData recibido en el sobre SOAP V3 y lo transforma en
     * una lista de {@link DatoAdicional} (codigo = nombre de campo del token,
     * valor = valor decodificado), lista para exponer en el DTO de salida.
     */
    public DatosAdicionales parseResponseData(String responseData, TipoBiller biller, SubServicioMungye sub,
                                               TipoOperacionToken operacion, String canal) {
        if (StringUtils.isEmpty(responseData)) {
            log.warn("ResponseData vacio para {} / {} / {}", biller, sub, operacion);
            return null;
        }

        try {
            EspecificacionToken specQ1 = tokenSpecRegistry.getQ1(biller, sub, operacion);
            if (specQ1.getCampos().isEmpty()) {
                log.info("No hay especificacion Q1 definida para {} / {} / {} (biller no la documenta)", biller, sub, operacion);
                return null;
            }

            List<DatoAdicional> lista = new ArrayList<>();
            if (specQ1.isArregloPipe()) {
                List<LinkedHashMap<String, String>> bloques = tokenFieldEngine.parseQ1ArregloPipe(specQ1, responseData);
                for (int i = 0; i < bloques.size(); i++) {
                    int idx = i + 1;
                    bloques.get(i).forEach((campo, valor) -> lista.add(new DatoAdicional(campo + "_ITEM" + idx, valor)));
                }
            } else {
                LinkedHashMap<String, String> valores = tokenFieldEngine.parseQ1(specQ1, responseData);
                valores.forEach((campo, valor) -> lista.add(new DatoAdicional(campo, valor)));
            }
            lista.add(new DatoAdicional(Labels.CANAL, canal));
            return new DatosAdicionales(lista);
        } catch (CustomException ex) {
            log.error("Error decodificando ResponseData V3 [{} / {} / {}]: {}", biller, sub, operacion, ex.getMessage());
            return null;
        }
    }

    /** Busca el valor de un campo especifico ya decodificado (ver {@link #parseResponseData}). */
    public static String getValor(DatosAdicionales datos, String nombreCampo) {
        if (datos == null || datos.getDatoAdicional() == null)
            return StringUtils.EMPTY;
        return datos.getDatoAdicional().stream()
                .filter(d -> nombreCampo.equals(d.getCodigo()))
                .map(DatoAdicional::getValor)
                .findFirst()
                .orElse(StringUtils.EMPTY);
    }
}
