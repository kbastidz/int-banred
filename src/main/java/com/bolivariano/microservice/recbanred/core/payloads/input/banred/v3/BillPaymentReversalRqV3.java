package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v3;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillPaymentReversalRq", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {"billPaymentReversalRequest"})
public class BillPaymentReversalRqV3 {

    @XmlElement(name = "BillPaymentReversalRequest", namespace = "http://tempuri.org/")
    private BillPaymentReversalRequestV3 billPaymentReversalRequest;
}
