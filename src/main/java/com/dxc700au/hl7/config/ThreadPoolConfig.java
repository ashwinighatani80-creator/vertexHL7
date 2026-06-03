package com.dxc700au.hl7.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ThreadPoolConfig {

    private final MachineConfigLoader configLoader;

    @Bean(destroyMethod = "shutdown")
    public ExecutorService hl7ExecutorService() {

        int poolSize =
                configLoader.getThreadPoolSize();

        log.info("========== HL7 THREAD POOL INITIALIZATION ==========");
        log.info("Configured Pool Size : {}", poolSize);

        ExecutorService executorService =
                Executors.newFixedThreadPool(
                        poolSize,
                        hl7ThreadFactory()
                );

        log.info("HL7 Thread Pool Created Successfully");

        return executorService;
    }

    private ThreadFactory hl7ThreadFactory() {

        AtomicInteger counter =
                new AtomicInteger(1);

        return runnable -> {

            Thread thread =
                    new Thread(runnable);

            thread.setName(
                    "hl7-worker-"
                            + counter.getAndIncrement()
            );

            thread.setDaemon(false);

            thread.setUncaughtExceptionHandler(
                    (t, e) -> log.error(
                            "Unhandled Exception In Thread : {}",
                            t.getName(),
                            e
                    )
            );

            return thread;
        };
    }
}