package com.anz.fastpayment.inward.util;

import com.anz.fastpayment.inward.avro.UnifiedPaymentMessage;
import com.anz.fastpayment.inward.model.ProcessedTransactionMessage;
import com.anz.fastpayment.inward.model.TransactionMessage;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;

/**
 * Utility class for converting between Avro SpecificRecord and POJO objects
 */
public class AvroConverter {

    private static final Logger logger = LoggerFactory.getLogger(AvroConverter.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Convert Avro UnifiedPaymentMessage to TransactionMessage POJO
     * 
     * @param avroRecord The Avro UnifiedPaymentMessage
     * @return TransactionMessage POJO
     */
    public static TransactionMessage convertToTransactionMessage(GenericRecord avroRecord) {
        if (avroRecord == null) {
            return null;
        }

        try {
            // Cast to the specific Avro class
            UnifiedPaymentMessage unifiedMessage = (UnifiedPaymentMessage) avroRecord;
            
            TransactionMessage transactionMessage = new TransactionMessage();
            
            // Extract all fields from the Avro message
            transactionMessage.setTransactionId(unifiedMessage.getTransactionId());
            transactionMessage.setAmount(BigDecimal.valueOf(unifiedMessage.getAmount()));
            transactionMessage.setCurrency(unifiedMessage.getCurrency());
            transactionMessage.setSenderAccount(unifiedMessage.getSenderAccount());
            transactionMessage.setReceiverAccount(unifiedMessage.getReceiverAccount());
            transactionMessage.setTransactionType(unifiedMessage.getTransactionType());
            transactionMessage.setPriority(unifiedMessage.getPriority());
            
            // Parse timestamp string to LocalDateTime
            if (unifiedMessage.getTimestamp() != null) {
                try {
                    LocalDateTime timestamp = LocalDateTime.parse(unifiedMessage.getTimestamp(), TIMESTAMP_FORMATTER);
                    transactionMessage.setTimestamp(timestamp);
                } catch (Exception e) {
                    logger.warn("Could not parse timestamp: {}", unifiedMessage.getTimestamp());
                    transactionMessage.setTimestamp(LocalDateTime.now());
                }
            }
            
            logger.debug("Successfully converted Avro UnifiedPaymentMessage to TransactionMessage: {}", 
                       transactionMessage.getTransactionId());
            
            return transactionMessage;
            
        } catch (Exception e) {
            logger.error("Error converting Avro record to TransactionMessage", e);
            throw new RuntimeException("Failed to convert Avro record to TransactionMessage", e);
        }
    }

    /**
     * Convert ProcessedTransactionMessage POJO to Avro ProcessedTransactionMessage
     * 
     * @param processedMessage The ProcessedTransactionMessage POJO
     * @return Avro ProcessedTransactionMessage
     */
    public static GenericRecord convertToAvroRecord(ProcessedTransactionMessage processedMessage) {
        if (processedMessage == null) {
            return null;
        }

        try {
            // Create Avro ProcessedTransactionMessage using the generated class
            com.anz.fastpayment.inward.avro.ProcessedTransactionMessage avroMessage = new com.anz.fastpayment.inward.avro.ProcessedTransactionMessage();
            
            // Set all fields from the POJO
            avroMessage.setTransactionId(processedMessage.getTransactionId());
            avroMessage.setAmount(processedMessage.getAmount() != null ? processedMessage.getAmount().doubleValue() : 0.0);
            avroMessage.setCurrency(processedMessage.getCurrency());
            avroMessage.setSenderAccount(processedMessage.getSenderAccount());
            avroMessage.setReceiverAccount(processedMessage.getReceiverAccount());
            avroMessage.setTransactionType(processedMessage.getTransactionType());
            avroMessage.setPriority(processedMessage.getPriority());
            
            // Handle timestamps
            if (processedMessage.getOriginalTimestamp() != null) {
                avroMessage.setTimestamp(processedMessage.getOriginalTimestamp().format(TIMESTAMP_FORMATTER));
            }
            if (processedMessage.getProcessingTimestamp() != null) {
                avroMessage.setProcessingTimestamp(processedMessage.getProcessingTimestamp().format(TIMESTAMP_FORMATTER));
            }
            
            avroMessage.setProcessingNodeId(processedMessage.getProcessingNodeId());
            avroMessage.setStatus(processedMessage.getStatus());
            
            // Set validation and business rules results
            if (processedMessage.getBusinessRuleResults() != null) {
                // Use overallStatus to determine if validation and business rules passed
                String overallStatus = processedMessage.getBusinessRuleResults().getOverallStatus();
                avroMessage.setValidationPassed("PASSED".equals(overallStatus));
                avroMessage.setBusinessRulesPassed("PASSED".equals(overallStatus));
            } else {
                avroMessage.setValidationPassed(false);
                avroMessage.setBusinessRulesPassed(false);
            }
            
            // Set error message if status indicates failure
            if ("FAILED".equals(processedMessage.getStatus())) {
                avroMessage.setErrorMessage("Processing failed");
            }
            
            logger.debug("Successfully converted ProcessedTransactionMessage POJO to Avro: {}", 
                       processedMessage.getTransactionId());
            
            return avroMessage;
            
        } catch (Exception e) {
            logger.error("Error converting ProcessedTransactionMessage to Avro record", e);
            throw new RuntimeException("Failed to convert ProcessedTransactionMessage to Avro record", e);
        }
    }

    /**
     * Create a new Avro UnifiedPaymentMessage from individual fields
     * 
     * @param transactionId Unique transaction identifier
     * @param amount Transaction amount
     * @param currency Currency code
     * @param senderAccount Sender account number
     * @param receiverAccount Receiver account number
     * @param transactionType Type of transaction
     * @param priority Transaction priority
     * @param timestamp Transaction timestamp
     * @param componentName Component name
     * @param uuid Unique identifier
     * @param channel Channel identifier
     * @param direction Direction (I for Inward, O for Outward)
     * @param domainName Domain name
     * @return Avro UnifiedPaymentMessage
     */
    public static UnifiedPaymentMessage createUnifiedPaymentMessage(
            String transactionId, double amount, String currency, String senderAccount,
            String receiverAccount, String transactionType, String priority, String timestamp,
            String componentName, String uuid, String channel, String direction, String domainName) {
        
        try {
            UnifiedPaymentMessage message = new UnifiedPaymentMessage();
            message.setTransactionId(transactionId);
            message.setAmount(amount);
            message.setCurrency(currency);
            message.setSenderAccount(senderAccount);
            message.setReceiverAccount(receiverAccount);
            message.setTransactionType(transactionType);
            message.setPriority(priority);
            message.setTimestamp(timestamp);
            message.setComponentName(componentName);
            message.setUuid(uuid);
            message.setChannel(channel);
            message.setDirection(direction);
            message.setDomainName(domainName);
            
            logger.debug("Created new Avro UnifiedPaymentMessage: {}", transactionId);
            return message;
            
        } catch (Exception e) {
            logger.error("Error creating UnifiedPaymentMessage", e);
            throw new RuntimeException("Failed to create UnifiedPaymentMessage", e);
        }
    }

    /**
     * Create a new Avro ProcessedTransactionMessage from individual fields
     * 
     * @param transactionId Unique transaction identifier
     * @param amount Transaction amount
     * @param currency Currency code
     * @param senderAccount Sender account number
     * @param receiverAccount Receiver account number
     * @param transactionType Type of transaction
     * @param priority Transaction priority
     * @param timestamp Original transaction timestamp
     * @param processingTimestamp When the transaction was processed
     * @param processingNodeId ID of the processing node
     * @param status Processing status
     * @param validationPassed Whether validation passed
     * @param businessRulesPassed Whether business rules passed
     * @param errorMessage Error message if processing failed
     * @return Avro ProcessedTransactionMessage
     */
    public static com.anz.fastpayment.inward.avro.ProcessedTransactionMessage createProcessedTransactionMessage(
            String transactionId, double amount, String currency, String senderAccount,
            String receiverAccount, String transactionType, String priority, String timestamp,
            String processingTimestamp, String processingNodeId, String status,
            boolean validationPassed, boolean businessRulesPassed, String errorMessage) {
        
        try {
            com.anz.fastpayment.inward.avro.ProcessedTransactionMessage message = new com.anz.fastpayment.inward.avro.ProcessedTransactionMessage();
            message.setTransactionId(transactionId);
            message.setAmount(amount);
            message.setCurrency(currency);
            message.setSenderAccount(senderAccount);
            message.setReceiverAccount(receiverAccount);
            message.setTransactionType(transactionType);
            message.setPriority(priority);
            message.setTimestamp(timestamp);
            message.setProcessingTimestamp(processingTimestamp);
            message.setProcessingNodeId(processingNodeId);
            message.setStatus(status);
            message.setValidationPassed(validationPassed);
            message.setBusinessRulesPassed(businessRulesPassed);
            message.setErrorMessage(errorMessage);
            
            logger.debug("Created new Avro ProcessedTransactionMessage: {}", transactionId);
            return message;
            
        } catch (Exception e) {
            logger.error("Error creating ProcessedTransactionMessage", e);
            throw new RuntimeException("Failed to create ProcessedTransactionMessage", e);
        }
    }

    /**
     * Extract transaction ID from Avro message
     * 
     * @param avroRecord The Avro GenericRecord
     * @return Transaction ID as string
     */
    public static String extractTransactionId(GenericRecord avroRecord) {
        if (avroRecord == null) {
            return null;
        }

        try {
            if (avroRecord instanceof UnifiedPaymentMessage) {
                UnifiedPaymentMessage message = (UnifiedPaymentMessage) avroRecord;
                return message.getTransactionId();
            } else if (avroRecord instanceof com.anz.fastpayment.inward.avro.ProcessedTransactionMessage) {
                com.anz.fastpayment.inward.avro.ProcessedTransactionMessage message = (com.anz.fastpayment.inward.avro.ProcessedTransactionMessage) avroRecord;
                return message.getTransactionId();
            } else {
                // Fallback to generic record access
                Object transactionId = avroRecord.get("transactionId");
                return transactionId != null ? transactionId.toString() : null;
            }
        } catch (Exception e) {
            logger.warn("Could not extract transaction ID from Avro message", e);
            return null;
        }
    }

    /**
     * Extract amount from Avro message
     * 
     * @param avroRecord The Avro GenericRecord
     * @return Amount as double
     */
    public static Double extractAmount(GenericRecord avroRecord) {
        if (avroRecord == null) {
            return null;
        }

        try {
            if (avroRecord instanceof UnifiedPaymentMessage) {
                UnifiedPaymentMessage message = (UnifiedPaymentMessage) avroRecord;
                return message.getAmount();
            } else if (avroRecord instanceof com.anz.fastpayment.inward.avro.ProcessedTransactionMessage) {
                com.anz.fastpayment.inward.avro.ProcessedTransactionMessage message = (com.anz.fastpayment.inward.avro.ProcessedTransactionMessage) avroRecord;
                return message.getAmount();
            } else {
                // Fallback to generic record access
                Object amount = avroRecord.get("amount");
                if (amount instanceof Number) {
                    return ((Number) amount).doubleValue();
                }
                return null;
            }
        } catch (Exception e) {
            logger.warn("Could not extract amount from Avro message", e);
            return null;
        }
    }

    /**
     * Extract currency from Avro message
     * 
     * @param avroRecord The Avro GenericRecord
     * @return Currency as string
     */
    public static String extractCurrency(GenericRecord avroRecord) {
        if (avroRecord == null) {
            return null;
        }

        try {
            if (avroRecord instanceof UnifiedPaymentMessage) {
                UnifiedPaymentMessage message = (UnifiedPaymentMessage) avroRecord;
                return message.getCurrency();
            } else if (avroRecord instanceof com.anz.fastpayment.inward.avro.ProcessedTransactionMessage) {
                com.anz.fastpayment.inward.avro.ProcessedTransactionMessage message = (com.anz.fastpayment.inward.avro.ProcessedTransactionMessage) avroRecord;
                return message.getCurrency();
            } else {
                // Fallback to generic record access
                Object currency = avroRecord.get("currency");
                return currency != null ? currency.toString() : null;
            }
        } catch (Exception e) {
            logger.warn("Could not extract currency from Avro message", e);
            return null;
        }
    }

    /**
     * Validate that an Avro record conforms to the expected schema
     * 
     * @param avroRecord The Avro GenericRecord to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidAvroRecord(GenericRecord avroRecord) {
        if (avroRecord == null) {
            return false;
        }

        try {
            // Check if it's one of our expected types
            if (avroRecord instanceof UnifiedPaymentMessage || avroRecord instanceof com.anz.fastpayment.inward.avro.ProcessedTransactionMessage) {
                // Extract required fields to validate
                String transactionId = extractTransactionId(avroRecord);
                Double amount = extractAmount(avroRecord);
                String currency = extractCurrency(avroRecord);
                
                // Basic validation - transaction ID and amount are required
                return transactionId != null && !transactionId.trim().isEmpty() 
                       && amount != null && amount > 0 
                       && currency != null && !currency.trim().isEmpty();
            }
            
            return false;
        } catch (Exception e) {
            logger.warn("Error validating Avro record", e);
            return false;
        }
    }

    /**
     * Create a sample UnifiedPaymentMessage for testing purposes
     * 
     * @return Sample UnifiedPaymentMessage
     */
    public static UnifiedPaymentMessage createSampleUnifiedPaymentMessage() {
        return createUnifiedPaymentMessage(
            "TEST-TXN-" + System.currentTimeMillis(),
            1000.00,
            "SGD",
            "1234567890",
            "0987654321",
            "CTI",
            "HIGH",
            Instant.now().toString(),
            "PSPAPFAFAST",
            "UUID-" + System.currentTimeMillis(),
            "G3I",
            "I",
            "PAYMENTS"
        );
    }

    /**
     * Create a sample ProcessedTransactionMessage for testing purposes
     * 
     * @return Sample ProcessedTransactionMessage
     */
    public static com.anz.fastpayment.inward.avro.ProcessedTransactionMessage createSampleProcessedTransactionMessage() {
        return createProcessedTransactionMessage(
            "TEST-TXN-" + System.currentTimeMillis(),
            1000.00,
            "SGD",
            "1234567890",
            "0987654321",
            "CTI",
            "HIGH",
            Instant.now().toString(),
            Instant.now().toString(),
            "NODE-001",
            "SUCCESS",
            true,
            true,
            null
        );
    }
}
