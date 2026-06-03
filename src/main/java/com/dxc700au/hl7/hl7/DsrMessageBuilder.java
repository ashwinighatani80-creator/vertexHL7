package com.dxc700au.hl7.hl7;
import ca.uhn.hl7v2.model.v231.message.DSR_Q03;
import ca.uhn.hl7v2.model.v231.segment.DSC;
import ca.uhn.hl7v2.model.v231.segment.MSA;
import ca.uhn.hl7v2.model.v231.segment.QAK;
import ca.uhn.hl7v2.model.v231.segment.QRD;
import ca.uhn.hl7v2.model.v231.segment.QRF;
import ca.uhn.hl7v2.parser.PipeParser;

import com.dxc700au.hl7.dto.SampleResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DsrMessageBuilder {

    /*
     * ============================================================
     * DEPENDENCIES
     * ============================================================
     */
    private final DspFieldMapper dspFieldMapper;

    private final PipeParser parser =
            new PipeParser();

    /*
     * ============================================================
     * CONSTRUCTOR
     * ============================================================
     */
    public DsrMessageBuilder(
            DspFieldMapper dspFieldMapper
    ) {

        this.dspFieldMapper =
                dspFieldMapper;
    }

    /*
     * ============================================================
     * BUILD DSR MESSAGE
     * ============================================================
     */
    public String build(
            String controlId,
            SampleResponse response,
            QRD inboundQrd,
            QRF inboundQrf
    ) throws Exception {

        log.info(
                """
                        
                        =====================================================
                        DSR BUILD START
                        
                        controlId={}
                        barcode={}
                        
                        =====================================================
                        """,
                controlId,
                response.getBarcode()
        );

        /*
         * ============================================================
         * CREATE DSR
         * ============================================================
         */
        DSR_Q03 dsr =
                new DSR_Q03();

        dsr.initQuickstart(
                "DSR",
                "Q03",
                "P"
        );

        /*
         * ============================================================
         * MSH
         * ============================================================
         */
        dsr.getMSH()
                .getCharacterSet(0)
                .setValue("UNICODE UTF-8");

        dsr.getMSH()
                .getVersionID()
                .getVersionID()
                .setValue("2.3.1");

        dsr.getMSH()
                .getMessageControlID()
                .setValue(controlId);

        /*
         * MSH-15
         */
        dsr.getMSH()
                .getAcceptAcknowledgmentType()
                .setValue("AL");

        /*
         * MSH-16
         */
        dsr.getMSH()
                .getApplicationAcknowledgmentType()
                .setValue("AL");

        /*
         * ============================================================
         * MSA
         * ============================================================
         */
        MSA msa =
                dsr.getMSA();

        msa.getAcknowledgementCode()
                .setValue("AA");

        msa.getMessageControlID()
                .setValue(controlId);

        msa.getTextMessage()
                .setValue("MESSAGE ACCEPTED");

        /*
         * ============================================================
         * QAK
         * ============================================================
         */
        QAK qak =
                dsr.getQAK();

        qak.getQueryTag()
                .setValue(controlId);

        qak.getQueryResponseStatus()
                .setValue("OK");

        /*
         * ============================================================
         * QRD ECHO
         * ============================================================
         */
        if (inboundQrd != null) {

            QRD qrd =
                    dsr.getQRD();

            if (inboundQrd.getQueryID()
                    .getValue() != null) {

                qrd.getQueryID()
                        .setValue(
                                inboundQrd.getQueryID()
                                        .getValue()
                        );
            }
        }

        /*
         * ============================================================
         * QRF ECHO
         * ============================================================
         */
        if (inboundQrf != null) {

            QRF qrf =
                    dsr.getQRF();

            if (inboundQrf.getWhereSubjectFilterReps() > 0) {

                qrf.getWhereSubjectFilter(0)
                        .setValue(
                                inboundQrf.getWhereSubjectFilter(0)
                                        .getValue()
                        );
            }
        }

        /*
         * ============================================================
         * DSP FIELD MAPPING
         * ============================================================
         */
        dspFieldMapper.mapFields(
                dsr,
                response
        );

        /*
         * ============================================================
         * DSC
         * ============================================================
         */
        DSC dsc =
                dsr.getDSC();

        dsc.getContinuationPointer()
                .setValue("0");

        /*
         * ============================================================
         * ENCODE
         * ============================================================
         */
        String encoded =
                parser.encode(dsr);

        log.debug(
                "FINAL DSR : \n{}",
                encoded.replace("\r", "\n")
        );

        log.info(
                """
                        
                        =====================================================
                        DSR BUILD SUCCESS
                        
                        controlId={}
                        
                        =====================================================
                        """,
                controlId
        );

        return encoded;
    }
}

