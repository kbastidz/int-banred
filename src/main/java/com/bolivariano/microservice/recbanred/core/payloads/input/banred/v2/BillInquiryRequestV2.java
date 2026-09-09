package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v2;

import jakarta.xml.bind.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "aditionalData"
})
@XmlRootElement(name = "BillInquiryRequest", namespace = "http://tempuri.org/")
public class BillInquiryRequestV2 extends BaseRequest {

    @XmlElement(name = "AditionalData")
    private String aditionalData;
    
}
