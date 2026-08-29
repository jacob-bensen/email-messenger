package com.emailmessenger.api;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppInfoControllerTest {

    private static final Instant FIXED = Instant.parse("2026-08-29T13:45:00Z");

    @Autowired MockMvc mockMvc;

    @MockitoBean BillingService billingService;
    @MockitoBean ReplyService replyService;

    @TestConfiguration
    static class FixedClockConfig {
        @Bean @Primary
        Clock testClock() {
            return Clock.fixed(FIXED, ZoneOffset.UTC);
        }
    }

    @Test
    void appInfoReturnsContractShapeUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/app-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product").value("ConexusMail"))
                .andExpect(jsonPath("$.apiVersion").value("v1"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.serverTime").value("2026-08-29T13:45:00Z"));
        }
}
