package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillPaymentRs", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {"billPaymentResponse"})
public class BillPaymentRsV3 {

    @XmlElement(name = "BillPaymentResponse", namespace = "http://tempuri.org/")
    private BillPaymentResponseV3 billPaymentResponse;
}
