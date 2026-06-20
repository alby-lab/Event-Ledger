package com.eventledger.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record EventRequest(
        @NotBlank(message = "eventId is mandatory")
        @JsonProperty("eventId") String eventId,

        @NotBlank(message = "accountId is mandatory")
        @JsonProperty("accountId") String accountId,

        @NotNull(message = "type is mandatory")
        @JsonProperty("type") EventType type,

        @NotNull(message = "amount is mandatory")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        @JsonProperty("amount") BigDecimal amount,

        @NotBlank(message = "currency is mandatory")
        @JsonProperty("currency") String currency,

        @NotNull(message = "eventTimestamp is mandatory")
        @JsonProperty("eventTimestamp") Instant eventTimestamp,

        @JsonProperty("metadata") Map<String, String> metadata
) {}
