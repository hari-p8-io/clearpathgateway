package com.anz.fastpayment.inward.util;

import com.anz.fastpayment.inward.avro.UnifiedPaymentMessage;
import com.anz.fastpayment.inward.model.TransactionMessage;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Test class for AvroConverter utility
 */
public class AvroConverterTest {

    private GenericRecord mockAvroRecord;
    private UnifiedPaymentMessage specificRecord;

    @BeforeEach
    void setUp() {
        // Create a mock GenericRecord using the actual UnifiedPaymentMessage schema
        mockAvroRecord = new GenericData.Record(UnifiedPaymentMessage.SCHEMA$);
        
        // Set the required fields according to the schema
        mockAvroRecord.put("transactionId", "TEST-TXN-123");
        mockAvroRecord.put("amount", 100.50);
        mockAvroRecord.put("currency", "SGD");
        mockAvroRecord.put("senderAccount", "1234567890");
        mockAvroRecord.put("receiverAccount", "0987654321");
        mockAvroRecord.put("transactionType", "CTI");
        mockAvroRecord.put("priority", "HIGH");
        mockAvroRecord.put("timestamp", Instant.parse("2023-12-21T10:30:56.789Z"));
        mockAvroRecord.put("componentName", "PSPAPFAFAST");
        mockAvroRecord.put("uuid", "TEST-UUID-123");
        mockAvroRecord.put("channel", "G3I");
        mockAvroRecord.put("direction", "I");
        mockAvroRecord.put("domainName", "PAYMENTS");

        // Create a specific record for comparison testing
        specificRecord = new UnifiedPaymentMessage();
        specificRecord.setTransactionId("TEST-TXN-123");
        specificRecord.setAmount(100.50);
        specificRecord.setCurrency("SGD");
        specificRecord.setSenderAccount("1234567890");
        specificRecord.setReceiverAccount("0987654321");
        specificRecord.setTransactionType("CTI");
        specificRecord.setPriority("HIGH");
        specificRecord.setTimestamp(Instant.parse("2023-12-21T10:30:56.789Z"));
        specificRecord.setComponentName("PSPAPFAFAST");
        specificRecord.setUuid("TEST-UUID-123");
        specificRecord.setChannel("G3I");
        specificRecord.setDirection("I");
        specificRecord.setDomainName("PAYMENTS");
    }

    @Test
    void testExtractTransactionId() {
        String transactionId = AvroConverter.extractTransactionId(mockAvroRecord);
        assertNotNull(transactionId);
        assertEquals("TEST-TXN-123", transactionId);
    }

    @Test
    void testExtractTransactionIdWithSpecificRecord() {
        String transactionId = AvroConverter.extractTransactionId(specificRecord);
        assertNotNull(transactionId);
        assertEquals("TEST-TXN-123", transactionId);
    }

    @Test
    void testExtractTransactionIdWithNullRecord() {
        String transactionId = AvroConverter.extractTransactionId(null);
        assertNull(transactionId);
    }

    @Test
    void testExtractAmount() {
        // Test with GenericRecord
        Double amount = AvroConverter.extractAmount(mockAvroRecord);
        assertNotNull(amount);
        assertEquals(100.50, amount, 0.01);
        
        // Verify the mockAvroRecord actually contains the expected value
        Object rawAmount = mockAvroRecord.get("amount");
        assertNotNull(rawAmount);
        assertTrue(rawAmount instanceof Number, "Amount should be a Number, but was: " + rawAmount.getClass().getName());
        assertEquals(100.50, ((Number) rawAmount).doubleValue(), 0.01);
    }

    @Test
    void testExtractAmountWithSpecificRecord() {
        Double amount = AvroConverter.extractAmount(specificRecord);
        assertNotNull(amount);
        assertEquals(100.50, amount, 0.01);
    }

    @Test
    void testExtractAmountWithNullRecord() {
        Double amount = AvroConverter.extractAmount(null);
        assertNull(amount);
    }

