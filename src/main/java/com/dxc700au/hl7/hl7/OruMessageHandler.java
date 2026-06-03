package com.dxc700au.hl7.hl7;
import ca.uhn.hl7v2.model.v231.message.ORU_R01;
import ca.uhn.hl7v2.model.v231.segment.MSH;
import ca.uhn.hl7v2.util.Terser;

import com.dxc700au.hl7.dto.ResultUploadRequest;
import com.dxc700au.hl7.util.ResultValueParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OruMessageHandler {

    private final AckMessageBuilder ackBuilder;

    private final AsyncResultProcessor asyncResultProcessor;

    private final SequenceValidator sequenceValidator;

    public String handle(
            ORU_R01 oru
    ) throws Exception {

        long startTime =
                System.currentTimeMillis();

        String flowId =
                "ORU-"
                        + LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMddHHmmssSSS"
                                )
                        )
                        + "-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(0, 6)
                        .toUpperCase();

        log.info(
                """
                        
                        ============================================================
                        [{}] ORU PROCESSING STARTED
                        ============================================================
                        """,
                flowId
        );

        try {

            Terser terser =
                    new Terser(oru);

            /*
             * ============================================================
             * MSH DETAILS
             * ============================================================
             */
            MSH msh =
                    oru.getMSH();

            String controlId =
                    sanitize(
                            msh.getMessageControlID()
                                    .getValue()
                    );

            String sendingApplication =
                    sanitize(
                            msh.getSendingApplication()
                                    .getNamespaceID()
                                    .getValue()
                    );

            String sendingFacility =
                    sanitize(
                            msh.getSendingFacility()
                                    .getNamespaceID()
                                    .getValue()
                    );

            String messageTimestamp =
                    sanitize(
                            msh.getDateTimeOfMessage()
                                    .getTimeOfAnEvent()
                                    .getValue()
                    );

            /*
             * ============================================================
             * QC / PATIENT RESULT
             * ============================================================
             */
            String resultType =
                    sanitize(
                            msh.getApplicationAcknowledgmentType()
                                    .getValue()
                    );

            boolean qualityControl =
                    "1".equals(resultType);

            log.info(
                    """
                            
                            [{}] ORU MESSAGE DETAILS
                            
                            controlId={}
                            sendingApplication={}
                            sendingFacility={}
                            messageTimestamp={}
                            qualityControl={}
                            
                            """,
                    flowId,
                    controlId,
                    sendingApplication,
                    sendingFacility,
                    messageTimestamp,
                    qualityControl
            );

            /*
             * ============================================================
             * SEQUENCE VALIDATION
             * ============================================================
             */
            sequenceValidator.validateSequence(
                    flowId,
                    controlId,
                    "ORU"
            );

            /*
             * ============================================================
             * SAMPLE NUMBER
             * ============================================================
             */
            String sampleNumber =
                    sanitize(
                            terser.get("/.OBR-2-1")
                    );

            if (sampleNumber.isBlank()) {

                log.error(
                        """
                                
                                [{}] SAMPLE NUMBER MISSING
                                
                                OBR-2 EMPTY
                                
                                """,
                        flowId
                );

                return ackBuilder.buildApplicationRejectAck(
                        oru,
                        "Sample Number Missing"
                );
            }

            log.info(
                    """
                            
                            [{}] SAMPLE DETAILS
                            
                            sampleNumber={}
                            
                            """,
                    flowId,
                    sampleNumber
            );

            /*
             * ============================================================
             * PROCESS OBX
             * ============================================================
             */
            int processedResults = 0;

            int obxIndex = 0;

            while (true) {

                String testCode;

                try {

                    testCode =
                            sanitize(
                                    terser.get(
                                            "/.OBX("
                                                    + obxIndex +
                                                    ")-3-1"
                                    )
                            );

                } catch (Exception e) {

                    log.info(
                            "[{}] NO MORE OBX SEGMENTS FOUND | totalProcessed={}",
                            flowId,
                            processedResults
                    );

                    break;
                }

                /*
                 * ============================================================
                 * NO MORE OBX
                 * ============================================================
                 */
                if (testCode.isBlank()) {

                    break;
                }

                String valueType =
                        sanitize(
                                terser.get(
                                        "/.OBX("
                                                + obxIndex +
                                                ")-2"
                                )
                        );

                /*
                 * ============================================================
                 * SKIP CHROMATOGRAM / BINARY DATA
                 * ============================================================
                 */
                if ("ED".equalsIgnoreCase(valueType)) {

                    log.info(
                            """
                                    
                                    [{}] CHROMATOGRAM OBX SKIPPED
                                    
                                    testCode={}
                                    
                                    """,
                            flowId,
                            testCode
                    );

                    obxIndex++;

                    continue;
                }

                String rawValue =
                        sanitize(
                                terser.get(
                                        "/.OBX("
                                                + obxIndex +
                                                ")-5"
                                )
                        );

                String unit =
                        sanitize(
                                terser.get(
                                        "/.OBX("
                                                + obxIndex +
                                                ")-6-1"
                                )
                        );

                String abnormalFlag =
                        sanitize(
                                terser.get(
                                        "/.OBX("
                                                + obxIndex +
                                                ")-8"
                                )
                        );

                String resultStatus =
                        sanitize(
                                terser.get(
                                        "/.OBX("
                                                + obxIndex +
                                                ")-11"
                                )
                        );

                String resultDate =
                        sanitize(
                                terser.get(
                                        "/.OBX("
                                                + obxIndex +
                                                ")-14"
                                )
                        );

                log.info(
                        """
                                
                                [{}] RESULT PARSED
                                
                                obxIndex={}
                                testCode={}
                                valueType={}
                                rawValue={}
                                unit={}
                                abnormalFlag={}
                                resultStatus={}
                                resultDate={}
                                
                                """,
                        flowId,
                        obxIndex,
                        testCode,
                        valueType,
                        rawValue,
                        unit,
                        abnormalFlag,
                        resultStatus,
                        resultDate
                );

                /*
                 * ============================================================
                 * BUILD RESULT REQUEST
                 * ============================================================
                 */
                ResultUploadRequest request =
                        new ResultUploadRequest();

                request.setSampleNumber(
                        sampleNumber
                );

                request.setTestIdentifier(
                        testCode
                );

                request.setUnit(
                        unit
                );

                request.setLowHigh(
                        abnormalFlag
                );

                request.setResultDate(
                        resultDate
                );

                request.setResultstatus(
                        resultStatus
                );

                request.setResultabnormalflags(
                        abnormalFlag
                );

                /*
                 * ============================================================
                 * RESULT VALUE
                 * ============================================================
                 */
                request.setTestVal(
                        ResultValueParser.parse(rawValue)
                );

                /*
                 * ============================================================
                 * ASYNC PROCESS
                 * ============================================================
                 */
                asyncResultProcessor.process(
                        request
                );

                processedResults++;

                obxIndex++;
            }

            /*
             * ============================================================
             * SUCCESS ACK
             * ============================================================
             */
            String ack =
                    ackBuilder.buildApplicationAcceptAck(
                            oru
                    );

            long totalTime =
                    System.currentTimeMillis()
                            - startTime;

            log.info(
                    """
                            
                            ============================================================
                            [{}] ORU PROCESSING COMPLETED SUCCESSFULLY
                            
                            sampleNumber={}
                            processedResults={}
                            executionTime={}ms
                            
                            ============================================================
                            """,
                    flowId,
                    sampleNumber,
                    processedResults,
                    totalTime
            );

            return ack;

        } catch (Exception e) {

            long totalTime =
                    System.currentTimeMillis()
                            - startTime;

            log.error(
                    """
                            
                            ============================================================
                            [{}] ORU PROCESSING FAILED
                            
                            executionTime={}ms
                            
                            exceptionType={}
                            exceptionMessage={}
                            
                            ============================================================
                            """,
                    flowId,
                    totalTime,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );

            throw e;
        }
    }

    private String sanitize(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value.trim();
    }
}

