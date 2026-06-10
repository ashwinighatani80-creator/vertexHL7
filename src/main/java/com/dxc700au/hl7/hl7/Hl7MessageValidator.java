package com.dxc700au.hl7.hl7;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Hl7MessageValidator {

    private static final String VERSION_23 = "2.3";
    private static final String VERSION_231 = "2.3.1";
    private static final String PROCESSING_ID = "P";

    public void validate(Message message) throws Exception {

        if (message == null) {
            throw new RuntimeException("HL7 message is null");
        }

        Terser terser = new Terser(message);

        String msgType = terser.get("/MSH-9");
        String controlId = terser.get("/MSH-10");
        String processingId = terser.get("/MSH-11");
        String version = terser.get("/MSH-12");
        String timestamp = terser.get("/MSH-7");

        validateField(msgType, "MSH-9 MESSAGE TYPE");
        validateField(controlId, "MSH-10 CONTROL ID");
        validateField(processingId, "MSH-11 PROCESSING ID");
        validateField(version, "MSH-12 VERSION");
        validateField(timestamp, "MSH-7 TIMESTAMP");

        if (!VERSION_23.equals(version)
                && !VERSION_231.equals(version)) {

            log.warn("Unexpected HL7 Version={}", version);
        }

        if (!PROCESSING_ID.equals(processingId)) {

            log.warn("Unexpected Processing ID={}", processingId);
        }

        log.info(
                "HL7 validation successful | type={} | controlId={} | version={}",
                msgType,
                controlId,
                version
        );
    }

    private void validateField(
            String value,
            String field
    ) {

        if (value == null || value.isBlank()) {

            throw new RuntimeException(
                    field + " missing"
            );
        }
    }
}