    @Test
    void testExtractAmountWithDifferentNumericTypes() {
        // Test with different numeric types to ensure robustness
        GenericRecord testRecord = new GenericData.Record(UnifiedPaymentMessage.SCHEMA$);
        testRecord.put("transactionId", "TEST-TXN-NUMERIC");
        testRecord.put("amount", 200.75); // double
        testRecord.put("currency", "USD");
        testRecord.put("senderAccount", "1111111111");
        testRecord.put("receiverAccount", "2222222222");
        testRecord.put("transactionType", "DDI");
        testRecord.put("priority", "NORMAL");
        testRecord.put("timestamp", Instant.parse("2023-12-21T12:00:00Z"));
        testRecord.put("componentName", "PSPAPFAFAST");
        testRecord.put("uuid", "TEST-UUID-NUMERIC");
        testRecord.put("channel", "G3I");
        testRecord.put("direction", "I");
        testRecord.put("domainName", "PAYMENTS");

        Double amount = AvroConverter.extractAmount(testRecord);
        assertNotNull(amount);
        assertEquals(200.75, amount, 0.01);
        
        // Test with integer amount
        GenericRecord intRecord = new GenericData.Record(UnifiedPaymentMessage.SCHEMA$);
        intRecord.put("transactionId", "TEST-TXN-INT");
        intRecord.put("amount", 300); // int
        intRecord.put("currency", "EUR");
        intRecord.put("senderAccount", "3333333333");
        intRecord.put("receiverAccount", "4444444444");
        intRecord.put("transactionType", "CTI");
        intRecord.put("priority", "LOW");
        intRecord.put("timestamp", Instant.parse("2023-12-21T13:00:00Z"));
        intRecord.put("componentName", "PSPAPFAFAST");
        intRecord.put("uuid", "TEST-UUID-INT");
        intRecord.put("channel", "G3I");
        intRecord.put("direction", "I");
        intRecord.put("domainName", "PAYMENTS");

        Double intAmount = AvroConverter.extractAmount(intRecord);
        assertNotNull(intAmount);
        assertEquals(300.0, intAmount, 0.01);
    }

    @Test
    void testExtractAmountFallbackMechanism() {
        // Test that the fallback mechanism works for generic records
        // This test ensures that when the record is not a SpecificRecord,
        // the generic record access fallback works correctly
        
        // Create a minimal record with just the amount field to test fallback
        GenericRecord minimalRecord = new GenericData.Record(UnifiedPaymentMessage.SCHEMA$);
        minimalRecord.put("amount", 500.25);
        
        // The extractAmount should use the fallback mechanism since this is a GenericRecord
        Double amount = AvroConverter.extractAmount(minimalRecord);
        assertNotNull(amount);
        assertEquals(500.25, amount, 0.01);
        
        // Verify the raw value is accessible
        Object rawAmount = minimalRecord.get("amount");
        assertNotNull(rawAmount);
        assertTrue(rawAmount instanceof Number);
        assertEquals(500.25, ((Number) rawAmount).doubleValue(), 0.01);
    }

    @Test
    void testExtractCurrency() {
        String currency = AvroConverter.extractCurrency(mockAvroRecord);
        assertNotNull(currency);
        assertEquals("SGD", currency);
    }

    @Test
    void testExtractCurrencyWithSpecificRecord() {
        String currency = AvroConverter.extractCurrency(specificRecord);
        assertNotNull(currency);
        assertEquals("SGD", currency);
    }

