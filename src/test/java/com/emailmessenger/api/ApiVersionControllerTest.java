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
class ApiVersionControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BillingService billingService;
    @MockitoBean ReplyService replyService;

    @Test
    void versionEndpointIsReachableUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("conexusmail-backend"))
                .andExpect(jsonPath("$.apiVersion").value(1))
                .andExpect(jsonPath("$.version").isString());
    }
}
