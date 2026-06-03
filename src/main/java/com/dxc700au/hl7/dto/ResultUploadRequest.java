package com.dxc700au.hl7.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultUploadRequest {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("patientName")
    private String patientName;

    @JsonProperty("testIdentifier")
    private String testIdentifier;

    @JsonProperty("sampleType")
    private String sampleType;

    @JsonProperty("testName")
    private String testName;

    @JsonProperty("refRange")
    private String refRange;

    @JsonProperty("testVal")
    private String testVal;

    @JsonProperty("machineId")
    private Long machineId;

    @JsonProperty("unitId")
    private Long unitId;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("resultDate")
    private String resultDate;

    @JsonProperty("lowHigh")
    private String lowHigh;

    @JsonProperty("equipmentName")
    private String equipmentName;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("runCode")
    private Integer runCode;

    @JsonProperty("patientid")
    private Long patientid;

    @JsonProperty("testid")
    private Long testid;

    @JsonProperty("dilutions")
    private String dilutions;

    @JsonProperty("resultabnormalflags")
    private String resultabnormalflags;

    @JsonProperty("natureofabnormalitytesting")
    private String natureofabnormalitytesting;

    @JsonProperty("resultstatus")
    private String resultstatus;

    @JsonProperty("dateTimeteststarted")
    private String dateTimeteststarted;

    @JsonProperty("dateTimetestcompleted")
    private String dateTimetestcompleted;

    @JsonProperty("resultcommenttext")
    private String resultcommenttext;

    @JsonProperty("resultcommenttype")
    private String resultcommenttype;

    @JsonProperty("resultcommentsource")
    private String resultcommentsource;

    @JsonProperty("sampleNumber")
    private String sampleNumber;


}