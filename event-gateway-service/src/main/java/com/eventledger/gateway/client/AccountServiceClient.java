package com.eventledger.gateway.client;

import com.eventledger.gateway.dto.AccountTransactionRequest;
import com.eventledger.gateway.dto.AccountTransactionResponse;
import com.eventledger.gateway.dto.BalanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "account-service",
        url = "${account.service.url:http://localhost:8081}",
        fallbackFactory = AccountServiceClientFallbackFactory.class
)
public interface AccountServiceClient {

    @PostMapping("/accounts/{accountId}/transactions")
    AccountTransactionResponse processTransaction(
            @PathVariable("accountId") String accountId,
            @RequestBody AccountTransactionRequest request
    );

    @GetMapping("/accounts/{accountId}/balance")
    BalanceResponse getBalance(@PathVariable("accountId") String accountId);
}
