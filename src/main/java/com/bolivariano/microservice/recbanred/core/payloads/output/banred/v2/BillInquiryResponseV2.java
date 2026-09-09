package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2;

import jakarta.xml.bind.annotation.*;
import lombok.*;
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
        "amount",
        "responseData",
        "commissionsClient",
        "commissionsComerce",
        "billerCutoverDate",
        "partialPayment"
})
public class BillInquiryResponseV2 {

    @XmlElement(name = "SwitchAuditNumber")
    private String switchAuditNumber;

    @XmlElement(name = "ResultCode")
    private String resultCode;

    @XmlElement(name = "ErrorMessage")
    private String errorMessage;

    @XmlElement(name = "Amount")
    private String amount;

    @XmlElement(name = "ResponseData")
    private String responseData;

    @XmlElement(name = "CommissionsClient")
    private String commissionsClient;

    @XmlElement(name = "CommissionsComerce")
    private String commissionsComerce;

    @XmlElement(name = "BillerCutoverDate")
    private String billerCutoverDate;

    @XmlElement(name = "PartialPayment")
    private String partialPayment;
}
