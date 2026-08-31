package com.emailmessenger.api;

import com.emailmessenger.billing.BillingService;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MetaControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BillingService billingService;
    @MockitoBean ReplyService replyService;

    @Test
    void metaEndpointReturnsDiscoveryPayloadUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiVersion").value("1"))
                .andExpect(jsonPath("$.serverVersion").isString())
                .andExpect(jsonPath("$.minMobileBuild").value(1))
                .andExpect(jsonPath("$.serverTime").isString())
                .andExpect(jsonPath("$.plans[0]").value("FREE"))
                .andExpect(jsonPath("$.plans[2]").value("BUSINESS"))
                // Test profile leaves auth.google.client-id blank.
                .andExpect(jsonPath("$.features.googleOAuth").value(false));
    }
}
