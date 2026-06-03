package com.dxc700au.hl7.hl7;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v231.message.ACK;
import ca.uhn.hl7v2.model.v231.segment.ERR;
import ca.uhn.hl7v2.model.v231.segment.MSA;
import ca.uhn.hl7v2.parser.PipeParser;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AckMessageBuilder {

    private final PipeParser parser =
            new PipeParser();

    public String buildAck(
            Message inboundMessage,
            String ackCode,
            String text
    ) {

        try {

            ACK ack =
                    (ACK) inboundMessage.generateACK();

            /*
             * ============================================================
             * UTF-8
             * ============================================================
             */
            ack.getMSH()
                    .getCharacterSet(0)
                    .setValue("UNICODE UTF-8");

            /*
             * ============================================================
             * VERSION
             * ============================================================
             */
            ack.getMSH()
                    .getVersionID()
                    .getVersionID()
                    .setValue("2.3.1");

            /*
             * ============================================================
             * MSA
             * ============================================================
             */
            MSA msa =
                    ack.getMSA();

            msa.getAcknowledgementCode()
                    .setValue(ackCode);

            msa.getTextMessage()
                    .setValue(text);

            /*
             * ============================================================
             * ERR SEGMENT
             * ============================================================
             */
            ERR err =
                    ack.getERR();

            /*
             * 0 = NO ERROR
             */
            err.getErrorCodeAndLocation(0)
                    .getCodeIdentifyingError()
                    .getIdentifier()
                    .setValue("0");

            String encodedAck =
                    parser.encode(ack);

            log.info(
                    """
                    
                    ============================================================
                    ACK GENERATED SUCCESSFULLY
                    
                    ackCode={}
                    text={}
                    
                    ============================================================
                    """,
                    ackCode,
                    text
            );

            return encodedAck;

        } catch (Exception e) {

            log.error(
                    "ACK BUILD FAILED",
                    e
            );

            throw new RuntimeException(
                    "ACK GENERATION FAILED",
                    e
            );
        }
    }

    /*
     * ============================================================
     * AA
     * ============================================================
     */
    public String buildApplicationAcceptAck(
            Message message
    ) {

        return buildAck(
                message,
                "AA",
                "MESSAGE ACCEPTED"
        );
    }

    /*
     * ============================================================
     * AE
     * ============================================================
     */
    public String buildApplicationErrorAck(
            Message message,
            String error
    ) {

        return buildAck(
                message,
                "AE",
                error
        );
    }

    /*
     * ============================================================
     * AR
     * ============================================================
     */
    public String buildApplicationRejectAck(
            Message message,
            String error
    ) {

        return buildAck(
                message,
                "AR",
                error
        );
    }

    /*
     * ============================================================
     * SMART ERROR ACK
     * ============================================================
     */
    public String buildErrorAck(
            Message message,
            Exception exception
    ) {

        String error =
                exception.getMessage();

        if (error == null
                || error.isBlank()) {

            error = "UNKNOWN ERROR";
        }

        if (error.contains("MSH")
                || error.contains("VERSION")
                || error.contains("HL7")) {

            return buildApplicationRejectAck(
                    message,
                    error
            );
        }

        return buildApplicationErrorAck(
                message,
                error
        );
    }
}

