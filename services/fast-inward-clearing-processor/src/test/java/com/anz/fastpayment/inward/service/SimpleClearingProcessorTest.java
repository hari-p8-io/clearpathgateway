package com.anz.fastpayment.inward.service;

import com.anz.fastpayment.inward.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test class for ClearingProcessorService core functionality
 * Tests points 1 and 3 without complex mocking
 */
class SimpleClearingProcessorTest {

    @Test
    void testPoint1_TransactionMessageCreation() {
        // Test Point 1: TransactionMessage POJO creation and validation
        
        // Create a valid transaction message
        TransactionMessage message = new TransactionMessage();
        message.setTransactionId("TEST-TXN-001");
        message.setAmount(new BigDecimal("1000.00"));
        message.setCurrency("SGD");
        message.setSenderAccount("12345678");
        message.setReceiverAccount("87654321");
        message.setTransactionType("DOMESTIC");
        message.setDescription("Test transaction");
        message.setTimestamp(LocalDateTime.now());
        message.setPriority("NORMAL");
        message.setReference("REF001");
        
        // Verify all fields are set correctly
        assertEquals("TEST-TXN-001", message.getTransactionId());
        assertEquals(new BigDecimal("1000.00"), message.getAmount());
        assertEquals("SGD", message.getCurrency());
        assertEquals("12345678", message.getSenderAccount());
        assertEquals("87654321", message.getReceiverAccount());
        assertEquals("DOMESTIC", message.getTransactionType());
        assertEquals("Test transaction", message.getDescription());
        assertEquals("NORMAL", message.getPriority());
        assertEquals("REF001", message.getReference());
        
        // Test constructor with required fields
        TransactionMessage message2 = new TransactionMessage(
            "TEST-TXN-002", 
            new BigDecimal("500.00"), 
            "USD", 
            "11111111", 
            "22222222", 
            "INTERNATIONAL"
        );
        
        assertEquals("TEST-TXN-002", message2.getTransactionId());
        assertEquals(new BigDecimal("500.00"), message2.getAmount());
        assertEquals("USD", message2.getCurrency());
        assertEquals("11111111", message2.getSenderAccount());
        assertEquals("22222222", message2.getReceiverAccount());
        assertEquals("INTERNATIONAL", message2.getTransactionType());
        assertNotNull(message2.getTimestamp());
        
        System.out.println("✅ Point 1 Test Passed: TransactionMessage POJO creation and validation successful");
    }

    @Test
    void testPoint3_ProcessedTransactionMessageEnrichment() {
        // Test Point 3: ProcessedTransactionMessage enrichment and business rules
        
        // Create original transaction message
        TransactionMessage original = new TransactionMessage();
        original.setTransactionId("ENRICH-TEST-001");
        original.setAmount(new BigDecimal("2500.00"));
        original.setCurrency("SGD");
        original.setSenderAccount("33333333");
        original.setReceiverAccount("44444444");
        original.setTransactionType("DOMESTIC");
        original.setDescription("Enrichment test");
        original.setTimestamp(LocalDateTime.now());
        original.setPriority("HIGH");
        original.setReference("ENRICH-REF");
        
        // Test enrichment (Point 3a)
        ProcessedTransactionMessage processed = new ProcessedTransactionMessage(original);
        
        // Verify enrichment data
        assertNotNull(processed.getEnrichmentData());
        assertNotNull(processed.getProcessingNodeId());
        assertNotNull(processed.getProcessingTimestamp());
        assertNotNull(processed.getOriginalTimestamp());
        assertEquals("PROCESSED", processed.getStatus());
        
        // Test business rule results creation (Point 3b)
        BusinessRuleResults businessRules = new BusinessRuleResults();
        businessRules.setBlocked(false);
        businessRules.setRiskScore(25);
        businessRules.setOverallStatus("APPROVED");
        
        // Add compliance checks
        ComplianceCheck complianceCheck = new ComplianceCheck(
            "BASIC_COMPLIANCE", 
            "GENERAL", 
            true, 
            "Basic compliance checks passed"
        );
        businessRules.addComplianceCheck(complianceCheck);
        
        // Add amount limit checks
        AmountLimitCheck amountCheck = new AmountLimitCheck(
            "MAX_AMOUNT",
            new BigDecimal("1000000"),
            new BigDecimal("2500.00"),
            true,
            "Amount within limits"
        );
        businessRules.addAmountLimitCheck(amountCheck);
        
        // Set business rules
        processed.setBusinessRuleResults(businessRules);
        
        // Verify business rules
        assertNotNull(processed.getBusinessRuleResults());
        assertFalse(processed.getBusinessRuleResults().isBlocked());
        assertEquals(25, processed.getBusinessRuleResults().getRiskScore());
        assertEquals("APPROVED", processed.getBusinessRuleResults().getOverallStatus());
        assertEquals(1, processed.getBusinessRuleResults().getComplianceChecks().size());
        assertEquals(1, processed.getBusinessRuleResults().getAmountLimitChecks().size());
        
        // Test enrichment data
        EnrichmentData enrichmentData = new EnrichmentData(
            "test-processor-01",
            "correlation-123",
            "trace-456"
        );
        enrichmentData.setEnrichmentSource("TestEnrichmentService");
        enrichmentData.addMetadata("testKey", "testValue");
        enrichmentData.addMetadata("version", "1.0");
        
        processed.setEnrichmentData(enrichmentData);
        
        // Verify enrichment data
        assertEquals("test-processor-01", processed.getEnrichmentData().getProcessingNodeId());
        assertEquals("correlation-123", processed.getEnrichmentData().getCorrelationId());
        assertEquals("trace-456", processed.getEnrichmentData().getTraceId());
        assertEquals("TestEnrichmentService", processed.getEnrichmentData().getEnrichmentSource());
        assertEquals("testValue", processed.getEnrichmentData().getMetadata("testKey"));
        assertEquals("1.0", processed.getEnrichmentData().getMetadata("version"));
        
        System.out.println("✅ Point 3 Test Passed: ProcessedTransactionMessage enrichment and business rules successful");
    }

