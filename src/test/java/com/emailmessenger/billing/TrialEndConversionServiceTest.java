package com.emailmessenger.billing;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TrialEndConversionServiceTest {

    @Autowired TrialEndConversionService trialEndService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired DigestEmailPreferenceRepository preferences;

    @MockBean JavaMailSender mailSender;
    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage((Session) null));
    }

    private Subscription seedTrialing(String email, Plan plan, LocalDateTime trialEndsAt) {
        User user = userService.register(email, "password1", null);
        Subscription sub = new Subscription(user, "cus_" + email, "trialing");
        sub.setPlan(plan);
        sub.setBillingPeriod(BillingPeriod.MONTHLY);
        sub.setTrialEndsAt(trialEndsAt);
        return subscriptions.save(sub);
    }

    @Test
    void trialingPaidPlanWithinT1WindowGetsEmailAndStampIsPersisted() throws Exception {
        Subscription sub = seedTrialing("expiring@example.com", Plan.PERSONAL,
                LocalDateTime.now().plusHours(12));

        int sent = trialEndService.runTrialEndCycle();

        assertThat(sent).isEqualTo(1);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertThat(mime.getAllRecipients()).hasSize(1);
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("expiring@example.com");
        assertThat(mime.getSubject()).contains("Personal").contains("trial");
        String body = (String) mime.getContent();
        DigestEmailPreference prefs = preferences.findByUser(sub.getUser()).orElseThrow();
        assertThat(body).contains("/pricing");
        assertThat(body).contains("/billing");
        assertThat(body).contains("/demo");
        assertThat(body).contains("/digest/opt-out?token=" + prefs.getOptOutToken());
        Subscription after = subscriptions.findById(sub.getId()).orElseThrow();
        assertThat(after.getLastTrialEndEmailSentAt()).isNotNull();
    }

    @Test
    void trialEndingBeyondTheWindowIsNotInCohort() {
        seedTrialing("future@example.com", Plan.PERSONAL,
                LocalDateTime.now().plusDays(7));

        int sent = trialEndService.runTrialEndCycle();

        assertThat(sent).isEqualTo(0);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void alreadyEmailedSubscriptionIsSkippedEvenIfStillTrialing() {
        Subscription sub = seedTrialing("repeat@example.com", Plan.PERSONAL,
                LocalDateTime.now().plusHours(6));
        subscriptions.touchTrialEndEmailSent(sub.getId(), LocalDateTime.now().minusHours(2));

        int sent = trialEndService.runTrialEndCycle();

        assertThat(sent).isEqualTo(0);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void optedOutUserIsSkippedEvenIfOtherwiseEligible() {
        Subscription sub = seedTrialing("optout@example.com", Plan.PERSONAL,
                LocalDateTime.now().plusHours(6));
        DigestEmailPreference prefs = preferences.save(
                new DigestEmailPreference(sub.getUser(), "preset-trial-end-optout"));
        prefs.setOptedOut(true);
        preferences.save(prefs);

        boolean sent = trialEndService.sendTrialEndFor(sub, LocalDateTime.now());

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
        Subscription after = subscriptions.findById(sub.getId()).orElseThrow();
        assertThat(after.getLastTrialEndEmailSentAt()).isNull();
    }

    @Test
    void activeStatusIsNotInCohortBecauseTheUserAlreadyConverted() {
        User user = userService.register("paid@example.com", "password1", null);
        Subscription sub = new Subscription(user, "cus_paid@example.com", "active");
        sub.setPlan(Plan.PERSONAL);
        sub.setBillingPeriod(BillingPeriod.MONTHLY);
        sub.setTrialEndsAt(LocalDateTime.now().plusHours(6));
        subscriptions.save(sub);

        int sent = trialEndService.runTrialEndCycle();

        assertThat(sent).isEqualTo(0);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void enterprisePlanIsExcludedFromCohort() {
        seedTrialing("enterprise@example.com", Plan.ENTERPRISE,
                LocalDateTime.now().plusHours(6));

        int sent = trialEndService.runTrialEndCycle();

        assertThat(sent).isEqualTo(0);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void trialEndCycleIsIdempotentOncePerSubscription() {
        seedTrialing("dupe@example.com", Plan.TEAM,
                LocalDateTime.now().plusHours(12));

        int first = trialEndService.runTrialEndCycle();
        int second = trialEndService.runTrialEndCycle();

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(0);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void cohortReturnsOnlySubsInsideTheWindow() {
        seedTrialing("eligible@example.com", Plan.PERSONAL,
                LocalDateTime.now().plusHours(20));
        seedTrialing("too-far@example.com", Plan.PERSONAL,
                LocalDateTime.now().plusDays(5));
        seedTrialing("enterprise@example.com", Plan.ENTERPRISE,
                LocalDateTime.now().plusHours(12));

        int sent = trialEndService.runTrialEndCycle();

        assertThat(sent).isEqualTo(1);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void teamPlanBodyMentionsTeamLabelInSubject() throws Exception {
        seedTrialing("team@example.com", Plan.TEAM,
                LocalDateTime.now().plusHours(18));

        int sent = trialEndService.runTrialEndCycle();
        assertThat(sent).isEqualTo(1);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).contains("Team");
    }
}
