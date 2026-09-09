package com.bolivariano.microservice.recbanred.core.payloads.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfoPerson {

    private String category;
    private String documentType;
    private String documentID;
    private String fullName;
}
