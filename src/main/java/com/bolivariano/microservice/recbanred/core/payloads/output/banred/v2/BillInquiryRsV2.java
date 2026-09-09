package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillInquiryRs", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {
        "billInquiryResponse"
})
public class BillInquiryRsV2 {

    @XmlElement(name = "BillInquiryResponse", namespace = "http://tempuri.org/")
    private BillInquiryResponseV2 billInquiryResponse;

}
