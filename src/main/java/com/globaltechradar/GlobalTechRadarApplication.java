package com.globaltechradar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GlobalTechRadarApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlobalTechRadarApplication.class, args);
    }
}
