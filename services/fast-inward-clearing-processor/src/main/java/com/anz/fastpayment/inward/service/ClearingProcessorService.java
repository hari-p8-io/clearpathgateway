package com.anz.fastpayment.inward.service;

import com.anz.fastpayment.inward.model.TransactionMessage;
import com.anz.fastpayment.inward.model.ProcessedTransactionMessage;
import org.apache.avro.generic.GenericRecord;

/**
 * Clearing Processor Service
 * Main service interface for processing incoming transactions with Avro support
 */
public interface ClearingProcessorService {

    /**
     * Process a transaction message
     * 
     * @param transactionMessage The incoming transaction message
     * @return Processed transaction message
     * @throws Exception if processing fails
     */
    ProcessedTransactionMessage processTransaction(TransactionMessage transactionMessage) throws Exception;

    /**
     * Process an Avro transaction message
     * 
     * @param avroMessage The incoming Avro transaction message
     * @return Processed transaction message
     * @throws Exception if processing fails
     */
    com.anz.fastpayment.inward.avro.ProcessedTransactionMessage processAvroTransaction(GenericRecord avroMessage) throws Exception;

    /**
     * Validate transaction message
     * 
     * @param transactionMessage The transaction message to validate
     * @return true if valid, false otherwise
     */
    boolean validateTransaction(TransactionMessage transactionMessage);

    /**
     * Validate Avro transaction message
     * 
     * @param avroMessage The Avro transaction message to validate
     * @return true if valid, false otherwise
     */
    boolean validateAvroTransaction(GenericRecord avroMessage);

    /**
     * Enrich transaction message with additional metadata
     * 
     * @param transactionMessage The original transaction message
     * @return Enriched transaction message
     */
    ProcessedTransactionMessage enrichTransaction(TransactionMessage transactionMessage);

    /**
     * Enrich Avro transaction message with additional metadata
     * 
     * @param avroMessage The original Avro transaction message
     * @return Enriched Avro transaction message
     */
    com.anz.fastpayment.inward.avro.ProcessedTransactionMessage enrichAvroTransaction(GenericRecord avroMessage);

    /**
     * Apply business rules to transaction
     * 
     * @param processedMessage The processed transaction message
     * @return Updated processed message with business rule results
     */
    ProcessedTransactionMessage applyBusinessRules(ProcessedTransactionMessage processedMessage);

    /**
     * Apply business rules to Avro transaction
     * 
     * @param processedMessage The processed Avro transaction message
     * @return Updated processed Avro message with business rule results
     */
    com.anz.fastpayment.inward.avro.ProcessedTransactionMessage applyBusinessRulesToAvro(com.anz.fastpayment.inward.avro.ProcessedTransactionMessage processedMessage);

    /**
     * Check if transaction is a duplicate (idempotency check)
     * 
     * @param transactionId The transaction ID to check
     * @return true if duplicate, false otherwise
     */
    boolean isDuplicateTransaction(String transactionId);

    /**
     * Mark transaction as processed for idempotency
     * 
     * @param transactionId The transaction ID to mark
     */
    void markTransactionAsProcessed(String transactionId);
}
