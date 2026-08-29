package com.emailmessenger.api;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/v1/health is the mobile client's liveness probe. Public
 * (no auth), CSRF-exempt, returns the {status, version, time} envelope
 * documented in docs/API.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ReplyService replyService;
    @MockitoBean EmailThreadRepository threadRepository;
    @MockitoBean BillingService billingService;

    @Test
    void returnsOkStatusVersionAndTime() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.version").isString())
                .andExpect(jsonPath("$.time").isString());
    }

    @Test
    void isReachableWithoutAuthOrCsrfToken() throws Exception {
        // Anonymous request with no CSRF token still hits the endpoint —
        // /api/** is exempt from CSRF and /api/v1/health is on the
        // permit-all list in SecurityConfig.
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }
}
