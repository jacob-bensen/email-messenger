package com.emailmessenger.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MarketingController.class)
class MarketingControllerTest {

    @Autowired MockMvc mockMvc;

    // ThreadController is component-scanned by @WebMvcTest's controller filter;
    // its constructor needs these beans even though we never exercise them here.
    @MockitoBean EmailThreadRepository threadRepository;
    @MockitoBean ThreadViewService threadViewService;
    @MockitoBean ReplyService replyService;

    @Test
    void pricingRouteReturnsOkAndPricingView() throws Exception {
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(view().name("pricing"));
    }

    @Test
    void pricingPageRendersAllFourPlans() throws Exception {
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString(">Free<"),
                        containsString(">Personal<"),
                        containsString(">Team<"),
                        containsString(">Enterprise<")
                )));
    }

    @Test
    void pricingPageShowsCanonicalMonthlyPrices() throws Exception {
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("$0"),
                        containsString("$9"),
                        containsString("$29"),
                        containsString("$99")
                )));
    }

    @Test
    void pricingPageExposesAnnualPricesForJsToggle() throws Exception {
        // The JS toggle reads data-annual="…" off the price-amount span.
        // Annual = monthly * 10 / 12 (≈16% discount, matches APP_SPEC).
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("data-annual=\"$7.50\""),
                        containsString("data-annual=\"$24\""),
                        containsString("data-annual=\"$82\"")
                )));
    }

    @Test
    void pricingPageHasTrialCtaPointingToProduct() throws Exception {
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Start 14-day free trial"),
                        containsString("href=\"/threads\"")
                )));
    }

    @Test
    void pricingPageHasNoCreditCardCopy() throws Exception {
        // Removing this copy is a known conversion regression — guard it.
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No credit card required")));
    }

    @Test
    void pricingPageBillingToggleRendersBothOptions() throws Exception {
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("data-period=\"monthly\""),
                        containsString("data-period=\"annual\""),
                        containsString("Save 16%")
                )));
    }

    @Test
    void pricingPageDeclaresSeoDescription() throws Exception {
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("name=\"description\""),
                        containsString("MailIM turns your email into chat")
                )));
    }

    @Test
    void pricingPageDoesNotLeakWhitelabelError() throws Exception {
        mockMvc.perform(get("/pricing"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))));
    }
}
