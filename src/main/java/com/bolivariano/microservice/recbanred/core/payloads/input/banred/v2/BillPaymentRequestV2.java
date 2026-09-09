package com.bolivariano.microservice.recbanred.core.payloads.input.banred.v2;

import jakarta.xml.bind.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillPaymentRequest", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {
        "accountId1",
        "amount",
        "formaPago",
        "indicatorReversal",
        "trazabilidad",
        "unicidad",
        "aditionalData"
})
public class BillPaymentRequestV2 extends BaseRequest {

    @XmlElement(name = "AccountId1")
    private String accountId1;

    @XmlElement(name = "Amount")
    private long amount;

    @XmlElement(name = "FormaPago")
    private String formaPago;

    @XmlElement(name = "IndicatorReversal")
    private String indicatorReversal;

    @XmlElement(name = "Trazabilidad")
    private String trazabilidad;

    @XmlElement(name = "Unicidad")
    private String unicidad;

    @XmlElement(name = "AditionalData")
    private String aditionalData;

}