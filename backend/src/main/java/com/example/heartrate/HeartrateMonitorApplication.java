package com.example.heartrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HeartrateMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeartrateMonitorApplication.class, args);
    }
}
