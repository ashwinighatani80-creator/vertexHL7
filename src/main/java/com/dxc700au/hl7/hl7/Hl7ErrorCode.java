package com.dxc700au.hl7.hl7;

public enum Hl7ErrorCode {

    MESSAGE_ACCEPTED("AA"),

    APPLICATION_ERROR("AE"),

    APPLICATION_REJECT("AR");

    private final String code;

    Hl7ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}