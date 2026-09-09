package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v1;

import jakarta.xml.bind.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

//@Getter
//@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "", propOrder = {
        "channel",
        "primaryAcctNumber",
        "txCode",
        "acquirerAuditNumber",
        "transactionTime",
        "transactionDate",
        "businessDate",
        "captureDate",
        "posEntryMode",
        "cardSequenceNumber",
        "acquirerInstitutionID",
        "track2",
        "retrievalReferenceNumber",
        "terminalNumber",
        "cardAcceptorIdCode",
        "cardAcceptorNameLoc",
        "currencyCode",
        "terminalData",
        "receivingInstitutionIdCode",
        "accountId1",
        "tellerCode",
        "billReference",
        "billServiceCode",
        "billCompanyCode",
        "routingData",
        "inputData",
        "branchCode",
        "tokenData"
})
@XmlRootElement(name = "BillInquiryRequest")
public class BillInquiryRequestV1 extends BaseRequest {

    private String tellerCode;

    @Override
    @XmlElement(name = "Channel")
    public int getChannel() {
        return super.channel;
    }

    @Override
    public void setChannel(int channel) {
        super.channel = channel;
    }

    @Override
    @XmlElement(name = "PrimaryAcctNumber")
    public String getPrimaryAcctNumber() {
        return super.primaryAcctNumber;
    }

    @Override
    public void setPrimaryAcctNumber(String primaryAcctNumber) {
        super.primaryAcctNumber = primaryAcctNumber;
    }

    @Override
    @XmlElement(name = "TxCode")
    public String getTxCode() {
        return super.txCode;
    }

    @Override
    public void setTxCode(String txCode) {
        super.txCode = txCode;
    }

    @Override
    @XmlElement(name = "AcquirerAuditNumber")
    public String getAcquirerAuditNumber() {
        return super.acquirerAuditNumber;
    }

    @Override
    public void setAcquirerAuditNumber(String acquirerAuditNumber) {
        super.acquirerAuditNumber = acquirerAuditNumber;
    }

    @Override
    @XmlElement(name = "TransactionTime")
    public String getTransactionTime() {
        return super.transactionTime;
    }

    @Override
    public void setTransactionTime(String transactionTime) {
        super.transactionTime = transactionTime;
    }

    @Override
    @XmlElement(name = "TransactionDate")
    public String getTransactionDate() {
        return super.transactionDate;
    }

    @Override
    public void setTransactionDate(String transactionDate) {
        super.transactionDate = transactionDate;
    }

    @Override
    @XmlElement(name = "BusinessDate")
    public String getBusinessDate() {
        return super.businessDate;
    }

    @Override
    public void setBusinessDate(String businessDate) {
        super.businessDate = businessDate;
    }

    @Override
    @XmlElement(name = "CaptureDate")
    public String getCaptureDate() {
        return super.captureDate;
    }

    @Override
    public void setCaptureDate(String captureDate) {
        super.captureDate = captureDate;
    }

    @Override
    @XmlElement(name = "PosEntryMode")
    public String getPosEntryMode() {
        return super.posEntryMode;
    }

    @Override
    public void setPosEntryMode(String posEntryMode) {
        super.posEntryMode = posEntryMode;
    }

    @Override
    @XmlElement(name = "CardSequenceNumber")
    public int getCardSequenceNumber() {
        return super.cardSequenceNumber;
    }

    @Override
    public void setCardSequenceNumber(int cardSequenceNumber) {
        super.cardSequenceNumber = cardSequenceNumber;
    }

    @Override
    @XmlElement(name = "AcquirerInstitutionID")
    public String getAcquirerInstitutionID() {
        return super.acquirerInstitutionID;
    }

    @Override
    public void setAcquirerInstitutionID(String acquirerInstitutionID) {
        super.acquirerInstitutionID = acquirerInstitutionID;
    }

    @Override
    @XmlElement(name = "Track2")
    public String getTrack2() {
        return super.track2;
    }

    @Override
    public void setTrack2(String track2) {
        super.track2 = track2;
    }