    @Test
    void testConvertToTransactionMessage() {
        // Test conversion from GenericRecord
        TransactionMessage result = AvroConverter.convertToTransactionMessage(mockAvroRecord);
        
        // Verify the result is not null
        assertNotNull(result);
        
        // Verify key fields are mapped correctly
        assertEquals("TEST-TXN-123", result.getTransactionId());
        assertEquals(100.50, result.getAmount().doubleValue(), 0.01);
        assertEquals("SGD", result.getCurrency());
        assertEquals("1234567890", result.getSenderAccount());
        assertEquals("0987654321", result.getReceiverAccount());
        assertEquals("CTI", result.getTransactionType());
        assertEquals("HIGH", result.getPriority());
        
        // Verify timestamp is parsed correctly from Instant to LocalDateTime
        assertNotNull(result.getTimestamp());
        // The timestamp should be parsed from the Instant in mockAvroRecord
        // We set it to "2023-12-21T10:30:56.789Z" which should convert to LocalDateTime
        LocalDateTime expectedTimestamp = LocalDateTime.ofInstant(
            Instant.parse("2023-12-21T10:30:56.789Z"), 
            java.time.ZoneOffset.UTC
        );
        assertEquals(expectedTimestamp.getYear(), result.getTimestamp().getYear());
        assertEquals(expectedTimestamp.getMonth(), result.getTimestamp().getMonth());
        assertEquals(expectedTimestamp.getDayOfMonth(), result.getTimestamp().getDayOfMonth());
        assertEquals(expectedTimestamp.getHour(), result.getTimestamp().getHour());
        assertEquals(expectedTimestamp.getMinute(), result.getTimestamp().getMinute());
        
        // Verify optional fields are handled correctly (should be null as not set in mock)
        assertNull(result.getDescription());
        assertNull(result.getReference());
    }

    @Test
    void testConvertToTransactionMessageWithSpecificRecord() {
        // Test conversion from SpecificRecord
        TransactionMessage result = AvroConverter.convertToTransactionMessage(specificRecord);
        
        // Verify the result is not null
        assertNotNull(result);
        
        // Verify key fields are mapped correctly
        assertEquals("TEST-TXN-123", result.getTransactionId());
        assertEquals(100.50, result.getAmount().doubleValue(), 0.01);
        assertEquals("SGD", result.getCurrency());
        assertEquals("1234567890", result.getSenderAccount());
        assertEquals("0987654321", result.getReceiverAccount());
        assertEquals("CTI", result.getTransactionType());
        assertEquals("HIGH", result.getPriority());
        
        // Verify timestamp is parsed correctly from Instant to LocalDateTime
        assertNotNull(result.getTimestamp());
        // The timestamp should be parsed from the Instant in specificRecord
        // We set it to "2023-12-21T10:30:56.789Z" which should convert to LocalDateTime
        LocalDateTime expectedTimestamp = LocalDateTime.ofInstant(
            Instant.parse("2023-12-21T10:30:56.789Z"), 
            java.time.ZoneOffset.UTC
        );
        assertEquals(expectedTimestamp.getYear(), result.getTimestamp().getYear());
        assertEquals(expectedTimestamp.getMonth(), result.getTimestamp().getMonth());
        assertEquals(expectedTimestamp.getDayOfMonth(), result.getTimestamp().getDayOfMonth());
        assertEquals(expectedTimestamp.getHour(), result.getTimestamp().getHour());
        assertEquals(expectedTimestamp.getMinute(), result.getTimestamp().getMinute());
        
        // Verify optional fields are handled correctly (should be null as not set in specificRecord)
        assertNull(result.getDescription());
        assertNull(result.getReference());
    }

