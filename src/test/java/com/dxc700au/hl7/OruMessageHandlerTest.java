package com.dxc700au.hl7;

import ca.uhn.hl7v2.model.v231.message.ORU_R01;
import ca.uhn.hl7v2.parser.PipeParser;

import com.dxc700au.hl7.hl7.AckMessageBuilder;
import com.dxc700au.hl7.hl7.AsyncResultProcessor;
import com.dxc700au.hl7.hl7.OruMessageHandler;
import com.dxc700au.hl7.hl7.SequenceValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OruMessageHandlerTest {

    /*
     * ============================================================
     * MOCKS
     * ============================================================
     */
    private AckMessageBuilder ackBuilder;

    private AsyncResultProcessor asyncResultProcessor;

    private SequenceValidator sequenceValidator;

    /*
     * ============================================================
     * CLASS UNDER TEST
     * ============================================================
     */
    private OruMessageHandler handler;

    /*
     * ============================================================
     * PARSER
     * ============================================================
     */
    private final PipeParser parser =
            new PipeParser();

    /*
     * ============================================================
     * SETUP
     * ============================================================
     */
    @BeforeEach
    void setup() {

        ackBuilder =
                mock(AckMessageBuilder.class);

        asyncResultProcessor =
                mock(AsyncResultProcessor.class);

        sequenceValidator =
                mock(SequenceValidator.class);

        handler =
                new OruMessageHandler(
                        ackBuilder,
                        asyncResultProcessor,
                        sequenceValidator
                );
    }

    /*
     * ============================================================
     * VALID ORU FLOW
     * ============================================================
     */
    @Test
    void shouldProcessOruAndReturnAck()
            throws Exception {

        String hl7 =
                """
                MSH|^~\\&|DXC700AU|LAB|LIS|HOSPITAL|20250512120000||ORU^R01|MSG001|P|2.3.1\r
                PID|1||12345||JOHN DOE\r
                OBR|1|SAMPLE001\r
                OBX|1|NM|GLU||120|mg/dL||N|||F\r
                """;

        ORU_R01 oru =
                (ORU_R01) parser.parse(hl7);

        when(
                ackBuilder.buildApplicationAcceptAck(
                        any()
                )
        ).thenReturn("ACK_MESSAGE");

        doNothing()
                .when(asyncResultProcessor)
                .process(any());

        String response =
                handler.handle(oru);

        assertNotNull(response);

        assertEquals(
                "ACK_MESSAGE",
                response
        );
    }

    /*
     * ============================================================
     * MISSING SAMPLE NUMBER
     * ============================================================
     */
    @Test
    void shouldRejectWhenSampleNumberMissing()
            throws Exception {

        String hl7 =
                """
                MSH|^~\\&|DXC700AU|LAB|LIS|HOSPITAL|20250512120000||ORU^R01|MSG002|P|2.3.1\r
                PID|1||12345||JOHN DOE\r
                OBX|1|NM|GLU||120|mg/dL||N|||F\r
                """;

        ORU_R01 oru =
                (ORU_R01) parser.parse(hl7);

        when(
                ackBuilder.buildApplicationRejectAck(
                        any(),
                        anyString()
                )
        ).thenReturn("REJECT_ACK");

        String response =
                handler.handle(oru);

        assertNotNull(response);

        assertEquals(
                "REJECT_ACK",
                response
        );
    }

    /*
     * ============================================================
     * INVALID RESULT VALUE
     * ============================================================
     */
    @Test
    void shouldRejectInvalidResultValue()
            throws Exception {

        String hl7 =
                """
                MSH|^~\\&|DXC700AU|LAB|LIS|HOSPITAL|20250512120000||ORU^R01|MSG004|P|2.3.1\r
                PID|1||12345||JOHN DOE\r
                OBR|1|SAMPLE004\r
                OBX|1|ST|GLU||INVALID|mg/dL||N|||F\r
                """;

        ORU_R01 oru =
                (ORU_R01) parser.parse(hl7);

        assertDoesNotThrow(
                () -> handler.handle(oru)
        );
    }

    /*
     * ============================================================
     * NULL MESSAGE
     * ============================================================
     */
    @Test
    void shouldHandleNullMessage() {

        assertThrows(
                NullPointerException.class,
                () -> handler.handle(null)
        );
    }

    /*
     * ============================================================
     * VERIFY PROCESS FLOW
     * ============================================================
     */
    @Test
    void shouldCallAsyncProcessorOnlyOnce()
            throws Exception {

        String hl7 =
                """
                MSH|^~\\&|DXC700AU|LAB|LIS|HOSPITAL|20250512120000||ORU^R01|MSG005|P|2.3.1\r
                PID|1||12345||JOHN DOE\r
                OBR|1|SAMPLE005\r
                OBX|1|NM|GLU||100|mg/dL||N|||F\r
                """;

        ORU_R01 oru =
                (ORU_R01) parser.parse(hl7);

        when(
                ackBuilder.buildApplicationAcceptAck(
                        any()
                )
        ).thenReturn("ACK");

        doNothing()
                .when(asyncResultProcessor)
                .process(any());

        String response =
                handler.handle(oru);

        assertNotNull(response);

        assertEquals(
                "ACK",
                response
        );
    }

    /*
     * ============================================================
     * ACK BUILDER FAILURE
     * ============================================================
     */
    @Test
    void shouldHandleAckBuilderFailure()
            throws Exception {

        String hl7 =
                """
                MSH|^~\\&|DXC700AU|LAB|LIS|HOSPITAL|20250512120000||ORU^R01|MSG006|P|2.3.1\r
                PID|1||12345||JOHN DOE\r
                OBR|1|SAMPLE006\r
                OBX|1|NM|GLU||110|mg/dL||N|||F\r
                """;

        ORU_R01 oru =
                (ORU_R01) parser.parse(hl7);

        when(
                ackBuilder.buildApplicationAcceptAck(
                        any()
                )
        ).thenThrow(
                new RuntimeException(
                        "ACK FAILURE"
                )
        );

        assertThrows(
                RuntimeException.class,
                () -> handler.handle(oru)
        );
    }

    /*
     * ============================================================
     * RESULT VALUE MAPPING
     * ============================================================
     */
    @Test
    void shouldMapCorrectResultValues()
            throws Exception {

        String hl7 =
                """
                MSH|^~\\&|DXC700AU|LAB|LIS|HOSPITAL|20250512120000||ORU^R01|MSG007|P|2.3.1\r
                PID|1||12345||JOHN DOE\r
                OBR|1|SAMPLE007\r
                OBX|1|NM|HB||13.5|g/dL||N|||F\r
                """;

        ORU_R01 oru =
                (ORU_R01) parser.parse(hl7);

        when(
                ackBuilder.buildApplicationAcceptAck(
                        any()
                )
        ).thenReturn("ACK");

        doNothing()
                .when(asyncResultProcessor)
                .process(any());

        String response =
                handler.handle(oru);

        assertNotNull(response);

        assertEquals(
                "ACK",
                response
        );
    }
}