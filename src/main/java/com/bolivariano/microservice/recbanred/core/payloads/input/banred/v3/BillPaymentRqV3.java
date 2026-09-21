package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v3;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillPaymentRq", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {"billPaymentRequest"})
public class BillPaymentRqV3 {

    @XmlElement(name = "BillPaymentRequest", namespace = "http://tempuri.org/")
    private BillPaymentRequestV3 billPaymentRequest;
}
