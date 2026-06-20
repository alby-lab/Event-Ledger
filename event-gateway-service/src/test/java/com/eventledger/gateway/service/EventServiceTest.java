package com.eventledger.gateway.service;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.dto.AccountTransactionResponse;
import com.eventledger.gateway.dto.EventRequest;
import com.eventledger.gateway.dto.EventResponse;
import com.eventledger.gateway.dto.EventType;
import com.eventledger.gateway.entity.EventEntity;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import com.eventledger.gateway.exception.EventNotFoundException;
import com.eventledger.gateway.mapper.EventMapper;
import com.eventledger.gateway.repository.EventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private AccountServiceClient accountServiceClient;

    private EventServiceImpl eventService;

    private static final String EVENT_ID = "evt-001";
    private static final String ACCOUNT_ID = "acct-123";

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(
                eventRepository,
                accountServiceClient,
                new EventMapper(),
                new SimpleMeterRegistry()
        );
    }

    private EventRequest buildRequest(String eventId, EventType type, BigDecimal amount) {
        return new EventRequest(eventId, ACCOUNT_ID, type, amount, "USD",
                Instant.parse("2026-05-15T14:02:11Z"), Map.of("source", "test"));
    }

    private EventEntity buildEntity(String eventId) {
        return EventEntity.builder()
                .id(1L).eventId(eventId).accountId(ACCOUNT_ID)
                .type(EventType.CREDIT).amount(BigDecimal.valueOf(150))
                .currency("USD").eventTimestamp(Instant.parse("2026-05-15T14:02:11Z"))
                .createdAt(Instant.now()).build();
    }

    @Test
    @DisplayName("New event is submitted, stored, and marked as new")
    void submitEvent_newEvent_success() {
        EventRequest request = buildRequest(EVENT_ID, EventType.CREDIT, BigDecimal.valueOf(150));
        EventEntity saved = buildEntity(EVENT_ID);

        when(eventRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(accountServiceClient.processTransaction(any(), any()))
                .thenReturn(new AccountTransactionResponse(1L, EVENT_ID, ACCOUNT_ID, "CREDIT",
                        BigDecimal.valueOf(150), request.eventTimestamp(), Instant.now()));
        when(eventRepository.save(any())).thenReturn(saved);

        EventSubmissionResult result = eventService.submitEvent(request);

        assertThat(result.isNew()).isTrue();
        assertThat(result.event().eventId()).isEqualTo(EVENT_ID);
        verify(accountServiceClient).processTransaction(eq(ACCOUNT_ID), any());
        verify(eventRepository).save(any());
    }

    @Test
    @DisplayName("Duplicate eventId returns existing event without re-processing")
    void submitEvent_duplicate_returnsExisting() {
        EventRequest request = buildRequest(EVENT_ID, EventType.CREDIT, BigDecimal.valueOf(150));
        EventEntity existing = buildEntity(EVENT_ID);

        when(eventRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(existing));

        EventSubmissionResult result = eventService.submitEvent(request);

        assertThat(result.isNew()).isFalse();
        assertThat(result.event().eventId()).isEqualTo(EVENT_ID);
        verifyNoInteractions(accountServiceClient);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Account Service failure prevents event storage and propagates exception")
    void submitEvent_accountServiceDown_throwsException() {
        EventRequest request = buildRequest(EVENT_ID, EventType.CREDIT, BigDecimal.valueOf(150));

        when(eventRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(accountServiceClient.processTransaction(any(), any()))
                .thenThrow(new AccountServiceUnavailableException("Account Service unavailable"));

        assertThatThrownBy(() -> eventService.submitEvent(request))
                .isInstanceOf(AccountServiceUnavailableException.class)
                .hasMessageContaining("Account Service unavailable");
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("getEvent returns stored event by eventId")
    void getEvent_found() {
        EventEntity entity = buildEntity(EVENT_ID);
        when(eventRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(entity));

        EventResponse response = eventService.getEvent(EVENT_ID);

        assertThat(response.eventId()).isEqualTo(EVENT_ID);
        assertThat(response.accountId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    @DisplayName("getEvent throws EventNotFoundException for unknown eventId")
    void getEvent_notFound_throwsException() {
        when(eventRepository.findByEventId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent("unknown"))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    @DisplayName("getEventsByAccount returns events sorted by eventTimestamp ascending")
    void getEventsByAccount_sortedByTimestamp() {
        Instant t1 = Instant.parse("2026-01-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T12:00:00Z");
        Instant t3 = Instant.parse("2026-01-01T08:00:00Z");

        EventEntity e1 = EventEntity.builder().id(1L).eventId("e1").accountId(ACCOUNT_ID)
                .type(EventType.CREDIT).amount(BigDecimal.TEN).currency("USD")
                .eventTimestamp(t1).createdAt(Instant.now()).build();
        EventEntity e2 = EventEntity.builder().id(2L).eventId("e2").accountId(ACCOUNT_ID)
                .type(EventType.DEBIT).amount(BigDecimal.ONE).currency("USD")
                .eventTimestamp(t2).createdAt(Instant.now()).build();
        EventEntity e3 = EventEntity.builder().id(3L).eventId("e3").accountId(ACCOUNT_ID)
                .type(EventType.CREDIT).amount(BigDecimal.valueOf(5)).currency("USD")
                .eventTimestamp(t3).createdAt(Instant.now()).build();

        // Repository returns already sorted by eventTimestamp ASC
        when(eventRepository.findByAccountIdOrderByEventTimestampAsc(ACCOUNT_ID))
                .thenReturn(List.of(e3, e1, e2));

        List<EventResponse> events = eventService.getEventsByAccount(ACCOUNT_ID);

        assertThat(events).hasSize(3);
        assertThat(events.get(0).eventTimestamp()).isEqualTo(t3);
        assertThat(events.get(1).eventTimestamp()).isEqualTo(t1);
        assertThat(events.get(2).eventTimestamp()).isEqualTo(t2);
    }

    @Test
    @DisplayName("DEBIT event is submitted successfully")
    void submitEvent_debit_success() {
        EventRequest request = buildRequest("evt-debit", EventType.DEBIT, BigDecimal.valueOf(50));
        EventEntity saved = EventEntity.builder()
                .id(2L).eventId("evt-debit").accountId(ACCOUNT_ID)
                .type(EventType.DEBIT).amount(BigDecimal.valueOf(50))
                .currency("USD").eventTimestamp(request.eventTimestamp())
                .createdAt(Instant.now()).build();

        when(eventRepository.findByEventId("evt-debit")).thenReturn(Optional.empty());
        when(accountServiceClient.processTransaction(any(), any()))
                .thenReturn(new AccountTransactionResponse(2L, "evt-debit", ACCOUNT_ID, "DEBIT",
                        BigDecimal.valueOf(50), request.eventTimestamp(), Instant.now()));
        when(eventRepository.save(any())).thenReturn(saved);

        EventSubmissionResult result = eventService.submitEvent(request);

        assertThat(result.isNew()).isTrue();
        assertThat(result.event().type()).isEqualTo(EventType.DEBIT);
    }
}
