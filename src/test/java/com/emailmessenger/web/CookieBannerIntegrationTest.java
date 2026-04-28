package com.emailmessenger.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CookieBannerIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void landingPageIncludesCookieBanner() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"cookie-banner\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/cookie-banner.js")));
    }

    @Test
    void pricingPageIncludesCookieBanner() throws Exception {
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"cookie-banner\"")));
    }

    @Test
    void waitlistPageIncludesCookieBanner() throws Exception {
        mockMvc.perform(get("/waitlist"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"cookie-banner\"")));
    }

    @Test
    void demoPageIncludesCookieBanner() throws Exception {
        mockMvc.perform(get("/demo"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"cookie-banner\"")));
    }

    @Test
    void privacyPageIncludesCookieBannerAndLinksToItself() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"cookie-banner\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Privacy Policy")));
    }

    @Test
    void cookieBannerJsAssetIsServed() throws Exception {
        mockMvc.perform(get("/js/cookie-banner.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString("javascript")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mailim.cookieConsent.v1")));
    }

    @Test
    void waitlistPageRendersDefaultFormStateWithoutFlashAttributes() throws Exception {
        mockMvc.perform(get("/waitlist"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Get early access to MailIM")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("waitlist-form")));
    }
}
