package com.dxc700au.hl7.hl7;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v231.segment.MSH;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Hl7MessageValidator {

    private static final String VERSION_23 = "2.3";

    private static final String VERSION_231 = "2.3.1";

    private static final String PROCESSING_ID = "P";

    public void validate(
            Message message
    ) throws Exception {

        validateMessage(message);

        MSH msh =
                extractMsh(message);

        validateRequiredFields(msh);

        validateVersion(msh);

        validateProcessingId(msh);

        log.info(
                "HL7 validation successful"
        );
    }

    private void validateMessage(
            Message message
    ) {

        if (message == null) {

            throw new RuntimeException(
                    "HL7 message is null"
            );
        }
    }

    private MSH extractMsh(
            Message message
    ) throws Exception {

        MSH msh =
                (MSH) message.get("MSH");

        if (msh == null) {

            throw new RuntimeException(
                    "MSH segment missing"
            );
        }

        return msh;
    }

    private void validateRequiredFields(
            MSH msh
    ) throws Exception {

        validateField(
                msh.getMessageType()
                        .encode(),
                "MSH-9 MESSAGE TYPE"
        );

        validateField(
                msh.getMessageControlID()
                        .getValue(),
                "MSH-10 CONTROL ID"
        );

        validateField(
                msh.getProcessingID()
                        .getProcessingID()
                        .getValue(),
                "MSH-11 PROCESSING ID"
        );

        validateField(
                msh.getVersionID()
                        .getVersionID()
                        .getValue(),
                "MSH-12 VERSION"
        );

        validateField(
                msh.getDateTimeOfMessage()
                        .getTimeOfAnEvent()
                        .getValue(),
                "MSH-7 TIMESTAMP"
        );
    }

    private void validateVersion(
            MSH msh
    ) throws Exception {

        String version =
                msh.getVersionID()
                        .getVersionID()
                        .getValue();

        if (!VERSION_23.equals(version)
                && !VERSION_231.equals(version)) {

            throw new RuntimeException(
                    "Unsupported HL7 version"
            );
        }
    }

    private void validateProcessingId(
            MSH msh
    ) throws Exception {

        String processingId =
                msh.getProcessingID()
                        .getProcessingID()
                        .getValue();

        if (!PROCESSING_ID.equals(processingId)) {

            throw new RuntimeException(
                    "Invalid processing ID"
            );
        }
    }

    private void validateField(
            String value,
            String field
    ) {

        if (value == null
                || value.isBlank()) {

            throw new RuntimeException(
                    field + " missing"
            );
        }
    }
}