    @Test
    void testConvertToTransactionMessageWithOptionalFields() {
        // Test conversion with optional fields set to verify they're handled correctly
        GenericRecord recordWithOptionals = new GenericData.Record(UnifiedPaymentMessage.SCHEMA$);
        recordWithOptionals.put("transactionId", "TEST-TXN-OPTIONAL");
        recordWithOptionals.put("amount", 150.00);
        recordWithOptionals.put("currency", "EUR");
        recordWithOptionals.put("senderAccount", "5555555555");
        recordWithOptionals.put("receiverAccount", "6666666666");
        recordWithOptionals.put("transactionType", "INTERNATIONAL");
        recordWithOptionals.put("priority", "LOW");
        recordWithOptionals.put("timestamp", Instant.parse("2023-12-21T14:30:00Z"));
        recordWithOptionals.put("componentName", "PSPAPFAFAST");
        recordWithOptionals.put("uuid", "TEST-UUID-OPTIONAL");
        recordWithOptionals.put("channel", "G3I");
        recordWithOptionals.put("direction", "I");
        recordWithOptionals.put("domainName", "PAYMENTS");

        TransactionMessage result = AvroConverter.convertToTransactionMessage(recordWithOptionals);
        
        // Verify the result is not null
        assertNotNull(result);
        
        // Verify all required fields are mapped correctly
        assertEquals("TEST-TXN-OPTIONAL", result.getTransactionId());
        assertEquals(150.00, result.getAmount().doubleValue(), 0.01);
        assertEquals("EUR", result.getCurrency());
        assertEquals("5555555555", result.getSenderAccount());
        assertEquals("6666666666", result.getReceiverAccount());
        assertEquals("INTERNATIONAL", result.getTransactionType());
        assertEquals("LOW", result.getPriority());
        
        // Verify timestamp parsing
        assertNotNull(result.getTimestamp());
        LocalDateTime expectedTimestamp = LocalDateTime.ofInstant(
            Instant.parse("2023-12-21T14:30:00Z"), 
            java.time.ZoneOffset.UTC
        );
        assertEquals(expectedTimestamp.getYear(), result.getTimestamp().getYear());
        assertEquals(expectedTimestamp.getMonth(), result.getTimestamp().getMonth());
        assertEquals(expectedTimestamp.getDayOfMonth(), result.getTimestamp().getDayOfMonth());
        assertEquals(expectedTimestamp.getHour(), result.getTimestamp().getHour());
        assertEquals(expectedTimestamp.getMinute(), result.getTimestamp().getMinute());
        
        // Verify optional fields are null (as they're not in the Avro schema)
        assertNull(result.getDescription());
        assertNull(result.getReference());
    }

    @Test
    void testConvertToTransactionMessageTimestampEdgeCases() {
        // Test timestamp parsing edge cases
        GenericRecord recordWithEpochZero = new GenericData.Record(UnifiedPaymentMessage.SCHEMA$);
        recordWithEpochZero.put("transactionId", "TEST-TXN-EPOCH-ZERO");
        recordWithEpochZero.put("amount", 75.50);
        recordWithEpochZero.put("currency", "GBP");
        recordWithEpochZero.put("senderAccount", "7777777777");
        recordWithEpochZero.put("receiverAccount", "8888888888");
        recordWithEpochZero.put("transactionType", "DOMESTIC");
        recordWithEpochZero.put("priority", "NORMAL");
        recordWithEpochZero.put("timestamp", Instant.ofEpochMilli(0)); // Unix epoch start
        recordWithEpochZero.put("componentName", "PSPAPFAFAST");
        recordWithEpochZero.put("uuid", "TEST-UUID-EPOCH-ZERO");
        recordWithEpochZero.put("channel", "G3I");
        recordWithEpochZero.put("direction", "I");
        recordWithEpochZero.put("domainName", "PAYMENTS");

        TransactionMessage result = AvroConverter.convertToTransactionMessage(recordWithEpochZero);
        
        // Verify the result is not null
        assertNotNull(result);
        
        // Verify timestamp parsing for epoch zero
        assertNotNull(result.getTimestamp());
        LocalDateTime expectedEpochZero = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(0), 
            java.time.ZoneOffset.UTC
        );
        assertEquals(expectedEpochZero.getYear(), result.getTimestamp().getYear());
        assertEquals(expectedEpochZero.getMonth(), result.getTimestamp().getMonth());
        assertEquals(expectedEpochZero.getDayOfMonth(), result.getTimestamp().getDayOfMonth());
        assertEquals(expectedEpochZero.getHour(), result.getTimestamp().getHour());
        assertEquals(expectedEpochZero.getMinute(), result.getTimestamp().getMinute());
        
        // Test with current timestamp
        GenericRecord recordWithCurrentTime = new GenericData.Record(UnifiedPaymentMessage.SCHEMA$);
        Instant currentTime = Instant.now();
        recordWithCurrentTime.put("transactionId", "TEST-TXN-CURRENT");
        recordWithCurrentTime.put("amount", 200.00);
        recordWithCurrentTime.put("currency", "AUD");
        recordWithCurrentTime.put("senderAccount", "9999999999");
        recordWithCurrentTime.put("receiverAccount", "0000000000");
        recordWithCurrentTime.put("transactionType", "CTI");
        recordWithCurrentTime.put("priority", "HIGH");
        recordWithCurrentTime.put("timestamp", currentTime);
        recordWithCurrentTime.put("componentName", "PSPAPFAFAST");
        recordWithCurrentTime.put("uuid", "TEST-UUID-CURRENT");
        recordWithCurrentTime.put("channel", "G3I");
        recordWithCurrentTime.put("direction", "I");
        recordWithCurrentTime.put("domainName", "PAYMENTS");

