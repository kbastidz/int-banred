package com.bolivariano.microservice.recbanred.core.payloads.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Generated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"additionalData"})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalDataReversal implements Serializable {

    @JsonProperty("additionalData")
    private AdditionalData additionalData;

    @Serial
    private static final long serialVersionUID = 1L;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"detail"})
    public static class AdditionalData implements Serializable {

        @JsonProperty("detail")
        private Detail detail;

        @Serial
        private static final long serialVersionUID = 1L;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonPropertyOrder({"reversalObligation"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Detail implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @JsonProperty("reversalObligation")
        private ReversalObligation reversalObligation;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonPropertyOrder({"header"})
        public static class ReversalObligation implements Serializable {

            @JsonProperty("header")
            private Header header;

            @Serial
            private static final long serialVersionUID = 1L;

            @Data
            @AllArgsConstructor
            @NoArgsConstructor
            @JsonInclude(JsonInclude.Include.NON_NULL)
            @JsonPropertyOrder({"documents"})
            public static class Header implements Serializable {

                @Serial
                private static final long serialVersionUID = 1L;

                @JsonProperty("detailData")
                private DetailData detailData;

                @JsonProperty("documents")
                private Documents documents;

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                @JsonPropertyOrder({"reversals", "infoReversal"})
                public static class DetailData implements Serializable {

                    @Serial
                    private static final long serialVersionUID = 1L;

                    @JsonProperty("reversals")
                    private List<Reversals> reversals = null;

                    @JsonProperty("infoReversal")
                    private List<InfoReversal> infoReversal = null;

                    @Data
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @JsonPropertyOrder({"titulo", "anio", "rubro"})
                    @Generated("jsonschema2pojo")
                    public static class Reversals implements Serializable {

                        private static final long serialVersionUID = 1L;

                        @JsonProperty("titulo")
                        private String titulo;
                        @JsonProperty("anio")
                        private String anio;
                        @JsonProperty("rubro")
                        private String rubro;
                    }

                    @Data
                    @NoArgsConstructor
                    @AllArgsConstructor
                    @JsonInclude(JsonInclude.Include.NON_NULL)
                    @JsonPropertyOrder({"name", "value"})
                    @Generated("jsonschema2pojo")
                    public static class InfoReversal implements Serializable {

                        private static final long serialVersionUID = 1L;

                        @JsonProperty("name")
                        private String name;
                        @JsonProperty("value")
                        private String value;

                    }
                }

                @Data
                @NoArgsConstructor
                @AllArgsConstructor
                @JsonInclude(JsonInclude.Include.NON_NULL)
                @JsonPropertyOrder({"billerAuthorizationCode"})
                public static class Documents implements Serializable {

                    @Serial
                    private static final long serialVersionUID = 1L;

                    @JsonProperty("billerAuthorizationCode")
                    private String billerAuthorizationCode;

                }
            }
        }
    }
}