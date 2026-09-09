package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
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
public class BillPaymentReversalResponseV1 extends BillInquiryResponseV1 {

}
