package com.dxc700au.hl7.logging;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/*
 * ============================================================
 * HL7 AUDIT LOGGER
 * ============================================================
 *
 * FEATURES:
 *
 * ✅ RX HL7 LOGS
 * ✅ TX HL7 LOGS
 * ✅ SESSION TRACEABILITY
 * ✅ CONTROL ID TRACEABILITY
 * ✅ MESSAGE TYPE TRACEABILITY
 * ✅ MESSAGE SIZE LOGGING
 * ✅ AUDIT TIMESTAMP
 * ✅ ERROR TRACEABILITY
 * ✅ LARGE MESSAGE PROTECTION
 *
 * ============================================================
 */

@Slf4j
@Component
public class Hl7AuditLogger {

    /*
     * ============================================================
     * MAX LOG LENGTH
     * ============================================================
     */
    private static final int MAX_LOG_LENGTH =
            10000;

    /*
     * ============================================================
     * RX HL7
     * ============================================================
     */
    public void rx(
            String sessionId,
            String clientIp,
            String messageType,
            String controlId,
            String message
    ) {

        String auditId =
                generateAuditId();

        String sanitizedMessage =
                sanitize(message);

        int messageSize =
                message == null
                        ? 0
                        : message.length();

        log.info(
                """
                        
                        ============================================================
                        RX HL7 MESSAGE
                        
                        auditId={}
                        sessionId={}
                        clientIp={}
                        
                        direction=RECEIVE
                        
                        messageType={}
                        controlId={}
                        
                        messageSize={} chars
                        receivedTime={}
                        
                        -------------------- HL7 START --------------------
                        
                        {}
                        
                        --------------------- HL7 END ---------------------
                        
                        ============================================================
                        """,
                auditId,
                sessionId,
                clientIp,
                messageType,
                controlId,
                messageSize,
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd HH:mm:ss"
                                )
                        ),
                sanitizedMessage
        );
    }

    /*
     * ============================================================
     * TX HL7
     * ============================================================
     */
    public void tx(
            String sessionId,
            String clientIp,
            String messageType,
            String controlId,
            String message
    ) {

        String auditId =
                generateAuditId();

        String sanitizedMessage =
                sanitize(message);

        int messageSize =
                message == null
                        ? 0
                        : message.length();

        log.info(
                """
                        
                        ============================================================
                        TX HL7 MESSAGE
                        
                        auditId={}
                        sessionId={}
                        clientIp={}
                        
                        direction=TRANSMIT
                        
                        messageType={}
                        controlId={}
                        
                        messageSize={} chars
                        transmittedTime={}
                        
                        -------------------- HL7 START --------------------
                        
                        {}
                        
                        --------------------- HL7 END ---------------------
                        
                        ============================================================
                        """,
                auditId,
                sessionId,
                clientIp,
                messageType,
                controlId,
                messageSize,
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd HH:mm:ss"
                                )
                        ),
                sanitizedMessage
        );
    }

    /*
     * ============================================================
     * ERROR LOG
     * ============================================================
     */
    public void error(
            String sessionId,
            String operation,
            Exception exception
    ) {

        String auditId =
                generateAuditId();

        log.error(
                """
                        
                        ============================================================
                        HL7 ERROR
                        
                        auditId={}
                        sessionId={}
                        
                        operation={}
                        exceptionType={}
                        errorMessage={}
                        errorTime={}
                        
                        ============================================================
                        """,
                auditId,
                sessionId,
                operation,
                exception.getClass()
                        .getSimpleName(),
                exception.getMessage(),
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMddHHmmssSSS"
                                )
                        ),
                exception
        );
    }

    /*
     * ============================================================
     * CONNECTION EVENT
     * ============================================================
     */
    public void connection(
            String sessionId,
            String clientIp,
            String event
    ) {

        log.info(
                """
                        
                        ============================================================
                        HL7 CONNECTION EVENT
                        
                        sessionId={}
                        clientIp={}
                        event={}
                        timestamp={}
                        
                        ============================================================
                        """,
                sessionId,
                clientIp,
                event,
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMddHHmmssSSS"
                                )
                        )
        );
    }

    /*
     * ============================================================
     * SANITIZE MESSAGE
     * ============================================================
     */
    private String sanitize(
            String message
    ) {

        if (message == null) {

            return "";
        }

        String cleaned =
                message.replace(
                        "\r",
                        "\n"
                );

        /*
         * ============================================================
         * LARGE MESSAGE TRUNCATION
         * ============================================================
         */
        if (cleaned.length()
                > MAX_LOG_LENGTH) {

            log.warn(
                    """
                            
                            HL7 MESSAGE TRUNCATED
                            
                            originalSize={} chars
                            truncatedSize={} chars
                            
                            """,
                    cleaned.length(),
                    MAX_LOG_LENGTH
            );

            return cleaned.substring(
                    0,
                    MAX_LOG_LENGTH
            ) + "\n\n... MESSAGE TRUNCATED ...";
        }

        return cleaned;
    }

    /*
     * ============================================================
     * AUDIT ID
     * ============================================================
     */
    private String generateAuditId() {

        return "AUDIT-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}