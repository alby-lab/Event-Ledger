package com.eventledger.gateway.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eventledger.gateway.dto.BalanceResponse;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.service.EventService;
import com.eventledger.gateway.service.EventSubmissionResult;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/events")
public class EventController {

	private static final Logger log = LoggerFactory.getLogger(EventController.class);

	@Autowired
	private EventService eventService;

	@PostMapping
	public ResponseEntity<EventResponse> submitEvent(@Valid @RequestBody EventRequest request) {

		log.info("POST /events - eventId={}", request.eventId());
		EventSubmissionResult result = eventService.submitEvent(request);
		HttpStatus status = result.isNew() ? HttpStatus.CREATED : HttpStatus.OK;

		return ResponseEntity.status(status).body(result.event());
	}

	@GetMapping("/{eventId}")
	public ResponseEntity<EventResponse> getEvent(@PathVariable String eventId) {
		log.info("GET /events/{}", eventId);
		return ResponseEntity.ok(eventService.getEvent(eventId));
	}

	@GetMapping
	public ResponseEntity<List<EventResponse>> getEventsByAccount(@RequestParam("account") String accountId) {

		log.info("GET /events?account={}", accountId);
		return ResponseEntity.ok(eventService.getEventsByAccount(accountId));
	}

	@GetMapping("/accounts/{accountId}/balance")
	public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
		log.info("GET /accounts/{}/balance via gateway", accountId);
		return ResponseEntity.ok(eventService.getBalance(accountId));
	}
}
