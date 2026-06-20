package com.eventledger.gateway.resiliency;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.exception.AccountServiceUnavailableException;
import com.eventledger.gateway.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ResiliencyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EventRepository eventRepository;
    @MockBean  private AccountServiceClient accountServiceClient;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    private String eventJson(String eventId) {
        return """
                {"eventId":"%s","accountId":"acct-res","type":"CREDIT","amount":100,
                 "currency":"USD","eventTimestamp":"2026-01-01T10:00:00Z"}""".formatted(eventId);
    }

    @Test
    @DisplayName("Account Service failure returns 503 — not a 500")
    void accountServiceFailure_returns503() throws Exception {
        when(accountServiceClient.processTransaction(any(), any()))
                .thenThrow(new AccountServiceUnavailableException("Service down"));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("evt-res-1")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("ACCOUNT_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Events are not stored when Account Service is unavailable")
    void eventNotStoredWhenAccountServiceDown() throws Exception {
        when(accountServiceClient.processTransaction(any(), any()))
                .thenThrow(new AccountServiceUnavailableException("Service down"));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("evt-not-stored")))
                .andExpect(status().isServiceUnavailable());

        // Event must NOT be stored — GET should return 404
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getReq =
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/events/evt-not-stored");
        mockMvc.perform(getReq).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Circuit breaker: repeated failures all return 503 (no 500)")
    void repeatedAccountServiceFailures_allReturn503() throws Exception {
        when(accountServiceClient.processTransaction(any(), any()))
                .thenThrow(new AccountServiceUnavailableException("Timeout"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(eventJson("evt-cb-" + i)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503));
        }
    }

    @Test
    @DisplayName("GET /events continues working when Account Service is down")
    void getEvents_continuesWhenAccountServiceDown() throws Exception {
        // Account service is unavailable — GET endpoints must still work
        when(accountServiceClient.processTransaction(any(), any()))
                .thenThrow(new AccountServiceUnavailableException("Down"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/events").param("account", "acct-res"))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/health"))
                .andExpect(status().isOk());
    }
}
