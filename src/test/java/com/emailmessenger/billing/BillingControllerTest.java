package com.emailmessenger.billing;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.CancellationReason;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BillingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;

    @MockitoBean BillingService billingService;
    @MockitoBean ReplyService replyService;
    @MockitoBean EmailThreadRepository threadRepository;

    @Test
    void checkoutWithoutAuthRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/billing/checkout")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("plan", "pro"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void authenticatedCheckoutRedirectsToStripeUrl() throws Exception {
        User registered = userService.register("buyer@example.com", "password1", null);
        when(billingService.startCheckout(any(User.class), eq(Plan.PRO), eq(BillingPeriod.MONTHLY)))
                .thenReturn("https://checkout.stripe.com/c/pay/cs_test_xyz");

        mockMvc.perform(post("/billing/checkout")
                        .with(user(registered.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("plan", "pro"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.com/c/pay/cs_test_xyz"));

        verify(billingService).startCheckout(any(User.class), eq(Plan.PRO), eq(BillingPeriod.MONTHLY));
    }

    @Test
    void checkoutWithBillingAnnualParamPassesAnnualPeriodToService() throws Exception {
        User registered = userService.register("annualbuyer@example.com", "password1", null);
        when(billingService.startCheckout(any(User.class), eq(Plan.PRO), eq(BillingPeriod.ANNUAL)))
                .thenReturn("https://checkout.stripe.com/c/pay/cs_annual");

        mockMvc.perform(post("/billing/checkout")
                        .with(user(registered.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("plan", "pro")
                        .param("billing", "annual"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.com/c/pay/cs_annual"));

        verify(billingService).startCheckout(any(User.class), eq(Plan.PRO), eq(BillingPeriod.ANNUAL));
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

    @Test
    void cancelSubscriptionPageRedirectsToPricingWhenUserHasNeverPaid() throws Exception {
        User registered = userService.register("never-paid@example.com", "password1", null);
        when(billingService.hasManagedBilling(any(User.class))).thenReturn(false);

        mockMvc.perform(get("/billing/cancel-subscription")
                        .with(user(registered.getEmail())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pricing"));
    }

    @Test
    void cancelSubscriptionPageRendersReasonPickerForPayingUser() throws Exception {
        User registered = userService.register("paying@example.com", "password1", null);
        when(billingService.hasManagedBilling(any(User.class))).thenReturn(true);

        mockMvc.perform(get("/billing/cancel-subscription")
                        .with(user(registered.getEmail())))
                .andExpect(status().isOk())
                .andExpect(view().name("billing/cancel-subscription"))
                .andExpect(model().attributeExists("reasons"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("too_expensive")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("missing_feature")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("switching")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("temporary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Continue to Stripe")));
    }

    @Test
    void submitCancelSubscriptionRecordsReasonAndRedirectsToStripePortal() throws Exception {
        User registered = userService.register("submit-cancel@example.com", "password1", null);
        when(billingService.recordCancellationReason(any(User.class), eq(CancellationReason.TOO_EXPENSIVE)))
                .thenReturn(true);
        when(billingService.startPortal(any(User.class)))
                .thenReturn(Optional.of("https://billing.stripe.com/p/session/cx1"));

        mockMvc.perform(post("/billing/cancel-subscription")
                        .with(user(registered.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("reason", "too_expensive"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://billing.stripe.com/p/session/cx1"));

        verify(billingService).recordCancellationReason(any(User.class),
                eq(CancellationReason.TOO_EXPENSIVE));
        verify(billingService).startPortal(any(User.class));
    }

    @Test
    void submitCancelSubscriptionWithUnknownReasonReturnsBadRequest() throws Exception {
        User registered = userService.register("bad-reason@example.com", "password1", null);

        mockMvc.perform(post("/billing/cancel-subscription")
                        .with(user(registered.getEmail()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("reason", "made_up"))
                .andExpect(status().isBadRequest());

        verify(billingService, never()).recordCancellationReason(any(User.class), any());
    }

    @Test
    void cancelSubscriptionPageRequiresAuth() throws Exception {
        mockMvc.perform(get("/billing/cancel-subscription"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
