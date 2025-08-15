package com.anz.fastpayment.sender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Fast Sender Service Application
 * 
 * Handles outbound message transmission to G3 Host via CPG with delivery confirmation
 * and retry mechanisms for Singapore Fast Payment system.
 */
@SpringBootApplication
@EnableKafka
public class FastSenderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FastSenderServiceApplication.class, args);
    }
}