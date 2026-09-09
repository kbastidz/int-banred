package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(namespace = "http://tempuri.org/")
public class BaseResponse {

    @XmlElement(name = "SwitchAuditNumber")
    private String switchAuditNumber;

    @XmlElement(name = "ResultCode")
    private String resultCode;

    @XmlElement(name = "ErrorMessage")
    private String errorMessage;

    @XmlElement(name = "ResponseData")
    private String responseData;

    @XmlElement(name = "BillerCutoverDate")
    private String billerCutoverDate;

}
