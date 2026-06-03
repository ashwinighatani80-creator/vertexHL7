package com.dxc700au.hl7.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SampleResponse {

    @JsonProperty("patientid")
    private String patientId;

    @JsonProperty("firstName")
    private String patientName;

    @JsonProperty("dob")
    private String dob;

    @JsonProperty("genderid")
    private String gender;

    @JsonProperty("mrNo")
    private String medicalRecordNumber;

    @JsonProperty("barCode")
    private String barcode;

    @JsonProperty("sampleNumber")
    private String sampleNumber;

    @JsonProperty("paramCode")
    private List<String> paramCodes;

    @JsonProperty("priority")
    private String priority;
}