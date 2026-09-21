package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3;

import jakarta.xml.bind.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillPaymentResponse", namespace = "http://tempuri.org/")
@SuperBuilder
//@AllArgsConstructor
@NoArgsConstructor
public class BillPaymentResponseV3 extends BillInquiryResponseV3 {
}
