package com.dxc700au.hl7.tcp;

import com.dxc700au.hl7.constants.MllpConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class MllpFrameWriter {

    public void writeMessage(
            OutputStream outputStream,
            String hl7Message,
            String sessionId
    ) throws Exception {

        if (hl7Message == null || hl7Message.isBlank()) {

            throw new RuntimeException(
                    "HL7 MESSAGE EMPTY"
            );
        }

        log.info(
                """
                ============================================================
                [{}] SENDING HL7 MLLP FRAME
                ============================================================
                """,
                sessionId
        );

        outputStream.write(
                MllpConstants.START_BLOCK
        );

        outputStream.write(
                hl7Message.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        outputStream.write(
                MllpConstants.END_BLOCK
        );

        outputStream.write(
                MllpConstants.CARRIAGE_RETURN
        );

        outputStream.flush();

        log.info(
                """
                ============================================================
                [{}] HL7 MLLP FRAME SENT SUCCESSFULLY
                messageLength={}
                ============================================================
                """,
                sessionId,
                hl7Message.length()
        );
    }
}