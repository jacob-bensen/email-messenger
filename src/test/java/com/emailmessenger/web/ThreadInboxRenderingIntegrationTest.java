package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.PlanLimitKind;
import com.emailmessenger.billing.TrialConversionNudge;
import com.emailmessenger.billing.TrialConversionNudgeService;
import com.emailmessenger.billing.UpgradeModal;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.service.ReplyService;

import java.util.Optional;
import static org.mockito.Mockito.when;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real Thymeleaf stack and asserts the threads template renders
 * the sender drill-down rail, an active-sender pill, and pagination/search
 * links that round-trip the `from` query param. Covers what
 * `ThreadControllerTest` (standalone MockMvc) can't.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ThreadInboxRenderingIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserService userService;
    @Autowired EmailThreadRepository threadRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired ParticipantRepository participantRepository;

    @MockBean ReplyService replyService;
    @MockBean TrialConversionNudgeService trialConversionNudgeService;

    private User user;

    @BeforeEach
    void setUp() {
        user = userService.register("inbox-render@example.com", "password1", "Inbox");
        Participant ada = participantRepository.save(
                new Participant("ada-render@acme.com", "Ada Lovelace"));
        Participant grace = participantRepository.save(
                new Participant("grace-render@navy.mil", "Grace Hopper"));
        EmailThread t1 = threadRepository.save(new EmailThread(user, "Project Athena", "<a-r@x>"));
        EmailThread t2 = threadRepository.save(new EmailThread(user, "Project Olympus", "<o-r@x>"));
        messageRepository.save(new Message(t1, ada, "Project Athena",
                "Ada's first message", null, LocalDateTime.now()));
        messageRepository.save(new Message(t2, grace, "Project Olympus",
                "Grace's first message", null, LocalDateTime.now()));
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void inboxRendersSenderRailWithBothParticipants() throws Exception {
        String body = mockMvc.perform(get("/threads"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("class=\"sender-rail\"");
        assertThat(body).contains("Everyone");
        assertThat(body).contains("Ada Lovelace");
        assertThat(body).contains("Grace Hopper");
        // The default "Everyone" row is marked active when no sender filter is on.
        assertThat(body).containsPattern("sender-rail-everyone\\s+sender-rail-active");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void senderFilterRendersActivePillAndScopesResults() throws Exception {
        String body = mockMvc.perform(get("/threads").param("from", "ada-render@acme.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Active-sender pill is present, Everyone link clears the filter.
        assertThat(body).contains("class=\"sender-pill\"");
        assertThat(body).contains("ada-render@acme.com");
        assertThat(body).contains("Show all senders");
        // Only Ada's thread is in the result section title.
        assertThat(body).contains("1 thread from ada-render@acme.com");
        assertThat(body).contains("Project Athena");
        assertThat(body).doesNotContain("Project Olympus");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void inboxRendersInstallBannerForSignedInUsers() throws Exception {
        // Authenticated `/threads` is the post-signup surface where the
        // PWA install banner has to also show — Personal-tier retention
        // depends on home-screen install, and a user who only ever sees
        // the prompt on the public landing has already moved past it.
        String body = mockMvc.perform(get("/threads"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("id=\"install-banner\"");
        assertThat(body).contains("beforeinstallprompt");
        assertThat(body).contains("conexusmail-install-dismiss-v1");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void inboxRendersMobileViewportAndUsesSharedStylesheet() throws Exception {
        // Mobile-tuned shell: viewport-fit=cover so an installed PWA on
        // iOS reaches into the notch + home-bar area, and the same
        // /css/main.css carries the ≥44px tap targets + safe-area insets.
        // Without the viewport-fit hint, env(safe-area-inset-*) collapses
        // to 0 and the chat bubbles slide under the notch.
        String body = mockMvc.perform(get("/threads"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("viewport-fit=cover");
        assertThat(body).contains("href=\"/css/main.css\"");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void combinedQueryAndSenderPreservesBothInSearchFormAndLinks() throws Exception {
        String body = mockMvc.perform(get("/threads")
                        .param("q", "athena")
                        .param("from", "ada-render@acme.com"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Hidden `from` input rides along the search form so re-submitting
        // a new query keeps the sender filter in place.
        assertThat(body).contains("name=\"from\" value=\"ada-render@acme.com\"");
        // Clear link is rendered (either q or sender active).
        assertThat(body).contains("class=\"inbox-search-clear\"");
        // Header reflects both filters via the search-query path.
        assertThat(body).contains("&quot;athena&quot;");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void upgradeModalExposesMonthlyAnnualToggleAndAnnualBilledAsLine() throws Exception {
        UpgradeModal modal = new UpgradeModal(
                Plan.FREE, PlanLimitKind.THREAD_COUNT, 500L, 500L,
                Plan.PERSONAL, "$9", "$7", "$84");

        String body = mockMvc.perform(get("/threads").flashAttr("upgradeModal", modal))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Monthly|Annual sub-toggle is wired to a hidden `billing` input that
        // posts through to /billing/checkout.
        assertThat(body).contains("class=\"billing-period-toggle\"");
        assertThat(body).contains("data-billing-period=\"monthly\"");
        assertThat(body).contains("data-billing-period=\"annual\"");
        assertThat(body).contains("id=\"upgrade-modal-billing\"");
        assertThat(body).contains("2 months free");
        // Annual line shows both the per-month equivalent and the cash amount
        // so the user sees both the mental model and what they'll be charged.
        assertThat(body).contains("$7/mo · billed annually as $84");
        // Monthly default remains visible without JS.
        assertThat(body).contains("Personal — $9/mo");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void trialNudgeWithinThreeDaysShowsAnnualSwitchCta() throws Exception {
        when(trialConversionNudgeService.nudgeFor(org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn(Optional.of(new TrialConversionNudge(
                        "Personal", "personal", 2L, "$9", "$7", "$84",
                        "conexusmail-trial-nudge-2026-06-10-d2")));

        String body = mockMvc.perform(get("/threads"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Final-window upsell copy + the dedicated annual CTA form post to
        // /billing/checkout with billing=annual so the trial-end user
        // converts at the higher-ARPU SKU.
        assertThat(body).contains("class=\"trial-nudge-annual\"");
        assertThat(body).contains("Save 2 months by switching to annual today");
        assertThat(body).contains("Switch to annual — $84/year");
        assertThat(body).contains("name=\"billing\" value=\"annual\"");
        // Monthly continue CTA still present (the annual is additive, not
        // a replacement) — final-window user still has the original choice.
        assertThat(body).contains("Continue on Personal — $9/mo");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void trialNudgeBeyondThreeDaysOmitsAnnualSwitchCta() throws Exception {
        // daysLeft=5 → outside the final-window upsell trigger; the annual
        // sub-block should not render so we don't pre-empt the customer's
        // own decision earlier in the trial.
        when(trialConversionNudgeService.nudgeFor(org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn(Optional.of(new TrialConversionNudge(
                        "Personal", "personal", 5L, "$9", "$7", "$84",
                        "conexusmail-trial-nudge-2026-06-13-d5")));

        String body = mockMvc.perform(get("/threads"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("class=\"trial-nudge-annual\"");
        assertThat(body).doesNotContain("Switch to annual");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void onboardingNudgeRendersInsideProgressCardWithUpgradeCta() throws Exception {
        OnboardingNudge nudge = new OnboardingNudge(
                Plan.PERSONAL,
                "Free includes 1 saved search",
                "Personal is unlimited saved searches.",
                "Upgrade to Personal — $9/mo",
                "step3");

        String body = mockMvc.perform(get("/threads").flashAttr("onboardingNudge", nudge))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Nudge fragment renders inside the .onboarding-progress section and
        // posts to the same /billing/checkout entry point the upgrade modal
        // uses, so the Free user converts via the existing Stripe path.
        assertThat(body).contains("class=\"onboarding-nudge\"");
        assertThat(body).contains("data-trigger=\"step3\"");
        assertThat(body).contains("Free includes 1 saved search");
        assertThat(body).contains("Personal is unlimited saved searches.");
        assertThat(body).contains("Upgrade to Personal — $9/mo");
        assertThat(body).contains("action=\"/billing/checkout\"");
        assertThat(body).contains("name=\"plan\" value=\"personal\"");
    }

    @Test
    @WithMockUser(username = "inbox-render@example.com")
    void teamPlanNudgeRendersStandaloneWhenChecklistIsHidden() throws Exception {
        // When the checklist is complete (all four steps done) `onboarding`
        // is null but a Free user's Team-plan nudge still surfaces — the
        // .onboarding-progress section persists with just the nudge body,
        // no step list or progress bar.
        OnboardingNudge nudge = new OnboardingNudge(
                Plan.TEAM,
                "Sharing your inbox is on the Team plan",
                "Team adds shared threads.",
                "Upgrade to Team — $29/mo",
                "step4");

        String body = mockMvc.perform(get("/threads").flashAttr("onboardingNudge", nudge))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("class=\"onboarding-nudge\"");
        assertThat(body).contains("data-trigger=\"step4\"");
        assertThat(body).contains("Sharing your inbox is on the Team plan");
        assertThat(body).contains("Upgrade to Team — $29/mo");
        assertThat(body).contains("name=\"plan\" value=\"team\"");
    }
}
