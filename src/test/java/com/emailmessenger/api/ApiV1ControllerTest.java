package com.emailmessenger.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiV1ControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    void health_returnsOk_publicly() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void version_reportsProductNameAndApiVersion() throws Exception {
        mockMvc.perform(get("/api/v1/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("conexus-mail"))
                .andExpect(jsonPath("$.apiVersion").value("v1"))
                .andExpect(jsonPath("$.serverVersion").isString())
                .andExpect(jsonPath("$.minSupportedClient").isString());
    }

    @Test
    void plans_listsFreeProBusiness_withUnlimitedAsNull() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.plans[0].id").value("FREE"))
                .andExpect(jsonPath("$.plans[0].monthlyCents").value(0))
                .andExpect(jsonPath("$.plans[0].limits.mailboxes").value(3))
                .andExpect(jsonPath("$.plans[0].limits.threads").value(500))
                .andExpect(jsonPath("$.plans[0].limits.savedSearches").isEmpty())
                .andExpect(jsonPath("$.plans[1].id").value("PRO"))
                .andExpect(jsonPath("$.plans[1].monthlyCents").value(699))
                .andExpect(jsonPath("$.plans[1].annualMonthlyCents").value(599))
                .andExpect(jsonPath("$.plans[1].limits.mailboxes").value(5))
                .andExpect(jsonPath("$.plans[1].limits.threads").isEmpty())
                .andExpect(jsonPath("$.plans[2].id").value("BUSINESS"))
                .andExpect(jsonPath("$.plans[2].limits.mailboxes").isEmpty());
    }
}
