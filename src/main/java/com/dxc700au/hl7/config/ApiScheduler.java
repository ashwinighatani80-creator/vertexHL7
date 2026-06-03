package com.dxc700au.hl7.config;

import com.dxc700au.hl7.hl7.Hl7Parser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiScheduler {

    private final RestTemplate restTemplate;

    private final MachineConfigLoader config;
    private final Hl7Parser hl7Parser;

    @Scheduled(fixedDelay = 180000)
    public void fetchWorklist() {

        log.info("========== WORKLIST FETCH START ==========");

        try {

            var device = config.getFirstDevice();

            Long machineId = device.path("HIS_deviceID").asLong();

            Long unitId = device.path("HIS_unitID").asLong();

            LocalDateTime now = LocalDateTime.now();

            LocalDateTime from = now.minusDays(2);

            // FIXED FORMAT
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

            Map<String, Object> request = new HashMap<>();

            request.put("machineID", machineId);

            request.put("unitID", unitId);

            request.put("fromDate", from.format(formatter));

            request.put("toDate", now.format(formatter));

            String url = config.getAppServer() + config.getWorklistEndpoint() + "?token=" + config.getToken();

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("URL : {}", url);

            log.info("REQUEST : {}", request);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);


            log.info(
                    "WORKLIST RESPONSE SIZE={} bytes",
                    response.getBody() == null
                            ? 0
                            : response.getBody().length()
            );
            log.info("WORKLIST FETCH SUCCESS");

            log.info("STATUS : {}", response.getStatusCode());

//            log.info("BODY : {}", response.getBody());
            log.info("Records received________________________");

        } catch (HttpStatusCodeException e) {

            log.error("WORKLIST API ERROR");

            log.error("STATUS : {}", e.getStatusCode());

            log.error("BODY : {}", e.getResponseBodyAsString());

        } catch (Exception e) {

            log.error("WORKLIST FETCH FAILED", e);
        }

        log.info("========== WORKLIST FETCH END ==========");
    }
}