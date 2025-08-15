package com.anz.fastpayment.liquidity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Error Response
 * 
 * Standard error response format for API errors
 */
@Schema(description = "Standard error response format")
public class ErrorResponse {

    @Schema(description = "Error status", example = "ERROR")
    @JsonProperty("status")
    private String status = "ERROR";

    @Schema(description = "Error code", example = "INSUFFICIENT_FUNDS")
    @JsonProperty("errorCode")
    private String errorCode;

    @Schema(description = "Human-readable error message", 
            example = "Insufficient funds for transaction. Available: 1000.00, Required: 50000.00")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Additional error context")
    @JsonProperty("details")
    private Map<String, Object> details;

    @Schema(description = "Error timestamp", example = "2025-01-15T10:30:00.789Z")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @Schema(description = "Request identifier", example = "REQ-20250115-103000-003")
    @JsonProperty("requestId")
    private String requestId;

    // Constructors
    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String errorCode, String message) {
        this();
        this.errorCode = errorCode;
        this.message = message;
    }

    public ErrorResponse(String errorCode, String message, String requestId) {
        this(errorCode, message);
        this.requestId = requestId;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Error codes used throughout the liquidity service
     */
    public static final class ErrorCodes {
        public static final String INVALID_PARTICIPANT = "INVALID_PARTICIPANT";
        public static final String INVALID_CURRENCY = "INVALID_CURRENCY";
        public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
        public static final String DUPLICATE_TRANSACTION = "DUPLICATE_TRANSACTION";
        public static final String INVALID_AMOUNT = "INVALID_AMOUNT";
        public static final String SCHEME_NOT_SUPPORTED = "SCHEME_NOT_SUPPORTED";
        public static final String PARTICIPANT_SUSPENDED = "PARTICIPANT_SUSPENDED";
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
        public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
        public static final String NET_DEBIT_CAP_EXCEEDED = "NET_DEBIT_CAP_EXCEEDED";
        public static final String UNAUTHORIZED_TRANSACTION = "UNAUTHORIZED_TRANSACTION";
        public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    }
}