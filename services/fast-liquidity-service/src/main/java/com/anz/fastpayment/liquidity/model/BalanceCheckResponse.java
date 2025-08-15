package com.anz.fastpayment.liquidity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Balance Check Response
 * 
 * Response object for balance check operations
 */
@Schema(description = "Response from balance check operation")
public class BalanceCheckResponse {

    @Schema(description = "Operation status", example = "SUCCESS")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Whether the transaction would be authorized", example = "true")
    @JsonProperty("authorized")
    private boolean authorized;

    @Schema(description = "Current available balance", example = "1500000.00")
    @JsonProperty("currentBalance")
    private String currentBalance;

    @Schema(description = "What the balance would be after the transaction", example = "1450000.00")
    @JsonProperty("projectedBalance")
    private String projectedBalance;

    @Schema(description = "Currency code", example = "SGD")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Minimum balance threshold", example = "0.00")
    @JsonProperty("minimumBalance")
    private String minimumBalance;

    @Schema(description = "Maximum amount available for debits", example = "1500000.00")
    @JsonProperty("availableAmount")
    private String availableAmount;

    @Schema(description = "Response timestamp", example = "2025-01-15T10:30:00.123Z")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @Schema(description = "Request identifier", example = "CHK-20250115-103000-001")
    @JsonProperty("requestId")
    private String requestId;

    @Schema(description = "Reason for rejection if authorized=false", example = "Insufficient funds")
    @JsonProperty("rejectionReason")
    private String rejectionReason;

    @Schema(description = "Any warnings about the proposed transaction")
    @JsonProperty("warnings")
    private List<String> warnings;

    // Constructors
    public BalanceCheckResponse() {}

    public BalanceCheckResponse(String status, boolean authorized, String currentBalance, 
                               String projectedBalance, String currency, Instant timestamp, String requestId) {
        this.status = status;
        this.authorized = authorized;
        this.currentBalance = currentBalance;
        this.projectedBalance = projectedBalance;
        this.currency = currency;
        this.timestamp = timestamp;
        this.requestId = requestId;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public String getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(String currentBalance) {
        this.currentBalance = currentBalance;
    }

    public String getProjectedBalance() {
        return projectedBalance;
    }

    public void setProjectedBalance(String projectedBalance) {
        this.projectedBalance = projectedBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(String minimumBalance) {
        this.minimumBalance = minimumBalance;
    }

    public String getAvailableAmount() {
        return availableAmount;
    }

    public void setAvailableAmount(String availableAmount) {
        this.availableAmount = availableAmount;
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

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}