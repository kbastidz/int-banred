package com.bolivariano.microservice.recbanred.core.payloads.output.banred;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenData {

    @SerializedName("DatosCabecera")
    private DatosCabecera datosCabecera;

    @SerializedName("DocTrxDetalle")
    private List<DocTrxDetalle> docTrxDetalle;

    @SerializedName("DocPagoRespuesta")
    private DocPagoRespuesta docPagoRespuesta;

    @SerializedName("DocReversoRespuesta")
    private DocReversoRespuesta docReversoRespuesta;
}
