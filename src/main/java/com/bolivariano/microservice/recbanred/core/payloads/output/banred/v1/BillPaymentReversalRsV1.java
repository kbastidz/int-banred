package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "billPaymentReversalResponse"
})
@XmlRootElement(name = "BillPaymentReversalRs", namespace = "http://tempuri.org/")
public class BillPaymentReversalRsV1 {

    @XmlElement(name = "BillPaymentReversalResponse", namespace = "http://tempuri.org/")
    private BillPaymentReversalResponseV1 billPaymentReversalResponse;

}
