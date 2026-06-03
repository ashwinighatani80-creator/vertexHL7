package com.dxc700au.hl7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableRetry
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class Hl7Application {

    public static void main(String[] args) {

        SpringApplication.run(
                Hl7Application.class,
                args
        );
    }
}