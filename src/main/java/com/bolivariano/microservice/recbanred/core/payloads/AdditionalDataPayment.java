package com.bolivariano.microservice.recbanred.core.payloads;


import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"additionalData"})
public class AdditionalDataPayment implements Serializable {

    @JsonProperty("additionalData")
    private AdditionalData additionalData;

    @Serial
    private static final long serialVersionUID = 1L;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"detail"})
    public static class AdditionalData implements Serializable {

        @JsonProperty("detail")
        private Detail detail;

        @Serial
        private static final long serialVersionUID = 1L;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonPropertyOrder({"paymentObligation"})
        public static class Detail implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

            @JsonProperty("paymentObligation")
            private PaymentObligation paymentObligation;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonPropertyOrder({"header", "infoPerson", "additionalBiller", "documents"})
            public static class PaymentObligation implements Serializable {

                @Serial
                private static final long serialVersionUID = 1L;

                @JsonProperty("header")
                private Header header;
                @JsonProperty("infoPerson")
                private InfoPerson infoPerson;
                @JsonProperty("additionalBiller")
                private List<AdditionalBiller> additionalBiller = null;
                @JsonProperty("documents")
                private List<Document> documents = null;

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                @JsonPropertyOrder({"category", "idRubroBCE", "nameRubroBCE"})
                public static class Header implements Serializable {

                    @Serial
                    private static final long serialVersionUID = 1L;

                    @JsonProperty("category")
                    private String category;
                    @JsonProperty("idRubroBCE")
                    private String idRubroBCE;
                    @JsonProperty("nameRubroBCE")
                    private String nameRubroBCE;
                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                @JsonPropertyOrder({"documentType", "documentID", "fullName", "legalPerson"})
                public static class InfoPerson implements Serializable {

                    @Serial
                    private static final long serialVersionUID = 1L;

                    @JsonProperty("documentType")
                    private String documentType;
                    @JsonProperty("documentID")
                    private String documentID;
                    @JsonProperty("fullName")
                    private String fullName;
                    @JsonProperty("telephone")
                    private String telephone;
                    @JsonProperty("email")
                    private String email;
                    @JsonProperty("legalPerson")
                    private List<LegalPerson> legalPerson = null;

                    @Data
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @JsonPropertyOrder({"legalTypePerson", "legalNameOwner", "legalDocumentIDOwner"})
                    public static class LegalPerson implements Serializable {

                        @Serial
                        private static final long serialVersionUID = 1L;

                        @JsonProperty("legalTypePerson")
                        private String legalTypePerson;
                        @JsonProperty("legalNameOwner")
                        private String legalNameOwner;
                        @JsonProperty("legalDocumentIDOwner")
                        private String legalDocumentIDOwner;

                    }

                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                @JsonPropertyOrder({"name", "value"})
                public static class AdditionalBiller implements Serializable {

                    @Serial
                    private static final long serialVersionUID = 1L;

                    @JsonProperty("name")
                    private String name;
                    @JsonProperty("value")
                    private String value;

                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                @JsonPropertyOrder({"documentType", "number", "documentDate", "reference1", "reference2",
                        "serviceName", "dueDate", "amount", "authorizationCode", "base", "discount", "interest",
                        "penalty", "retention", "previousPaymentBalance", "order", "taxes"})
                public static class Document implements Serializable {

                    @Serial
                    private static final long serialVersionUID = 1L;

                    @JsonProperty("documentType")
                    private String documentType;
                    @JsonProperty("number")
                    private String number;
                    @JsonProperty("documentDate")
                    private String documentDate;
                    @JsonProperty("reference1")
                    private String reference1;
                    @JsonProperty("reference2")
                    private String reference2;
                    @JsonProperty("serviceName")
                    private String serviceName;
                    @JsonProperty("dueDate")
                    private String dueDate;
                    @JsonProperty("amount")
                    private String amount;
                    @JsonProperty("authorizationCode")
                    private String authorizationCode;
                    @JsonProperty("base")
                    private String base;
                    @JsonProperty("discount")
                    private String discount;
                    @JsonProperty("interest")
                    private String interest;
                    @JsonProperty("penalty")
                    private String penalty;
                    @JsonProperty("retention")
                    private String retention;
                    @JsonProperty("previousPaymentBalance")
                    private String previousPaymentBalance;
                    @JsonProperty("order")
                    private Integer order;
                    @JsonProperty("taxes")
                    private List<Tax> taxes = null;

                    @Data
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @JsonPropertyOrder({"name", "reference", "value"})
                    public static class Tax implements Serializable {

                        @Serial
                        private static final long serialVersionUID = 1L;

                        @JsonProperty("name")
                        private String name;
                        @JsonProperty("reference")
                        private String reference;
                        @JsonProperty("value")
                        private String value;

                    }

                }
            }

        }

    }

}