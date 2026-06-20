package com.eventledger.gateway.client;

import com.eventledger.gateway.dto.AccountTransactionRequest;
import com.eventledger.gateway.dto.AccountTransactionResponse;
import com.eventledger.gateway.dto.BalanceResponse;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountServiceClientFallbackFactory implements FallbackFactory<AccountServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClientFallbackFactory.class);

    @Override
    public AccountServiceClient create(Throwable cause) {
        log.error("Account Service fallback triggered: {}", cause.getMessage());
        return new AccountServiceClient() {
            @Override
            public AccountTransactionResponse processTransaction(String accountId,
                                                                 AccountTransactionRequest request) {
                // 4xx = business error from Account Service (e.g. bad request) — rethrow as-is
                if (cause instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500) {
                    throw fe;
                }
                throw new AccountServiceUnavailableException(
                        "Account Service unavailable: " + cause.getMessage(), cause);
            }

            @Override
            public BalanceResponse getBalance(String accountId) {
                // 4xx = business error (e.g. account not found) — rethrow so caller gets proper error
                if (cause instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500) {
                    throw fe;
                }
                throw new AccountServiceUnavailableException(
                        "Account Service unavailable: " + cause.getMessage(), cause);
            }
        };
    }
}
