package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@SuperBuilder
@AllArgsConstructor
@XmlRootElement(name = "BillPaymentReversalResponse", namespace = "http://tempuri.org/")
public class BillPaymentReversalResponseV2 extends BillPaymentResponseV2 {

}
