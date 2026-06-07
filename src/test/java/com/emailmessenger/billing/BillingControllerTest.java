package com.emailmessenger.billing;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class BillingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;

    @MockBean BillingService billingService;
    @MockBean ReplyService replyService;
    @MockBean EmailThreadRepository threadRepository;

    @Test
    void checkoutWithoutAuthRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/billing/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("plan", "personal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void authenticatedCheckoutRedirectsToStripeUrl() throws Exception {
        User registered = userService.register("buyer@example.com", "password1", null);
        when(billingService.startCheckout(any(User.class), eq(Plan.PERSONAL), eq(BillingPeriod.MONTHLY)))
                .thenReturn("https://checkout.stripe.com/c/pay/cs_test_xyz");

        mockMvc.perform(post("/billing/checkout")
                        .with(user(registered.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("plan", "personal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.com/c/pay/cs_test_xyz"));

        verify(billingService).startCheckout(any(User.class), eq(Plan.PERSONAL), eq(BillingPeriod.MONTHLY));
    }

    @Test
    void checkoutWithBillingAnnualParamPassesAnnualPeriodToService() throws Exception {
        User registered = userService.register("annualbuyer@example.com", "password1", null);
        when(billingService.startCheckout(any(User.class), eq(Plan.PERSONAL), eq(BillingPeriod.ANNUAL)))
                .thenReturn("https://checkout.stripe.com/c/pay/cs_annual");

        mockMvc.perform(post("/billing/checkout")
                        .with(user(registered.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("plan", "personal")
                        .param("billing", "annual"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.com/c/pay/cs_annual"));

        verify(billingService).startCheckout(any(User.class), eq(Plan.PERSONAL), eq(BillingPeriod.ANNUAL));
    }

    @Test
    void unknownPlanReturnsBadRequest() throws Exception {
        User registered = userService.register("badplan@example.com", "password1", null);
        mockMvc.perform(post("/billing/checkout")
                        .with(user(registered.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("plan", "platinum"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void portalWithoutAuthRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/billing/portal")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void portalForUserWithStripeCustomerRedirectsToStripeUrl() throws Exception {
        User registered = userService.register("manage@example.com", "password1", null);
        when(billingService.startPortal(any(User.class)))
                .thenReturn(Optional.of("https://billing.stripe.com/p/session/xyz"));

        mockMvc.perform(post("/billing/portal")
                        .with(user(registered.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://billing.stripe.com/p/session/xyz"));

        verify(billingService).startPortal(any(User.class));
    }

    @Test
    void portalForUserWithoutSubscriptionRedirectsToPricing() throws Exception {
        User registered = userService.register("nostripe@example.com", "password1", null);
        when(billingService.startPortal(any(User.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/billing/portal")
                        .with(user(registered.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pricing"));
    }
}
