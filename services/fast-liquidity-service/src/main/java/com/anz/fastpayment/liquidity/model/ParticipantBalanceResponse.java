package com.anz.fastpayment.liquidity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Participant Balance Response
 * 
 * Response object containing current balance information for a participant
 */
@Schema(description = "Current balance information for a participant")
public class ParticipantBalanceResponse {

    @Schema(description = "Participant identifier", example = "ANZBSGSG")
    @JsonProperty("participantId")
    private String participantId;

    @Schema(description = "Currency code", example = "SGD")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Current total balance", example = "1500000.00")
    @JsonProperty("currentBalance")
    private String currentBalance;

    @Schema(description = "Available balance after reserved amounts", example = "1400000.00")
    @JsonProperty("availableBalance")
    private String availableBalance;

    @Schema(description = "Amount currently reserved for pending transactions", example = "100000.00")
    @JsonProperty("reservedAmount")
    private String reservedAmount;

    @Schema(description = "Maximum net debit cap for this participant", example = "5000000.00")
    @JsonProperty("netDebitCapLimit")
    private String netDebitCapLimit;

    @Schema(description = "Current utilization of net debit cap (0.0 to 1.0)", example = "0.3")
    @JsonProperty("netDebitCapUtilization")
    private Double netDebitCapUtilization;

    @Schema(description = "Minimum balance threshold", example = "0.00")
    @JsonProperty("minimumBalance")
    private String minimumBalance;

    @Schema(description = "Response timestamp", example = "2025-01-15T10:30:00.123Z")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @Schema(description = "Timestamp of last transaction", example = "2025-01-15T10:25:30.456Z")
    @JsonProperty("lastTransactionAt")
    private Instant lastTransactionAt;

    // Constructors
    public ParticipantBalanceResponse() {}

    public ParticipantBalanceResponse(String participantId, String currency, String currentBalance, 
                                    String availableBalance, Instant timestamp) {
        this.participantId = participantId;
        this.currency = currency;
        this.currentBalance = currentBalance;
        this.availableBalance = availableBalance;
        this.timestamp = timestamp;
    }

    // Getters and Setters
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

    public String getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(String currentBalance) {
        this.currentBalance = currentBalance;
    }

    public String getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(String availableBalance) {
        this.availableBalance = availableBalance;
    }

    public String getReservedAmount() {
        return reservedAmount;
    }

    public void setReservedAmount(String reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    public String getNetDebitCapLimit() {
        return netDebitCapLimit;
    }

    public void setNetDebitCapLimit(String netDebitCapLimit) {
        this.netDebitCapLimit = netDebitCapLimit;
    }

    public Double getNetDebitCapUtilization() {
        return netDebitCapUtilization;
    }

    public void setNetDebitCapUtilization(Double netDebitCapUtilization) {
        this.netDebitCapUtilization = netDebitCapUtilization;
    }

    public String getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(String minimumBalance) {
        this.minimumBalance = minimumBalance;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Instant getLastTransactionAt() {
        return lastTransactionAt;
    }

    public void setLastTransactionAt(Instant lastTransactionAt) {
        this.lastTransactionAt = lastTransactionAt;
    }
}