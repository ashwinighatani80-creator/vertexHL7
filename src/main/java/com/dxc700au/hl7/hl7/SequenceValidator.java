package com.dxc700au.hl7.hl7;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



@Slf4j
@Component
public class SequenceValidator {

    /*
     * ============================================================
     * MESSAGE STATE CACHE
     * ============================================================
     */
    private final Map<String, SequenceState> sequenceMap =
            new ConcurrentHashMap<>();

    /*
     * ============================================================
     * DUPLICATE WINDOW
     * ============================================================
     */
    private static final long DUPLICATE_WINDOW_SECONDS =
            300;

    /*
     * ============================================================
     * RESEND WINDOW
     * ============================================================
     */
    private static final long RESEND_WINDOW_SECONDS =
            60;

    /*
     * ============================================================
     * VALIDATE FLOW
     * ============================================================
     */
    public synchronized void validateSequence(
            String sessionId,
            String controlId,
            String messageType
    ) {

        cleanup();

        Instant now =
                Instant.now();

        log.info(
                """
                        
                        ============================================================
                        [{}] HL7 SEQUENCE VALIDATION START
                        
                        controlId={}
                        messageType={}
                        
                        ============================================================
                        """,
                sessionId,
                controlId,
                messageType
        );

        /*
         * ============================================================
         * EXISTING STATE
         * ============================================================
         */
        SequenceState existing =
                sequenceMap.get(controlId);

        /*
         * ============================================================
         * FIRST MESSAGE
         * ============================================================
         */
        if (existing == null) {

            sequenceMap.put(
                    controlId,
                    new SequenceState(
                            messageType,
                            now,
                            1
                    )
            );

            log.info(
                    """
                            
                            [{}] NEW HL7 MESSAGE SEQUENCE
                            
                            controlId={}
                            messageType={}
                            sequenceStatus=NEW
                            
                            """,
                    sessionId,
                    controlId,
                    messageType
            );

            validateExpectedFlow(
                    sessionId,
                    messageType
            );

            return;
        }

        /*
         * ============================================================
         * TIME DIFFERENCE
         * ============================================================
         */
        long seconds =
                now.getEpochSecond()
                        - existing.timestamp()
                        .getEpochSecond();

        /*
         * ============================================================
         * DUPLICATE DETECTION
         * ============================================================
         */
        if (existing.messageType()
                .equals(messageType)
                && seconds <= DUPLICATE_WINDOW_SECONDS) {

            int resendCount =
                    existing.resendCount() + 1;

            sequenceMap.put(
                    controlId,
                    new SequenceState(
                            messageType,
                            now,
                            resendCount
                    )
            );

            log.warn(
                    """
                            
                            ============================================================
                            [{}] DUPLICATE HL7 MESSAGE DETECTED
                            
                            controlId={}
                            messageType={}
                            resendCount={}
                            resendDelay={} sec
                            
                            POSSIBLE REASONS:
                            - ACK NOT RECEIVED
                            - ANALYZER TIMEOUT
                            - NETWORK LATENCY
                            - SOCKET RESET
                            
                            ============================================================
                            """,
                    sessionId,
                    controlId,
                    messageType,
                    resendCount,
                    seconds
            );

            /*
             * ============================================================
             * TIMEOUT RESEND
             * ============================================================
             */
            if (seconds >= RESEND_WINDOW_SECONDS) {

                log.warn(
                        """
                                
                                [{}] TIMEOUT RESEND DETECTED
                                
                                controlId={}
                                resendAfter={} sec
                                
                                ANALYZER POSSIBLY DID NOT RECEIVE ACK
                                
                                """,
                        sessionId,
                        controlId,
                        seconds
                );
            }

            return;
        }

        /*
         * ============================================================
         * INVALID SEQUENCE
         * ============================================================
         */
        log.error(
                """
                        
                        ============================================================
                        [{}] INVALID HL7 MESSAGE SEQUENCE
                        
                        controlId={}
                        previousType={}
                        currentType={}
                        
                        ============================================================
                        """,
                sessionId,
                controlId,
                existing.messageType(),
                messageType
        );

        /*
         * ============================================================
         * UPDATE STATE
         * ============================================================
         */
        sequenceMap.put(
                controlId,
                new SequenceState(
                        messageType,
                        now,
                        existing.resendCount() + 1
                )
        );
    }

    /*
     * ============================================================
     * EXPECTED FLOW
     * ============================================================
     */
    private void validateExpectedFlow(
            String sessionId,
            String messageType
    ) {

        switch (messageType) {

            /*
             * ============================================================
             * QRY FLOW
             * ============================================================
             */
            case "QRY" -> log.info(
                    """
                            
                            ============================================================
                            [{}] EXPECTED HL7 FLOW
                            
                            RECEIVED : QRY
                            EXPECTED : DSR RESPONSE
                            
                            QUERY WORKFLOW VALID
                            
                            ============================================================
                            """,
                    sessionId
            );

            /*
             * ============================================================
             * ORU FLOW
             * ============================================================
             */
            case "ORU" -> log.info(
                    """
                            
                            ============================================================
                            [{}] EXPECTED HL7 FLOW
                            
                            RECEIVED : ORU
                            EXPECTED : ACK RESPONSE
                            
                            RESULT WORKFLOW VALID
                            
                            ============================================================
                            """,
                    sessionId
            );

            /*
             * ============================================================
             * UNKNOWN FLOW
             * ============================================================
             */
            default -> log.warn(
                    """
                            
                            [{}] UNKNOWN HL7 FLOW
                            
                            messageType={}
                            
                            NO FLOW RULE CONFIGURED
                            
                            """,
                    sessionId,
                    messageType
            );
        }
    }

    /*
     * ============================================================
     * CLEANUP
     * ============================================================
     */
    private void cleanup() {

        Instant now =
                Instant.now();

        int beforeSize =
                sequenceMap.size();

        sequenceMap.entrySet()
                .removeIf(entry -> {

                    long age =
                            now.getEpochSecond()
                                    - entry.getValue()
                                    .timestamp()
                                    .getEpochSecond();

                    return age > DUPLICATE_WINDOW_SECONDS;
                });

        int afterSize =
                sequenceMap.size();

        if (beforeSize != afterSize) {

            log.info(
                    """
                            
                            HL7 SEQUENCE CACHE CLEANUP
                            
                            removedEntries={}
                            remainingEntries={}
                            
                            """,
                    beforeSize - afterSize,
                    afterSize
            );
        }
    }

    /*
     * ============================================================
     * STATE
     * ============================================================
     */
    private record SequenceState(
            String messageType,
            Instant timestamp,
            int resendCount
    ) {
    }
}