package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillInquiryRs", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {"billInquiryResponse"})
public class BillInquiryRsV3 {

    @XmlElement(name = "BillInquiryResponse", namespace = "http://tempuri.org/")
    private BillInquiryResponseV3 billInquiryResponse;
}
