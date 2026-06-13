package com.emailmessenger.digest;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.DigestEmailPreference;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.SavedSearch;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.DigestEmailPreferenceRepository;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.repository.SavedSearchRepository;
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
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class WeeklyDigestServiceTest {

    @Autowired WeeklyDigestService digestService;
    @Autowired SavedSearchRepository savedSearches;
    @Autowired EmailThreadRepository threadRepo;
    @Autowired MessageRepository messageRepo;
    @Autowired ParticipantRepository participantRepo;
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

    private User newUser(String email) {
        userService.register(email, "password1", "Owner");
        return users.findByEmail(email).orElseThrow();
    }

    private void grantPersonal(User user) {
        Subscription sub = new Subscription(user, "cus_" + user.getId(), "active");
        sub.setPlan(Plan.PERSONAL);
        subscriptions.save(sub);
    }

    private EmailThread newThread(User owner, String subject, LocalDateTime sentAt,
                                  String senderEmail, String body) {
        Participant sender = participantRepo.findByEmail(senderEmail)
                .orElseGet(() -> participantRepo.save(new Participant(senderEmail, "Sender")));
        EmailThread t = threadRepo.save(new EmailThread(owner, subject, "<" + subject + "@test>"));
        Message m = new Message(t, sender, subject, body, "<p>" + body + "</p>", sentAt);
        m.setMessageIdHeader("<" + subject + "@test>");
        m.addRecipient(sender, RecipientType.TO);
        messageRepo.save(m);
        t.addMessage(m);
        return threadRepo.saveAndFlush(t);
    }

    @Test
    void paidUserWithNewMatchesGetsDigestAndLastSentAtUpdates() {
        User user = newUser("paid@example.com");
        grantPersonal(user);
        newThread(user, "Invoice March", LocalDateTime.now(ZoneOffset.UTC).minusDays(2),
                "ada@example.com", "monthly invoice");
        newThread(user, "Re Q1 review", LocalDateTime.now(ZoneOffset.UTC).minusDays(4),
                "ada@example.com", "review notes");
        savedSearches.save(new SavedSearch(user, "From Ada",
                null, "ada@example.com", null, false, false));

        boolean sent = digestService.sendDigestFor(user);

        assertThat(sent).isTrue();
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertThat(mime).isNotNull();
        DigestEmailPreference prefs = preferences.findByUser(user).orElseThrow();
        assertThat(prefs.getLastSentAt()).isNotNull();
        assertThat(prefs.getOptOutToken()).isNotBlank();
        assertThat(prefs.isOptedOut()).isFalse();
    }

    @Test
    void freeUserIsSkipped() {
        User user = newUser("free@example.com");
        newThread(user, "Hello", LocalDateTime.now(ZoneOffset.UTC).minusDays(1),
                "ada@example.com", "hi");
        savedSearches.save(new SavedSearch(user, "From Ada",
                null, "ada@example.com", null, false, false));

        boolean sent = digestService.sendDigestFor(user);

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
        assertThat(preferences.findByUser(user)).isEmpty();
    }

    @Test
    void optedOutUserIsSkippedButTokenRowIsPreserved() {
        User user = newUser("optout@example.com");
        grantPersonal(user);
        newThread(user, "Should not appear", LocalDateTime.now(ZoneOffset.UTC).minusDays(1),
                "ada@example.com", "hi");
        savedSearches.save(new SavedSearch(user, "From Ada",
                null, "ada@example.com", null, false, false));
        DigestEmailPreference existing = preferences.save(
                new DigestEmailPreference(user, "preset-opt-out-token-1234"));
        existing.setOptedOut(true);
        preferences.save(existing);

        boolean sent = digestService.sendDigestFor(user);

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
        DigestEmailPreference after = preferences.findByUser(user).orElseThrow();
        assertThat(after.isOptedOut()).isTrue();
        assertThat(after.getLastSentAt()).isNull();
    }

    @Test
    void paidUserWithoutNewMatchesIsSkippedAndNoMailSent() {
        User user = newUser("empty@example.com");
        grantPersonal(user);
        // Saved search exists but no thread matches in the 7-day window.
        savedSearches.save(new SavedSearch(user, "From Grace",
                null, "grace@example.com", null, false, false));

        boolean sent = digestService.sendDigestFor(user);

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
        // Pref row is provisioned so the token is ready next cycle.
        DigestEmailPreference prefs = preferences.findByUser(user).orElseThrow();
        assertThat(prefs.getLastSentAt()).isNull();
        assertThat(prefs.getOptOutToken()).isNotBlank();
    }

    @Test
    void runDigestCycleSweepsOnlyUsersWithSavedSearchesAndCountsSends() {
        User paidWithMatches = newUser("sweep1@example.com");
        grantPersonal(paidWithMatches);
        newThread(paidWithMatches, "Match thread", LocalDateTime.now(ZoneOffset.UTC).minusDays(2),
                "ada@example.com", "body");
        savedSearches.save(new SavedSearch(paidWithMatches, "From Ada",
                null, "ada@example.com", null, false, false));

        User freeWithSearch = newUser("sweep2@example.com");
        savedSearches.save(new SavedSearch(freeWithSearch, "From Ada",
                null, "ada@example.com", null, false, false));

        // A user with no saved searches at all should not be visited.
        newUser("sweep3-noss@example.com");

        int sent = digestService.runDigestCycle();

        assertThat(sent).isEqualTo(1);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void everyMimeMessageHasFromToAndUnsubscribeFooter() throws Exception {
        User user = newUser("footer@example.com");
        grantPersonal(user);
        newThread(user, "Subject A", LocalDateTime.now(ZoneOffset.UTC).minusDays(1),
                "ada@example.com", "body");
        savedSearches.save(new SavedSearch(user, "From Ada",
                null, "ada@example.com", null, false, false));

        boolean sent = digestService.sendDigestFor(user);

        assertThat(sent).isTrue();
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        assertThat(mime.getAllRecipients()).hasSize(1);
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("footer@example.com");
        assertThat(mime.getSubject()).contains("MailIM weekly digest");
        DigestEmailPreference prefs = preferences.findByUser(user).orElseThrow();
        String body = (String) mime.getContent();
        assertThat(body).contains("From Ada");
        assertThat(body).contains("Subject A");
        assertThat(body).contains("/digest/opt-out?token=" + prefs.getOptOutToken());
    }
}
