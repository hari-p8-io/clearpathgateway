package com.anz.fastpayment.liquidity.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Transaction Type Enumeration
 * 
 * Defines the types of liquidity transactions supported by the system
 */
public enum TransactionType {
    DEBIT("DEBIT"),
    CREDIT("CREDIT"),
    RESERVE("RESERVE"),
    RELEASE("RELEASE");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static TransactionType fromValue(String value) {
        for (TransactionType type : TransactionType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + value);
    }
}