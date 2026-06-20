package com.eventledger.gateway.service;



import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.dto.AccountTransactionRequest;
import com.eventledger.gateway.dto.BalanceResponse;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.entity.EventEntity;
import com.eventledger.gateway.exception.EventNotFoundException;
import com.eventledger.gateway.mapper.EventMapper;
import com.eventledger.gateway.repository.EventRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class EventServiceImpl implements EventService {

	private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

	private final EventRepository eventRepository;
    private final AccountServiceClient accountServiceClient;
    private final EventMapper eventMapper;
    private final Counter eventsSubmittedCounter;
    private final Counter eventsDuplicateCounter;
    private final Counter eventsErrorCounter;

    public EventServiceImpl(
            EventRepository eventRepository,
            AccountServiceClient accountServiceClient,
            EventMapper eventMapper,
            MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.accountServiceClient = accountServiceClient;
        this.eventMapper = eventMapper;
        this.eventsSubmittedCounter = Counter.builder("events.submitted.total")
                .description("Total events successfully submitted")
                .register(meterRegistry);
        this.eventsDuplicateCounter = Counter.builder("events.duplicate.total")
                .description("Total duplicate events received")
                .register(meterRegistry);
        this.eventsErrorCounter = Counter.builder("events.error.total")
                .description("Total event processing errors")
                .register(meterRegistry);
    }

	@Override
	@Transactional
	public EventSubmissionResult submitEvent(EventRequest request) {
		 log.info("Processing event: eventId={}, accountId={}, type={}, amount={}",
	                request.eventId(), request.accountId(), request.type(), request.amount());

		     //check existing event or not
	        Optional<EventEntity> existing = eventRepository.findByEventId(request.eventId());
	        if (existing.isPresent()) {
	            log.info("Duplicate event detected, returning existing: eventId={}", request.eventId());
	            eventsDuplicateCounter.increment();
	            return new EventSubmissionResult(eventMapper.toResponse(existing.get()), false);
	        }
	        //convert event request to Account transaction request 
	        AccountTransactionRequest txRequest = eventMapper.toTransactionRequest(request);

	        try {
	        	//calling account service to save the transaction in the account service
	            accountServiceClient.processTransaction(request.accountId(), txRequest);
	            log.info("Account Service processed transaction: eventId={}", request.eventId());
	        } catch (Exception e) {
	            eventsErrorCounter.increment();
	            log.error("Account Service call failed: eventId={}, error={}", request.eventId(), e.getMessage());
	            throw e;
	        }
	        //if it is the new event save event 
	        EventEntity entity = eventMapper.toEntity(request);
	        EventEntity saved = eventRepository.save(entity);
	        eventsSubmittedCounter.increment();
	        log.info("Event stored: eventId={}, id={}", saved.getEventId(), saved.getId());

	        return new EventSubmissionResult(eventMapper.toResponse(saved), true);
	}

	@Override
    @Transactional(readOnly = true)
    public EventResponse getEvent(String eventId) {
        log.info("Fetching event: eventId={}", eventId);
        return eventRepository.findByEventId(eventId)
                .map(eventMapper::toResponse)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByAccount(String accountId) {
        log.info("Fetching events for account: accountId={}", accountId);
        return eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId)
                .stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BalanceResponse getBalance(String accountId) {
        log.info("Fetching balance for account: accountId={}", accountId);
        return accountServiceClient.getBalance(accountId);
    }
}
