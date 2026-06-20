package com.eventledger.gateway.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountTransactionRequest(
        String eventId,
        String accountId,
        String type,
        BigDecimal amount,
        Instant eventTimestamp,
        String currency
) {}
