package com.dxc700au.hl7.cache;

import lombok.Data;

import java.util.List;

@Data
public class WorklistRecord {

    private Long patientId;

    private String patientName;

    private String dob;

    private String gender;

    private String sampleNumber;

    private String barcode;

    private String sampleType;

    private String mrNo;

    private List<String> testCodes;
}