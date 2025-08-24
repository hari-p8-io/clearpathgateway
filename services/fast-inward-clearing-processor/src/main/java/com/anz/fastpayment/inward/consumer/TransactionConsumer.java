package com.anz.fastpayment.inward.consumer;

import com.anz.fastpayment.inward.avro.ProcessedTransactionMessage;
import com.anz.fastpayment.inward.service.ClearingProcessorService;
import com.anz.fastpayment.inward.util.AvroConverter;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Transaction Consumer with Avro Support
 * Consumes Avro transaction messages from input topic, processes them, and produces Avro results
 */
@Component
public class TransactionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumer.class);
    private static final AtomicLong messageCounter = new AtomicLong(0);

    @Autowired
    private ClearingProcessorService clearingProcessorService;

    @Autowired
    private KafkaTemplate<String, GenericRecord> kafkaTemplate;

    @Autowired
    @Qualifier("outputTopic")
    private String outputTopic;

    @Autowired
    @Qualifier("dlqTopic")
    private String dlqTopic;

    /**
     * Kafka Listener for consuming Avro transaction messages
     * 
     * @param consumerRecord The consumed Kafka record with Avro message
     * @param acknowledgment The acknowledgment for manual commit
     */
    @KafkaListener(
        topics = "#{inputTopic}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransaction(
            @Payload ConsumerRecord<String, GenericRecord> consumerRecord,
            Acknowledgment acknowledgment) {
        
        String transactionId = consumerRecord.key();
        GenericRecord avroMessage = consumerRecord.value();
        long messageNumber = messageCounter.incrementAndGet();
        
        logger.info("Consuming Avro message #{} - Transaction: {} from partition: {}, offset: {}", 
            messageNumber, transactionId, consumerRecord.partition(), consumerRecord.offset());

        try {
            // Process the Avro transaction message
            ProcessedTransactionMessage processedMessage = clearingProcessorService.processAvroTransaction(avroMessage);
            
            // Send to output topic
            sendToOutputTopic(transactionId, processedMessage);
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
            logger.info("Avro message #{} processed successfully - Transaction: {}", messageNumber, transactionId);
            
        } catch (Exception e) {
            logger.error("Error processing Avro message #{} - Transaction: {}", messageNumber, transactionId, e);
            
            // Send to Dead Letter Queue
            sendToDeadLetterQueue(transactionId, avroMessage, e);
            
            // Acknowledge the message to prevent reprocessing
            acknowledgment.acknowledge();
        }
    }

    /**
     * Send processed Avro message to output topic
     */
    private void sendToOutputTopic(String transactionId, com.anz.fastpayment.inward.avro.ProcessedTransactionMessage processedMessage) {
        try {
            // Use the Avro message directly since it's already in Avro format
            GenericRecord avroRecord = (GenericRecord) processedMessage;
            
            ProducerRecord<String, GenericRecord> record = new ProducerRecord<>(
                outputTopic, 
                transactionId, 
                avroRecord
            );
            
            // Add headers for tracing
            // Extract values from Avro message
            String processingNodeId = processedMessage.getProcessingNodeId() != null ? processedMessage.getProcessingNodeId().toString() : "unknown";
            String status = processedMessage.getStatus() != null ? processedMessage.getStatus().toString() : "unknown";
            record.headers().add("processingNodeId", processingNodeId.getBytes());
            record.headers().add("status", status.getBytes());
            
            kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.debug("Avro message sent to output topic successfully - Transaction: {}, Partition: {}, Offset: {}", 
                            transactionId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        logger.error("Failed to send Avro message to output topic - Transaction: {}", transactionId, ex);
                    }
                });
                
        } catch (Exception e) {
            logger.error("Error sending Avro message to output topic - Transaction: {}", transactionId, e);
            throw e;
        }
    }

    /**
     * Send failed Avro message to Dead Letter Queue
     */
    private void sendToDeadLetterQueue(String transactionId, GenericRecord avroMessage, Exception error) {
        try {
            // Create a simple error message for DLQ
            com.anz.fastpayment.inward.avro.ProcessedTransactionMessage errorMessage = createAvroErrorMessage(avroMessage, error);
            
            // Use the Avro message directly
            GenericRecord avroRecord = (GenericRecord) errorMessage;
            
            ProducerRecord<String, GenericRecord> record = new ProducerRecord<>(
                dlqTopic, 
                transactionId, 
                avroRecord
            );
            
            // Add error headers
            record.headers().add("error", error.getMessage().getBytes());
            record.headers().add("errorType", error.getClass().getSimpleName().getBytes());
            
            kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Avro message sent to DLQ successfully - Transaction: {}, Partition: {}, Offset: {}", 
                            transactionId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    } else {
                        logger.error("Failed to send Avro message to DLQ - Transaction: {}", transactionId, ex);
                    }
                });
                
        } catch (Exception e) {
            logger.error("Error sending Avro message to DLQ - Transaction: {}", transactionId, e);
            // Don't throw here to avoid infinite loop
        }
    }

    /**
     * Create Avro error message for DLQ
     */
    private com.anz.fastpayment.inward.avro.ProcessedTransactionMessage createAvroErrorMessage(GenericRecord avroMessage, Exception error) {
        try {
            // Create a simple Avro error message
            com.anz.fastpayment.inward.avro.ProcessedTransactionMessage errorMessage = new com.anz.fastpayment.inward.avro.ProcessedTransactionMessage();
            errorMessage.setStatus("FAILED");
            errorMessage.setProcessingNodeId("DLQ");
            
            // Extract basic information from Avro message if possible
            if (avroMessage != null) {
                try {
                    // Extract transaction ID from Avro message
                    String extractedId = AvroConverter.extractTransactionId(avroMessage);
                    if (extractedId != null) {
                        errorMessage.setTransactionId(extractedId);
                    }
                } catch (Exception e) {
                    logger.warn("Could not extract information from Avro message for error handling", e);
                }
            }
            
            return errorMessage;
        } catch (Exception e) {
            logger.error("Error creating Avro error message", e);
            // Return a basic error message
            com.anz.fastpayment.inward.avro.ProcessedTransactionMessage basicError = new com.anz.fastpayment.inward.avro.ProcessedTransactionMessage();
            basicError.setStatus("FAILED");
            basicError.setProcessingNodeId("DLQ");
            basicError.setTransactionId("UNKNOWN");
            return basicError;
        }
    }

    /**
     * Get current message counter for monitoring
     */
    public long getMessageCounter() {
        return messageCounter.get();
    }
}
