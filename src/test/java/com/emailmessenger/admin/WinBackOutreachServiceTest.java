package com.emailmessenger.admin;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.CancellationReason;
import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class WinBackOutreachServiceTest {

    @Autowired WinBackOutreachService winBack;
    @Autowired UserService userService;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired DigestEmailPreferenceRepository preferences;

    @MockBean JavaMailSender mailSender;
    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    @BeforeEach
    void stubMimeFactory() {
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage((Session) null));
    }

    private Subscription seedCanceled(String email, Plan plan, BillingPeriod period,
                                      CancellationReason reason) {
        User user = userService.register(email, "password1", null);
        Subscription sub = new Subscription(user, "cus_" + email, "canceled");
        sub.setPlan(plan);
        sub.setBillingPeriod(period);
        if (reason != null) {
            sub.setCancellationReason(reason);
            sub.setCancellationReasonAt(LocalDateTime.now());
        }
        return subscriptions.save(sub);
    }

    @Test
    void canceledPaidSubscriberGetsEmailAndStampIsPersisted() throws Exception {
        Subscription sub = seedCanceled("walked@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, CancellationReason.TOO_EXPENSIVE);

        WinBackOutreachService.Outcome outcome = winBack.sendWinBackFor(sub.getId());

        assertThat(outcome).isEqualTo(WinBackOutreachService.Outcome.SENT);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertThat(mime.getAllRecipients()).hasSize(1);
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("walked@example.com");
        assertThat(mime.getSubject()).contains("ConexusMail").contains("Personal");
        String body = (String) mime.getContent();
        DigestEmailPreference prefs = preferences.findByUser(sub.getUser()).orElseThrow();
        assertThat(body).contains("/pricing");
        assertThat(body).contains("/digest/opt-out?token=" + prefs.getOptOutToken());
        assertThat(body).contains("too expensive");
        Subscription after = subscriptions.findById(sub.getId()).orElseThrow();
        assertThat(after.getLastWinBackEmailSentAt()).isNotNull();
    }

    @Test
    void teamReasonChoicesProduceReasonSpecificCopy() throws Exception {
        Subscription sub = seedCanceled("featuregap@example.com", Plan.TEAM,
                BillingPeriod.ANNUAL, CancellationReason.MISSING_FEATURE);

        winBack.sendWinBackFor(sub.getId());

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String body = (String) captor.getValue().getContent();
        assertThat(body).contains("Team").contains("annual");
        assertThat(body).contains("missing feature");
    }

    @Test
    void alreadyStampedSubscriptionIsSkipped() {
        Subscription sub = seedCanceled("repeat@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, CancellationReason.OTHER);
        subscriptions.touchWinBackEmailSent(sub.getId(), LocalDateTime.now().minusHours(1));

        WinBackOutreachService.Outcome outcome = winBack.sendWinBackFor(sub.getId());

        assertThat(outcome).isEqualTo(WinBackOutreachService.Outcome.ALREADY_SENT);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void optedOutUserIsSkippedAndStampNotWritten() {
        Subscription sub = seedCanceled("optout@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, CancellationReason.TEMPORARY);
        DigestEmailPreference prefs = preferences.save(
                new DigestEmailPreference(sub.getUser(), "preset-winback-optout"));
        prefs.setOptedOut(true);
        preferences.save(prefs);

        WinBackOutreachService.Outcome outcome = winBack.sendWinBackFor(sub.getId());

        assertThat(outcome).isEqualTo(WinBackOutreachService.Outcome.OPTED_OUT);
        verify(mailSender, never()).send(any(MimeMessage.class));
        Subscription after = subscriptions.findById(sub.getId()).orElseThrow();
        assertThat(after.getLastWinBackEmailSentAt()).isNull();
    }

    @Test
    void activeSubscriptionIsNotAValidTargetBecauseTheyAreAlreadyBack() {
        User user = userService.register("active@example.com", "password1", null);
        Subscription sub = new Subscription(user, "cus_active", "active");
        sub.setPlan(Plan.PERSONAL);
        sub.setBillingPeriod(BillingPeriod.MONTHLY);
        subscriptions.save(sub);

        WinBackOutreachService.Outcome outcome = winBack.sendWinBackFor(sub.getId());

        assertThat(outcome).isEqualTo(WinBackOutreachService.Outcome.NOT_CANCELED);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void freePlanRowIsRejectedAsUnsupportedTarget() {
        User user = userService.register("free@example.com", "password1", null);
        Subscription sub = new Subscription(user, "cus_free", "canceled");
        sub.setPlan(Plan.FREE);
        sub.setBillingPeriod(BillingPeriod.MONTHLY);
        subscriptions.save(sub);

        WinBackOutreachService.Outcome outcome = winBack.sendWinBackFor(sub.getId());

        assertThat(outcome).isEqualTo(WinBackOutreachService.Outcome.UNSUPPORTED_PLAN);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void unknownSubscriptionIdSurfacesNotFoundWithoutSendingMail() {
        WinBackOutreachService.Outcome outcome = winBack.sendWinBackFor(987654L);

        assertThat(outcome).isEqualTo(WinBackOutreachService.Outcome.NOT_FOUND);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void mailFailureLeavesStampNullSoOperatorCanRetry() {
        Subscription sub = seedCanceled("flaky@example.com", Plan.TEAM,
                BillingPeriod.MONTHLY, CancellationReason.SWITCHING);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        WinBackOutreachService.Outcome outcome = winBack.sendWinBackFor(sub.getId());

        assertThat(outcome).isEqualTo(WinBackOutreachService.Outcome.MAIL_FAILED);
        Subscription after = subscriptions.findById(sub.getId()).orElseThrow();
        assertThat(after.getLastWinBackEmailSentAt()).isNull();
    }

    @Test
    void nullCancellationReasonStillSendsWithGenericOtherCopy() throws Exception {
        Subscription sub = seedCanceled("noreason@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, null);

        WinBackOutreachService.Outcome outcome = winBack.sendWinBackFor(sub.getId());

        assertThat(outcome).isEqualTo(WinBackOutreachService.Outcome.SENT);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String body = (String) captor.getValue().getContent();
        assertThat(body).contains("didn't get a reason");
    }
}
