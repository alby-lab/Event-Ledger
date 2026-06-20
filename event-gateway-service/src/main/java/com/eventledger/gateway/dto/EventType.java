package com.eventledger.gateway.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType {
    CREDIT, DEBIT;

    @JsonCreator
    public static EventType fromValue(String value) {
        if (value == null) throw new IllegalArgumentException("Event type must not be null");
        for (EventType type : values()) {
            if (type.name().equalsIgnoreCase(value)) return type;
        }
        throw new IllegalArgumentException("Invalid event type: " + value + ". Must be CREDIT or DEBIT");
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
