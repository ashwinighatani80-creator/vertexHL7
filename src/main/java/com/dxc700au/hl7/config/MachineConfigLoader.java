package com.dxc700au.hl7.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileInputStream;

import java.io.InputStream;

@Slf4j
@Getter
@Component
public class MachineConfigLoader {

    @Value("${machine.config.path}")
    private String configPath;

    private JsonNode rootNode;

    private String appServer;

    private String sampleFetchEndpoint;

    private String worklistEndpoint;

    private String resultUploadEndpoint;

    private String token;


    @PostConstruct
    public void loadConfig() {

        try {

            log.info("========== LOADING MACHINE CONFIG ==========");

            InputStream is;

            File file = new File(configPath);

            if (file.exists()) {

                log.info("Loading config from external file: {}", file.getAbsolutePath());

                is = new FileInputStream(file);

            } else {

                log.info("Loading config from classpath: {}", configPath);

                is = new ClassPathResource(configPath).getInputStream();
            }
            ObjectMapper mapper =
                    new ObjectMapper();

            rootNode =
                    mapper.readTree(is);

            validateRootNode();

            JsonNode lisApi =
                    rootNode.path("lisApi");

            appServer =
                    sanitize(
                            rootNode.path("appServer")
                                    .asText()
                    );

            sampleFetchEndpoint =
                    sanitize(
                            lisApi.path("sampleFetchEndpoint")
                                    .asText()
                    );

            worklistEndpoint =
                    sanitize(
                            lisApi.path("worklistEndpoint")
                                    .asText()
                    );

            resultUploadEndpoint =
                    sanitize(
                            lisApi.path("resultUploadEndpoint")
                                    .asText()
                    );

            token =
                    sanitize(
                            lisApi.path("token")
                                    .asText()
                    );

            validateMandatoryFields();

            log.info("========== CONFIG LOADED SUCCESSFULLY ==========");
            log.info("APP SERVER : {}", appServer);
            log.info("SAMPLE FETCH ENDPOINT : {}", sampleFetchEndpoint);
            log.info("RESULT UPLOAD ENDPOINT : {}", resultUploadEndpoint);
            log.info("WORKLIST ENDPOINT : {}", worklistEndpoint);

        } catch (Exception e) {

            log.error(
                    "CONFIG LOAD FAILED",
                    e
            );

            throw new RuntimeException(
                    "FAILED TO LOAD MACHINE CONFIG",
                    e
            );
        }
    }

    private void validateRootNode() {

        if (rootNode == null) {

            throw new RuntimeException(
                    "CONFIG ROOT NODE IS NULL"
            );
        }
    }

    private void validateMandatoryFields() {

        if (isBlank(appServer)) {

            throw new RuntimeException(
                    "appServer MISSING IN CONFIG"
            );
        }

        if (isBlank(sampleFetchEndpoint)) {

            throw new RuntimeException(
                    "sampleFetchEndpoint MISSING IN CONFIG"
            );
        }

        if (isBlank(resultUploadEndpoint)) {

            throw new RuntimeException(
                    "resultUploadEndpoint MISSING IN CONFIG"
            );
        }
    }

    public JsonNode getFirstDevice() {

        JsonNode devices =
                rootNode.path("loadDevice");

        if (devices.isArray()
                && devices.size() > 0) {

            return devices.get(0);
        }

        throw new RuntimeException(
                "NO DEVICE FOUND IN loadDevice"
        );
    }

    // ================= API CONFIG =================

    public String getAppServerUrl() {
        return appServer;
    }

    public String getSampleApiEndpoint() {
        return sampleFetchEndpoint;
    }

    public String getAppServer() {
        return appServer;
    }

    public String getResultUploadEndpoint() {
        return resultUploadEndpoint;
    }

    public String getToken() {
        return token;
    }

    public String getWorklistEndpoint() {
        return worklistEndpoint;
    }

    // ================= TCP CONFIG =================

    public int getTcpPort() {

        return getFirstDevice()
                .path("TCPIP_params")
                .path("tcpPort")
                .asInt(51142);
    }

    public int getSocketTimeout() {

        return getFirstDevice()
                .path("TCPIP_params")
                .path("socketTimeout")
                .asInt(600000);
    }

    // ================= API TIMEOUT =================

    public int getConnectTimeout() {

        return rootNode.path("lisApi")
                .path("connectTimeout")
                .asInt(1000000);
    }

    public int getReadTimeout() {

        return rootNode.path("lisApi")
                .path("readTimeout")
                .asInt(1200000);
    }

    public int getThreadPoolSize() {

        return rootNode.path("lisApi")
                .path("threadPoolSize")
                .asInt(20);
    }
    public Long getMachineId() {

        return getFirstDevice()
                .path("HIS_deviceID")
                .asLong();
    }

    public Long getUnitId() {

        return getFirstDevice()
                .path("HIS_unitID")
                .asLong();
    }

    public String getEquipmentName() {

        return getFirstDevice()
                .path("deviceName")
                .asText("AH600");
    }

    // ================= COMMON =================

    private String sanitize(
            String value
    ) {

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.trim().isEmpty();
    }
}