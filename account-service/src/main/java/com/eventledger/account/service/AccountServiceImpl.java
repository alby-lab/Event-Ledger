package com.eventledger.account.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventledger.account.dto.AccountResponse;
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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class AccountServiceImpl implements AccountService {

	private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final AccountMapper accountMapper;
	private final TransactionMapper transactionMapper;
	private final Counter transactionsProcessedCounter;
	private final Counter transactionsDuplicateCounter;

	public AccountServiceImpl(AccountRepository accountRepository, TransactionRepository transactionRepository,
			AccountMapper accountMapper, TransactionMapper transactionMapper, MeterRegistry meterRegistry) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.accountMapper = accountMapper;
		this.transactionMapper = transactionMapper;
		this.transactionsProcessedCounter = Counter.builder("transactions.processed.total")
				.description("Total transactions processed").register(meterRegistry);
		this.transactionsDuplicateCounter = Counter.builder("transactions.duplicate.total")
				.description("Total duplicate transactions skipped").register(meterRegistry);
	}

	@Override
	@Transactional
	public TransactionResponse processTransaction(String accountId, TransactionRequest request) {

		log.info("Processing transaction: eventId={}, accountId={}, type={}, amount={}", request.eventId(), accountId,
				request.type(), request.amount());
		// check the event is exit or not
		Optional<TransactionEntity> existing = transactionRepository.findByEventId(request.eventId());
		if (existing.isPresent()) {
			log.info("Duplicate transaction skipped: eventId={}", request.eventId());
			transactionsDuplicateCounter.increment();
			return transactionMapper.toResponse(existing.get());
		}

		TransactionType type = TransactionType.fromValue(request.type());
		String currency = request.currency() != null ? request.currency() : "USD";
		// if it is new event save in to the db
		AccountEntity account = accountRepository.findByAccountId(accountId).orElseGet(() -> {
			log.info("Auto-creating account: accountId={}", accountId);
			return accountRepository.save(AccountEntity.builder().accountId(accountId).currentBalance(BigDecimal.ZERO)
					.currency(currency).build());
		});
		// calculate balance
		BigDecimal newBalance = type == TransactionType.CREDIT ? account.getCurrentBalance().add(request.amount())
				: account.getCurrentBalance().subtract(request.amount());

		account.setCurrentBalance(newBalance);
		accountRepository.save(account);
		// create new transaction
		TransactionEntity transaction = TransactionEntity.builder().eventId(request.eventId()).accountId(accountId)
				.type(type).amount(request.amount()).eventTimestamp(request.eventTimestamp()).build();

		TransactionEntity saved = transactionRepository.save(transaction);
		transactionsProcessedCounter.increment();

		log.info("Transaction recorded: eventId={}, newBalance={}", request.eventId(), newBalance);
		return transactionMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public BalanceResponse getBalance(String accountId) {
		log.info("Fetching balance: accountId={}", accountId);
		AccountEntity account = accountRepository.findByAccountId(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));
		return accountMapper.toBalanceResponse(account);
	}

	@Override
	@Transactional(readOnly = true)
	public AccountResponse getAccount(String accountId) {
		log.info("Fetching account: accountId={}", accountId);
		AccountEntity account = accountRepository.findByAccountId(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));
		return accountMapper.toResponse(account);
	}
}
