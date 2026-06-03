package com.dxc700au.hl7.hl7;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v231.message.ACK;
import ca.uhn.hl7v2.model.v231.message.ORU_R01;
import ca.uhn.hl7v2.model.v231.message.QRY_Q02;
import ca.uhn.hl7v2.model.v231.segment.MSH;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Hl7MessageRouter {

    /*
     * ============================================================
     * DEPENDENCIES
     * ============================================================
     */
    private final QryMessageHandler qryHandler;

    private final OruMessageHandler oruHandler;

    /*
     * ============================================================
     * ROUTE MESSAGE
     * ============================================================
     */
    public Object route(
            Message message
    ) throws Exception {

        long startTime =
                System.currentTimeMillis();

        validateMessage(message);

        String messageType =
                extractMessageType(message);

        String controlId =
                extractControlId(message);

        log.info(
                """

                ============================================================
                HL7 ROUTING STARTED

                messageType={}
                controlId={}

                ============================================================
                """,
                messageType,
                controlId
        );

        try {

            Object response;

            /*
             * ============================================================
             * QRY_Q02
             * ============================================================
             */
            if (message instanceof QRY_Q02 qry) {

                log.info(
                        """

                        ============================================================
                        QRY^Q02 RECEIVED FROM ANALYZER

                        controlId={}

                        ============================================================
                        """,
                        controlId
                );

                response =
                        qryHandler.handle(qry);
            }

            /*
             * ============================================================
             * ORU_R01
             * ============================================================
             */
            else if (message instanceof ORU_R01 oru) {

                log.info(
                        """

                        ============================================================
                        ORU^R01 RESULT RECEIVED

                        controlId={}

                        ============================================================
                        """,
                        controlId
                );

                response =
                        oruHandler.handle(oru);
            }

            /*
             * ============================================================
             * ANALYZER ACK
             * ============================================================
             */
            else if (message instanceof ACK ack) {

                log.info(
                        """

                        ============================================================
                        ANALYZER ACK RECEIVED

                        controlId={}
                        messageType=ACK

                        ============================================================
                        """,
                        controlId
                );

                return null;
            }

            /*
             * ============================================================
             * UNSUPPORTED
             * ============================================================
             */
            else {

                log.error(
                        """

                        ============================================================
                        UNSUPPORTED HL7 MESSAGE

                        messageType={}
                        controlId={}

                        ============================================================
                        """,
                        messageType,
                        controlId
                );

                throw new RuntimeException(
                        "UNSUPPORTED HL7 MESSAGE : "
                                + messageType
                );
            }

            long totalTime =
                    System.currentTimeMillis()
                            - startTime;

            log.info(
                    """

                    ============================================================
                    HL7 ROUTING COMPLETED

                    messageType={}
                    controlId={}
                    executionTime={}ms

                    ============================================================
                    """,
                    messageType,
                    controlId,
                    totalTime
            );

            return response;

        } catch (Exception e) {

            log.error(
                    """

                    ============================================================
                    HL7 ROUTING FAILED

                    messageType={}
                    controlId={}

                    ============================================================
                    """,
                    messageType,
                    controlId,
                    e
            );

            throw e;
        }
    }

    /*
     * ============================================================
     * VALIDATE MESSAGE
     * ============================================================
     */
    private void validateMessage(
            Message message
    ) {

        if (message == null) {

            throw new RuntimeException(
                    "HL7 MESSAGE CANNOT BE NULL"
            );
        }
    }

    /*
     * ============================================================
     * EXTRACT MESSAGE TYPE
     * ============================================================
     */
    private String extractMessageType(
            Message message
    ) {

        try {

            MSH msh =
                    (MSH) message.get("MSH");

            return msh.getMessageType()
                    .getMessageType()
                    .getValue();

        } catch (Exception e) {

            return "UNKNOWN";
        }
    }

    /*
     * ============================================================
     * EXTRACT CONTROL ID
     * ============================================================
     */
    private String extractControlId(
            Message message
    ) {

        try {

            MSH msh =
                    (MSH) message.get("MSH");

            return msh.getMessageControlID()
                    .getValue();

        } catch (Exception e) {

            return "UNKNOWN";
        }
    }
}