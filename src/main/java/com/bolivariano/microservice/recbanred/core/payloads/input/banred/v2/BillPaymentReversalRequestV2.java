package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v2;

import jakarta.xml.bind.annotation.*;
import lombok.*;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillPaymentReversalRequest", namespace = "http://tempuri.org/")
@XmlType(name = "BillPaymentReversalRequest", propOrder = {
        "channel",
        "primaryAcctNumber",
        "txCode",
        "acquirerAuditNumber",
        "transactionTime",
        "transactionDate",
        "acquirerInstitutionID",
        "track2",
        "terminalID",
        "currencyCode",
        "billReference",
        "billServiceCode",
        "billCompanyCode",
        "switchAuditNumber",
        "sequenceAcquire",
        "billerCutoverDate",
        "medio",
        "branchId",
        "accountId1",
        "amount",
        "formaPago",
        "indicatorReversal",
        "trazabilidad",
        "unicidad",
        "aditionalData"
})
public class BillPaymentReversalRequestV2 {

    @Setter
    @XmlElement(name = "Channel")
    protected int channel;

    @XmlElement(name = "PrimaryAcctNumber")
    protected String primaryAcctNumber;

    @XmlElement(name = "TxCode")
    protected String txCode;

    @XmlElement(name = "AcquirerAuditNumber")
    protected String acquirerAuditNumber;

    @XmlElement(name = "TransactionTime")
    protected String transactionTime;

    @XmlElement(name = "TransactionDate")
    protected String transactionDate;

    @XmlElement(name = "AcquirerInstitutionID")
    protected String acquirerInstitutionID;

    @XmlElement(name = "Track2")
    protected String track2;

    @XmlElement(name = "TerminalID")
    protected String terminalID;

    @XmlElement(name = "CurrencyCode")
    protected int currencyCode;

    @XmlElement(name = "BillReference")
    protected String billReference;

    @XmlElement(name = "BillServiceCode")
    protected int billServiceCode;

    @XmlElement(name = "BillCompanyCode")
    protected int billCompanyCode;

    @XmlElement(name = "SwitchAuditNumber")
    protected String switchAuditNumber;

    @XmlElement(name = "SequenceAcquire")
    protected String sequenceAcquire;

    @XmlElement(name = "BillerCutoverDate")
    protected String billerCutoverDate;

    @XmlElement(name = "Medio")
    protected String medio;

    @XmlElement(name = "BranchId")
    protected String branchId;

    @XmlElement(name = "AccountId1")
    protected String accountId1;

    @XmlElement(name = "Amount")
    protected long amount;

    @XmlElement(name = "FormaPago")
    protected String formaPago;

    @XmlElement(name = "IndicatorReversal")
    protected String indicatorReversal;

    @XmlElement(name = "Trazabilidad")
    protected String trazabilidad;

    @XmlElement(name = "Unicidad")
    protected String unicidad;

    @XmlElement(name = "AditionalData")
    protected String aditionalData;

}
