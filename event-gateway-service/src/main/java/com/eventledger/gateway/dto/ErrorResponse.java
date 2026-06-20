package com.eventledger.gateway.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String message,
        String error,
        int status,
        Instant timestamp,
        List<String> details
) {
    public ErrorResponse(String message, String error, int status) {
        this(message, error, status, Instant.now(), null);
    }

    public ErrorResponse(String message, String error, int status, List<String> details) {
        this(message, error, status, Instant.now(), details);
    }
}
