package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v1;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Setter
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "billPaymentReversalRequest"
})
@XmlRootElement(name = "BillPaymentReversalRq", namespace = "http://tempuri.org/")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillPaymentReversalRqV1 {

    @XmlElement(name = "BillPaymentReversalRequest", namespace = "http://tempuri.org/")
    private BillPaymentReversalRequestV1 billPaymentReversalRequest;

}