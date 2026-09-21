package com.bolivariano.microservice.recbanred.util.banred.v3;

import com.bolivariano.microservice.recbanred.core.configuration.BillerV3Configuration;
import com.bolivariano.microservice.recbanred.core.enums.banred.SubServicioMungye;
import com.bolivariano.microservice.recbanred.core.enums.banred.TipoBiller;
import com.bolivariano.microservice.recbanred.core.exceptions.CustomException;
import org.springframework.stereotype.Component;

import static com.bolivariano.microservice.recbanred.core.constants.CodeDefaults.VALIDATION_ERROR;

/**
 * Resuelve, a partir del BillCompanyCode (codigo de empresa asignado por
 * BANRED) y del BillServiceCode, cual {@link TipoBiller} y, si aplica,
 * cual {@link SubServicioMungye} corresponde usar para construir/parsear
 * el token V3.
 *
 * El mapeo companyCode -> biller se configura en application.yml bajo el
 * prefijo "company.transformation.versions.v3" (ver {@link BillerV3Configuration}),
 * de la misma forma en que V1/V2 configuran su lista de companies.
 */
@Component
public class BillerResolver {

    private final BillerV3Configuration billerV3Configuration;

    public BillerResolver(BillerV3Configuration billerV3Configuration) {
        this.billerV3Configuration = billerV3Configuration;
    }

    public TipoBiller resolverBiller(String companyCode) throws CustomException {
        if (billerV3Configuration.getCompaniesCnel().contains(companyCode))
            return TipoBiller.CNEL;
        if (billerV3Configuration.getCompaniesMeer().contains(companyCode))
            return TipoBiller.MEER;
        if (billerV3Configuration.getCompaniesMungye().contains(companyCode))
            return TipoBiller.MUNGYE;

        throw new CustomException("BillCompanyCode no mapeado a ningun biller V3: " + companyCode,
                null, VALIDATION_ERROR);
    }

    public SubServicioMungye resolverSubServicioMungye(int billServiceCode) throws CustomException {
        try {
            return SubServicioMungye.fromBillServiceCode(billServiceCode);
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ex.getMessage(), ex, VALIDATION_ERROR);
        }
    }

    public boolean esCompanyV3(String companyCode) {
        return billerV3Configuration.getCompaniesCnel().contains(companyCode)
                || billerV3Configuration.getCompaniesMeer().contains(companyCode)
                || billerV3Configuration.getCompaniesMungye().contains(companyCode);
    }
}
