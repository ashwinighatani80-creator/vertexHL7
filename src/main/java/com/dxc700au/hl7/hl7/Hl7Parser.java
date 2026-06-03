package com.dxc700au.hl7.hl7;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Hl7Parser {

    private final PipeParser parser = new PipeParser();

    public Message parse(String rawMessage) throws Exception {

        /*
         * ============================================================
         * BASIC VALIDATION
         * ============================================================
         */
        validateMessage(rawMessage);

        try {

            log.info("HL7 Message Parsing Started");

            /*
             * ============================================================
             * RAW MESSAGE LOG
             * ============================================================
             */
            log.debug("Incoming HL7 Message : \n{}", rawMessage);


            rawMessage = sanitize(rawMessage);

            /*
             * ============================================================
             * PARSE HL7
             * ============================================================
             */
            Message parsedMessage = parser.parse(rawMessage);

            String messageType = parsedMessage.getName();

            log.info("HL7 Message Parsed Successfully | type={}", messageType);

            return parsedMessage;

        } catch (Exception e) {

            log.error("HL7 Message Parsing Failed", e);

            throw new RuntimeException("INVALID HL7 MESSAGE", e);
        }
    }

    /*
     * ============================================================
     * SANITIZE MESSAGE
     * ============================================================
     */
    private String sanitize(String rawMessage) {

        if (rawMessage == null) {

            return null;
        }

        return rawMessage

                /*
                 * NULL BYTE
                 */.replace("\0", "")

                /*
                 * CTRL-Z
                 */.replace("\u001A", "")

                /*
                 * REMOVE NON PRINTABLE CONTROL CHARS
                 * EXCEPT CR/LF/TAB
                 */.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")

                /*
                 * NORMALIZE LINE ENDINGS
                 */.replace("\n", "\r")

                /*
                 * TRIM
                 */.trim();
    }

    /*
     * ============================================================
     * VALIDATE RAW MESSAGE
     * ============================================================
     */
    private void validateMessage(String rawMessage) {

        if (rawMessage == null || rawMessage.isBlank()) {

            log.error("Received Empty HL7 Message");

            throw new RuntimeException("HL7 message is empty");
        }

        /*
         * ============================================================
         * FLEXIBLE MSH DETECTION
         * ============================================================
         */
        if (!rawMessage.contains("MSH")) {

            log.error("Invalid HL7 Message | MSH segment missing");

            throw new RuntimeException("MSH segment missing");
        }
    }
}