        TransactionMessage currentResult = AvroConverter.convertToTransactionMessage(recordWithCurrentTime);
        
        // Verify the result is not null
        assertNotNull(currentResult);
        
        // Verify timestamp parsing for current time (should be within reasonable bounds)
        assertNotNull(currentResult.getTimestamp());
        LocalDateTime expectedCurrentTime = LocalDateTime.ofInstant(currentTime, java.time.ZoneOffset.UTC);
        assertEquals(expectedCurrentTime.getYear(), currentResult.getTimestamp().getYear());
        assertEquals(expectedCurrentTime.getMonth(), currentResult.getTimestamp().getMonth());
        assertEquals(expectedCurrentTime.getDayOfMonth(), currentResult.getTimestamp().getDayOfMonth());
        assertEquals(expectedCurrentTime.getHour(), currentResult.getTimestamp().getHour());
        assertEquals(expectedCurrentTime.getMinute(), currentResult.getTimestamp().getMinute());
    }

    @Test
    void testConvertToTransactionMessageWithNullRecord() {
        // The implementation returns null when given a null record
        TransactionMessage result = AvroConverter.convertToTransactionMessage(null);
        assertNull(result);
    }

    @Test
    void testCreateUnifiedPaymentMessage() {
        UnifiedPaymentMessage message = AvroConverter.createUnifiedPaymentMessage(
            "TEST-TXN-456", 200.75, "USD", "1111111111", "2222222222",
            "DDI", "NORMAL", Instant.parse("2023-12-21T11:45:00Z"), "PSPAPFAFAST",
            "TEST-UUID-456", "G3I", "I", "PAYMENTS"
        );
        
        assertNotNull(message);
        assertEquals("TEST-TXN-456", message.getTransactionId());
        assertEquals(200.75, message.getAmount(), 0.01);
        assertEquals("USD", message.getCurrency());
        assertEquals("DDI", message.getTransactionType());
        assertEquals("NORMAL", message.getPriority());
    }

    @Test
    void testGenericRecordConstruction() {
        // Verify that the GenericRecord is properly constructed
        assertNotNull(mockAvroRecord);
        assertEquals(UnifiedPaymentMessage.SCHEMA$, mockAvroRecord.getSchema());
        
        // Verify all fields are properly set
        assertEquals("TEST-TXN-123", mockAvroRecord.get("transactionId"));
        assertEquals(100.50, mockAvroRecord.get("amount"));
        assertEquals("SGD", mockAvroRecord.get("currency"));
        assertEquals("1234567890", mockAvroRecord.get("senderAccount"));
        assertEquals("0987654321", mockAvroRecord.get("receiverAccount"));
        assertEquals("CTI", mockAvroRecord.get("transactionType"));
        assertEquals("HIGH", mockAvroRecord.get("priority"));
        assertNotNull(mockAvroRecord.get("timestamp"));
        assertEquals("PSPAPFAFAST", mockAvroRecord.get("componentName"));
        assertEquals("TEST-UUID-123", mockAvroRecord.get("uuid"));
        assertEquals("G3I", mockAvroRecord.get("channel"));
        assertEquals("I", mockAvroRecord.get("direction"));
        assertEquals("PAYMENTS", mockAvroRecord.get("domainName"));
    }

    @Test
    void testIsValidAvroRecord() {
        // Test with valid GenericRecord
        assertTrue(AvroConverter.isValidAvroRecord(mockAvroRecord));
        
        // Test with valid SpecificRecord
        assertTrue(AvroConverter.isValidAvroRecord(specificRecord));
        
        // Test with null
        assertFalse(AvroConverter.isValidAvroRecord(null));
    }
}
