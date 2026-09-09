package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v1;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillPaymentResponse", namespace = "http://tempuri.org/")
@SuperBuilder
@AllArgsConstructor
public class BillPaymentResponseV1 extends BillInquiryResponseV1 {

}