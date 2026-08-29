package com.emailmessenger.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlansApiControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PlansApiController()).build();
    }

    @Test
    void listReturnsAllThreePlansInEnumOrder() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.plans.length()").value(3))
                .andExpect(jsonPath("$.plans[0].id").value("FREE"))
                .andExpect(jsonPath("$.plans[1].id").value("PRO"))
                .andExpect(jsonPath("$.plans[2].id").value("BUSINESS"));
    }

    @Test
    void proCarriesMonthlyAndAnnualPricingFromPlanPricing() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans[1].monthlyCents").value(699))
                .andExpect(jsonPath("$.plans[1].annualMonthlyCents").value(599));
    }

    @Test
    void freeAndBusinessBothBillZeroCents() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans[0].monthlyCents").value(0))
                .andExpect(jsonPath("$.plans[0].annualMonthlyCents").value(0))
                .andExpect(jsonPath("$.plans[2].monthlyCents").value(0))
                .andExpect(jsonPath("$.plans[2].annualMonthlyCents").value(0));
    }

    @Test
    void freeCarriesMailboxAndThreadCapsButUnlimitedSavedSearches() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans[0].mailboxLimit").value(3))
                .andExpect(jsonPath("$.plans[0].threadLimit").value(500))
                .andExpect(jsonPath("$.plans[0].savedSearchLimit").doesNotExist());
    }

    @Test
    void proLiftsThreadCapButKeepsFiveMailboxCap() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans[1].mailboxLimit").value(5))
                .andExpect(jsonPath("$.plans[1].threadLimit").doesNotExist())
                .andExpect(jsonPath("$.plans[1].savedSearchLimit").doesNotExist());
    }

    @Test
    void businessRendersAllCapsAsUnlimited() throws Exception {
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans[2].mailboxLimit").doesNotExist())
                .andExpect(jsonPath("$.plans[2].threadLimit").doesNotExist())
                .andExpect(jsonPath("$.plans[2].savedSearchLimit").doesNotExist());
    }
}
