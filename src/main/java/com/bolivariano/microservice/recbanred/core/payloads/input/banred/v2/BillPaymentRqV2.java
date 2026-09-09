package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v2;

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
public class BillPaymentRqV2 {

    @XmlElement(name = "BillPaymentRequest", namespace = "http://tempuri.org/")
    private BillPaymentRequestV2 billPaymentRequest;

}
