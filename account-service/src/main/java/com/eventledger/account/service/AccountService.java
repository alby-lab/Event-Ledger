package com.eventledger.account.service;

import com.eventledger.account.dto.AccountResponse;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionRequest;
import com.eventledger.account.dto.TransactionResponse;

public interface AccountService {
    TransactionResponse processTransaction(String accountId, TransactionRequest request);
    BalanceResponse getBalance(String accountId);
    AccountResponse getAccount(String accountId);
}
