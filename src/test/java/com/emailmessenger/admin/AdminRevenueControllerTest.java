package com.emailmessenger.admin;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.billing.BillingProperties;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.billing.StripeSubscriptionGateway;
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
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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
    @Autowired BillingProperties billingProperties;

    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean StripeSubscriptionGateway stripeSubscriptionGateway;
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

    @Test
    @WithMockUser(username = "operator@example.com")
    void adminCanTriggerReconcileAndIsRedirectedBackWithFlashSummary() throws Exception {
        userService.register("operator@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));
        billingProperties.setPersonalAnnualPriceId("price_personal_annual");

        User payer = userService.register("payer@example.com", "password1", null);
        Subscription sub = new Subscription(payer, "cus_payer", "active");
        sub.setPlan(Plan.PERSONAL);
        sub.setStripeSubscriptionId("sub_payer");
        // billingPeriod is intentionally left null to simulate pre-V17 state.
        subscriptions.save(sub);

        when(stripeSubscriptionGateway.currentPriceId("sub_payer"))
                .thenReturn(Optional.of("price_personal_annual"));

        mockMvc.perform(post("/admin/revenue/reconcile-billing-period").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/revenue"))
                .andExpect(flash().attributeExists("reconcile"));

        Subscription reloaded = subscriptions.findById(sub.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.getBillingPeriod())
                .isEqualTo(BillingPeriod.ANNUAL);
    }

    @Test
    @WithMockUser(username = "intruder@example.com")
    void nonAdminCannotTriggerReconcileEvenWithValidCsrf() throws Exception {
        userService.register("intruder@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));

        mockMvc.perform(post("/admin/revenue/reconcile-billing-period").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "operator@example.com")
    void reconcileWithNoCandidatesYieldsNoOpFlashResult() throws Exception {
        userService.register("operator@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));

        mockMvc.perform(post("/admin/revenue/reconcile-billing-period").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/revenue"))
                .andExpect(flash().attribute("reconcile",
                        org.hamcrest.Matchers.hasProperty("noOp", org.hamcrest.Matchers.is(true))));
    }

    @Test
    @WithMockUser(username = "operator@example.com")
    void revenuePageRendersReconcileButton() throws Exception {
        userService.register("operator@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));

        mockMvc.perform(get("/admin/revenue"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/revenue/reconcile-billing-period")))
                .andExpect(content().string(containsString("Reconcile from Stripe")));
    }

    @Test
    @WithMockUser(username = "operator@example.com")
    void revenuePageExposesFunnelModelAndRendersHeading() throws Exception {
        userService.register("operator@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));

        userService.register("recent@example.com", "password1", null, "producthunt");
        User payer = userService.register("payer@example.com", "password1", null, "producthunt");
        Subscription sub = new Subscription(payer, "cus_payer", "active");
        sub.setPlan(Plan.PERSONAL);
        sub.setBillingPeriod(BillingPeriod.MONTHLY);
        sub.setStripePriceId("price_personal_monthly");
        subscriptions.save(sub);

        mockMvc.perform(get("/admin/revenue"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("funnel"))
                .andExpect(content().string(containsString("Funnel — last 30 days")))
                .andExpect(content().string(containsString("Trial starts")))
                .andExpect(content().string(containsString("Paid conversions")))
                .andExpect(content().string(containsString("producthunt")));
    }

    @Test
    @WithMockUser(username = "operator@example.com")
    void revenuePageRendersOnboardingFunnelCardWithEverySignupBucket() throws Exception {
        userService.register("operator@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));

        // One signup gives the funnel a non-zero denominator; the per-step
        // figures don't matter for this test — we're proving the model
        // attribute is exposed and the new card heading + step labels
        // make it through to the rendered HTML.
        userService.register("recent-signup@example.com", "password1", null);

        mockMvc.perform(get("/admin/revenue"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("onboardingFunnel"))
                .andExpect(content().string(containsString("Onboarding funnel")))
                .andExpect(content().string(containsString("Mailbox connected")))
                .andExpect(content().string(containsString("10 threads imported")))
                .andExpect(content().string(containsString("Saved a search")))
                .andExpect(content().string(containsString("Invite sent")));
    }

    @Test
    @WithMockUser(username = "operator@example.com")
    void revenuePageRendersTeamAdoptionCardWithEngagementAndConversionLabels() throws Exception {
        userService.register("operator@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));

        mockMvc.perform(get("/admin/revenue"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("teamAdoption"))
                .andExpect(content().string(containsString("Team-plan adoption")))
                .andExpect(content().string(containsString("Notes posted")))
                .andExpect(content().string(containsString("Active note authors")))
                .andExpect(content().string(containsString("@mentions written")))
                .andExpect(content().string(containsString("Free → Team")))
                .andExpect(content().string(containsString("Personal → Team")))
                // Prior-window comparison labels must render so the
                // operator can see the EPIC-16 conversion lift instead
                // of a single 30-day number with no baseline.
                .andExpect(content().string(containsString("prior-window data")))
                .andExpect(content().string(containsString("admin-funnel-delta")));
    }

    @Test
    @WithMockUser(username = "operator@example.com")
    void revenuePageRendersChurnCardWithCancellationsLostMrrAndPerPlanBreakdown() throws Exception {
        userService.register("operator@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));

        User survivor = userService.register("keep@example.com", "password1", null);
        Subscription kept = new Subscription(survivor, "cus_keep", "active");
        kept.setPlan(Plan.TEAM);
        kept.setBillingPeriod(BillingPeriod.MONTHLY);
        subscriptions.save(kept);

        User gone = userService.register("gone@example.com", "password1", null);
        Subscription canceled = new Subscription(gone, "cus_gone", "canceled");
        canceled.setPlan(Plan.PERSONAL);
        canceled.setBillingPeriod(BillingPeriod.MONTHLY);
        subscriptions.save(canceled);

        mockMvc.perform(get("/admin/revenue"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("churn"))
                .andExpect(content().string(containsString("Churn")))
                .andExpect(content().string(containsString("Lost MRR")))
                .andExpect(content().string(containsString("Gross revenue churn")))
                .andExpect(content().string(containsString("Canceled subscribers")))
                // Per-plan breakdown header — both row count and "Canceled" column.
                .andExpect(content().string(containsString("Starting MRR")));
    }

    @Test
    @WithMockUser(username = "operator@example.com")
    void revenuePageRendersTrialEndConversionCardWithSentAndConvertedCounts() throws Exception {
        userService.register("operator@example.com", "password1", null);
        adminProperties.setEmails(List.of("operator@example.com"));

        User payer = userService.register("paid-after-nudge@example.com", "password1", null);
        Subscription paid = new Subscription(payer, "cus_paid", "active");
        paid.setPlan(Plan.PERSONAL);
        paid.setBillingPeriod(BillingPeriod.MONTHLY);
        paid.setLastTrialEndEmailSentAt(java.time.LocalDateTime.now().minusHours(6));
        subscriptions.save(paid);

        User lapsed = userService.register("lapsed-after-nudge@example.com", "password1", null);
        Subscription gone = new Subscription(lapsed, "cus_lapsed", "canceled");
        gone.setPlan(Plan.PERSONAL);
        gone.setBillingPeriod(BillingPeriod.MONTHLY);
        gone.setLastTrialEndEmailSentAt(java.time.LocalDateTime.now().minusHours(6));
        subscriptions.save(gone);

        mockMvc.perform(get("/admin/revenue"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("trialEnd"))
                .andExpect(content().string(containsString("Trial-end conversion")))
                .andExpect(content().string(containsString("Emails sent")))
                .andExpect(content().string(containsString("Converted to active")));
    }
}
