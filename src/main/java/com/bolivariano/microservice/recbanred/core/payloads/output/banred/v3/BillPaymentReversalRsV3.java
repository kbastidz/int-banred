package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillPaymentReversalRs", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {"billPaymentReversalResponse"})
public class BillPaymentReversalRsV3 {

    @XmlElement(name = "BillPaymentReversalResponse", namespace = "http://tempuri.org/")
    private BillPaymentReversalResponseV3 billPaymentReversalResponse;
}
