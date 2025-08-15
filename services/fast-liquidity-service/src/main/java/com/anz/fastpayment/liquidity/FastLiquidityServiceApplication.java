package com.anz.fastpayment.liquidity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Fast Liquidity Service Application
 * 
 * Multi-region liquidity management service for APEAFAST-SG ClearPath Gateway.
 * Provides real-time balance management, authorization checks, and transaction processing
 * for Singapore G3 and Hong Kong FPS payment schemes.
 * 
 * Key Features:
 * - Real-time liquidity balance checking and authorization
 * - Multi-country payment scheme support (SG=G3, HK=FPS)
 * - ISO 20022 message processing (PACS/CAMT)
 * - Net debit cap monitoring and enforcement
 * - Transaction history and audit trails
 * 
 * @author APEAFAST-SG Team
 * @version 21.0.0-apeafast-SNAPSHOT
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableTransactionManagement
public class FastLiquidityServiceApplication {

    public static void main(String[] args) {
        // Enable Virtual Threads for improved concurrency
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(FastLiquidityServiceApplication.class, args);
    }
}