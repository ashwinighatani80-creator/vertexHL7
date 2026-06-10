package com.dxc700au.hl7.tcp;

import com.dxc700au.hl7.constants.MllpConstants;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


@Slf4j
@Component
public class MllpFrameReader {

    /*
     * ============================================================
     * ASCII CONTROL BYTES
     * ============================================================
     */
    private static final int ENQ = 0x05;

    private static final int ACK = 0x06;

    private static final int NAK = 0x15;

    private static final int EOT = 0x04;

    private static final int STX = 0x02;

    private static final int ETX = 0x03;

    /*
     * ============================================================
     * MAX MESSAGE SIZE
     * ============================================================
     */
    private static final int MAX_MESSAGE_SIZE = 10* 1024 * 1024;

    /*
     * ============================================================
     * READ MESSAGE
     * ============================================================
     */
    public String readMessage(InputStream inputStream, OutputStream outputStream, String sessionId) throws Exception {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int character;


        boolean started = false;

        boolean endBlockReceived = false;

        long frameStartTime = System.currentTimeMillis();

        log.info(
                """
                        
                        ============================================================
                        [{}] WAITING FOR TRANSPORT DATA
                        ============================================================
                       
                        """,
                sessionId
        );

        while ((character = inputStream.read()) != -1) {

            log.info(
                    "[{}] RAW BYTE => DEC={} HEX=0x{}",
                    sessionId,
                    character,
                    String.format("%02X", character)
            );

            /*
             * ============================================================
             * ENQ RECEIVED
             * ============================================================
             */
            if (character == ENQ) {

                log.info(
                        """
                                
                                ============================================================
                                [{}] ENQ RECEIVED
                                
                                TRANSPORT HEARTBEAT DETECTED
                                ANALYZER REQUESTING ACKNOWLEDGEMENT
                                
                                BYTE=0x05
                                
                                ============================================================
                                """,
                        sessionId
                );

                /*
                 * ============================================================
                 * SEND TRANSPORT ACK
                 * ============================================================
                 */
                outputStream.write(ACK);

                outputStream.flush();

                log.info(
                        """
                                
                                ============================================================
                                [{}] TRANSPORT ACK SENT
                                
                                ACK BYTE=0x06
                                HEARTBEAT ACKNOWLEDGED
                                
                                ============================================================
                                """,
                        sessionId
                );

                continue;
            }

            /*
             * ============================================================
             * TRANSPORT ACK RECEIVED
             * ============================================================
             */
            if (character == ACK) {

                log.info(
                        """
                                
                                ============================================================
                                [{}] TRANSPORT ACK RECEIVED
                                
                                ANALYZER ACCEPTED PREVIOUS FRAME
                                
                                BYTE=0x06
                                
                                ============================================================
                                """,
                        sessionId
                );

                continue;
            }

            /*
             * ============================================================
             * NAK RECEIVED
             * ============================================================
             */
            if (character == NAK) {

                log.warn(
                        """
                                
                                ============================================================
                                [{}] TRANSPORT NAK RECEIVED
                                
                                ANALYZER REJECTED PREVIOUS FRAME
                                
                                BYTE=0x15
                                
                                POSSIBLE REASONS:
                                - FRAME CORRUPTION
                                - INVALID TERMINATION
                                - CHECKSUM FAILURE
                                - TRANSPORT ERROR
                                
                                ============================================================
                                """,
                        sessionId
                );

                continue;
            }

            /*
             * ============================================================
             * EOT RECEIVED
             * ============================================================
             */
            if (character == EOT) {

                log.info(
                        """
                                
                                ============================================================
                                [{}] EOT RECEIVED
                                
                                END OF TRANSMISSION DETECTED
                                ANALYZER CLOSED CURRENT TRANSPORT SESSION
                                
                                BYTE=0x04
                                
                                ============================================================
                                """,
                        sessionId
                );

                continue;
            }

            /*
             * ============================================================
             * STX RECEIVED
             * ============================================================
             */
            if (character == STX) {

                log.info(
                        """
                                
                                [{}] STX RECEIVED
                                
                                START OF TEXT DETECTED
                                
                                BYTE=0x02
                                
                                """,
                        sessionId
                );

                continue;
            }

            /*
             * ============================================================
             * ETX RECEIVED
             * ============================================================
             */
            if (character == ETX) {

                log.info(
                        """
                                
                                [{}] ETX RECEIVED
                                
                                END OF TEXT DETECTED
                                
                                BYTE=0x03
                                
                                """,
                        sessionId
                );

                continue;
            }

            /*
             * ============================================================
             * MLLP START BLOCK
             * ============================================================
             */
            if (character == MllpConstants.START_BLOCK) {

                started = true;

                frameStartTime = System.currentTimeMillis();

                log.info(
                        """
                                
                                ============================================================
                                [{}] MLLP START BLOCK RECEIVED
                                
                                START_BLOCK=0x0B
                                HL7 FRAME TRANSMISSION STARTED
                                
                                ============================================================
                                """,
                        sessionId
                );

                continue;
            }


            if (!started) {
                log.error(
                        """
                        UNKNOWN PREAMBLE BYTE
                
                        DEC={}
                        HEX=0x{}
                
                        EXPECTED START BLOCK = 0x0B
                        """,
                        character,
                        String.format("%02X", character)
                );

                continue;
            }
            /*
             * ============================================================
             * MLLP END BLOCK
             * ============================================================
             */
            if (character == MllpConstants.END_BLOCK) {

                endBlockReceived = true;

                log.info(
                        """
                                
                                ============================================================
                                [{}] MLLP END BLOCK RECEIVED
                                
                                END_BLOCK=0x1C
                                
                                ============================================================
                                """,
                        sessionId
                );

                int trailing = inputStream.read();

                /*
                 * ============================================================
                 * VERIFY CR
                 * ============================================================
                 */
                if (trailing != MllpConstants.CARRIAGE_RETURN) {

                    log.error(
                            """
                                    
                                    ============================================================
                                    [{}] INVALID FRAME ENDING
                                    
                                    EXPECTED=0x0D
                                    RECEIVED={}
                                    
                                    POSSIBLE:
                                    - CORRUPTED TRANSPORT FRAME
                                    - SOCKET INTERRUPTION
                                    - ANALYZER BUG
                                    
                                    ============================================================
                                    """,
                            sessionId,
                            trailing
                    );

                } else {

                    log.info(
                            """
                                    
                                    [{}] FRAME TERMINATION VERIFIED
                                    
                                    CARRIAGE_RETURN=0x0D
                                    
                                    """,
                            sessionId
                    );
                }

                break;
            }

            /*
             * ============================================================
             * STORE FRAME DATA
             * ============================================================
             */
            buffer.write(character);

            /*
             * ============================================================
             * FRAME SIZE LOGGING
             * ============================================================
             */
            if (buffer.size() % 1024 == 0) {

                log.info("[{}] CURRENT FRAME SIZE={} bytes", sessionId, buffer.size());
            }

            /*
             * ============================================================
             * MAX SIZE VALIDATION
             * ============================================================
             */
            if (buffer.size() > MAX_MESSAGE_SIZE) {

                log.error(
                        """
                                
                                ============================================================
                                [{}] FRAME SIZE LIMIT EXCEEDED
                                
                                CURRENT_SIZE={} bytes
                                MAX_ALLOWED={} bytes
                                
                                POSSIBLE:
                                - CHROMATOGRAM DATA
                                - BINARY PAYLOAD
                                - MALFORMED HL7
                                - FRAME LOOP
                                
                                ============================================================
                                """,
                        sessionId,
                        buffer.size(),
                        MAX_MESSAGE_SIZE
                );

                throw new RuntimeException(
                        "MLLP FRAME SIZE EXCEEDED"
                );
            }
        }

        /*
         * ============================================================
         * SOCKET CLOSED
         * ============================================================
         */
        if (character == -1 && buffer.size() == 0) {

            log.warn(
                    """
                            
                            ============================================================
                            [{}] SOCKET STREAM CLOSED
                            
                            ANALYZER DISCONNECTED
                            
                            ============================================================
                            """,
                    sessionId
            );

            throw new EOFException("EOF EXCEPTION --> SOCKET DISCONNECTED");
        }

        /*
         * ============================================================
         * PARTIAL FRAME DETECTED
         * ============================================================
         */
        if (started && !endBlockReceived) {

            log.error(
                    """
                            
                            ============================================================
                            [{}] PARTIAL FRAME DETECTED
                            
                            START BLOCK RECEIVED
                            END BLOCK NOT RECEIVED
                            
                            RECEIVED_SIZE={} bytes
                            
                            POSSIBLE:
                            - NETWORK INTERRUPTION
                            - ANALYZER CRASH
                            - SOCKET RESET
                            - INCOMPLETE TRANSMISSION
                            
                            ============================================================
                            """,
                    sessionId,
                    buffer.size()
            );

            throw new RuntimeException(
                    "PARTIAL MLLP FRAME DETECTED"
            );
        }

        /*
         * ============================================================
         * CONVERT FRAME
         * ============================================================
         */
        String message = buffer.toString(StandardCharsets.UTF_8);

        /*
         * ============================================================
         * EMPTY FRAME
         * ============================================================
         */
        if (message.isBlank()) {

            log.warn(
                    "[{}] EMPTY HL7 FRAME RECEIVED",
                    sessionId
            );

            return null;
        }

        long frameTime = System.currentTimeMillis() - frameStartTime;

        /*
         * ============================================================
         * FINAL SUCCESS LOG
         * ============================================================
         */
        log.info(
                """
                        
                        ============================================================
                        [{}] MLLP FRAME RECEIVED SUCCESSFULLY
                        
                        FRAME_SIZE={} bytes
                        MESSAGE_LENGTH={} chars
                        TRANSPORT_TIME={}ms
                        
                        ============================================================
                        """,
                sessionId,
                buffer.size(),
                message.length(),
                frameTime
        );

        log.debug(
                "[{}] RAW HL7 MESSAGE : \n{}",
                sessionId,
                message.replace("\r", "\n")
        );

        return message.trim();
    }
}