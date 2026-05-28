package com.relayflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RelayFlowApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RelayFlowApiApplication.class, args);
    }
}
