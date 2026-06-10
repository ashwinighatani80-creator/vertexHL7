package com.dxc700au.hl7.hl7;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v231.segment.MSH;

import ca.uhn.hl7v2.util.Terser;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



@Slf4j
@Component
public class DuplicateMessageDetector {

    /*
     * ============================================================
     * CACHE EXPIRY
     * ============================================================
     */
    private static final long EXPIRY_SECONDS =
            3600;

    /*
     * ============================================================
     * DUPLICATE WINDOW
     * ============================================================
     */
    private static final long RESEND_WINDOW_SECONDS =
            120;

    /*
     * ============================================================
     * MESSAGE CACHE
     * ============================================================
     */
    private final Map<String, CachedMessage>
            processedMessages =
            new ConcurrentHashMap<>();

    /*
     * ============================================================
     * CHECK DUPLICATE
     * ============================================================
     */
    public boolean isDuplicate(
            String controlId
    ) {

        cleanup();

        if (controlId == null
                || controlId.isBlank()) {

            log.warn(
                    """
                            
                            DUPLICATE CHECK FAILED
                            
                            REASON=EMPTY CONTROL ID
                            
                            """
            );

            return false;
        }

        CachedMessage cached =
                processedMessages.get(controlId);

        /*
         * ============================================================
         * CACHE MISS
         * ============================================================
         */
        if (cached == null) {

            log.info(
                    """
                            
                            CACHE MISS
                            
                            controlId={}
                            status=NEW MESSAGE
                            
                            """,
                    controlId
            );

            return false;
        }

        /*
         * ============================================================
         * DUPLICATE DETECTED
         * ============================================================
         */
        long ageSeconds =
                Instant.now()
                        .getEpochSecond()
                        - cached.timestamp()
                        .getEpochSecond();

        log.warn(
                """
                        
                        ============================================================
                        DUPLICATE MESSAGE DETECTED
                        
                        controlId={}
                        messageType={}
                        age={} sec
                        
                        ============================================================
                        """,
                controlId,
                cached.messageType(),
                ageSeconds
        );

        /*
         * ============================================================
         * RESEND DETECTED
         * ============================================================
         */
        if (ageSeconds <= RESEND_WINDOW_SECONDS) {

            log.warn(
                    """
                            
                            RESEND DETECTED
                            
                            controlId={}
                            resendDelay={} sec
                            
                            POSSIBLE:
                            - ACK NOT RECEIVED
                            - ANALYZER TIMEOUT
                            - NETWORK LATENCY
                            - SOCKET INTERRUPTION
                            
                            """,
                    controlId,
                    ageSeconds
            );
        }

        /*
         * ============================================================
         * CACHED ACK
         * ============================================================
         */
        if (cached.cachedAck() != null
                && !cached.cachedAck().isBlank()) {

            log.info(
                    """
                            
                            RETURNING CACHED ACK
                            
                            controlId={}
                            cachedAckSize={} chars
                            
                            """,
                    controlId,
                    cached.cachedAck().length()
            );
        }

        return true;
    }

    /*
     * ============================================================
     * STORE MESSAGE
     * ============================================================
     */
    public void markProcessed(
            Message message,
            String ackMessage
    ) {

        try {

            Terser terser =
                    new Terser(message);

            String controlId =
                    terser.get("/MSH-10");

            String messageType =
                    terser.get("/MSH-9-1");

            if (controlId == null
                    || controlId.isBlank()) {

                log.warn(
                        """
                                
                                CACHE STORE FAILED
                                
                                REASON=EMPTY CONTROL ID
                                
                                """
                );

                return;
            }

            processedMessages.put(
                    controlId,
                    new CachedMessage(
                            controlId,
                            messageType,
                            ackMessage,
                            Instant.now()
                    )
            );

            log.info(
                    """
                            
                            ============================================================
                            HL7 MESSAGE CACHED
                            
                            controlId={}
                            messageType={}
                            ackCached={}
                            cacheSize={}
                            
                            ============================================================
                            """,
                    controlId,
                    messageType,
                    ackMessage != null,
                    processedMessages.size()
            );

        } catch (Exception e) {

            log.error(
                    "FAILED TO STORE DUPLICATE CACHE",
                    e
            );
        }
    }

    /*
     * ============================================================
     * GET CACHED ACK
     * ============================================================
     */
    public String getCachedAck(
            String controlId
    ) {

        CachedMessage cached =
                processedMessages.get(controlId);

        if (cached == null) {

            log.warn(
                    """
                            
                            CACHED ACK NOT FOUND
                            
                            controlId={}
                            
                            """,
                    controlId
            );

            return null;
        }

        log.info(
                """
                        
                        CACHE HIT
                        
                        controlId={}
                        messageType={}
                        
                        """,
                controlId,
                cached.messageType()
        );

        return cached.cachedAck();
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
                processedMessages.size();

        processedMessages.entrySet()
                .removeIf(entry -> {

                    boolean expired =
                            now.minusSeconds(
                                            EXPIRY_SECONDS
                                    )
                                    .isAfter(
                                            entry.getValue()
                                                    .timestamp()
                                    );

                    if (expired) {

                        log.info(
                                """
                                        
                                        CACHE ENTRY EXPIRED
                                        
                                        controlId={}
                                        
                                        """,
                                entry.getKey()
                        );
                    }

                    return expired;
                });

        int afterSize =
                processedMessages.size();

        if (beforeSize != afterSize) {

            log.info(
                    """
                            
                            DUPLICATE CACHE CLEANUP COMPLETED
                            
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
     * CACHE MODEL
     * ============================================================
     */
    private record CachedMessage(
            String controlId,
            String messageType,
            String cachedAck,
            Instant timestamp
    ) {
    }
}