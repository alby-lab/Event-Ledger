package com.eventledger.account.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventledger.account.dto.AccountResponse;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionRequest;
import com.eventledger.account.dto.TransactionResponse;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

   

    @Override
    @Transactional
    public TransactionResponse processTransaction(String accountId, TransactionRequest request) {

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId) {
       
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
       
        return null;
    }
}
