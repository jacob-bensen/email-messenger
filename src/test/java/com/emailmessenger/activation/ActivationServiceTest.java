package com.emailmessenger.activation;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import com.emailmessenger.repository.MailAccountRepository;
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
class ActivationServiceTest {

    @Autowired ActivationService activationService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired MailAccountRepository mailAccounts;
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

    private User newUser(String email) {
        userService.register(email, "password1", "Pending");
        return users.findByEmail(email).orElseThrow();
    }

    @Test
    void candidateWithoutMailboxGetsEmailAndStampIsPersisted() {
        User user = newUser("cold@example.com");
        users.flush();
        int rows = backdateCreatedAt(user.getId(), 2);
        assertThat(rows).isEqualTo(1);

        boolean sent = activationService.sendActivationFor(
                users.findById(user.getId()).orElseThrow(), LocalDateTime.now());

        assertThat(sent).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
        User after = users.findById(user.getId()).orElseThrow();
        assertThat(after.getLastActivationNudgeSentAt()).isNotNull();
    }

    @Test
    void candidateWhoAlreadyHasMailAccountIsNotEvenInTheCohort() {
        User user = newUser("connected@example.com");
        backdateCreatedAt(user.getId(), 2);
        mailAccounts.save(new MailAccount(user, "imap.example.com", 993, true,
                "connected@example.com", "ciphertext"));

        int sent = activationService.runActivationCycle();

        assertThat(sent).isEqualTo(0);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void freshSignupWithinCooloffIsNotInTheCohort() {
        User user = newUser("fresh@example.com");
        // No backdating — created_at is "now", within the 24h cool-off.

        int sent = activationService.runActivationCycle();

        assertThat(sent).isEqualTo(0);
        // The /threads-touch-style stamp is irrelevant — the cohort query
        // excludes them at the source.
        User after = users.findById(user.getId()).orElseThrow();
        assertThat(after.getLastActivationNudgeSentAt()).isNull();
    }

    @Test
    void alreadyNudgedUserIsSkippedEvenIfStillUnconnected() {
        User user = newUser("repeat@example.com");
        backdateCreatedAt(user.getId(), 3);
        users.touchActivationNudgeSent(user.getId(), LocalDateTime.now().minusDays(1));

        int sent = activationService.runActivationCycle();

        assertThat(sent).isEqualTo(0);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void optedOutUserIsSkippedEvenIfOtherwiseAEligible() {
        User user = newUser("optout@example.com");
        backdateCreatedAt(user.getId(), 2);
        DigestEmailPreference prefs = preferences.save(
                new DigestEmailPreference(user, "preset-token-activation-xyz"));
        prefs.setOptedOut(true);
        preferences.save(prefs);

        boolean sent = activationService.sendActivationFor(
                users.findById(user.getId()).orElseThrow(), LocalDateTime.now());

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
        User after = users.findById(user.getId()).orElseThrow();
        // No stamp written — opt-out is the gate, not "we tried and failed".
        assertThat(after.getLastActivationNudgeSentAt()).isNull();
    }

    @Test
    void runActivationCycleSweepsOnlyTheRightCohortAndReturnsSendCount() {
        // Eligible: created 2 days ago, no mailbox.
        User coldA = newUser("sweep-a@example.com");
        backdateCreatedAt(coldA.getId(), 2);
        // Ineligible: created 2 days ago BUT has a mailbox.
        User connected = newUser("sweep-b@example.com");
        backdateCreatedAt(connected.getId(), 2);
        mailAccounts.save(new MailAccount(connected, "imap.example.com", 993, true,
                "sweep-b@example.com", "ct"));
        // Ineligible: created today, still in cool-off.
        newUser("sweep-c@example.com");

        int sent = activationService.runActivationCycle();

        assertThat(sent).isEqualTo(1);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void emailBodyLinksToMailboxFormAndDemoAndCarriesUnsubscribe() throws Exception {
        User user = newUser("body@example.com");
        backdateCreatedAt(user.getId(), 2);

        boolean sent = activationService.sendActivationFor(
                users.findById(user.getId()).orElseThrow(), LocalDateTime.now());
        assertThat(sent).isTrue();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertThat(mime.getAllRecipients()).hasSize(1);
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("body@example.com");
        assertThat(mime.getSubject()).contains("Connect your mailbox");
        String body = (String) mime.getContent();
        DigestEmailPreference prefs = preferences.findByUser(user).orElseThrow();
        assertThat(body).contains("/mailboxes/new");
        assertThat(body).contains("/demo");
        assertThat(body).contains("/digest/opt-out?token=" + prefs.getOptOutToken());
    }

    @Autowired ActivationTestSupport testSupport;

    private int backdateCreatedAt(Long userId, int daysAgo) {
        return testSupport.backdateCreatedAt(userId, LocalDateTime.now().minusDays(daysAgo));
    }
}
