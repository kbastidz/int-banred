package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v3;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Campos base del sobre SOAP V3, validados 1:1 contra ejemplos reales de
 * BillInquiryRq/Rs para CNEL, MEER y MUNGYE (mismo layout que V1, pero
 * SIN el campo TokenData: en V3 el InputData ya contiene el token de
 * posicion fija completo, construido dinamicamente por TokenFieldEngine).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseRequest {

    protected int channel;
    protected String primaryAcctNumber;
    protected String txCode;
    protected String acquirerAuditNumber;
    protected String transactionTime;
    protected String transactionDate;
    protected String businessDate;
    protected String captureDate;
    protected String posEntryMode;
    protected int cardSequenceNumber;
    protected String acquirerInstitutionID;
    protected String track2;
    protected String retrievalReferenceNumber;
    protected String terminalNumber;
    protected String cardAcceptorIdCode;
    protected String cardAcceptorNameLoc;
    protected int currencyCode;
    protected String terminalData;
    protected String receivingInstitutionIdCode;
    protected String accountId1;
    protected String tellerCode;
    protected String billReference;
    protected int billServiceCode;
    protected int billCompanyCode;
    protected String routingData;
    protected String inputData;
    protected String branchCode;
}
