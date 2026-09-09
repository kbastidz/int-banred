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
        "billPaymentReversalResponse"
})
@XmlRootElement(name = "BillPaymentReversalRs")
public class BillPaymentReversalRsV2 {

    @XmlElement(name = "BillPaymentReversalResponse")
    private BillPaymentReversalResponseV2 billPaymentReversalResponse;

}
