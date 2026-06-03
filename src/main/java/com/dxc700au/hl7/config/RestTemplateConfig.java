//package com.dxc700au.hl7.config;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.BufferingClientHttpRequestFactory;
//import org.springframework.http.client.ClientHttpRequestInterceptor;
//import org.springframework.http.client.SimpleClientHttpRequestFactory;
//import org.springframework.web.client.RestTemplate;
//
//import java.nio.charset.StandardCharsets;
//
//@Slf4j
//@Configuration
//@RequiredArgsConstructor
//public class RestTemplateConfig {
//
//    private static final int MAX_LOG_SIZE = 10000;
//
//    private final MachineConfigLoader configLoader;
//
//    @Bean
//    public RestTemplate restTemplate() {
//
//        log.info("========== REST TEMPLATE CONFIG START ==========");
//
//        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
//
//        factory.setConnectTimeout(configLoader.getConnectTimeout());
//
//        factory.setReadTimeout(configLoader.getReadTimeout());
//
//        RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(factory));
//
//        restTemplate.getInterceptors().add(loggingInterceptor());
//
//        log.info("========== REST TEMPLATE CONFIG SUCCESS ==========");
//
//        return restTemplate;
//    }
//
//    private ClientHttpRequestInterceptor loggingInterceptor() {
//
//        return (request, body, execution) -> {
//
//            long startTime = System.currentTimeMillis();
//
//            /*
//             * REQUEST LOG
//             */
//            log.info("========== HTTP REQUEST START ==========");
//
//            log.info("METHOD : {}", request.getMethod());
//
//            log.info("URL : {}", request.getURI());
//
//            log.info("HEADERS : {}", request.getHeaders());
//
//            if (body != null && body.length > 0) {
//
//                int requestLength = Math.min(body.length, MAX_LOG_SIZE);
//
//                String requestBody = new String(body, 0, requestLength, StandardCharsets.UTF_8);
//
//                if (body.length > MAX_LOG_SIZE) {
//
//                    requestBody += "\n...TRUNCATED";
//                }
//
//                log.info("REQUEST BODY : {}", requestBody);
//            }
//
//            /*
//             * EXECUTE
//             */
//            var response = execution.execute(request, body);
//
//            /*
//             * RESPONSE LOG
//             */
//            log.info("========== HTTP RESPONSE START ==========");
//
//            log.info("STATUS CODE : {}", response.getStatusCode());
////
////            try {
//
////                byte[] responseBytes = response.getBody().readNBytes(MAX_LOG_SIZE);
////
////                String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
////
////                if (responseBytes.length >= MAX_LOG_SIZE) {
////
////                    responseBody += "\n...TRUNCATED";
////                }
////
////                log.info("RESPONSE BODY : {}", responseBody);
//
////            } catch (Exception e) {
////
////                log.warn("FAILED TO READ RESPONSE BODY", e);
////            }
//
//            long totalTime = System.currentTimeMillis() - startTime;
//
//            log.info("HTTP EXECUTION TIME : {} ms", totalTime);
//
//            log.info("========== HTTP RESPONSE END ==========");
//
//            return response;
//        };
//    }
//}

package com.dxc700au.hl7.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private static final int MAX_LOG_SIZE = 10000;

    private final MachineConfigLoader configLoader;

    @Bean
    public RestTemplate restTemplate() {

        log.info("========== REST TEMPLATE CONFIG START ==========");

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(
                configLoader.getConnectTimeout()
        );

        factory.setReadTimeout(
                configLoader.getReadTimeout()
        );

        log.info(
                """
                
                REST TEMPLATE TIMEOUT CONFIG
                
                CONNECT_TIMEOUT={} ms
                READ_TIMEOUT={} ms
                
                """,
                configLoader.getConnectTimeout(),
                configLoader.getReadTimeout()
        );

        RestTemplate restTemplate =
                new RestTemplate(
                        new BufferingClientHttpRequestFactory(factory)
                );

        restTemplate.getInterceptors()
                .add(loggingInterceptor());

        log.info("========== REST TEMPLATE CONFIG SUCCESS ==========");

        return restTemplate;
    }

    private ClientHttpRequestInterceptor loggingInterceptor() {

        return (request, body, execution) -> {

            long startTime =
                    System.currentTimeMillis();

            try {

                log.info("========== HTTP REQUEST START ==========");

                log.info("METHOD : {}", request.getMethod());

                log.info("URL : {}", request.getURI());

                log.info("HEADERS : {}", request.getHeaders());

                if (body != null && body.length > 0) {

                    int requestLength =
                            Math.min(body.length, MAX_LOG_SIZE);

                    String requestBody =
                            new String(
                                    body,
                                    0,
                                    requestLength,
                                    StandardCharsets.UTF_8
                            );

                    if (body.length > MAX_LOG_SIZE) {

                        requestBody += "\n...TRUNCATED";
                    }

                    log.info("REQUEST BODY : {}", requestBody);
                }

                var response =
                        execution.execute(request, body);

                long totalTime =
                        System.currentTimeMillis() - startTime;

                log.info("========== HTTP RESPONSE START ==========");

                log.info("STATUS CODE : {}", response.getStatusCode());

                log.info("HTTP EXECUTION TIME : {} ms", totalTime);

                log.info("========== HTTP RESPONSE END ==========");

                return response;

            } catch (Exception ex) {

                long totalTime =
                        System.currentTimeMillis() - startTime;

                log.error(
                        """
                        
                        HTTP REQUEST FAILED
                        
                        URL={}
                        METHOD={}
                        EXECUTION_TIME={} ms
                        
                        """,
                        request.getURI(),
                        request.getMethod(),
                        totalTime,
                        ex
                );

                throw ex;
            }
        };
    }
}