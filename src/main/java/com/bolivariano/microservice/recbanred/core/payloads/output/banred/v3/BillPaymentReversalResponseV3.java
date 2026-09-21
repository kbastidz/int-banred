package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@SuperBuilder
//@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "BillPaymentReversalResponse", namespace = "http://tempuri.org/")
public class BillPaymentReversalResponseV3 extends BillInquiryResponseV3 {
}
