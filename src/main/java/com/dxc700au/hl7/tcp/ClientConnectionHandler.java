package com.dxc700au.hl7.tcp;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v231.segment.MSH;

import com.dxc700au.hl7.config.MachineConfigLoader;
import com.dxc700au.hl7.hl7.AckMessageBuilder;
import com.dxc700au.hl7.hl7.DuplicateMessageDetector;
import com.dxc700au.hl7.hl7.Hl7MessageRouter;
import com.dxc700au.hl7.hl7.Hl7MessageValidator;
import com.dxc700au.hl7.hl7.Hl7Parser;
import com.dxc700au.hl7.hl7.QueryResponse;
import com.dxc700au.hl7.logging.Hl7AuditLogger;
import com.dxc700au.hl7.monitoring.AnalyzerIdleMonitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientConnectionHandler {

    /*
     * ============================================================
     * DEPENDENCIES
     * ============================================================
     */
    private final MllpFrameReader frameReader;

    private final MllpFrameWriter frameWriter;

    private final Hl7Parser hl7Parser;

    private final Hl7MessageRouter router;

    private final AckMessageBuilder ackBuilder;

    private final Hl7MessageValidator validator;

    private final DuplicateMessageDetector duplicateDetector;

    private final Hl7AuditLogger auditLogger;

    private final MachineConfigLoader configLoader;

    private final AnalyzerIdleMonitor analyzerIdleMonitor;

    /*
     * ============================================================
     * SESSION COUNTER
     * ============================================================
     */
    private static final AtomicLong SESSION_COUNTER = new AtomicLong(1);

    /*
     * ============================================================
     * HANDLE CONNECTION
     * ============================================================
     */
    public void handle(Socket socket) {

        String sessionId = generateSessionId();

        String clientIp = socket.getInetAddress().getHostAddress();

        int clientPort = socket.getPort();

        long connectionStartTime = System.currentTimeMillis();

        int processedMessages = 0;

        Message parsedMessage = null;

        log.info("""
                
                ============================================================
                ANALYZER SESSION STARTED
                
                sessionId={}
                clientIp={}
                clientPort={}
                connectedAt={}
                
                ============================================================
                """, sessionId, clientIp, clientPort, LocalDateTime.now());

        auditLogger.connection(sessionId, clientIp, "CONNECTED");

        try {

            /*
             * ============================================================
             * SOCKET CONFIG
             * ============================================================
             */
            socket.setSoTimeout(configLoader.getSocketTimeout());

            socket.setKeepAlive(true);

            socket.setTcpNoDelay(true);

            log.info("""
                    
                    SOCKET CONFIGURED
                    
                    sessionId={}
                    timeout={}ms
                    keepAlive={}
                    tcpNoDelay={}
                    
                    """, sessionId, configLoader.getSocketTimeout(), socket.getKeepAlive(), socket.getTcpNoDelay());

            /*
             * ============================================================
             * MAIN LOOP
             * ============================================================
             */
            while (!socket.isClosed()) {

                long messageStartTime = System.currentTimeMillis();

                log.info("[{}] WAITING FOR HL7 MESSAGE...", sessionId);

                /*
                 * ============================================================
                 * READ MESSAGE
                 * ============================================================
                 */
                String inboundMessage = frameReader.readMessage(socket.getInputStream(), socket.getOutputStream(), sessionId);
                log.info("""
                        
                        ============================================================
                        RAW HL7 MESSAGE RECEIVED
                        
                        {}
                        
                        ============================================================
                        """, inboundMessage.replace("\r", "\n"));

                /*
                 * ============================================================
                 * EMPTY MESSAGE
                 * ============================================================
                 */
                if (inboundMessage == null || inboundMessage.isBlank()) {

                    log.warn("[{}] EMPTY HL7 MESSAGE RECEIVED", sessionId);

                    continue;
                }

                /*
                 * ============================================================
                 * PARSE
                 * ============================================================
                 */
                parsedMessage = hl7Parser.parse(inboundMessage);

                /*
                 * ============================================================
                 * VALIDATE
                 * ============================================================
                 */
                validator.validate(parsedMessage);

                /*
                 * ============================================================
                 * EXTRACT MSH
                 * ============================================================
                 */
                MSH msh = (MSH) parsedMessage.get("MSH");

                String controlId = msh.getMessageControlID().getValue();

                String messageType = msh.getMessageType().getMessageType().getValue();

                /*
                 * ============================================================
                 * UPDATE IDLE MONITOR
                 * ============================================================
                 */
                analyzerIdleMonitor.updateActivity(sessionId, clientIp, messageType);

                /*
                 * ============================================================
                 * AUDIT RX
                 * ============================================================
                 */
                auditLogger.rx(sessionId, clientIp, messageType, controlId, inboundMessage);

                /*
                 * ============================================================
                 * DUPLICATE CHECK
                 * ============================================================
                 */
                if (duplicateDetector.isDuplicate(controlId)) {

                    log.warn("""
                            
                            ============================================================
                            [{}] DUPLICATE MESSAGE RECEIVED
                            
                            controlId={}
                            messageType={}
                            
                            ============================================================
                            """, sessionId, controlId, messageType);

                    String cachedAck = duplicateDetector.getCachedAck(controlId);

                    /*
                     * ============================================================
                     * SEND CACHED ACK
                     * ============================================================
                     */
                    if (cachedAck != null && !cachedAck.isBlank()) {

                        synchronized (socket) {

                            frameWriter.writeMessage(socket.getOutputStream(), cachedAck, sessionId);
                        }

                        auditLogger.tx(sessionId, clientIp, "ACK", controlId, cachedAck);

                        log.info("[{}] CACHED ACK SENT", sessionId);
                    }

                    /*
                     * ============================================================
                     * FALLBACK ACK
                     * ============================================================
                     */
                    else {

                        String ack = ackBuilder.buildApplicationAcceptAck(parsedMessage);

                        synchronized (socket) {

                            frameWriter.writeMessage(socket.getOutputStream(), ack, sessionId);
                        }

                        auditLogger.tx(sessionId, clientIp, "ACK", controlId, ack);

                        log.info("[{}] FALLBACK ACK SENT", sessionId);
                    }

                    continue;
                }

                /*
                 * ============================================================
                 * ROUTE
                 * ============================================================
                 */
                Object response = router.route(parsedMessage);

                /*
                 * ============================================================
                 * SINGLE RESPONSE
                 * ============================================================
                 */
                if (response instanceof String singleResponse) {

                    synchronized (socket) {

                        frameWriter.writeMessage(socket.getOutputStream(), singleResponse, sessionId);
                    }

                    auditLogger.tx(sessionId, clientIp, "HL7_RESPONSE", controlId, singleResponse);

                    duplicateDetector.markProcessed(parsedMessage, singleResponse);
                }

                /*
                 * ============================================================
                 * QUERY RESPONSE
                 * ============================================================
                 */
                if (response instanceof QueryResponse queryResponse) {

                    /*
                     * ============================================================
                     * SEND QCK
                     * ============================================================
                     */
                    if (queryResponse.getQck() != null) {

                        synchronized (socket) {

                            frameWriter.writeMessage(socket.getOutputStream(), queryResponse.getQck(), sessionId);
                        }

                        auditLogger.tx(sessionId, clientIp, "QCK", controlId, queryResponse.getQck());

                        log.info("""
                                
                                ============================================================
                                QCK^Q02 SENT TO ANALYZER
                                
                                sessionId={}
                                controlId={}
                                responseType=QCK^Q02
                                
                                ============================================================
                                """, sessionId, controlId);

                        Thread.sleep(100);
                    }

                    /*
                     * ============================================================
                     * SEND DSR
                     * ============================================================
                     */
                    if (queryResponse.getDsr() != null) {

                        synchronized (socket) {

                            frameWriter.writeMessage(socket.getOutputStream(), queryResponse.getDsr(), sessionId);
                        }

                        auditLogger.tx(sessionId, clientIp, "DSR", controlId, queryResponse.getDsr());

                        log.info("""
                                
                                ============================================================
                                DSR^Q03 SENT TO ANALYZER
                                
                                sessionId={}
                                controlId={}
                                responseType=DSR^Q03
                                
                                ============================================================
                                """, sessionId, controlId);
                    }

                    duplicateDetector.markProcessed(parsedMessage, queryResponse.getQck());
                }

                processedMessages++;

                long totalExecutionTime = System.currentTimeMillis() - messageStartTime;

                log.info("""
                        
                        ============================================================
                        [{}] MESSAGE PROCESSING COMPLETED
                        
                        controlId={}
                        messageType={}
                        
                        processedMessages={}
                        executionTime={}ms
                        
                        ============================================================
                        """, sessionId, controlId, messageType, processedMessages, totalExecutionTime);
            }

        } catch (SocketTimeoutException e) {

            log.error("""
                    
                    ============================================================
                    [{}] SOCKET TIMEOUT
                    
                    clientIp={}
                    timeout={}ms
                    
                    ============================================================
                    """, sessionId, clientIp, configLoader.getSocketTimeout(), e);

        } catch (EOFException e) {

            log.warn("""
                    
                    ============================================================
                    [{}] ANALYZER DISCONNECTED
                    
                    clientIp={}
                    
                    ============================================================
                    """, sessionId, clientIp);

        } catch (Exception e) {

            log.error("""
                    
                    ============================================================
                    [{}] HL7 PROCESSING FAILED
                    
                    clientIp={}
                    
                    ============================================================
                    """, sessionId, clientIp, e);

            auditLogger.error(sessionId, "HL7_PROCESSING", e);

            /*
             * ============================================================
             * ERROR ACK
             * ============================================================
             */
            try {

                if (parsedMessage != null) {

                    MSH msh = (MSH) parsedMessage.get("MSH");

                    String controlId = msh.getMessageControlID().getValue();

                    String errorAck = ackBuilder.buildErrorAck(parsedMessage, e);

                    synchronized (socket) {

                        frameWriter.writeMessage(socket.getOutputStream(), errorAck, sessionId);
                    }

                    auditLogger.tx(sessionId, clientIp, "ERROR_ACK", controlId, errorAck);

                    log.info("[{}] ERROR ACK SENT", sessionId);
                }

            } catch (Exception ackEx) {

                log.error("[{}] FAILED TO SEND ERROR ACK", sessionId, ackEx);
            }

        } finally {

            analyzerIdleMonitor.removeSession(sessionId);

            closeSocket(socket, clientIp, sessionId, processedMessages, connectionStartTime);
        }
    }

    /*
     * ============================================================
     * CLOSE SOCKET
     * ============================================================
     */
    private void closeSocket(Socket socket, String clientIp, String sessionId, int processedMessages, long connectionStartTime) {

        try {

            if (socket != null && !socket.isClosed()) {

                socket.close();
            }

            long totalConnectionTime = System.currentTimeMillis() - connectionStartTime;

            log.info("""
                    
                    ============================================================
                    ANALYZER CONNECTION CLOSED
                    
                    sessionId={}
                    clientIp={}
                    
                    processedMessages={}
                    connectionDuration={}ms
                    
                    ============================================================
                    """, sessionId, clientIp, processedMessages, totalConnectionTime);

            auditLogger.connection(sessionId, clientIp, "DISCONNECTED");

        } catch (Exception e) {

            log.error("[{}] SOCKET CLOSE FAILED", sessionId, e);
        }
    }

    /*
     * ============================================================
     * SESSION ID
     * ============================================================
     */
    private String generateSessionId() {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        long counter = SESSION_COUNTER.getAndIncrement();

        String shortUuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        return "DXC-" + timestamp + "-" + counter + "-" + shortUuid;
    }
}