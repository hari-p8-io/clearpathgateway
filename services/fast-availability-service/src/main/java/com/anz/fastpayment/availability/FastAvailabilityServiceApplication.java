package com.anz.fastpayment.availability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Fast Availability Service Application
 * 
 * Manages participant bank status tracking and availability notifications
 * for Singapore Fast Payment system.
 */
@SpringBootApplication
@EnableKafka
public class FastAvailabilityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FastAvailabilityServiceApplication.class, args);
    }
}