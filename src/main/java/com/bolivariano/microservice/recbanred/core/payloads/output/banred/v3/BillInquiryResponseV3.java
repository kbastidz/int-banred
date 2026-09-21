package com.bolivariano.microservice.recbanred.core.payloads.output.banred.v3;

import jakarta.xml.bind.annotation.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Response V3 de Consulta. Estructuralmente igual al sobre de V1, con la
 * diferencia CLAVE que motiva esta version: el campo de datos del biller
 * se llama "ResponseData" (no "TokenData") y su contenido es el token de
 * posicion fija crudo documentado en los Anexos (NO es JSON, a diferencia
 * de V1/V2) -> requiere TokenFieldEngine para decodificarlo.
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BillInquiryResponse", namespace = "http://tempuri.org/")
@XmlType(name = "", propOrder = {
        "switchAuditNumber", "resultCode", "errorMessage", "billerCutoverDate",
        "partialPayment", "commissions", "amount", "additionalData", "responseData"
})
public class BillInquiryResponseV3 {

    @XmlElement(name = "SwitchAuditNumber") private String switchAuditNumber;
    @XmlElement(name = "ResultCode") private String resultCode;
    @XmlElement(name = "ErrorMessage") private String errorMessage;
    @XmlElement(name = "BillerCutoverDate") private String billerCutoverDate;
    @XmlElement(name = "PartialPayment") private String partialPayment;
    @XmlElement(name = "Commissions") private String commissions;
    @XmlElement(name = "Amount") private String amount;
    @XmlElement(name = "AdditionalData") private String additionalData;
    @XmlElement(name = "ResponseData") private String responseData;
}
