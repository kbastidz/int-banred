package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3.helper;

import com.bolivariano.microservice.recbanred.core.configuration.BanredConfiguration;
import com.bolivariano.microservice.recbanred.core.configuration.BillerV3Configuration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class DatosComplementarios {

    private final BanredConfiguration banredConfig;
    private final BillerV3Configuration billerV3Config;


    public DatosComplementarios(BanredConfiguration banredConfig,
                           BillerV3Configuration billerV3Config) {
        this.banredConfig = banredConfig;
        this.billerV3Config = billerV3Config;
    }
    // ==================================================================
    // HELPERS
    // ==================================================================

    /**
     * Retorna el cardAcceptorNameLoc apropiado según el companyCode.
     * Si el companyCode pertenece a MUNGYE, usa cardAcceptorNameLocGye de la configuracion.
     * En caso contrario, usa cardAcceptorNameLoc de BanredConfiguration.
     */
    public String getCardAcceptorNameLoc(String companyCode) {
        if (billerV3Config.getCompaniesMungye().contains(companyCode) && billerV3Config.getMungye() != null
                && StringUtils.isNotEmpty(billerV3Config.getMungye().getCardAcceptorNameLocGye())) {
            return billerV3Config.getMungye().getCardAcceptorNameLocGye();
        }
        return this.banredConfig.getCardAcceptorNameLoc();
    }

    /**
     * Retorna el primaryAcctNumber apropiado según el companyCode.
     * Si el companyCode pertenece a MUNGYE, usa primaryAcctNumberGye de la configuracion.
     * En caso contrario, usa primaryAcctNumber de BanredConfiguration.
     */
    public String getPrimaryAcctNumber(String companyCode) {
        if (billerV3Config.getCompaniesMungye().contains(companyCode) && billerV3Config.getMungye() != null
                && StringUtils.isNotEmpty(billerV3Config.getMungye().getPrimaryAcctNumberGye())) {
            return billerV3Config.getMungye().getPrimaryAcctNumberGye();
        }
        return this.banredConfig.getPrimaryAcctNumber();
    }

    /**
     * Retorna el accpIdTellerCode (terminalData) apropiado según el companyCode.
     * Si el companyCode pertenece a MUNGYE, usa accpIdTellerCodeGye de la configuracion.
     * En caso contrario, usa accpIdTellerCode de BanredConfiguration.
     */
    public String getAccpIdTellerCode(String companyCode) {
        if (billerV3Config.getCompaniesMungye().contains(companyCode) && billerV3Config.getMungye() != null
                && StringUtils.isNotEmpty(billerV3Config.getMungye().getAccpIdTellerCodeGye())) {
            return billerV3Config.getMungye().getAccpIdTellerCodeGye();
        }
        return this.banredConfig.getAccpIdTellerCode();
    }

    /**
     * Retorna el branchId apropiado según el companyCode.
     * Si el companyCode pertenece a MUNGYE, usa branchIdGye de la configuracion.
     * En caso contrario, usa branchId de BanredConfiguration.
     */
    public String getBranchId(String companyCode) {
        if (billerV3Config.getCompaniesMungye().contains(companyCode) && billerV3Config.getMungye() != null
                && StringUtils.isNotEmpty(billerV3Config.getMungye().getBranchIdGye())) {
            return billerV3Config.getMungye().getBranchIdGye();
        }
        return this.banredConfig.getBranchId();
    }

    /**
     * Retorna el posEntryMode apropiado según el companyCode.
     * Si el companyCode pertenece a MUNGYE, usa posEntryModeGye de la configuracion.
     * En caso contrario, usa posEntryMode de BanredConfiguration.
     */
    public String getPosEntryMode(String companyCode) {
        if (billerV3Config.getCompaniesMungye().contains(companyCode) && billerV3Config.getMungye() != null
                && StringUtils.isNotEmpty(billerV3Config.getMungye().getPosEntryModeGye())) {
            return billerV3Config.getMungye().getPosEntryModeGye();
        }
        return this.banredConfig.getPosEntryMode();
    }

    /**
     * Retorna el receivingInstitutionIdCode apropiado según el companyCode.
     * Si el companyCode pertenece a MUNGYE, usa receivingInstitutionIdCodeGye de la configuracion.
     * En caso contrario, usa receivingInstitutionIdCode de BanredConfiguration.
     */
    public String getReceivingInstitutionIdCode(String companyCode) {
        if (billerV3Config.getCompaniesMungye().contains(companyCode) && billerV3Config.getMungye() != null
                && StringUtils.isNotEmpty(billerV3Config.getMungye().getReceivingInstitutionIdCodeGye())) {
            return billerV3Config.getMungye().getReceivingInstitutionIdCodeGye();
        }
        return this.banredConfig.getReceivingInstitutionIdCode();
    }

    /**
     * Retorna el primaryAcctNumber para Pago apropiado según el companyCode.
     * Si el companyCode pertenece a MUNGYE, usa primaryAcctNumberPGye de la configuracion.
     * En caso contrario, usa primaryAcctNumber de BanredConfiguration.
     */
    public String getPrimaryAcctNumberPago(String companyCode) {
        if (billerV3Config.getCompaniesMungye().contains(companyCode) && billerV3Config.getMungye() != null
                && StringUtils.isNotEmpty(billerV3Config.getMungye().getPrimaryAcctNumberPGye())) {
            return billerV3Config.getMungye().getPrimaryAcctNumberPGye();
        }
        return this.banredConfig.getPrimaryAcctNumber();
    }

    /**
     * Retorna el accountId1 (financialAccount) apropiado según el companyCode.
     * Si el companyCode pertenece a MUNGYE, usa accountId1Gye de la configuracion.
     * En caso contrario, usa accountId1 de BanredConfiguration.
     */
    public String getAccountId1Gye(String companyCode) {
        if (billerV3Config.getCompaniesMungye().contains(companyCode) && billerV3Config.getMungye() != null
                && StringUtils.isNotEmpty(billerV3Config.getMungye().getAccountId1Gye())) {
            return billerV3Config.getMungye().getAccountId1Gye();
        }
        return this.banredConfig.getAccountId1();
    }

    /**
     * Retorna el track2 apropiado según el companyCode.
     * Si el companyCode pertenece a MUNGYE, usa track2Gye de la configuracion.
     * En caso contrario, retorna vacío.
     */
    public String getTrack2Gye(String companyCode) {
        if (billerV3Config.getCompaniesMungye().contains(companyCode) && billerV3Config.getMungye() != null
                && StringUtils.isNotEmpty(billerV3Config.getMungye().getTrack2Gye())) {
            return billerV3Config.getMungye().getTrack2Gye();
        }
        return StringUtils.EMPTY;
    }

    /**
     * Retorna el formato de fecha para Pago apropiado según el companyCode.
     * Si el companyCode pertenece a MUNGYE, usa formatDatePGye de la configuracion.
     * En caso contrario, retorna null para que se use el formato por defecto de versionConfig.
     */
    public String getFormatDatePago(String companyCode) {
        if (billerV3Config.getCompaniesMungye().contains(companyCode) && billerV3Config.getMungye() != null
                && StringUtils.isNotEmpty(billerV3Config.getMungye().getFormatDatePGye())) {
            return billerV3Config.getMungye().getFormatDatePGye();
        }
        return null;
    }
}
