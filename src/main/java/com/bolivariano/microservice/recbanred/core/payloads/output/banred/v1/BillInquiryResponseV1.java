package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillInquiryResponse", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {
        "switchAuditNumber",
        "resultCode",
        "errorMessage",
        "billerCutoverDate",
        "partialPayment",
        "commissions",
        "amount",
        "additionalData",
        "tokenData",
})
public class BillInquiryResponseV1 {

    @XmlElement(name = "SwitchAuditNumber")
    private String switchAuditNumber;

    @XmlElement(name = "ResultCode")
    private String resultCode;

    @XmlElement(name = "ErrorMessage")
    private String errorMessage;

    @XmlElement(name = "BillerCutoverDate")
    private String billerCutoverDate;

    @XmlElement(name = "PartialPayment")
    private String partialPayment;

    @XmlElement(name = "Commissions")
    private String commissions;

    @XmlElement(name = "Amount")
    private String amount;

    @XmlElement(name = "AdditionalData")
    private String additionalData;

    @XmlElement(name = "TokenData")
    private String tokenData;
}
