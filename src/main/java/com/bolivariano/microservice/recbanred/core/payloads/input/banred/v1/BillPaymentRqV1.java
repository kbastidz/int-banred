package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v1;

import jakarta.xml.bind.annotation.*;
import lombok.*;

@Setter
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "billPaymentRequest"
})
@XmlRootElement(name = "BillPaymentRq", namespace = "http://tempuri.org/")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillPaymentRqV1 {

    @XmlElement(name = "BillPaymentRequest", namespace = "http://tempuri.org/")
    private BillPaymentRequestV1 billPaymentRequest;

}
