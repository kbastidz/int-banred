package com.bolivariano.microservice.recbanred.util.banred.v3;

import com.bolivariano.microservice.recbanred.core.configuration.BillerV3Configuration;
import com.bolivariano.microservice.recbanred.core.configuration.MunQuitoConfiguration;
import com.bolivariano.microservice.recbanred.core.enums.banred.SubServicioMungye;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoBiller;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.VALIDATION_ERROR;

/**
 * Resuelve, a partir del BillCompanyCode (codigo de empresa asignado por
 * BANRED) y del BillServiceCode, cual {@link TipoBiller} y, si aplica,
 * cual {@link SubServicioMungye} corresponde usar para construir/parsear
 * el token V3.
 *
 * También expone métodos para detectar si un companyCode corresponde a
 * V3 (SOAP trama fija) o V4 (ISO 8583 TCP – Municipio de Quito).
 */
@Component
public class BillerResolver {

    private final BillerV3Configuration billerV3Configuration;
    private final MunQuitoConfiguration munQuitoConfiguration;

    public BillerResolver(BillerV3Configuration billerV3Configuration,
                          MunQuitoConfiguration munQuitoConfiguration) {
        this.billerV3Configuration = billerV3Configuration;
        this.munQuitoConfiguration = munQuitoConfiguration;
    }

    public TipoBiller resolverBiller(String companyCode) throws CustomException {
        if (billerV3Configuration.getCompaniesCnel().contains(companyCode))
            return TipoBiller.CNEL;
        if (billerV3Configuration.getCompaniesMeer().contains(companyCode))
            return TipoBiller.MEER;
        if (billerV3Configuration.getCompaniesMungye().contains(companyCode))
            return TipoBiller.MUNGYE;
        if (getCompaniesMunQuitoList().contains(companyCode))
            return TipoBiller.MUNQUITO;

        throw new CustomException("BillCompanyCode no mapeado a ningun biller V3/V4: " + companyCode,
                null, VALIDATION_ERROR);
    }

    public SubServicioMungye resolverSubServicioMungye(int billServiceCode) throws CustomException {
        try {
            return SubServicioMungye.fromBillServiceCode(billServiceCode);
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ex.getMessage(), ex, VALIDATION_ERROR);
        }
    }

    /** true si el companyCode pertenece a un biller de trama fija SOAP (V3). */
    public boolean esCompanyV3(String companyCode) {
        return billerV3Configuration.getCompaniesCnel().contains(companyCode)
                || billerV3Configuration.getCompaniesMeer().contains(companyCode)
                || billerV3Configuration.getCompaniesMungye().contains(companyCode);
    }

    /** true si el companyCode pertenece al Municipio de Quito (V4 – ISO 8583 TCP). */
    public boolean esCompanyV4(String companyCode) {
        return getCompaniesMunQuitoList().contains(companyCode);
    }

    private List<String> getCompaniesMunQuitoList() {
        String companies = munQuitoConfiguration.getCompanies();
        if (StringUtils.isBlank(companies)) return List.of();
        return Arrays.stream(companies.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
