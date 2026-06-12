package com.emailmessenger.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class LandingPageContentIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void landingPageRendersWhyMailIMComparisonSection() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("landing-why")))
                .andExpect(content().string(containsString("why-grid")))
                .andExpect(content().string(containsString("Inbox today vs. inbox with MailIM")))
                .andExpect(content().string(containsString("Your inbox today")))
                .andExpect(content().string(containsString("Your inbox with MailIM")));
    }

    @Test
    void landingPageWhySectionListsBothPainsAndSolutions() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Walls of quoted text")))
                .andExpect(content().string(containsString("Quoted-reply noise stripped automatically")))
                .andExpect(content().string(containsString("why-icon-bad")))
                .andExpect(content().string(containsString("why-icon-good")));
    }

    @Test
    void landingPageRetainsHeroAndPrimaryCtas() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Email, reimagined as chat")))
                .andExpect(content().string(containsString("Join the waitlist")))
                .andExpect(content().string(containsString("Try the live demo")));
    }
}
