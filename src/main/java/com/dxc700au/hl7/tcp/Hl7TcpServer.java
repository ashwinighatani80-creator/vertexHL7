package com.dxc700au.hl7.tcp;

import com.dxc700au.hl7.config.MachineConfigLoader;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class Hl7TcpServer {

    /*
     * ============================================================
     * DEPENDENCIES
     * ============================================================
     */
    private final ExecutorService executorService;

    private final ClientConnectionHandler handler;

    private final MachineConfigLoader configLoader;

    /*
     * ============================================================
     * SERVER STATE
     * ============================================================
     */
    private final AtomicBoolean running =
            new AtomicBoolean(true);

    private final AtomicInteger activeConnections =
            new AtomicInteger(0);

    /*
     * ============================================================
     * MAX CONNECTIONS
     * ============================================================
     */
    private static final int MAX_CONNECTIONS =
            20;

    /*
     * ============================================================
     * SOCKET
     * ============================================================
     */
    private ServerSocket serverSocket;

    /*
     * ============================================================
     * SERVER START
     * ============================================================
     */
    @PostConstruct
    public void startServer() {

        log.info("""
                
                ============================================================
                HL7 TCP SERVER INITIALIZATION STARTED
                ============================================================
                """);

        executorService.submit(() -> {

            while (running.get()) {

                try {

                    startTcpServer();

                } catch (Exception e) {

                    log.error(
                            """
                                    
                                    ============================================================
                                    HL7 TCP SERVER CRASHED
                                    
                                    RESTARTING SERVER IN 5 SECONDS
                                    
                                    ============================================================
                                    """,
                            e
                    );

                    try {

                        Thread.sleep(5000);

                    } catch (InterruptedException ex) {

                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
    }

    /*
     * ============================================================
     * MAIN TCP SERVER
     * ============================================================
     */
    private void startTcpServer() throws Exception {

        JsonNode device = configLoader.getFirstDevice();

        /*
         * ============================================================
         * CONFIG
         * ============================================================
         */
        int port = configLoader.getTcpPort();

        int timeout = configLoader.getSocketTimeout();

        String deviceName = device.path("deviceName").asText("UNKNOWN");

        String protocol = device.path("protocol").asText("HL7");

        String version = device.path("hl7Version").asText("2.3.1");

        String connectorType = device.path("connectorType").asText("TCPIP");

        String communicationMode = device.path("communicationMode").asText("BiDirection");

        JsonNode tcp = device.path("TCPIP_params");

        String transportMode = tcp.path("mllpEnabled").asText("YES").equalsIgnoreCase("YES") ? "MLLP" : "RAW_TCP";

        String socketMode = tcp.path("mode").asText("SERVER");

        String bindIp = tcp.path("blindIp").asText("0.0.0.0");

        /*
         * ============================================================
         * HOST INFO
         * ============================================================
         */
        String hostIp = InetAddress.getLocalHost().getHostAddress();

        String hostName = InetAddress.getLocalHost().getHostName();

        /*
         * ============================================================
         * PROTOCOL MODE LOGS
         * ============================================================
         */
        log.info("""
                
                ============================================================
                ANALYZER PROTOCOL CONFIGURATION
                
                deviceName={}
                protocol={}
                hl7Version={}
                transport={}
                socketMode={}
                connectorType={}
                communicationMode={}
                
                ============================================================
                """, deviceName, protocol, version, transportMode, socketMode, connectorType, communicationMode);

        /*
         * ============================================================
         * NETWORK CONFIG LOGS
         * ============================================================
         */
        log.info("""
                
                ============================================================
                TCP NETWORK CONFIGURATION
                
                bindIP={}
                serverPort={}
                socketTimeout={}ms
                maxConnections={}
                
                ============================================================
                """, bindIp, port, timeout, MAX_CONNECTIONS);

        /*
         * ============================================================
         * JVM ENVIRONMENT LOGS
         * ============================================================
         */
        log.info("""
                
                ============================================================
                SERVER ENVIRONMENT
                
                hostName={}
                hostIp={}
                javaVersion={}
                osName={}
                fileEncoding={}
                
                ============================================================
                """, hostName, hostIp, System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("file.encoding"));

        /*
         * ============================================================
         * CREATE SOCKET
         * ============================================================
         */

        serverSocket = new ServerSocket(port, MAX_CONNECTIONS, InetAddress.getByName(bindIp));


        serverSocket.setReuseAddress(true);

        /*
         * ============================================================
         * SERVER STARTED
         * ============================================================
         */
        log.info("""
                
                ============================================================
                HL7 TCP SERVER STARTED SUCCESSFULLY
                
                protocol={}
                transport={}
                listeningPort={}
                status=READY
                
                WAITING FOR ANALYZER CONNECTION...
                
                ============================================================
                """, protocol, transportMode, port);

        /*
         * ============================================================
         * ACCEPT LOOP
         * ============================================================
         */
        while (running.get()) {

            log.info("""
                    
                    SERVER SOCKET STATE
                    
                    activeConnections={}
                    maxConnections={}
                    serverStatus=LISTENING
                    
                    """, activeConnections.get(), MAX_CONNECTIONS);

            log.info("SERVER WAITING ON {}:{}", bindIp, port);

            Socket socket = serverSocket.accept();

            log.info("TCP CONNECTION RECEIVED FROM {}:{}",
                    socket.getInetAddress().getHostAddress(),
                    socket.getPort());

            /*
             * ============================================================
             * CONNECTION LIMIT
             * ============================================================
             */
            if (activeConnections.get() >= MAX_CONNECTIONS) {

                log.error("""
                        
                        ============================================================
                        CONNECTION LIMIT EXCEEDED
                        
                        activeConnections={}
                        maxConnections={}
                        
                        REJECTING NEW CONNECTION
                        
                        ============================================================
                        """, activeConnections.get(), MAX_CONNECTIONS);

                socket.close();

                continue;
            }

            /*
             * ============================================================
             * SOCKET CONFIG
             * ============================================================
             */
            socket.setKeepAlive(true);

            socket.setTcpNoDelay(true);

            socket.setSoTimeout(timeout);


            int currentConnections = activeConnections.incrementAndGet();

            /*
             * ============================================================
             * CONNECTION LOG
             * ============================================================
             */
            log.info("""
                    
                    ============================================================
                    ANALYZER CONNECTED
                    
                    clientIp={}
                    clientPort={}
                    
                    socketKeepAlive={}
                    tcpNoDelay={}
                    socketTimeout={}ms
                    
                    activeConnections={}
                    
                    ============================================================
                    """, socket.getInetAddress().getHostAddress(), socket.getPort(), socket.getKeepAlive(), socket.getTcpNoDelay(), timeout, currentConnections);


            /*
             * ============================================================
             * PROCESS CONNECTION
             * ============================================================
             */
            executorService.submit(() -> {

                try {

                    handler.handle(socket);

                } finally {

                    /*
                     * ============================================================
                     * CLOSE SOCKET SAFELY
                     * ============================================================
                     */
                    try {

                        if (socket != null && !socket.isClosed()) {

                            socket.close();

                            log.info("""
                                    
                                    SOCKET CLOSED SUCCESSFULLY
                                    
                                    clientIp={}
                                    clientPort={}
                                    
                                    """, socket.getInetAddress().getHostAddress(), socket.getPort());
                        }

                    } catch (Exception e) {

                        log.error("SOCKET CLOSE FAILED", e);
                    }

                    /*
                     * ============================================================
                     * DECREMENT ACTIVE CONNECTIONS
                     * ============================================================
                     */
                    int remaining = activeConnections.decrementAndGet();

                    log.info("""
                            
                            ============================================================
                            CONNECTION CLOSED
                            
                            remainingConnections={}
                            
                            ============================================================
                            """, remaining);
                }
            });
        }
    }

    /*
     * ============================================================
     * SHUTDOWN
     * ============================================================
     */
    @PreDestroy
    public void stopServer() {

        log.info("""
                
                ============================================================
                HL7 TCP SERVER SHUTDOWN STARTED
                
                closingServerSocket=true
                activeConnections={}
                
                ============================================================
                """, activeConnections.get());

        running.set(false);

        try {

            if (serverSocket != null && !serverSocket.isClosed()) {

                serverSocket.close();

                log.info("SERVER SOCKET CLOSED SUCCESSFULLY");
            }

        } catch (Exception e) {

            log.error("SERVER SOCKET CLOSE FAILED", e);
        }

        log.info("""
                
                ============================================================
                HL7 TCP SERVER SHUTDOWN COMPLETED
                ============================================================
                """);
    }
}