package com.dxc700au.hl7.hl7;

import ca.uhn.hl7v2.model.v231.message.QRY_Q02;
import ca.uhn.hl7v2.model.v231.segment.MSH;
import ca.uhn.hl7v2.model.v231.segment.QRD;
import ca.uhn.hl7v2.model.v231.segment.QRF;

import com.dxc700au.hl7.cache.WorklistCache;
import com.dxc700au.hl7.dto.SampleResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QryMessageHandler {

    private final WorklistCache worklistCache;

    private final QckMessageBuilder qckBuilder;

    private final DsrMessageBuilder dsrBuilder;

    public QueryResponse handle(
            QRY_Q02 qry
    ) throws Exception {

        log.info(
                "========== QRY PROCESSING START =========="
        );

        String controlId =
                extractControlId(qry);

        QRD qrd =
                qry.getQRD();

        QRF qrf = null;

        try {

            qrf = qry.getQRF();

        } catch (Exception ignored) {
        }

        String barcode =
                extractBarcode(qrd);
        log.info("""
        
        ==================================================
        QRY REQUEST RECEIVED
        
        controlId={}
        barcode={}
        
        cacheContains={}
        
        ==================================================
        """,
                controlId,
                barcode,
                worklistCache.contains(barcode)
        );
        log.info(
                """
        
                ============================================================
                QRY^Q02 RECEIVED FROM ANALYZER
        
                controlId={}
                barcode={}
                queryFormat=QRY^Q02
        
                ============================================================
                """,
                controlId,
                barcode
        );

        log.info(
                "BARCODE={}",
                barcode
        );

        /*
         * ============================================================
         * CACHE FETCH
         * ============================================================
         */
        SampleResponse response =
                worklistCache.get(barcode);

        /*
         * ============================================================
         * SAMPLE NOT FOUND
         * ============================================================
         */
        if (response == null) {

            String qck =
                    qckBuilder.build(
                            controlId,
                            false
                    );

            return new QueryResponse(
                    qck,
                    null
            );
        }

        /*
         * ============================================================
         * SEND QCK
         * ============================================================
         */
        String qck =
                qckBuilder.build(
                        controlId,
                        true
                );

        /*
         * ============================================================
         * SEND DSR
         * ============================================================
         */
        String dsr =
                dsrBuilder.build(
                        controlId,
                        response,
                        qrd,
                        qrf
                );

        log.info(
                "========== QRY PROCESSING END =========="
        );

        return new QueryResponse(
                qck,
                dsr
        );
    }

    private String extractControlId(
            QRY_Q02 qry
    ) {

        MSH msh =
                qry.getMSH();

        return msh.getMessageControlID()
                .getValue();
    }

    private String extractBarcode(
            QRD qrd
    ) throws Exception {

        return qrd
                .getWhoSubjectFilter(0)
                .getIDNumber()
                .getValue();
    }
}