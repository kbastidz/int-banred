package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v3;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillInquiryRq", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {"billInquiryRequest"})
public class BillInquiryRqV3 {

    @XmlElement(name = "BillInquiryRequest", namespace = "http://tempuri.org/")
    private BillInquiryRequestV3 billInquiryRequest;
}
