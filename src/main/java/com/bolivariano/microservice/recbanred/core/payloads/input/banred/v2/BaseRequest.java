package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v2;

import jakarta.xml.bind.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="BaseRequestV2", namespace = "http://tempuri.org/")
@XmlType(propOrder = {
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
        "medio",
        "branchId"
})
public class BaseRequest {

    @XmlElement(name = "Channel")
    private int channel;

    @XmlElement(name = "PrimaryAcctNumber")
    private String primaryAcctNumber;

    @XmlElement(name = "TxCode")
    private String txCode;

    @XmlElement(name = "AcquirerAuditNumber")
    private String acquirerAuditNumber;

    @XmlElement(name = "TransactionTime")
    private String transactionTime;

    @XmlElement(name = "TransactionDate")
    private String transactionDate;

    @XmlElement(name = "AcquirerInstitutionID")
    private String acquirerInstitutionID;

    @XmlElement(name = "Track2")
    private String track2;

    @XmlElement(name = "TerminalID")
    private String terminalID;

    @XmlElement(name = "CurrencyCode")
    private int currencyCode;

    @XmlElement(name = "BillReference")
    private String billReference;

    @XmlElement(name = "BillServiceCode")
    private int billServiceCode;

    @XmlElement(name = "BillCompanyCode")
    private int billCompanyCode;

    @XmlElement(name = "Medio")
    private String medio;

    @XmlElement(name = "BranchId")
    private String branchId;
}
