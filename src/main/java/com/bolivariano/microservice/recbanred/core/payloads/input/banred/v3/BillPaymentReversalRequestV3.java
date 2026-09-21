package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v3;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BillPaymentReversalRequest", propOrder = {
        "channel", "primaryAcctNumber", "txCode", "amount", "acquirerAuditNumber", "transactionTime",
        "transactionDate", "businessDate", "captureDate", "posEntryMode", "cardSequenceNumber",
        "acquirerInstitutionID", "track2", "retrievalReferenceNumber", "terminalNumber",
        "cardAcceptorIdCode", "cardAcceptorNameLoc", "currencyCode", "terminalData",
        "receivingInstitutionIdCode", "accountId1", "financialAccount", "billReference",
        "billServiceCode", "billCompanyCode", "routingData", "inputData", "branchCode",
        "reversalIndicator", "originalData"
})
public class BillPaymentReversalRequestV3 {

    @XmlElement(name = "Channel") private int channel;
    @XmlElement(name = "PrimaryAcctNumber") private String primaryAcctNumber;
    @XmlElement(name = "TxCode") private String txCode;
    @XmlElement(name = "Amount") private long amount;
    @XmlElement(name = "AcquirerAuditNumber") private String acquirerAuditNumber;
    @XmlElement(name = "TransactionTime") private String transactionTime;
    @XmlElement(name = "TransactionDate") private String transactionDate;
    @XmlElement(name = "BusinessDate") private String businessDate;
    @XmlElement(name = "CaptureDate") private String captureDate;
    @XmlElement(name = "PosEntryMode") private String posEntryMode;
    @XmlElement(name = "CardSequenceNumber") private int cardSequenceNumber;
    @XmlElement(name = "AcquirerInstitutionID") private String acquirerInstitutionID;
    @XmlElement(name = "Track2") private String track2;
    @XmlElement(name = "RetrievalReferenceNumber") private String retrievalReferenceNumber;
    @XmlElement(name = "TerminalNumber") private String terminalNumber;
    @XmlElement(name = "CardAcceptorIdCode") private String cardAcceptorIdCode;
    @XmlElement(name = "CardAcceptorNameLoc") private String cardAcceptorNameLoc;
    @XmlElement(name = "CurrencyCode") private int currencyCode;
    @XmlElement(name = "TerminalData") private String terminalData;
    @XmlElement(name = "ReceivingInstitutionIdCode") private String receivingInstitutionIdCode;
    @XmlElement(name = "AccountId1") private String accountId1;
    @XmlElement(name = "FinancialAccount") private String financialAccount;
    @XmlElement(name = "BillReference") private String billReference;
    @XmlElement(name = "BillServiceCode") private int billServiceCode;
    @XmlElement(name = "BillCompanyCode") private int billCompanyCode;
    @XmlElement(name = "RoutingData") private String routingData;
    @XmlElement(name = "InputData") private String inputData;
    @XmlElement(name = "BranchCode") private String branchCode;
    @XmlElement(name = "ReversalIndicator") private String reversalIndicator;
    @XmlElement(name = "OriginalData") private String originalData;
}
