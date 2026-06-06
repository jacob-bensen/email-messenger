package com.emailmessenger.reengagement;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
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
class ReengagementServiceTest {

    @Autowired ReengagementService reengagementService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired EmailThreadRepository threadRepo;
    @Autowired MessageRepository messageRepo;
    @Autowired ParticipantRepository participantRepo;
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
        userService.register(email, "password1", "Dormant");
        return users.findByEmail(email).orElseThrow();
    }

    /**
     * Plants a dormant user: registered N days ago, never logged in or
     * touched /threads, with no reengagement send recorded yet.
     */
    private User dormantUser(String email, int daysAgo) {
        User u = newUser(email);
        LocalDateTime when = LocalDateTime.now().minusDays(daysAgo);
        u.setLastLoginAt(when);
        u.setLastInboxVisitAt(when);
        return users.saveAndFlush(u);
    }

    private EmailThread newUnreadThread(User owner, String subject) {
        Participant sender = participantRepo.findByEmail("ada@example.com")
                .orElseGet(() -> participantRepo.save(new Participant("ada@example.com", "Ada")));
        EmailThread t = threadRepo.save(new EmailThread(owner, subject, "<" + subject + "@test>"));
        Message m = new Message(t, sender, subject, "body", "<p>body</p>", LocalDateTime.now());
        m.setMessageIdHeader("<" + subject + "@test>");
        m.addRecipient(sender, RecipientType.TO);
        messageRepo.save(m);
        t.addMessage(m);
        return threadRepo.saveAndFlush(t);
    }

    @Test
    void dormantUserWithUnreadThreadsGetsEmailAndStampIsUpdated() {
        User user = dormantUser("dormant@example.com", 14);
        newUnreadThread(user, "Subject A");
        newUnreadThread(user, "Subject B");

        boolean sent = reengagementService.sendReengagementFor(user, LocalDateTime.now());

        assertThat(sent).isTrue();
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        // Stamp persisted so the same user doesn't re-receive next sweep.
        User after = users.findById(user.getId()).orElseThrow();
        assertThat(after.getLastReengagementSentAt()).isNotNull();
    }

    @Test
    void dormantUserWithZeroUnreadIsSkipped() {
        User user = dormantUser("nounread@example.com", 14);
        // No threads at all → unread count is zero → no nudge.

        boolean sent = reengagementService.sendReengagementFor(user, LocalDateTime.now());

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void optedOutUserIsSkippedEvenIfDormantWithUnread() {
        User user = dormantUser("optout@example.com", 14);
        newUnreadThread(user, "Subject A");
        DigestEmailPreference prefs = preferences.save(
                new DigestEmailPreference(user, "preset-token-reeng-abc"));
        prefs.setOptedOut(true);
        preferences.save(prefs);

        boolean sent = reengagementService.sendReengagementFor(user, LocalDateTime.now());

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void secondCycleDoesNotResendWhenUserStillDormant() {
        User user = dormantUser("repeat@example.com", 14);
        newUnreadThread(user, "Subject A");

        LocalDateTime firstRun = LocalDateTime.now();
        assertThat(reengagementService.sendReengagementFor(user, firstRun)).isTrue();

        // Reload the user to pick up the touchReengagementSent stamp and try again.
        User after = users.findById(user.getId()).orElseThrow();
        boolean second = reengagementService.sendReengagementFor(after, firstRun.plusHours(2));

        assertThat(second).isFalse();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void userWhoBecameActiveSinceLastSendGetsASecondNudgeAfterReDisappearing() {
        User user = dormantUser("re-dormant@example.com", 14);
        newUnreadThread(user, "Subject A");

        LocalDateTime firstRun = LocalDateTime.now().minusDays(14);
        assertThat(reengagementService.sendReengagementFor(user, firstRun)).isTrue();

        // User comes back briefly (login OR inbox visit), then disappears again.
        User after = users.findById(user.getId()).orElseThrow();
        LocalDateTime visit = firstRun.plusDays(2);
        after.setLastInboxVisitAt(visit);
        after.setLastLoginAt(visit);
        users.saveAndFlush(after);

        boolean second = reengagementService.sendReengagementFor(after, LocalDateTime.now());

        assertThat(second).isTrue();
    }

    @Test
    void recentlyActiveUserIsNotSweptIntoTheCycle() {
        User active = newUser("active@example.com");
        // last_login_at defaulted by registration is null, but created_at is now,
        // so the user is not dormant yet.
        newUnreadThread(active, "Subject A");

        int sent = reengagementService.runReengagementCycle();

        assertThat(sent).isEqualTo(0);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void runReengagementCycleSweepsOnlyDormantUsersAndCountsSends() {
        // Dormant + unread → sent.
        User dormantA = dormantUser("sweep-a@example.com", 14);
        newUnreadThread(dormantA, "Subject A");
        // Dormant + no unread → skipped.
        dormantUser("sweep-b@example.com", 14);
        // Recent → not in dormant query at all.
        User recent = newUser("sweep-c@example.com");
        newUnreadThread(recent, "Subject C");

        int sent = reengagementService.runReengagementCycle();

        assertThat(sent).isEqualTo(1);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void emailBodyMentionsUnreadCountAndCarriesUnsubscribeLink() throws Exception {
        User user = dormantUser("body@example.com", 9);
        newUnreadThread(user, "Subject A");
        newUnreadThread(user, "Subject B");

        boolean sent = reengagementService.sendReengagementFor(user, LocalDateTime.now());
        assertThat(sent).isTrue();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertThat(mime.getAllRecipients()).hasSize(1);
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("body@example.com");
        assertThat(mime.getSubject()).contains("2 unread threads");
        String body = (String) mime.getContent();
        DigestEmailPreference prefs = preferences.findByUser(user).orElseThrow();
        assertThat(body).contains("2 unread threads waiting");
        assertThat(body).contains("/digest/opt-out?token=" + prefs.getOptOutToken());
        assertThat(body).contains("/threads");
    }
}
