package com.bolivariano.microservice.recbanred.core.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "banred")
@Data
public class BanredConfiguration {

    String url;
    int connectionRequestTimeout;
    int connectionTimeout;
    int readTimeout;

    String nombreCanalBancaMovil;
    int codigoCanalBancaMovil;
    int cardSequenceBancaMovil;

    String nombreCanalBancaVirtual;
    int codigoCanalBancaVirtual;
    int cardSequenceBancaVirtual;

    String nombreCanalSAT;
    int codigoCanalSAT;
    int cardSequenceSAT;

    String nombreCanalVentanilla;
    int codigoCanalVentanilla;
    int cardSequenceVentanilla;

    String nombreCanalCNB;
    int codigoCanalCNB;
    int cardSequenceCNB;

    String txCodeConsulta;
    String txCodePagoDebito;
    String txCodePagoEfectivo;
    String txCodeReverso;

    int currencyCode;

    String medio;

    String primaryAcctNumber;

    String acquirerInstitutionID;
    String receivingInstitutionIdCode;

    String branchId;

    String genericErrorResponse;
    String systemErrorBanred;
    String notPermitedHoursError;

    String accpIdTellerCode;
    String cardAcceptorNameLoc;
    String accountId1;
    String routingData;
    String inputDataInqPayment;
    String inputDataReversal;
    String posEntryMode;
}
