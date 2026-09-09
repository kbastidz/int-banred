package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2;

import jakarta.xml.bind.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BillPaymentResponse", propOrder = {
        "sequenceAcquire",
        "standIN"
})
@XmlRootElement(name = "BillPaymentResponse", namespace = "http://tempuri.org/")
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BillPaymentResponseV2 extends BaseResponse {

    @XmlElement(name = "SequenceAcquire")
    private String sequenceAcquire;

    @XmlElement(name = "StandIN")
    private String standIN;

}