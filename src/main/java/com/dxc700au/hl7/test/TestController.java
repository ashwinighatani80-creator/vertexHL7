package com.dxc700au.hl7.test;

import com.dxc700au.hl7.api.LisApiClient;
import com.dxc700au.hl7.hl7.DsrMessageBuilder;
import com.dxc700au.hl7.hl7.Hl7Parser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final Hl7Parser hl7Parser;
    private final LisApiClient lisApiClient;
    private final DsrMessageBuilder dsrMessageBuilder;

    @GetMapping
    public Map<String, Object> checkParser() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // Step 1: fetch real worklist from LIS API
            List<Map<String, Object>> records = lisApiClient.getWorklist();
            result.put("totalRecords", records.size());
            result.put("rawApiResponse", records);

            if (records.isEmpty()) {
                result.put("status", "NO_DATA");
                return result;
            }

            // Step 2: take first record's barcode and fetch merged sample
            String barcode = String.valueOf(records.get(0).get("barCode"));
            result.put("testedBarcode", barcode);

            var sample = lisApiClient.getSampleByBarcode(barcode);
            if (sample == null) {
                result.put("status", "SAMPLE_NOT_FOUND");
                return result;
            }

            // Step 3: build HL7 DSR from real sample
            String rawHl7 = dsrMessageBuilder.build("TEST-CTRL-001", sample, null, null);
            result.put("builtHl7", rawHl7);

            // Step 4: parse the built HL7
            var parsed = hl7Parser.parse(rawHl7);
            result.put("status", "SUCCESS");
            result.put("parsedMessageType", parsed.getName());
            result.put("parsedVersion", parsed.getVersion());

        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }
        return result;
    }
}
