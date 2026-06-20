package com.eventledger.gateway.dto;

import java.math.BigDecimal;

public record BalanceResponse(
        String accountId,
        BigDecimal balance,
        String currency
) {}
