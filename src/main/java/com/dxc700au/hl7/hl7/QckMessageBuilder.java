package com.dxc700au.hl7.hl7;
import ca.uhn.hl7v2.model.v231.message.QCK_Q02;
import ca.uhn.hl7v2.model.v231.segment.ERR;
import ca.uhn.hl7v2.model.v231.segment.MSA;
import ca.uhn.hl7v2.model.v231.segment.QAK;
import ca.uhn.hl7v2.parser.PipeParser;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QckMessageBuilder {

    private final PipeParser parser =
            new PipeParser();

    public String build(
            String controlId,
            boolean found
    ) {

        try {

            QCK_Q02 qck =
                    new QCK_Q02();

            qck.initQuickstart(
                    "QCK",
                    "Q02",
                    "P"
            );

            /*
             * ============================================================
             * UTF-8
             * ============================================================
             */
            qck.getMSH()
                    .getCharacterSet(0)
                    .setValue("UNICODE UTF-8");

            /*
             * ============================================================
             * VERSION
             * ============================================================
             */
            qck.getMSH()
                    .getVersionID()
                    .getVersionID()
                    .setValue("2.3.1");

            /*
             * ============================================================
             * MSA
             * ============================================================
             */
            MSA msa =
                    qck.getMSA();

            msa.getAcknowledgementCode()
                    .setValue("AA");

            msa.getMessageControlID()
                    .setValue(controlId);

            msa.getTextMessage()
                    .setValue(
                            found
                                    ? "SAMPLE FOUND"
                                    : "SAMPLE NOT FOUND"
                    );

            /*
             * ============================================================
             * QAK
             * ============================================================
             */
            QAK qak =
                    qck.getQAK();

            qak.getQueryTag()
                    .setValue(controlId);

            qak.getQueryResponseStatus()
                    .setValue(
                            found
                                    ? "OK"
                                    : "NF"
                    );

            /*
             * ============================================================
             * ERR SEGMENT
             * ============================================================
             */
            ERR err =
                    qck.getERR();

            err.getErrorCodeAndLocation(0)
                    .getCodeIdentifyingError()
                    .getIdentifier()
                    .setValue("0");

            String encodedMessage =
                    parser.encode(qck);

            log.info(
                    """
                    
                    ============================================================
                    QCK MESSAGE GENERATED
                    
                    controlId={}
                    found={}
                    
                    ============================================================
                    """,
                    controlId,
                    found
            );

            return encodedMessage;

        } catch (Exception e) {

            log.error(
                    "QCK BUILD FAILED",
                    e
            );

            throw new RuntimeException(
                    "FAILED TO BUILD QCK",
                    e
            );
        }
    }
}

