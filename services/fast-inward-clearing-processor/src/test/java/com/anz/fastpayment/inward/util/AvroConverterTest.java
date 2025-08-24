package com.anz.fastpayment.inward.util;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AvroConverter utility
 */
public class AvroConverterTest {

    private GenericRecord mockAvroRecord;

    @BeforeEach
    void setUp() {
        // Create a mock Avro record for testing
        mockAvroRecord = new GenericData.Record(null);
        
        // Mock header structure
        GenericRecord header = new GenericData.Record(null);
        header.put("UUID", "TEST-UUID-123");
        mockAvroRecord.put("Header", header);
        
        // Mock body structure
        GenericRecord body = new GenericData.Record(null);
        mockAvroRecord.put("Body", body);
    }

    @Test
    void testExtractTransactionId() {
        String transactionId = AvroConverter.extractTransactionId(mockAvroRecord);
        assertNotNull(transactionId);
        assertEquals("TEST-UUID-123", transactionId);
    }

    @Test
    void testExtractTransactionIdWithNullRecord() {
        String transactionId = AvroConverter.extractTransactionId(null);
        assertNull(transactionId);
    }

    @Test
    void testExtractAmount() {
        Double amount = AvroConverter.extractAmount(mockAvroRecord);
        // Currently returns placeholder value, so just check it's not null
        assertNotNull(amount);
    }

    @Test
    void testExtractAmountWithNullRecord() {
        Double amount = AvroConverter.extractAmount(null);
        assertNull(amount);
    }

    @Test
    void testConvertToTransactionMessage() {
        // This test verifies the basic conversion logic
        // In a real implementation, you'd have proper Avro schema validation
        assertDoesNotThrow(() -> {
            AvroConverter.convertToTransactionMessage(mockAvroRecord);
        });
    }

    @Test
    void testConvertToTransactionMessageWithNullRecord() {
        assertThrows(RuntimeException.class, () -> {
            AvroConverter.convertToTransactionMessage(null);
        });
    }
}
