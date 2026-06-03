package com.dxc700au.hl7.hl7;

import com.dxc700au.hl7.api.LisApiClient;
import com.dxc700au.hl7.dto.ResultUploadRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncResultProcessor {

    private final LisApiClient apiClient;

    /*
     * ============================================================
     * ASYNC PROCESS
     * ============================================================
     */
    @Async
    public void process(
            ResultUploadRequest request
    ) {

        /*
         * ============================================================
         * CORRELATION ID
         * ============================================================
         */
        String correlationId =
                generateCorrelationId();

        long startTime =
                System.currentTimeMillis();

        String threadName =
                Thread.currentThread()
                        .getName();

        /*
         * ============================================================
         * VALIDATION
         * ============================================================
         */
        if (request == null) {

            log.error(
                    """
                            
                            ============================================================
                            [{}] RESULT PROCESSING FAILED
                            
                            REASON=REQUEST IS NULL
                            
                            ============================================================
                            """,
                    correlationId
            );

            return;
        }

        /*
         * ============================================================
         * REQUEST DETAILS
         * ============================================================
         */
        String sampleNumber =
                request.getSampleNumber();

        String testCode =
                request.getTestIdentifier();

        String testValue =
                request.getTestVal();

        String equipmentName =
                request.getEquipmentName();

        /*
         * ============================================================
         * START LOG
         * ============================================================
         */
        log.info(
                """
                        
                        ============================================================
                        [{}] RESULT UPLOAD STARTED
                        
                        sampleNumber={}
                        testCode={}
                        testValue={}
                        equipmentName={}
                        threadName={}
                        uploadStartTime={}
                        
                        ============================================================
                        """,
                correlationId,
                sampleNumber,
                testCode,
                testValue,
                equipmentName,
                threadName,
                LocalDateTime.now()
        );

        try {

            /*
             * ============================================================
             * API CALL START
             * ============================================================
             */
            long apiStartTime =
                    System.currentTimeMillis();

            log.info(
                    """
                            
                            [{}] CALLING LIS RESULT API
                            
                            sampleNumber={}
                            testCode={}
                            
                            """,
                    correlationId,
                    sampleNumber,
                    testCode
            );

            /*
             * ============================================================
             * RESULT UPLOAD
             * ============================================================
             */
            boolean uploadSuccess =
                    apiClient.uploadResults(
                            request
                    );

            long apiExecutionTime =
                    System.currentTimeMillis()
                            - apiStartTime;

            long totalExecutionTime =
                    System.currentTimeMillis()
                            - startTime;

            /*
             * ============================================================
             * SUCCESS
             * ============================================================
             */
            if (uploadSuccess) {

                log.info(
                        """
                                
                                ============================================================
                                [{}] RESULT UPLOAD SUCCESS
                                
                                sampleNumber={}
                                testCode={}
                                testValue={}
                                
                                uploadStatus=SUCCESS
                                
                                apiExecutionTime={}ms
                                totalExecutionTime={}ms
                                
                                ============================================================
                                """,
                        correlationId,
                        sampleNumber,
                        testCode,
                        testValue,
                        apiExecutionTime,
                        totalExecutionTime
                );

            } else {

                /*
                 * ============================================================
                 * FAILURE
                 * ============================================================
                 */
                log.error(
                        """
                                
                                ============================================================
                                [{}] RESULT UPLOAD FAILED
                                
                                sampleNumber={}
                                testCode={}
                                testValue={}
                                
                                uploadStatus=FAILED
                                
                                apiExecutionTime={}ms
                                totalExecutionTime={}ms
                                
                                POSSIBLE:
                                - API REJECTED RESULT
                                - TOKEN FAILURE
                                - NETWORK ISSUE
                                - VALIDATION FAILURE
                                
                                ============================================================
                                """,
                        correlationId,
                        sampleNumber,
                        testCode,
                        testValue,
                        apiExecutionTime,
                        totalExecutionTime
                );
            }

        } catch (Exception e) {

            long failedExecutionTime =
                    System.currentTimeMillis()
                            - startTime;

            /*
             * ============================================================
             * EXCEPTION LOG
             * ============================================================
             */
            log.error(
                    """
                            
                            ============================================================
                            [{}] RESULT PROCESSING EXCEPTION
                            
                            sampleNumber={}
                            testCode={}
                            testValue={}
                            
                            executionTime={}ms
                            
                            exceptionType={}
                            exceptionMessage={}
                            
                            ============================================================
                            """,
                    correlationId,
                    sampleNumber,
                    testCode,
                    testValue,
                    failedExecutionTime,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
        }

        /*
         * ============================================================
         * PROCESS END
         * ============================================================
         */
        log.info(
                """
                        
                        ============================================================
                        [{}] RESULT PROCESSING COMPLETED
                        ============================================================
                        """,
                correlationId
        );
    }

    /*
     * ============================================================
     * CORRELATION ID GENERATOR
     * ============================================================
     */
    private String generateCorrelationId() {

        String timestamp =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMddHHmmssSSS"
                                )
                        );

        String shortUuid =
                UUID.randomUUID()
                        .toString()
                        .substring(0, 6)
                        .toUpperCase();

        return "RESULT-"
                + timestamp
                + "-"
                + shortUuid;
    }
}