package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillInquiryRs", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {
        "billInquiryResponse"
})
public class BillInquiryRsV1 {

    @XmlElement(name = "BillInquiryResponse", namespace = "http://tempuri.org/")
    private BillInquiryResponseV1 billInquiryResponse;

}
