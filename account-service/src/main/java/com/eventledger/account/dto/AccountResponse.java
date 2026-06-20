package com.eventledger.account.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        String accountId,
        BigDecimal currentBalance,
        String currency,
        Instant createdAt
) {}
