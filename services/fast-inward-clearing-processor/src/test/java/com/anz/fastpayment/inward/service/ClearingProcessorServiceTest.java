package com.anz.fastpayment.inward.service;

import com.anz.fastpayment.inward.model.*;
import com.anz.fastpayment.inward.service.impl.ClearingProcessorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClearingProcessorService
 */
@ExtendWith(MockitoExtension.class)
class ClearingProcessorServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ClearingProcessorServiceImpl clearingProcessorService;

    @BeforeEach
    void setUp() {
        clearingProcessorService = new ClearingProcessorServiceImpl();
        ReflectionTestUtils.setField(clearingProcessorService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(clearingProcessorService, "maxAmountStr", "SGD 1000000");
        ReflectionTestUtils.setField(clearingProcessorService, "highRiskCountries", "XX,YY,ZZ");
        ReflectionTestUtils.setField(clearingProcessorService, "idempotencyTtlHours", 24);
        ReflectionTestUtils.setField(clearingProcessorService, "processingNodeId", "test-processor-01");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testValidateTransaction_ValidMessage() {
        // Given
        TransactionMessage message = createValidTransactionMessage();

        // When
        boolean result = clearingProcessorService.validateTransaction(message);

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateTransaction_NullMessage() {
        // When
        boolean result = clearingProcessorService.validateTransaction(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateTransaction_MissingTransactionId() {
        // Given
        TransactionMessage message = createValidTransactionMessage();
        message.setTransactionId("");

        // When
        boolean result = clearingProcessorService.validateTransaction(message);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateTransaction_InvalidAmount() {
        // Given
        TransactionMessage message = createValidTransactionMessage();
        message.setAmount(BigDecimal.ZERO);

        // When
        boolean result = clearingProcessorService.validateTransaction(message);

        // Then
        assertFalse(result);
    }

    @Test
    void testEnrichTransaction() {
        // Given
        TransactionMessage message = createValidTransactionMessage();

        // When
        ProcessedTransactionMessage result = clearingProcessorService.enrichTransaction(message);

        // Then
        assertNotNull(result);
        assertEquals(message.getTransactionId(), result.getTransactionId());
        assertEquals(message.getAmount(), result.getAmount());
        assertEquals("test-processor-01", result.getProcessingNodeId());
        assertNotNull(result.getEnrichmentData());
        assertEquals("FastInwardClearingProcessor", result.getEnrichmentData().getEnrichmentSource());
    }

    @Test
    void testApplyBusinessRules_WithinLimits() {
        // Given
        ProcessedTransactionMessage message = createProcessedTransactionMessage(new BigDecimal("1000.00"));

        // When
        ProcessedTransactionMessage result = clearingProcessorService.applyBusinessRules(message);

        // Then
        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertNotNull(result.getBusinessRuleResults());
        assertEquals("APPROVED", result.getBusinessRuleResults().getOverallStatus());
        assertFalse(result.getBusinessRuleResults().isBlocked());
    }

    @Test
    void testApplyBusinessRules_ExceedsAmountLimit() {
        // Given
        ProcessedTransactionMessage message = createProcessedTransactionMessage(new BigDecimal("2000000.00"));

        // When
        ProcessedTransactionMessage result = clearingProcessorService.applyBusinessRules(message);

        // Then
        assertNotNull(result);
        assertEquals("BLOCKED", result.getStatus());
        assertNotNull(result.getBusinessRuleResults());
        assertEquals("BLOCKED", result.getBusinessRuleResults().getOverallStatus());
        assertTrue(result.getBusinessRuleResults().isBlocked());
        assertEquals("Amount exceeds maximum limit", result.getBusinessRuleResults().getBlockReason());
    }

    @Test
    void testApplyBusinessRules_HighRiskScore() {
        // Given
        ProcessedTransactionMessage message = createProcessedTransactionMessage(new BigDecimal("50000.00"));
        message.setTransactionType("INTERNATIONAL");
        message.setPriority("HIGH");

        // When
        ProcessedTransactionMessage result = clearingProcessorService.applyBusinessRules(message);

        // Then
        assertNotNull(result);
        assertEquals("BLOCKED", result.getStatus());
        assertNotNull(result.getBusinessRuleResults());
        assertTrue(result.getBusinessRuleResults().isBlocked());
        assertTrue(result.getBusinessRuleResults().getRiskScore() > 70);
    }

    @Test
    void testIsDuplicateTransaction_NotDuplicate() {
        // Given
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // When
        boolean result = clearingProcessorService.isDuplicateTransaction("test-transaction-123");

        // Then
        assertFalse(result);
        verify(redisTemplate).hasKey("transaction:processed:test-transaction-123");
    }

    @Test
    void testIsDuplicateTransaction_IsDuplicate() {
        // Given
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // When
        boolean result = clearingProcessorService.isDuplicateTransaction("test-transaction-123");

        // Then
        assertTrue(result);
        verify(redisTemplate).hasKey("transaction:processed:test-transaction-123");
    }

    @Test
    void testMarkTransactionAsProcessed() {
        // Given
        String transactionId = "test-transaction-123";

        // When
        clearingProcessorService.markTransactionAsProcessed(transactionId);

        // Then
        verify(valueOperations).set(eq("transaction:processed:test-transaction-123"), anyString(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testProcessTransaction_Success() throws Exception {
        // Given
        TransactionMessage message = createValidTransactionMessage();
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // When
        ProcessedTransactionMessage result = clearingProcessorService.processTransaction(message);

        // Then
        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        verify(valueOperations).set(anyString(), anyString(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void testProcessTransaction_DuplicateTransaction() {
        // Given
        TransactionMessage message = createValidTransactionMessage();
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            clearingProcessorService.processTransaction(message);
        });
    }

    @Test
    void testProcessTransaction_ValidationFailed() {
        // Given
        TransactionMessage message = createValidTransactionMessage();
        message.setAmount(BigDecimal.ZERO);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            clearingProcessorService.processTransaction(message);
        });
    }

    private TransactionMessage createValidTransactionMessage() {
        TransactionMessage message = new TransactionMessage();
        message.setTransactionId("test-transaction-123");
        message.setAmount(new BigDecimal("1000.00"));
        message.setCurrency("SGD");
        message.setSenderAccount("12345678");
        message.setReceiverAccount("87654321");
        message.setTransactionType("DOMESTIC");
        message.setDescription("Test transaction");
        message.setTimestamp(LocalDateTime.now());
        message.setPriority("NORMAL");
        message.setReference("REF123");
        return message;
    }

    private ProcessedTransactionMessage createProcessedTransactionMessage(BigDecimal amount) {
        TransactionMessage original = createValidTransactionMessage();
        original.setAmount(amount);
        return new ProcessedTransactionMessage(original);
    }
}