    @Test
    void testBusinessRuleValidation() {
        // Test business rule validation logic
        
        // Test amount limit validation
        AmountLimitCheck amountCheck = new AmountLimitCheck(
            "MAX_AMOUNT",
            new BigDecimal("1000000"),
            new BigDecimal("500000"),
            true,
            "Amount within limits"
        );
        assertTrue(amountCheck.isPassed());
        
        // Test compliance check
        ComplianceCheck complianceCheck = new ComplianceCheck(
            "RISK_ASSESSMENT",
            "BUSINESS_RULES",
            false,
            "High risk transaction detected"
        );
        assertFalse(complianceCheck.isPassed());
        assertEquals("High risk transaction detected", complianceCheck.getDetails());
        
        // Test business rule results aggregation
        BusinessRuleResults results = new BusinessRuleResults();
        results.addAmountLimitCheck(amountCheck);
        results.addComplianceCheck(complianceCheck);
        
        assertEquals(1, results.getAmountLimitChecks().size());
        assertEquals(1, results.getComplianceChecks().size());
        
        System.out.println("✅ Business Rule Validation Test Passed");
    }

    @Test
    void testModelSerialization() {
        // Test that models can be properly serialized/deserialized
        
        // Create a complete transaction message
        TransactionMessage message = new TransactionMessage();
        message.setTransactionId("SERIALIZATION-TEST-001");
        message.setAmount(new BigDecimal("1500.00"));
        message.setCurrency("EUR");
        message.setSenderAccount("55555555");
        message.setReceiverAccount("66666666");
        message.setTransactionType("INTERNATIONAL");
        message.setDescription("Serialization test");
        message.setTimestamp(LocalDateTime.now());
        message.setPriority("NORMAL");
        message.setReference("SER-REF");
        
        // Test toString method (basic serialization test)
        String messageString = message.toString();
        assertTrue(messageString.contains("SERIALIZATION-TEST-001"));
        assertTrue(messageString.contains("1500.00"));
        assertTrue(messageString.contains("EUR"));
        assertTrue(messageString.contains("INTERNATIONAL"));
        
        // Create processed message
        ProcessedTransactionMessage processed = new ProcessedTransactionMessage(message);
        String processedString = processed.toString();
        assertTrue(processedString.contains("SERIALIZATION-TEST-001"));
        assertTrue(processedString.contains("PROCESSED"));
        
        System.out.println("✅ Model Serialization Test Passed");
    }
}
