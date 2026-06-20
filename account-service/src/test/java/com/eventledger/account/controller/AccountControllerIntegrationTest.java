package com.eventledger.account.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;

    private static final String ACCOUNT_ID = "acct-intg-001";

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private String txJson(String eventId, String type, String amount) {
        return """
                {"eventId":"%s","accountId":"%s","type":"%s","amount":%s,
                 "eventTimestamp":"2026-01-15T10:00:00Z","currency":"USD"}
                """.formatted(eventId, ACCOUNT_ID, type, amount);
    }

    @Test
    @DisplayName("POST /accounts/{id}/transactions processes CREDIT and returns 201")
    void processTransaction_credit_returns201() throws Exception {
        mockMvc.perform(post("/accounts/{id}/transactions", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txJson("evt-001", "CREDIT", "100.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(100.0));
    }

    @Test
    @DisplayName("Duplicate transaction (same eventId) is idempotent")
    void processTransaction_duplicate_idempotent() throws Exception {
        String body = txJson("evt-dup", "CREDIT", "50.00");

        mockMvc.perform(post("/accounts/{id}/transactions", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Second call with same eventId
        mockMvc.perform(post("/accounts/{id}/transactions", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-dup"));

        // Balance should only reflect one transaction
        mockMvc.perform(get("/accounts/{id}/balance", ACCOUNT_ID))
                .andExpect(jsonPath("$.balance").value(50.0));
    }

    @Test
    @DisplayName("Balance reflects CREDIT minus DEBIT")
    void getBalance_reflectsCreditMinusDebit() throws Exception {
        mockMvc.perform(post("/accounts/{id}/transactions", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txJson("evt-c1", "CREDIT", "300.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{id}/transactions", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txJson("evt-d1", "DEBIT", "80.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/{id}/balance", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.balance").value(220.0))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("GET /accounts/{id} returns account details")
    void getAccount_returnsDetails() throws Exception {
        mockMvc.perform(post("/accounts/{id}/transactions", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(txJson("evt-init", "CREDIT", "500.00")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/{id}", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.currentBalance").value(500.0))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("GET /accounts/{id} returns 404 for unknown account")
    void getAccount_notFound_returns404() throws Exception {
        mockMvc.perform(get("/accounts/nonexistent-acct"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /accounts/{id}/balance returns 404 for unknown account")
    void getBalance_notFound_returns404() throws Exception {
        mockMvc.perform(get("/accounts/no-such-account/balance"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /accounts/{id}/transactions returns 400 for invalid request")
    void processTransaction_missingEventId_returns400() throws Exception {
        String badBody = """
                {"accountId":"%s","type":"CREDIT","amount":100,"eventTimestamp":"2026-01-01T00:00:00Z"}
                """.formatted(ACCOUNT_ID);

        mockMvc.perform(post("/accounts/{id}/transactions", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON).content(badBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Out-of-order events produce correct balance")
    void outOfOrderEvents_correctBalance() throws Exception {
        // Send "later" event first
        String later = """
                {"eventId":"evt-later","accountId":"%s","type":"CREDIT","amount":200,
                 "eventTimestamp":"2026-06-01T12:00:00Z","currency":"USD"}""".formatted(ACCOUNT_ID);
        // Then "earlier" event
        String earlier = """
                {"eventId":"evt-earlier","accountId":"%s","type":"DEBIT","amount":50,
                 "eventTimestamp":"2026-01-01T08:00:00Z","currency":"USD"}""".formatted(ACCOUNT_ID);

        mockMvc.perform(post("/accounts/{id}/transactions", ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON).content(later)).andExpect(status().isCreated());
        mockMvc.perform(post("/accounts/{id}/transactions", ACCOUNT_ID)
                .contentType(MediaType.APPLICATION_JSON).content(earlier)).andExpect(status().isCreated());

        // Balance: 200 - 50 = 150
        mockMvc.perform(get("/accounts/{id}/balance", ACCOUNT_ID))
                .andExpect(jsonPath("$.balance").value(150.0));
    }

    @Test
    @DisplayName("GET /health returns 200 UP")
    void health_returns200() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("account-service"));
    }
}
