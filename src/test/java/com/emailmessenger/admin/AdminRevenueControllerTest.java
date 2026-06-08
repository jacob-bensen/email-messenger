package com.emailmessenger.admin;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AdminRevenueControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired AdminProperties adminProperties;

    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    @BeforeEach
    void resetAdminEmails() {
        adminProperties.setEmails(List.of());
    }

    @Test
    void anonymousRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/revenue"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "intruder@example.com")
    void authenticatedNonAdminGetsNotFound() throws Exception {
        userService.register("intruder@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));

        mockMvc.perform(get("/admin/revenue"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "operator@example.com")
    void adminGetsRevenuePageWithComputedMetrics() throws Exception {
        userService.register("operator@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));

        User payer = userService.register("payer@example.com", "password1", null);
        Subscription sub = new Subscription(payer, "cus_payer", "active");
        sub.setPlan(Plan.PERSONAL);
        sub.setBillingPeriod(BillingPeriod.MONTHLY);
        sub.setStripePriceId("price_personal_monthly");
        subscriptions.save(sub);

        mockMvc.perform(get("/admin/revenue"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/revenue"))
                .andExpect(model().attributeExists("metrics"))
                .andExpect(content().string(containsString("Revenue")))
                .andExpect(content().string(containsString("$9")))
                .andExpect(content().string(containsString("payer@example.com")));
    }

    @Test
    @WithMockUser(username = "operator@example.com")
    void adminMatchIsCaseInsensitive() throws Exception {
        userService.register("operator@example.com", "password1", null);
        adminProperties.setEmails(List.of("OPERATOR@EXAMPLE.COM"));

        mockMvc.perform(get("/admin/revenue"))
                .andExpect(status().isOk());
    }
}
