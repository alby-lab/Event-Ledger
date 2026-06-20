package com.eventledger.account.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    CREDIT, DEBIT;

    @JsonCreator
    public static TransactionType fromValue(String value) {
        if (value == null) throw new IllegalArgumentException("Transaction type must not be null");
        for (TransactionType t : values()) {
            if (t.name().equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("Invalid transaction type: " + value + ". Must be CREDIT or DEBIT");
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
