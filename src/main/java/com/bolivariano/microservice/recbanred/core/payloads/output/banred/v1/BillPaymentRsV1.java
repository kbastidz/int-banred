package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillPaymentRs", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {
        "billPaymentResponse"
})
public class BillPaymentRsV1 {

    @XmlElement(name = "BillPaymentResponse", namespace = "http://tempuri.org/")
    private BillPaymentResponseV1 billPaymentResponse;

}
