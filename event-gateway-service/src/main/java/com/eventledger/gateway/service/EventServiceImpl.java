package com.eventledger.gateway.service;



import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventledger.gateway.dto.AccountTransactionRequest;
import com.eventledger.gateway.dto.BalanceResponse;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;

@Service
public class EventServiceImpl implements EventService {

	private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

	@Override
	@Transactional
	public EventSubmissionResult submitEvent(EventRequest request) {


	return null;
	}

	@Override
    @Transactional(readOnly = true)
    public EventResponse getEvent(String eventId) {
       
        return null;
    }

	@Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByAccount(String accountId) {
        
        return null;
    }

	@Override
    public BalanceResponse getBalance(String accountId) {
        
        return null;
    }
}
