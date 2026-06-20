package com.eventledger.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(
        @NotBlank(message = "eventId is mandatory") String eventId,
        @NotBlank(message = "accountId is mandatory") String accountId,
        @NotBlank(message = "type is mandatory") String type,
        @NotNull(message = "amount is mandatory")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero") BigDecimal amount,
        @NotNull(message = "eventTimestamp is mandatory") Instant eventTimestamp,
        String currency
) {}
