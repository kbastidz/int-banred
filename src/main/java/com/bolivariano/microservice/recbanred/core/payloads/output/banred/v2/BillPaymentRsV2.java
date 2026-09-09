package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v2;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "billPaymentResponse"
})
@XmlRootElement(name = "BillPaymentRs")
public class BillPaymentRsV2 {

    @XmlElement(name = "BillPaymentResponse")
    private BillPaymentResponseV2 billPaymentResponse;

}
