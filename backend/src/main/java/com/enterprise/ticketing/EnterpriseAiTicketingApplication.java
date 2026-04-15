package com.enterprise.ticketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EnterpriseAiTicketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseAiTicketingApplication.class, args);
    }
}

