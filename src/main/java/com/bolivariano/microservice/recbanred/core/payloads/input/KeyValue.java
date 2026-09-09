package com.bolivariano.microservice.recbanred.core.payloads.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyValue {
    String name;
    String value;
}
