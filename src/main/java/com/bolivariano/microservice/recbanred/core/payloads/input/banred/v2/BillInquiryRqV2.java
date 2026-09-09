package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v2;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillInquiryRq", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {
        "billInquiryRequest"
})
public class BillInquiryRqV2 {

    @XmlElement(name = "BillInquiryRequest", namespace = "http://tempuri.org/")
    private BillInquiryRequestV2 billInquiryRequest;

}