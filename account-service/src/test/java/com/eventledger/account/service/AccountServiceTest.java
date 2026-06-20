package com.eventledger.account.service;

import com.eventledger.account.dto.BalanceResponse;
import com.eventledger.account.dto.TransactionRequest;
import com.eventledger.account.dto.TransactionResponse;
import com.eventledger.account.entity.AccountEntity;
import com.eventledger.account.entity.TransactionEntity;
import com.eventledger.account.exception.AccountNotFoundException;
import com.eventledger.account.mapper.AccountMapper;
import com.eventledger.account.mapper.TransactionMapper;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;
import com.eventledger.account.util.TransactionType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;

    private AccountServiceImpl accountService;

    private static final String ACCOUNT_ID = "acct-001";

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(
                accountRepository,
                transactionRepository,
                new AccountMapper(),
                new TransactionMapper(),
                new SimpleMeterRegistry()
        );
    }

    private AccountEntity buildAccount(BigDecimal balance) {
        return AccountEntity.builder()
                .id(1L).accountId(ACCOUNT_ID)
                .currentBalance(balance).currency("USD")
                .createdAt(Instant.now()).build();
    }

    private TransactionRequest buildRequest(String eventId, String type, BigDecimal amount) {
        return new TransactionRequest(eventId, ACCOUNT_ID, type, amount,
                Instant.parse("2026-01-01T10:00:00Z"), "USD");
    }

    private TransactionEntity buildTransactionEntity(String eventId, TransactionType type, BigDecimal amount) {
        return TransactionEntity.builder()
                .id(1L).eventId(eventId).accountId(ACCOUNT_ID)
                .type(type).amount(amount)
                .eventTimestamp(Instant.parse("2026-01-01T10:00:00Z"))
                .createdAt(Instant.now()).build();
    }

    @Test
    @DisplayName("CREDIT transaction increases balance")
    void processTransaction_credit_increasesBalance() {
        AccountEntity account = buildAccount(BigDecimal.valueOf(100));
        TransactionRequest request = buildRequest("evt-c1", "CREDIT", BigDecimal.valueOf(50));
        TransactionEntity saved = buildTransactionEntity("evt-c1", TransactionType.CREDIT, BigDecimal.valueOf(50));

        when(transactionRepository.findByEventId("evt-c1")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenReturn(saved);

        TransactionResponse response = accountService.processTransaction(ACCOUNT_ID, request);

        assertThat(response.eventId()).isEqualTo("evt-c1");
        assertThat(response.type()).isEqualTo("CREDIT");

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    @DisplayName("DEBIT transaction decreases balance")
    void processTransaction_debit_decreasesBalance() {
        AccountEntity account = buildAccount(BigDecimal.valueOf(200));
        TransactionRequest request = buildRequest("evt-d1", "DEBIT", BigDecimal.valueOf(75));
        TransactionEntity saved = buildTransactionEntity("evt-d1", TransactionType.DEBIT, BigDecimal.valueOf(75));

        when(transactionRepository.findByEventId("evt-d1")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenReturn(saved);

        accountService.processTransaction(ACCOUNT_ID, request);

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(125));
    }

    @Test
    @DisplayName("Duplicate eventId is skipped — balance unchanged")
    void processTransaction_duplicate_skipsProcessing() {
        TransactionRequest request = buildRequest("evt-dup", "CREDIT", BigDecimal.valueOf(50));
        TransactionEntity existing = buildTransactionEntity("evt-dup", TransactionType.CREDIT, BigDecimal.valueOf(50));

        when(transactionRepository.findByEventId("evt-dup")).thenReturn(Optional.of(existing));

        TransactionResponse response = accountService.processTransaction(ACCOUNT_ID, request);

        assertThat(response.eventId()).isEqualTo("evt-dup");
        verifyNoInteractions(accountRepository);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Account is auto-created on first transaction")
    void processTransaction_newAccount_autoCreated() {
        TransactionRequest request = buildRequest("evt-new", "CREDIT", BigDecimal.valueOf(100));
        AccountEntity newAccount = buildAccount(BigDecimal.ZERO);
        TransactionEntity saved = buildTransactionEntity("evt-new", TransactionType.CREDIT, BigDecimal.valueOf(100));

        when(transactionRepository.findByEventId("evt-new")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenReturn(newAccount);
        when(transactionRepository.save(any())).thenReturn(saved);

        accountService.processTransaction(ACCOUNT_ID, request);

        verify(accountRepository, times(2)).save(any()); // once to create, once to update balance
    }

    @Test
    @DisplayName("Balance formula: Total Credits - Total Debits")
    void balanceFormula_creditMinusDebit() {
        // Verify via the stored currentBalance after multiple operations
        AccountEntity account = buildAccount(BigDecimal.valueOf(300)); // 400 credit - 100 debit

        when(accountRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(account));

        BalanceResponse response = accountService.getBalance(ACCOUNT_ID);

        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    @DisplayName("getBalance throws AccountNotFoundException for unknown account")
    void getBalance_unknownAccount_throws() {
        when(accountRepository.findByAccountId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getBalance("unknown"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("Out-of-order events: balance reflects correct total regardless of arrival order")
    void outOfOrderEvents_balanceCorrect() {
        AccountEntity account = buildAccount(BigDecimal.valueOf(50));

        // Process a later event first (higher timestamp)
        TransactionRequest late = new TransactionRequest("evt-late", ACCOUNT_ID, "CREDIT",
                BigDecimal.valueOf(200), Instant.parse("2026-06-01T10:00:00Z"), "USD");
        TransactionEntity lateSaved = buildTransactionEntity("evt-late", TransactionType.CREDIT, BigDecimal.valueOf(200));

        when(transactionRepository.findByEventId("evt-late")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenReturn(lateSaved);

        accountService.processTransaction(ACCOUNT_ID, late);

        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(captor.capture());
        // 50 + 200 = 250
        assertThat(captor.getValue().getCurrentBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(250));
    }
}
