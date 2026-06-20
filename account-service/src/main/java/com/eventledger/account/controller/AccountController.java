package com.eventledger.account.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventledger.account.dto.AccountResponse;
import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionRequest;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.service.AccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
     
    @Autowired
    private AccountService accountService;

    
    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<TransactionResponse> processTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody TransactionRequest request) {
    	 log.info("POST /accounts/{}/transactions - eventId={}", accountId, request.eventId());
         TransactionResponse response = accountService.processTransaction(accountId, request);
       
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
    	 log.info("GET /accounts/{}/balance", accountId);
       
        return ResponseEntity.ok(accountService.getBalance(accountId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
    	log.info("GET /accounts/{}", accountId);
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }
}
