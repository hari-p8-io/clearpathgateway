package com.anz.fastpayment.inward.service.impl;

import com.anz.fastpayment.inward.model.*;
import com.anz.fastpayment.inward.service.ClearingProcessorService;
import com.anz.fastpayment.inward.util.AvroConverter;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of Clearing Processor Service
 * Handles transaction processing, validation, enrichment, and business rules
 */
@Service
public class ClearingProcessorServiceImpl implements ClearingProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(ClearingProcessorServiceImpl.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${app.business-rules.max-amount:SGD 1000000}")
    private String maxAmountStr;

    @Value("${app.business-rules.high-risk-countries:}")
    private String highRiskCountries;

    @Value("${app.idempotency.ttl-hours:24}")
    private int idempotencyTtlHours;

    @Value("${app.processing.node-id:inward-processor-01}")
    private String processingNodeId;

    @Override
    public ProcessedTransactionMessage processTransaction(TransactionMessage transactionMessage) throws Exception {
        String transactionId = transactionMessage.getTransactionId();
        logger.info("Processing transaction: {}", transactionId);

        try {
            // Check for duplicate transaction
            if (isDuplicateTransaction(transactionId)) {
                logger.warn("Duplicate transaction detected: {}", transactionId);
                throw new IllegalStateException("Duplicate transaction: " + transactionId);
            }

            // Validate transaction
            if (!validateTransaction(transactionMessage)) {
                logger.error("Transaction validation failed: {}", transactionId);
                throw new IllegalArgumentException("Transaction validation failed for: " + transactionId);
            }

            // Enrich transaction
            ProcessedTransactionMessage processedMessage = enrichTransaction(transactionMessage);

            // Apply business rules
            processedMessage = applyBusinessRules(processedMessage);

            // Mark as processed for idempotency
            markTransactionAsProcessed(transactionId);

            logger.info("Transaction processed successfully: {}", transactionId);
            return processedMessage;

        } catch (Exception e) {
            logger.error("Error processing transaction: {}", transactionId, e);
            throw e;
        }
    }

    @Override
    public com.anz.fastpayment.inward.avro.ProcessedTransactionMessage processAvroTransaction(GenericRecord avroMessage) throws Exception {
        // Convert Avro message to POJO for processing
        TransactionMessage transactionMessage = AvroConverter.convertToTransactionMessage(avroMessage);
        if (transactionMessage == null) {
            throw new IllegalArgumentException("Failed to convert Avro message to TransactionMessage");
        }
        
        // Process using existing logic
        ProcessedTransactionMessage processedPojo = processTransaction(transactionMessage);
        
        // Convert back to Avro format
        return convertToAvroProcessedMessage(processedPojo);
    }

    @Override
    public boolean validateAvroTransaction(GenericRecord avroMessage) {
        try {
            TransactionMessage transactionMessage = AvroConverter.convertToTransactionMessage(avroMessage);
            return validateTransaction(transactionMessage);
        } catch (Exception e) {
            logger.error("Error validating Avro transaction", e);
            return false;
        }
    }

    @Override
    public com.anz.fastpayment.inward.avro.ProcessedTransactionMessage enrichAvroTransaction(GenericRecord avroMessage) {
        try {
            TransactionMessage transactionMessage = AvroConverter.convertToTransactionMessage(avroMessage);
            ProcessedTransactionMessage enrichedPojo = enrichTransaction(transactionMessage);
            return convertToAvroProcessedMessage(enrichedPojo);
        } catch (Exception e) {
            logger.error("Error enriching Avro transaction", e);
            throw new RuntimeException("Failed to enrich Avro transaction", e);
        }
    }

    @Override
    public com.anz.fastpayment.inward.avro.ProcessedTransactionMessage applyBusinessRulesToAvro(com.anz.fastpayment.inward.avro.ProcessedTransactionMessage processedMessage) {
        try {
            // Convert Avro to POJO, apply business rules, then convert back
            ProcessedTransactionMessage pojoMessage = convertFromAvroProcessedMessage(processedMessage);
            ProcessedTransactionMessage updatedPojo = applyBusinessRules(pojoMessage);
            return convertToAvroProcessedMessage(updatedPojo);
        } catch (Exception e) {
            logger.error("Error applying business rules to Avro transaction", e);
            throw new RuntimeException("Failed to apply business rules to Avro transaction", e);
        }
    }

    private com.anz.fastpayment.inward.avro.ProcessedTransactionMessage convertToAvroProcessedMessage(ProcessedTransactionMessage pojoMessage) {
        try {
            com.anz.fastpayment.inward.avro.ProcessedTransactionMessage avroMessage = new com.anz.fastpayment.inward.avro.ProcessedTransactionMessage();
            
            // Set basic fields
            avroMessage.setTransactionId(pojoMessage.getTransactionId());
            avroMessage.setAmount(pojoMessage.getAmount() != null ? pojoMessage.getAmount().doubleValue() : 0.0);
            avroMessage.setCurrency(pojoMessage.getCurrency());
            avroMessage.setSenderAccount(pojoMessage.getSenderAccount());
            avroMessage.setReceiverAccount(pojoMessage.getReceiverAccount());
            avroMessage.setTransactionType(pojoMessage.getTransactionType());
            avroMessage.setPriority(pojoMessage.getPriority());
            avroMessage.setTimestamp(pojoMessage.getOriginalTimestamp() != null ? pojoMessage.getOriginalTimestamp().toString() : null);
            avroMessage.setProcessingTimestamp(pojoMessage.getProcessingTimestamp() != null ? pojoMessage.getProcessingTimestamp().toString() : null);
            avroMessage.setProcessingNodeId(pojoMessage.getProcessingNodeId());
            avroMessage.setStatus(pojoMessage.getStatus());
            avroMessage.setValidationPassed(true); // Default to true for now
            avroMessage.setBusinessRulesPassed(true); // Default to true for now
            avroMessage.setErrorMessage(null); // No error by default
            
            return avroMessage;
        } catch (Exception e) {
            logger.error("Error converting POJO to Avro message", e);
            throw new RuntimeException("Failed to convert POJO to Avro message", e);
        }
    }

    private ProcessedTransactionMessage convertFromAvroProcessedMessage(com.anz.fastpayment.inward.avro.ProcessedTransactionMessage avroMessage) {
        try {
            ProcessedTransactionMessage pojoMessage = new ProcessedTransactionMessage();
            
            // Set basic fields
            pojoMessage.setTransactionId(avroMessage.getTransactionId());
            pojoMessage.setAmount(BigDecimal.valueOf(avroMessage.getAmount()));
            pojoMessage.setCurrency(avroMessage.getCurrency());
            pojoMessage.setSenderAccount(avroMessage.getSenderAccount());
            pojoMessage.setReceiverAccount(avroMessage.getReceiverAccount());
            pojoMessage.setTransactionType(avroMessage.getTransactionType());
            pojoMessage.setPriority(avroMessage.getPriority());
            pojoMessage.setOriginalTimestamp(avroMessage.getTimestamp() != null ? LocalDateTime.parse(avroMessage.getTimestamp()) : null);
            pojoMessage.setProcessingTimestamp(avroMessage.getProcessingTimestamp() != null ? LocalDateTime.parse(avroMessage.getProcessingTimestamp()) : null);
            pojoMessage.setProcessingNodeId(avroMessage.getProcessingNodeId());
            pojoMessage.setStatus(avroMessage.getStatus());
            // Note: POJO doesn't have validationPassed, businessRulesPassed, or errorMessage fields
            
            return pojoMessage;
        } catch (Exception e) {
            logger.error("Error converting Avro to POJO message", e);
            throw new RuntimeException("Failed to convert Avro to POJO message", e);
        }
    }

    @Override
    public boolean validateTransaction(TransactionMessage transactionMessage) {
        if (transactionMessage == null) {
            logger.error("Transaction message is null");
            return false;
        }

        // Check required fields
        if (!StringUtils.hasText(transactionMessage.getTransactionId())) {
            logger.error("Transaction ID is missing");
            return false;
        }

        if (transactionMessage.getAmount() == null || transactionMessage.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Invalid amount: {}", transactionMessage.getAmount());
            return false;
        }

        if (!StringUtils.hasText(transactionMessage.getCurrency())) {
            logger.error("Currency is missing");
            return false;
        }

        if (!StringUtils.hasText(transactionMessage.getSenderAccount())) {
            logger.error("Sender account is missing");
            return false;
        }

        if (!StringUtils.hasText(transactionMessage.getReceiverAccount())) {
            logger.error("Receiver account is missing");
            return false;
        }

        if (!StringUtils.hasText(transactionMessage.getTransactionType())) {
            logger.error("Transaction type is missing");
            return false;
        }

        logger.debug("Transaction validation passed for: {}", transactionMessage.getTransactionId());
        return true;
    }

    @Override
    public ProcessedTransactionMessage enrichTransaction(TransactionMessage transactionMessage) {
        logger.debug("Enriching transaction: {}", transactionMessage.getTransactionId());

        ProcessedTransactionMessage processedMessage = new ProcessedTransactionMessage(transactionMessage);
        
        // Add processing metadata
        processedMessage.setProcessingNodeId(processingNodeId);
        


        logger.debug("Transaction enriched successfully: {}", transactionMessage.getTransactionId());
        return processedMessage;
    }

    @Override
    public ProcessedTransactionMessage applyBusinessRules(ProcessedTransactionMessage processedMessage) {
        logger.debug("Applying business rules for transaction: {}", processedMessage.getTransactionId());

        BusinessRuleResults businessRuleResults = new BusinessRuleResults();
        
        // Check amount limits
        checkAmountLimits(processedMessage, businessRuleResults);
        
        // Check compliance rules
        checkComplianceRules(processedMessage, businessRuleResults);
        
        // Calculate risk score
        calculateRiskScore(processedMessage, businessRuleResults);
        
        // Set overall status
        if (businessRuleResults.isBlocked()) {
            processedMessage.setStatus("BLOCKED");
            businessRuleResults.setOverallStatus("BLOCKED");
        } else {
            processedMessage.setStatus("APPROVED");
            businessRuleResults.setOverallStatus("APPROVED");
        }
        
        processedMessage.setBusinessRuleResults(businessRuleResults);

        logger.debug("Business rules applied for transaction: {}", processedMessage.getTransactionId());
        return processedMessage;
    }

    private void checkAmountLimits(ProcessedTransactionMessage processedMessage, BusinessRuleResults businessRuleResults) {
        BigDecimal amount = processedMessage.getAmount();
        String currency = processedMessage.getCurrency();
        
        // Parse max amount from configuration (simplified - in production use proper parsing)
        BigDecimal maxAmount = parseMaxAmount(maxAmountStr);
        
        if (amount.compareTo(maxAmount) > 0) {
            businessRuleResults.setBlocked(true);
            businessRuleResults.setBlockReason("Amount exceeds maximum limit");
            logger.warn("Transaction blocked due to amount limit: {} {} > {} {}", 
                amount, currency, maxAmount, currency);
        }
    }

    private void checkComplianceRules(ProcessedTransactionMessage processedMessage, BusinessRuleResults businessRuleResults) {
        // Check for high-risk countries (simplified example)
        if (StringUtils.hasText(highRiskCountries)) {
            String[] countries = highRiskCountries.split(",");
            for (String country : countries) {
                if (country.trim().equalsIgnoreCase("XX")) { // Example high-risk country code
                    businessRuleResults.setBlocked(true);
                    businessRuleResults.setBlockReason("High-risk country detected");
                    logger.warn("Transaction blocked due to high-risk country: {}", country);
                    break;
                }
            }
        }
    }

    private void calculateRiskScore(ProcessedTransactionMessage processedMessage, BusinessRuleResults businessRuleResults) {
        int riskScore = 0;
        
        // Amount-based risk
        BigDecimal amount = processedMessage.getAmount();
        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            riskScore += 30;
        } else if (amount.compareTo(new BigDecimal("10000")) > 0) {
            riskScore += 15;
        }
        
        // Transaction type risk
        if ("INTERNATIONAL".equalsIgnoreCase(processedMessage.getTransactionType())) {
            riskScore += 25;
        }
        
        // Priority-based risk
        if ("HIGH".equalsIgnoreCase(processedMessage.getPriority())) {
            riskScore += 20;
        }
        
        businessRuleResults.setRiskScore(riskScore);
        
        // Block if risk score is too high
        if (riskScore > 70) {
            businessRuleResults.setBlocked(true);
            businessRuleResults.setBlockReason("Risk score too high: " + riskScore);
            logger.warn("Transaction blocked due to high risk score: {}", riskScore);
        }
    }

    private BigDecimal parseMaxAmount(String maxAmountStr) {
        try {
            // Simple parsing - in production use proper currency parsing
            String amountStr = maxAmountStr.replaceAll("[^0-9.]", "");
            return new BigDecimal(amountStr);
        } catch (Exception e) {
            logger.warn("Failed to parse max amount: {}, using default", maxAmountStr);
            return new BigDecimal("1000000"); // Default 1M
        }
    }

    @Override
    public boolean isDuplicateTransaction(String transactionId) {
        String key = "transaction:processed:" + transactionId;
        Boolean hasKey = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(hasKey);
    }

    @Override
    public void markTransactionAsProcessed(String transactionId) {
        String key = "transaction:processed:" + transactionId;
        String value = LocalDateTime.now().toString();
        
        redisTemplate.opsForValue().set(key, value, idempotencyTtlHours, TimeUnit.HOURS);
        logger.debug("Transaction marked as processed: {}", transactionId);
    }
}
