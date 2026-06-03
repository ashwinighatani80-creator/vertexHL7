package com.dxc700au.hl7.hl7;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QueryResponse {

    private String qck;

    private String dsr;
}