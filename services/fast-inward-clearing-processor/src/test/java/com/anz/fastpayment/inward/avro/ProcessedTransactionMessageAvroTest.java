package com.anz.fastpayment.inward.avro;

import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that ProcessedTransactionMessage Avro serialization
 * properly handles null values without throwing NullPointerException
 */
class ProcessedTransactionMessageAvroTest {

    @Test
    void testSerializationWithNullValues() throws IOException {
        // Create a ProcessedTransactionMessage with some null values
        ProcessedTransactionMessage message = new ProcessedTransactionMessage();
        message.setTransactionId("TEST-TXN-001"); // Required field - must be non-null
        message.setAmount(null); // Nullable field
        message.setCurrency(null); // Nullable field
        message.setSenderAccount("12345678"); // Nullable field with value
        message.setReceiverAccount(null); // Nullable field
        message.setTransactionType("DOMESTIC"); // Nullable field with value
        message.setPriority(null); // Nullable field
        message.setTimestamp(null); // Nullable field
        message.setProcessingTimestamp("2024-01-01T10:00:00Z"); // Nullable field with value
        message.setProcessingNodeId(null); // Nullable field
        message.setStatus("PENDING"); // Nullable field with value
        message.setValidationPassed(true); // Required field - must be non-null
        message.setBusinessRulesPassed(false); // Required field - must be non-null
        message.setErrorMessage(null); // Already nullable field

        // Test that serialization doesn't throw NullPointerException
        assertDoesNotThrow(() -> {
            serializeMessage(message);
        }, "Serialization should not throw NullPointerException with null values");

        // Test that deserialization works correctly
        byte[] serialized = serializeMessage(message);
        ProcessedTransactionMessage deserialized = deserializeMessage(serialized);

        // Verify that null values are preserved
        assertNotNull(deserialized.getTransactionId());
        assertEquals("TEST-TXN-001", deserialized.getTransactionId());
        assertNull(deserialized.getAmount());
        assertNull(deserialized.getCurrency());
        assertEquals("12345678", deserialized.getSenderAccount());
        assertNull(deserialized.getReceiverAccount());
        assertEquals("DOMESTIC", deserialized.getTransactionType());
        assertNull(deserialized.getPriority());
        assertNull(deserialized.getTimestamp());
        assertEquals("2024-01-01T10:00:00Z", deserialized.getProcessingTimestamp());
        assertNull(deserialized.getProcessingNodeId());
        assertEquals("PENDING", deserialized.getStatus());
        assertTrue(deserialized.getValidationPassed());
        assertFalse(deserialized.getBusinessRulesPassed());
        assertNull(deserialized.getErrorMessage());
    }

    @Test
    void testSerializationWithAllNonNullValues() throws IOException {
        // Create a ProcessedTransactionMessage with all fields populated
        ProcessedTransactionMessage message = new ProcessedTransactionMessage();
        message.setTransactionId("TEST-TXN-002");
        message.setAmount("1000.00");
        message.setCurrency("SGD");
        message.setSenderAccount("11111111");
        message.setReceiverAccount("22222222");
        message.setTransactionType("INTERNATIONAL");
        message.setPriority("HIGH");
        message.setTimestamp("2024-01-01T09:00:00Z");
        message.setProcessingTimestamp("2024-01-01T09:00:01Z");
        message.setProcessingNodeId("NODE-001");
        message.setStatus("SUCCESS");
        message.setValidationPassed(true);
        message.setBusinessRulesPassed(true);
        message.setErrorMessage("No errors");

        // Test serialization and deserialization
        byte[] serialized = serializeMessage(message);
        ProcessedTransactionMessage deserialized = deserializeMessage(serialized);

        // Verify all values are preserved
        assertEquals("TEST-TXN-002", deserialized.getTransactionId());
        assertEquals("1000.00", deserialized.getAmount());
        assertEquals("SGD", deserialized.getCurrency());
        assertEquals("11111111", deserialized.getSenderAccount());
        assertEquals("22222222", deserialized.getReceiverAccount());
        assertEquals("INTERNATIONAL", deserialized.getTransactionType());
        assertEquals("HIGH", deserialized.getPriority());
        assertEquals("2024-01-01T09:00:00Z", deserialized.getTimestamp());
        assertEquals("2024-01-01T09:00:01Z", deserialized.getProcessingTimestamp());
        assertEquals("NODE-001", deserialized.getProcessingNodeId());
        assertEquals("SUCCESS", deserialized.getStatus());
        assertTrue(deserialized.getValidationPassed());
        assertTrue(deserialized.getBusinessRulesPassed());
        assertEquals("No errors", deserialized.getErrorMessage());
    }

    @Test
    void testSerializationWithMixedNullValues() throws IOException {
        // Create a ProcessedTransactionMessage with mixed null and non-null values
        ProcessedTransactionMessage message = new ProcessedTransactionMessage();
        message.setTransactionId("TEST-TXN-003"); // Required
        message.setAmount("500.00"); // Non-null
        message.setCurrency(null); // Null
        message.setSenderAccount("33333333"); // Non-null
        message.setReceiverAccount(null); // Null
        message.setTransactionType("DOMESTIC"); // Non-null
        message.setPriority(null); // Null
        message.setTimestamp("2024-01-01T08:00:00Z"); // Non-null
        message.setProcessingTimestamp(null); // Null
        message.setProcessingNodeId("NODE-002"); // Non-null
        message.setStatus(null); // Null
        message.setValidationPassed(false); // Required
        message.setBusinessRulesPassed(true); // Required
        message.setErrorMessage("Some error occurred"); // Non-null

        // Test serialization and deserialization
        byte[] serialized = serializeMessage(message);
        ProcessedTransactionMessage deserialized = deserializeMessage(serialized);

        // Verify mixed values are preserved correctly
        assertEquals("TEST-TXN-003", deserialized.getTransactionId());
        assertEquals("500.00", deserialized.getAmount());
        assertNull(deserialized.getCurrency());
        assertEquals("33333333", deserialized.getSenderAccount());
        assertNull(deserialized.getReceiverAccount());
        assertEquals("DOMESTIC", deserialized.getTransactionType());
        assertNull(deserialized.getPriority());
        assertEquals("2024-01-01T08:00:00Z", deserialized.getTimestamp());
        assertNull(deserialized.getProcessingTimestamp());
        assertEquals("NODE-002", deserialized.getProcessingNodeId());
        assertNull(deserialized.getStatus());
        assertFalse(deserialized.getValidationPassed());
        assertTrue(deserialized.getBusinessRulesPassed());
        assertEquals("Some error occurred", deserialized.getErrorMessage());
    }

    private byte[] serializeMessage(ProcessedTransactionMessage message) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DatumWriter<ProcessedTransactionMessage> datumWriter = new SpecificDatumWriter<>(ProcessedTransactionMessage.class);
        Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        
        datumWriter.write(message, encoder);
        encoder.flush();
        outputStream.close();
        
        return outputStream.toByteArray();
    }

    private ProcessedTransactionMessage deserializeMessage(byte[] data) throws IOException {
        DatumReader<ProcessedTransactionMessage> datumReader = new SpecificDatumReader<>(ProcessedTransactionMessage.class);
        Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        
        return datumReader.read(null, decoder);
    }
}
