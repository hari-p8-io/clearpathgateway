package com.anz.fastpayment.liquidity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

/**
 * Balance Update Request
 * 
 * Request object for updating participant balances from payment transactions
 */
@Schema(description = "Request for updating liquidity balances from payment transactions")
public class BalanceUpdateRequest {

    @Schema(description = "ISO country code (SG maps to G3, HK maps to FPS)", 
            example = "SG", allowableValues = {"SG", "HK"})
    @JsonProperty("countryCode")
    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "^(SG|HK)$", message = "Country code must be SG or HK")
    private String countryCode;

    @Schema(description = "Participant whose balance is being updated", example = "ANZBSGSG")
    @JsonProperty("participantId")
    @NotBlank(message = "Participant ID is required")
    @Pattern(regexp = "^[A-Z]{8}$", message = "Participant ID must be 8 uppercase letters")
    private String participantId;

    @Schema(description = "ISO 4217 currency code", example = "SGD")
    @JsonProperty("currency")
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    private String currency;

    @Schema(description = "Amount to add/subtract (negative for debits, positive for credits)", 
            example = "-50000.00")
    @JsonProperty("amount")
    @NotBlank(message = "Amount is required")
    @Pattern(regexp = "^-?[0-9]+(\\.[0-9]{1,5})?$", message = "Amount must be numeric with up to 5 decimal places")
    private String amount;

    @Schema(description = "Type of balance operation", example = "DEBIT", 
            allowableValues = {"DEBIT", "CREDIT", "RESERVE", "RELEASE"})
    @JsonProperty("transactionType")
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @Schema(description = "ISO 20022 message type that triggered this update", 
            example = "pacs.008.001.08")
    @JsonProperty("messageType")
    @NotBlank(message = "Message type is required")
    private String messageType;

    @Schema(description = "Unique message identifier from the payment system", 
            example = "MSG20250115103000001")
    @JsonProperty("messageId")
    @NotBlank(message = "Message ID is required")
    @Size(max = 35, message = "Message ID cannot exceed 35 characters")
    private String messageId;

    @Schema(description = "Business reference or transaction reference", 
            example = "TXN-OUT-001")
    @JsonProperty("reference")
    @Size(max = 35, message = "Reference cannot exceed 35 characters")
    private String reference;

    @Schema(description = "Transaction timestamp from the original message", 
            example = "2025-01-15T10:30:00.123Z")
    @JsonProperty("timestamp")
    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    @Schema(description = "Counterparty participant (optional)", example = "DBSSSGSG")
    @JsonProperty("counterpartyId")
    @Pattern(regexp = "^[A-Z]{8}$", message = "Counterparty ID must be 8 uppercase letters")
    private String counterpartyId;

    @Schema(description = "Any additional country/scheme specific data")
    @JsonProperty("additionalData")
    @Valid
    private Map<String, Object> additionalData;

    // Constructors
    public BalanceUpdateRequest() {}

    public BalanceUpdateRequest(String countryCode, String participantId, String currency, 
                               String amount, TransactionType transactionType, String messageType, 
                               String messageId, Instant timestamp) {
        this.countryCode = countryCode;
        this.participantId = participantId;
        this.currency = currency;
        this.amount = amount;
        this.transactionType = transactionType;
        this.messageType = messageType;
        this.messageId = messageId;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
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

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getCounterpartyId() {
        return counterpartyId;
    }

    public void setCounterpartyId(String counterpartyId) {
        this.counterpartyId = counterpartyId;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    @Override
    public String toString() {
        return "BalanceUpdateRequest{" +
                "countryCode='" + countryCode + '\'' +
                ", participantId='" + participantId + '\'' +
                ", currency='" + currency + '\'' +
                ", amount='" + amount + '\'' +
                ", transactionType=" + transactionType +
                ", messageType='" + messageType + '\'' +
                ", messageId='" + messageId + '\'' +
                ", reference='" + reference + '\'' +
                ", timestamp=" + timestamp +
                ", counterpartyId='" + counterpartyId + '\'' +
                '}';
    }
}