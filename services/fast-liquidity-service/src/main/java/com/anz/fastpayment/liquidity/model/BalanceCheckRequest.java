package com.anz.fastpayment.liquidity.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Balance Check Request
 * 
 * Request object for checking liquidity balance authorization
 */
@Schema(description = "Request for checking liquidity balance and authorization")
public class BalanceCheckRequest {

    @Schema(description = "ISO country code (SG maps to G3, HK maps to FPS)", 
            example = "SG", allowableValues = {"SG", "HK"})
    @JsonProperty("countryCode")
    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "^(SG|HK)$", message = "Country code must be SG or HK")
    private String countryCode;

    @Schema(description = "Participant identifier (optional for in-house service)", 
            example = "ANZBSGSG")
    @JsonProperty("participantId")
    @Pattern(regexp = "^[A-Z]{8}$", message = "Participant ID must be 8 uppercase letters")
    private String participantId;

    @Schema(description = "ISO 4217 currency code", example = "SGD")
    @JsonProperty("currency")
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 uppercase letters")
    private String currency;

    @Schema(description = "Amount to check (negative for debits, positive for credits)", 
            example = "-50000.00")
    @JsonProperty("amount")
    @NotBlank(message = "Amount is required")
    @Pattern(regexp = "^-?[0-9]+(\\.[0-9]{1,5})?$", message = "Amount must be numeric with up to 5 decimal places")
    private String amount;

    @Schema(description = "Type of transaction being checked", 
            example = "DEBIT", allowableValues = {"DEBIT", "CREDIT"})
    @JsonProperty("transactionType")
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @Schema(description = "Optional reference for tracking the check request", 
            example = "TXN-CHECK-001")
    @JsonProperty("reference")
    @Size(max = 35, message = "Reference cannot exceed 35 characters")
    private String reference;

    // Constructors
    public BalanceCheckRequest() {}

    public BalanceCheckRequest(String countryCode, String currency, String amount, TransactionType transactionType) {
        this.countryCode = countryCode;
        this.currency = currency;
        this.amount = amount;
        this.transactionType = transactionType;
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

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public String toString() {
        return "BalanceCheckRequest{" +
                "countryCode='" + countryCode + '\'' +
                ", participantId='" + participantId + '\'' +
                ", currency='" + currency + '\'' +
                ", amount='" + amount + '\'' +
                ", transactionType=" + transactionType +
                ", reference='" + reference + '\'' +
                '}';
    }
}