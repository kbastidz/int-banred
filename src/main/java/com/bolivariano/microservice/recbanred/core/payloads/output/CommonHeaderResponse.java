package com.bolivariano.microservice.recbanred.core.payloads.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"operationInfo"})
public class CommonHeaderResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private OperationInfoResponse operationInfo;



    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"transactionId", "externalTransactionId", "transactionDate"})
    public static class OperationInfoResponse implements Serializable {
        private static final long serialVersionUID = 1L;
        private String transactionId;
        private String externalTransactionId;
        private Date transactionDate;

        public OperationInfoResponse(String transactionId, String externalTransactionId, Date transactionDate) {
            this.transactionId = transactionId;
            this.externalTransactionId = externalTransactionId;
            this.transactionDate = transactionDate;
        }
    }
}
