package com.eventledger.gateway.controller;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.dto.AccountTransactionResponse;
import com.eventledger.gateway.dto.EventType;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import com.eventledger.gateway.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EventControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EventRepository eventRepository;
    @MockBean  private AccountServiceClient accountServiceClient;

    private static final String ACCOUNT_ID = "acct-test-123";

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        when(accountServiceClient.processTransaction(any(), any()))
                .thenReturn(new AccountTransactionResponse(1L, "evt-001", ACCOUNT_ID, "CREDIT",
                        BigDecimal.valueOf(150), Instant.now(), Instant.now()));
    }

    private String buildEventJson(String eventId, String type, String amount) {
        return """
                {
                    "eventId": "%s",
                    "accountId": "%s",
                    "type": "%s",
                    "amount": %s,
                    "currency": "USD",
                    "eventTimestamp": "2026-05-15T14:02:11Z",
                    "metadata": {"source": "test"}
                }
                """.formatted(eventId, ACCOUNT_ID, type, amount);
    }

    @Test
    @DisplayName("POST /events returns 201 for a new valid event")
    void submitEvent_valid_returns201() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildEventJson("evt-001", "CREDIT", "150.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(150.0));
    }

    @Test
    @DisplayName("POST /events returns 200 for duplicate eventId (idempotency)")
    void submitEvent_duplicate_returns200() throws Exception {
        String body = buildEventJson("evt-dup", "CREDIT", "100.00");
        when(accountServiceClient.processTransaction(any(), any()))
                .thenReturn(new AccountTransactionResponse(1L, "evt-dup", ACCOUNT_ID, "CREDIT",
                        BigDecimal.valueOf(100), Instant.now(), Instant.now()));

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-dup"));

        verify(accountServiceClient, times(1)).processTransaction(any(), any());
    }

    @Test
    @DisplayName("POST /events returns 400 when eventId is missing")
    void submitEvent_missingEventId_returns400() throws Exception {
        String body = """
                {"accountId":"acct-x","type":"CREDIT","amount":100,"currency":"USD","eventTimestamp":"2026-01-01T00:00:00Z"}
                """;
        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details", hasItem(containsString("eventId"))));
    }

    @Test
    @DisplayName("POST /events returns 400 when amount is zero")
    void submitEvent_zeroAmount_returns400() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildEventJson("evt-zero", "CREDIT", "0.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /events returns 400 when type is invalid")
    void submitEvent_invalidType_returns400() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildEventJson("evt-bad", "TRANSFER", "100.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events returns 503 when Account Service is unavailable")
    void submitEvent_accountServiceDown_returns503() throws Exception {
        when(accountServiceClient.processTransaction(any(), any()))
                .thenThrow(new AccountServiceUnavailableException("Circuit open"));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildEventJson("evt-fail", "CREDIT", "200.00")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("ACCOUNT_SERVICE_UNAVAILABLE"));
    }

    @Test
    @DisplayName("GET /events/{eventId} returns 200 for existing event")
    void getEvent_found_returns200() throws Exception {
        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
                .content(buildEventJson("evt-get-1", "CREDIT", "75.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/events/evt-get-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-get-1"))
                .andExpect(jsonPath("$.amount").value(75.0));
    }

    @Test
    @DisplayName("GET /events/{eventId} returns 404 for unknown event")
    void getEvent_notFound_returns404() throws Exception {
        mockMvc.perform(get("/events/nonexistent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("EVENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /events?account returns events sorted by eventTimestamp")
    void getEventsByAccount_returnsSortedEvents() throws Exception {
        when(accountServiceClient.processTransaction(any(), any()))
                .thenReturn(new AccountTransactionResponse(1L, "e", ACCOUNT_ID, "CREDIT",
                        BigDecimal.ONE, Instant.now(), Instant.now()));

        String late = """
                {"eventId":"e-late","accountId":"%s","type":"CREDIT","amount":50,"currency":"USD",
                 "eventTimestamp":"2026-06-01T12:00:00Z"}""".formatted(ACCOUNT_ID);
        String early = """
                {"eventId":"e-early","accountId":"%s","type":"DEBIT","amount":10,"currency":"USD",
                 "eventTimestamp":"2026-01-01T08:00:00Z"}""".formatted(ACCOUNT_ID);

        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(late))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(early))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/events").param("account", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventId").value("e-early"))
                .andExpect(jsonPath("$[1].eventId").value("e-late"));
    }

    @Test
    @DisplayName("GET /health returns 200 UP")
    void health_returns200() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("event-gateway-service"));
    }
}