    @Override
    @XmlElement(name = "RetrievalReferenceNumber")
    public String getRetrievalReferenceNumber() {
        return super.retrievalReferenceNumber;
    }

    @Override
    public void setRetrievalReferenceNumber(String retrievalReferenceNumber) {
        super.retrievalReferenceNumber = retrievalReferenceNumber;
    }

    @Override
    @XmlElement(name = "TerminalNumber")
    public String getTerminalNumber() {
        return super.terminalNumber;
    }

    @Override
    public void setTerminalNumber(String terminalNumber) {
        super.terminalNumber = terminalNumber;
    }

    @Override
    @XmlElement(name = "CardAcceptorIdCode")
    public String getCardAcceptorIdCode() {
        return super.cardAcceptorIdCode;
    }

    @Override
    public void setCardAcceptorIdCode(String cardAcceptorIdCode) {
        super.cardAcceptorIdCode = cardAcceptorIdCode;
    }

    @Override
    @XmlElement(name = "CardAcceptorNameLoc")
    public String getCardAcceptorNameLoc() {
        return super.cardAcceptorNameLoc;
    }

    @Override
    public void setCardAcceptorNameLoc(String cardAcceptorNameLoc) {
        super.cardAcceptorNameLoc = cardAcceptorNameLoc;
    }

    @Override
    @XmlElement(name = "CurrencyCode")
    public int getCurrencyCode() {
        return super.currencyCode;
    }

    @Override
    public void setCurrencyCode(int currencyCode) {
        super.currencyCode = currencyCode;
    }

    @Override
    @XmlElement(name = "TerminalData")
    public String getTerminalData() {
        return super.terminalData;
    }

    @Override
    public void setTerminalData(String terminalData) {
        super.terminalData = terminalData;
    }

    @Override
    @XmlElement(name = "ReceivingInstitutionIdCode")
    public String getReceivingInstitutionIdCode() {
        return super.receivingInstitutionIdCode;
    }

    @Override
    public void setReceivingInstitutionIdCode(String receivingInstitutionIdCode) {
        super.receivingInstitutionIdCode = receivingInstitutionIdCode;
    }

    @Override
    @XmlElement(name = "AccountId1")
    public String getAccountId1() {
        return super.accountId1;
    }

    @Override
    public void setAccountId1(String accountId1) {
        super.accountId1 = accountId1;
    }

    @XmlElement(name = "TellerCode")
    public String getTellerCode() {
        return tellerCode;
    }

    public void setTellerCode(String tellerCode) {
        this.tellerCode = tellerCode;
    }

    @Override
    @XmlElement(name = "BillReference")
    public String getBillReference() {
        return super.billReference;
    }

    @Override
    public void setBillReference(String billReference) {
        super.billReference = billReference;
    }

    @Override
    @XmlElement(name = "BillServiceCode")
    public int getBillServiceCode() {
        return super.billServiceCode;
    }

    @Override
    public void setBillServiceCode(int billServiceCode) {
        super.billServiceCode = billServiceCode;
    }

    @Override
    @XmlElement(name = "BillCompanyCode")
    public int getBillCompanyCode() {
        return super.billCompanyCode;
    }

    @Override
    public void setBillCompanyCode(int billCompanyCode) {
        super.billCompanyCode = billCompanyCode;
    }

    @Override
    @XmlElement(name = "RoutingData")
    public String getRoutingData() {
        return super.routingData;
    }

    @Override
    public void setRoutingData(String routingData) {
        super.routingData = routingData;
    }

    @Override
    @XmlElement(name = "InputData")
    public String getInputData() {
        return super.inputData;
    }

    @Override
    public void setInputData(String inputData) {
        super.inputData = inputData;
    }

    @Override
    @XmlElement(name = "BranchCode")
    public String getBranchCode() {
        return super.branchCode;
    }

    @Override
    public void setBranchCode(String branchCode) {
        super.branchCode = branchCode;
    }

    @Override
    @XmlElement(name = "TokenData")
    public String getTokenData() {
        return super.tokenData;
    }

    @Override
    public void setTokenData(String tokenData) {
        super.tokenData = tokenData;
    }
}
