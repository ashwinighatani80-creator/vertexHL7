package com.dxc700au.hl7.monitoring;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AnalyzerIdleMonitor {

    /*
     * ============================================================
     * WARNING THRESHOLD
     * ============================================================
     */
    private static final long WARNING_IDLE_MINUTES =
            15;

    /*
     * ============================================================
     * CRITICAL THRESHOLD
     * ============================================================
     */
    private static final long CRITICAL_IDLE_MINUTES =
            30;

    /*
     * ============================================================
     * CLEANUP THRESHOLD
     * ============================================================
     */
    private static final long CLEANUP_MINUTES =
            120;

    /*
     * ============================================================
     * ANALYZER ACTIVITY CACHE
     * ============================================================
     */
    private final Map<String, AnalyzerState>
            analyzerStateMap =
            new ConcurrentHashMap<>();

    /*
     * ============================================================
     * UPDATE ACTIVITY
     * ============================================================
     */
    public void updateActivity(
            String sessionId,
            String clientIp,
            String messageType
    ) {

        AnalyzerState state =
                new AnalyzerState(
                        sessionId,
                        clientIp,
                        messageType,
                        LocalDateTime.now()
                );

        analyzerStateMap.put(
                sessionId,
                state
        );

        log.info(
                """
                        
                        ============================================================
                        ANALYZER ACTIVITY UPDATED
                        
                        sessionId={}
                        clientIp={}
                        lastMessageType={}
                        lastActivity={}
                        
                        ============================================================
                        """,
                sessionId,
                clientIp,
                messageType,
                state.lastActivityTime()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMddHHmmssSSS"
                                )
                        )
        );
    }

    /*
     * ============================================================
     * REMOVE SESSION
     * ============================================================
     */
    public void removeSession(
            String sessionId
    ) {

        AnalyzerState removed =
                analyzerStateMap.remove(
                        sessionId
                );

        if (removed != null) {

            log.info(
                    """
                            
                            ============================================================
                            ANALYZER SESSION REMOVED
                            
                            sessionId={}
                            clientIp={}
                            
                            ============================================================
                            """,
                    removed.sessionId(),
                    removed.clientIp()
            );
        }
    }

    /*
     * ============================================================
     * IDLE MONITOR
     * ============================================================
     */
    @Scheduled(fixedDelay = 60000)
    public void monitorIdleAnalyzers() {

        if (analyzerStateMap.isEmpty()) {

            log.debug(
                    "NO ACTIVE ANALYZER SESSIONS"
            );

            return;
        }

        log.info(
                """
                        
                        ============================================================
                        ANALYZER IDLE MONITOR CHECK
                        
                        activeSessions={}
                        
                        ============================================================
                        """,
                analyzerStateMap.size()
        );

        LocalDateTime now =
                LocalDateTime.now();

        analyzerStateMap.values()
                .forEach(state -> {

                    long idleMinutes =
                            Duration.between(
                                            state.lastActivityTime(),
                                            now
                                    )
                                    .toMinutes();

                    /*
                     * ============================================================
                     * CRITICAL IDLE
                     * ============================================================
                     */
                    if (idleMinutes >=
                            CRITICAL_IDLE_MINUTES) {

                        log.error(
                                """
                                        
                                        ============================================================
                                        CRITICAL ANALYZER IDLE DETECTED
                                        
                                        sessionId={}
                                        clientIp={}
                                        
                                        idleTime={} minutes
                                        
                                        lastMessageType={}
                                        lastActivity={}
                                        
                                        POSSIBLE:
                                        - ANALYZER POWER OFF
                                        - NETWORK FAILURE
                                        - SOCKET DEADLOCK
                                        - TRANSPORT FAILURE
                                        - MACHINE HANG
                                        
                                        NO HL7 MESSAGE RECEIVED FOR {} MINUTES
                                        
                                        ============================================================
                                        """,
                                state.sessionId(),
                                state.clientIp(),
                                idleMinutes,
                                state.lastMessageType(),
                                state.lastActivityTime()
                                        .format(
                                                DateTimeFormatter.ofPattern(
                                                        "yyyy-MM-dd HH:mm:ss"
                                                )
                                        ),
                                idleMinutes
                        );

                    }

                    /*
                     * ============================================================
                     * WARNING IDLE
                     * ============================================================
                     */
                    else if (idleMinutes >=
                            WARNING_IDLE_MINUTES) {

                        log.warn(
                                """
                                        
                                        ============================================================
                                        ANALYZER IDLE WARNING
                                        
                                        sessionId={}
                                        clientIp={}
                                        
                                        idleTime={} minutes
                                        
                                        lastMessageType={}
                                        lastActivity={}
                                        
                                        ANALYZER HAS BEEN IDLE
                                        
                                        ============================================================
                                        """,
                                state.sessionId(),
                                state.clientIp(),
                                idleMinutes,
                                state.lastMessageType(),
                                state.lastActivityTime()
                                        .format(
                                                DateTimeFormatter.ofPattern(
                                                        "yyyy-MM-dd HH:mm:ss"
                                                )
                                        )
                        );
                    }

                    /*
                     * ============================================================
                     * HEALTHY
                     * ============================================================
                     */
                    else {

                        log.info(
                                """
                                        
                                        ANALYZER HEALTHY
                                        
                                        sessionId={}
                                        idleTime={} minutes
                                        
                                        """,
                                state.sessionId(),
                                idleMinutes
                        );
                    }
                });

        cleanup();
    }

    /*
     * ============================================================
     * CLEANUP OLD SESSIONS
     * ============================================================
     */
    private void cleanup() {

        LocalDateTime now =
                LocalDateTime.now();

        int beforeSize =
                analyzerStateMap.size();

        analyzerStateMap.entrySet()
                .removeIf(entry -> {

                    long age =
                            Duration.between(
                                            entry.getValue()
                                                    .lastActivityTime(),
                                            now
                                    )
                                    .toMinutes();

                    boolean remove =
                            age >= CLEANUP_MINUTES;

                    if (remove) {

                        log.warn(
                                """
                                        
                                        ============================================================
                                        REMOVING STALE ANALYZER SESSION
                                        
                                        sessionId={}
                                        clientIp={}
                                        staleMinutes={}
                                        
                                        ============================================================
                                        """,
                                entry.getValue()
                                        .sessionId(),
                                entry.getValue()
                                        .clientIp(),
                                age
                        );
                    }

                    return remove;
                });

        int afterSize =
                analyzerStateMap.size();

        if (beforeSize != afterSize) {

            log.info(
                    """
                            
                            ANALYZER SESSION CLEANUP COMPLETED
                            
                            removedSessions={}
                            remainingSessions={}
                            
                            """,
                    beforeSize - afterSize,
                    afterSize
            );
        }
    }

    /*
     * ============================================================
     * ANALYZER STATE
     * ============================================================
     */
    private record AnalyzerState(
            String sessionId,
            String clientIp,
            String lastMessageType,
            LocalDateTime lastActivityTime
    ) {
    }
}