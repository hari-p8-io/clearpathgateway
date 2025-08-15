package com.anz.fastpayment.liquidity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Balance Update Response
 * 
 * Response object for balance update operations
 */
@Schema(description = "Response from balance update operation")
public class BalanceUpdateResponse {

    @Schema(description = "Operation status", example = "SUCCESS", allowableValues = {"SUCCESS", "REJECTED"})
    @JsonProperty("status")
    private String status;

    @Schema(description = "Internal transaction ID for this balance update", 
            example = "TXN-20250115-103000-001")
    @JsonProperty("transactionId")
    private String transactionId;

    @Schema(description = "Participant identifier", example = "ANZBSGSG")
    @JsonProperty("participantId")
    private String participantId;

    @Schema(description = "Currency code", example = "SGD")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Balance before the update", example = "1550000.00")
    @JsonProperty("balanceBefore")
    private String balanceBefore;

    @Schema(description = "Balance after the update", example = "1500000.00")
    @JsonProperty("balanceAfter")
    private String balanceAfter;

    @Schema(description = "Actual amount that was processed", example = "-50000.00")
    @JsonProperty("amountProcessed")
    private String amountProcessed;

    @Schema(description = "When the update was processed", example = "2025-01-15T10:30:00.456Z")
    @JsonProperty("processedAt")
    private Instant processedAt;

    @Schema(description = "Request identifier", example = "REQ-20250115-103000-002")
    @JsonProperty("requestId")
    private String requestId;

    @Schema(description = "Any warnings generated during processing")
    @JsonProperty("warnings")
    private List<String> warnings;

    // Constructors
    public BalanceUpdateResponse() {}

    public BalanceUpdateResponse(String status, String transactionId, String balanceAfter, Instant processedAt) {
        this.status = status;
        this.transactionId = transactionId;
        this.balanceAfter = balanceAfter;
        this.processedAt = processedAt;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(String balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public String getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(String balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getAmountProcessed() {
        return amountProcessed;
    }

    public void setAmountProcessed(String amountProcessed) {
        this.amountProcessed = amountProcessed;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}