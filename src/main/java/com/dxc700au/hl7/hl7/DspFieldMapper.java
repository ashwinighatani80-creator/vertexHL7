package com.dxc700au.hl7.hl7;

import ca.uhn.hl7v2.model.v231.message.DSR_Q03;
import ca.uhn.hl7v2.model.v231.segment.DSP;

import com.dxc700au.hl7.dto.SampleResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DspFieldMapper {

    /*
     * ============================================================
     * MAIN DSP MAPPING
     * ============================================================
     */
    public int mapFields(
            DSR_Q03 dsr,
            SampleResponse response
    ) throws Exception {

        log.info(
                """

                ============================================================
                DSP FIELD MAPPING START

                barcode={}
                patientId={}
                patientName={}
                totalTests={}

                ============================================================
                """,
                response.getBarcode(),
                response.getPatientId(),
                response.getPatientName(),
                response.getParamCodes() == null
                        ? 0
                        : response.getParamCodes().size()
        );

        int index = 0;

        /*
         * ============================================================
         * DSP|1 -> PATIENT ID
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                response.getPatientId()
        );

        /*
         * ============================================================
         * DSP|2 -> WARD
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                ""
        );

        /*
         * ============================================================
         * DSP|3 -> PATIENT NAME
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                response.getPatientName()
        );

        /*
         * ============================================================
         * DSP|4 -> DOB
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                response.getDob()
        );

        /*
         * ============================================================
         * DSP|5 -> GENDER
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                mapGender(
                        response.getGender()
                )
        );

        /*
         * ============================================================
         * DSP|6 -> DOCTOR
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                ""
        );

        /*
         * ============================================================
         * DSP|7 -> DEPARTMENT
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                ""
        );

        /*
         * ============================================================
         * DSP|8 -> BED
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                ""
        );

        /*
         * ============================================================
         * DSP|9 -> SAMPLE TYPE
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                "SERUM"
        );

        /*
         * ============================================================
         * DSP|10 -> PRIORITY
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                response.getPriority()
        );

        /*
         * ============================================================
         * DSP|11-20 -> TESTS
         * ============================================================
         */
        for (int i = 0; i < 10; i++) {

            String testCode = "";

            if (response.getParamCodes() != null
                    && response.getParamCodes().size() > i) {

                testCode =
                        response.getParamCodes()
                                .get(i);

                log.info(
                        "TEST MAPPED -> DSP|{} = {}",
                        index + 1,
                        testCode
                );
            }

            addDsp(
                    dsr,
                    index++,
                    testCode
            );
        }

        /*
         * ============================================================
         * DSP|21 -> BARCODE
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                response.getBarcode()
        );

        /*
         * ============================================================
         * DSP|22 -> DETECTION MODE
         * ============================================================
         */
        addDsp(
                dsr,
                index++,
                "0"
        );

        log.info(
                """

                ============================================================
                DSP FIELD MAPPING COMPLETE

                barcode={}
                totalDspSegments={}
                tests={}

                ============================================================
                """,
                response.getBarcode(),
                index,
                response.getParamCodes()
        );

        return index;
    }

    /*
     * ============================================================
     * ADD DSP
     * ============================================================
     */
    private void addDsp(
            DSR_Q03 dsr,
            int index,
            String value
    ) throws Exception {

        DSP dsp =
                dsr.getDSP(index);

        dsp.getSetIDDSP()
                .setValue(
                        String.valueOf(index + 1)
                );

        dsp.getDisplayLevel()
                .setValue("1");

        dsp.getDataLine()
                .setValue(
                        value == null
                                ? ""
                                : value
                );

        dsp.getLogicalBreakPoint()
                .setValue("Y");

        log.debug(
                "DSP CREATED | setId={} | value={}",
                index + 1,
                value
        );
    }

    /*
     * ============================================================
     * GENDER MAP
     * ============================================================
     */
    private String mapGender(
            String gender
    ) {

        if (gender == null
                || gender.isBlank()) {

            return "0";
        }

        gender =
                gender.trim()
                        .toUpperCase();

        return switch (gender) {

            case "M", "MALE" -> "1";

            case "F", "FEMALE" -> "2";

            default -> "0";
        };
    }
}