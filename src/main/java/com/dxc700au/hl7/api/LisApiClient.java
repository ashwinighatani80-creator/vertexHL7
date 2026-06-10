package com.dxc700au.hl7.api;
import com.dxc700au.hl7.config.MachineConfigLoader;
import com.dxc700au.hl7.dto.ResultUploadRequest;
import com.dxc700au.hl7.dto.SampleResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LisApiClient {

    private final RestTemplate restTemplate;

    private final MachineConfigLoader configLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
     * ============================================================
     * SAMPLE FETCH
     * ============================================================
     *
     * IMPORTANT:
     * IHMS RETURNS MULTIPLE RECORDS FOR SAME BARCODE
     * SO WE MERGE ALL TESTS INTO SINGLE SAMPLE
     *
     * ============================================================
     */
    public SampleResponse getSampleByBarcode(String barcode) {

        log.info("========== SAMPLE FETCH START ==========");

        log.info("REQUESTED BARCODE={}", barcode);

        try {

            /*
             * ============================================================
             * VALIDATION
             * ============================================================
             */
            if (barcode == null || barcode.isBlank()) {

                log.error("BARCODE IS NULL OR EMPTY");

                return null;
            }

            String cleanBarcode = barcode.trim();

            /*
             * ============================================================
             * URL
             * ============================================================
             */
            String url = configLoader.getAppServer() + configLoader.getWorklistEndpoint() + "?token=" + configLoader.getToken();

            /*
             * ============================================================
             * DATE RANGE
             * ============================================================
             */
            LocalDateTime now = LocalDateTime.now();


            LocalDateTime from = now.minusDays(2);


            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

            /*
             * ============================================================
             * REQUEST BODY
             * ============================================================
             */
            Map<String, Object> request = new HashMap<>();

            request.put("machineID", configLoader.getMachineId());

            request.put("unitID", configLoader.getUnitId());

            request.put("fromDate", from.format(formatter));

            request.put("toDate", now.format(formatter));

            /*
             * ============================================================
             * HEADERS
             * ============================================================
             */
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("CALLING WORKLIST API");

            log.info("URL={}", url);

            /*
             * ============================================================
             * API CALL
             * ============================================================
             */
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            log.info("WORKLIST API STATUS={}", response.getStatusCode());

            String body = response.getBody();

            if (body == null || body.isBlank()) {

                log.warn("EMPTY API RESPONSE");

                return null;
            }

            /*
             * ============================================================
             * JSON PARSE
             * ============================================================
             */
            List<Map<String, Object>> records = objectMapper.readValue(body, new TypeReference<>() {
            });

            log.info("TOTAL RECORDS RECEIVED={}", records.size());

            /*
             * ============================================================
             * FILTER BARCODE
             * ============================================================
             */
            List<Map<String, Object>> matchedRecords = records.stream().filter(record -> {

                Object apiBarcode = record.get("barCode");

                if (apiBarcode == null) {

                    return false;
                }

                return cleanBarcode.equalsIgnoreCase(String.valueOf(apiBarcode).trim());
            }).collect(Collectors.toList());

            log.info("MATCHED RECORDS={}", matchedRecords.size());

            /*
             * ============================================================
             * NO MATCH
             * ============================================================
             */
            if (matchedRecords.isEmpty()) {

                log.warn("NO SAMPLE FOUND barcode={}", cleanBarcode);

                return null;
            }

            /*
             * ============================================================
             * MERGE
             * ============================================================
             */
            SampleResponse merged = buildMergedSample(matchedRecords);

            log.info("""
                    
                    SAMPLE MERGED SUCCESSFULLY
                    barcode={}
                    sampleNumber={}
                    patientId={}
                    totalTests={}
                    """, merged.getBarcode(), merged.getSampleNumber(), merged.getPatientId(), merged.getParamCodes() == null ? 0 : merged.getParamCodes().size());

            log.info("========== SAMPLE FETCH END ==========");

            return merged;

        } catch (HttpStatusCodeException e) {

            log.error("""
                    
                    LIS API HTTP ERROR
                    status={}
                    body={}
                    """, e.getStatusCode(), e.getResponseBodyAsString());

        } catch (Exception e) {

            log.error("SAMPLE FETCH FAILED", e);
        }

        return null;
    }

    /*
     * ============================================================
     * FETCH FULL WORKLIST (RAW)
     * ============================================================
     */
    public List<Map<String, Object>> getWorklist() {

        log.info("========== WORKLIST FETCH START ==========");

        try {

            String url = configLoader.getAppServer() + configLoader.getWorklistEndpoint() + "?token=" + configLoader.getToken();

            LocalDateTime now = LocalDateTime.now();

            LocalDateTime from = now.minusDays(2);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

            Map<String, Object> request = new HashMap<>();

            request.put("machineID", configLoader.getMachineId());

            request.put("unitID", configLoader.getUnitId());

            request.put("fromDate", from.format(formatter));

            request.put("toDate", now.format(formatter));

            log.info("WORKLIST REQUEST BODY={}", request);

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            log.info("WORKLIST API STATUS={}", response.getStatusCode());

            String body = response.getBody();

            if (body == null || body.isBlank()) {

                log.warn("EMPTY WORKLIST RESPONSE");

                return List.of();
            }

            List<Map<String, Object>> records = objectMapper.readValue(body, new TypeReference<>() {});

            log.info("TOTAL WORKLIST RECORDS={}", records.size());

            return records;

        } catch (HttpStatusCodeException e) {

            log.error("WORKLIST API HTTP ERROR status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());

        } catch (Exception e) {

            log.error("WORKLIST FETCH FAILED", e);
        }

        return List.of();
    }

    /*
     * ============================================================
     * RESULT UPLOAD
     * ============================================================
     */
    public boolean uploadResults(ResultUploadRequest request) {

        log.info("========== RESULT UPLOAD START ==========");

        try {

            /*
             * ============================================================
             * VALIDATION
             * ============================================================
             */
            if (request == null) {

                log.error("RESULT REQUEST IS NULL");

                return false;
            }

            /*
             * ============================================================
             * URL
             * ============================================================
             */
            String url = configLoader.getAppServer() + configLoader.getResultUploadEndpoint() + "?token=" + configLoader.getToken();

            /*
             * ============================================================
             * HEADERS
             * ============================================================
             */
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<ResultUploadRequest> entity = new HttpEntity<>(request, headers);

            /*
             * ============================================================
             * LOG REQUEST
             * ============================================================
             */
            log.info("""
                    
                    RESULT UPLOAD REQUEST
                    sampleNumber={}
                    testCode={}
                    testValue={}
                    """, request.getSampleNumber(), request.getTestIdentifier(), request.getTestVal());
            log.info(
                    "FULL REQUEST JSON = {}",
                    objectMapper.writeValueAsString(request)
            );

            /*
             * ============================================================
             * API CALL
             * ============================================================
             */
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            /*
             * ============================================================
             * RESPONSE
             * ============================================================
             */
            log.info("""
                    
                    RESULT UPLOAD SUCCESS
                    status={}
                    """, response.getStatusCode());

            if (response.getBody() != null) {

                log.debug("UPLOAD RESPONSE : {}", response.getBody());
            }

            return response.getStatusCode().is2xxSuccessful();

        } catch (HttpStatusCodeException e) {

            log.error("""
                    
                    RESULT API HTTP ERROR
                    status={}
                    response={}
                    """, e.getStatusCode(), e.getResponseBodyAsString());

        } catch (Exception e) {

            log.error("RESULT UPLOAD FAILED", e);
        }

        return false;
    }

    /*
     * ============================================================
     * MERGE RECORDS
     * ============================================================
     */
    private SampleResponse buildMergedSample(List<Map<String, Object>> records) {

        Map<String, Object> first = records.get(0);

        SampleResponse sample = new SampleResponse();

        /*
         * ============================================================
         * BASIC FIELDS
         * ============================================================
         */
        sample.setPatientId(getString(first, "patientid"));

        sample.setPatientName(getString(first, "firstName"));

        sample.setDob(getString(first, "dob"));

        sample.setGender(getString(first, "genderid"));

        sample.setMedicalRecordNumber(getString(first, "mrNo"));

        sample.setBarcode(getString(first, "barCode"));

        sample.setSampleNumber(getString(first, "sampleNumber"));

        sample.setPriority(getString(first, "priority"));

        /*
         * ============================================================
         * MERGE TESTS
         * ============================================================
         */
        Set<String> uniqueTests = new LinkedHashSet<>();

        for (Map<String, Object> record : records) {

            Object paramCodesObj = record.get("paramCode");

            if (paramCodesObj instanceof List<?> list) {

                for (Object value : list) {

                    if (value != null) {

                        String testCode = value.toString().trim();

                        if (!testCode.isBlank()) {

                            uniqueTests.add(testCode);
                        }
                    }
                }
            }
        }

        sample.setParamCodes(new ArrayList<>(uniqueTests));

        log.info("MERGED UNIQUE TEST COUNT={}", uniqueTests.size());

        return sample;
    }

    /*
     * ============================================================
     * SAFE STRING
     * ============================================================
     */
    private String getString(Map<String, Object> map, String key) {

        Object value = map.get(key);

        if (value == null) {

            return null;
        }

        return String.valueOf(value).trim();
    }
}