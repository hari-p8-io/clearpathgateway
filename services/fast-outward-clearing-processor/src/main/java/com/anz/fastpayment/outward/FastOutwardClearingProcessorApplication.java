package com.anz.fastpayment.outward;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Fast Outward Clearing Processor Application
 * 
 * Handles CTO (Credit Transfer Outward) processing with liquidity authorization
 * and payment orchestration for Singapore Fast Payment system.
 */
@SpringBootApplication
@EnableKafka
public class FastOutwardClearingProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FastOutwardClearingProcessorApplication.class, args);
    }
}