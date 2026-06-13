package com.emailmessenger.web;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.SavedSearch;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.repository.SavedSearchRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class SavedSearchCountServiceTest {

    @Autowired SavedSearchCountService countService;
    @Autowired SavedSearchRepository savedSearches;
    @Autowired EmailThreadRepository threadRepo;
    @Autowired MessageRepository messageRepo;
    @Autowired ParticipantRepository participantRepo;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;

    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    private User newUser(String email) {
        userService.register(email, "password1", "Owner");
        return users.findByEmail(email).orElseThrow();
    }

    private void grantPersonal(User user) {
        Subscription sub = new Subscription(user, "cus_" + user.getId(), "active");
        sub.setPlan(Plan.PERSONAL);
        subscriptions.save(sub);
    }

    /**
     * Spin until the wall clock is at least 1ms past {@code t}. Entity
     * timestamps are flush-assigned from the real clock; two consecutive
     * stamps can land in the same tick (and DB column precision truncates
     * further), so tests separating "before" from "after" a cutoff must
     * force the clock forward between the two writes.
     */
    private void awaitClockPast(LocalDateTime t) {
        while (!LocalDateTime.now(ZoneOffset.UTC).isAfter(t.plusNanos(1_000_000))) {
            Thread.onSpinWait();
        }
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
    void matchCountReflectsCurrentFilterAndNewCountStartsAtMatchOnFreshSave() {
        User user = newUser("counts1@example.com");
        grantPersonal(user);
        newThread(user, "Invoice March", LocalDateTime.now(ZoneOffset.UTC).minusDays(2),
                "ada@example.com", "monthly invoice attached");
        newThread(user, "Invoice April", LocalDateTime.now(ZoneOffset.UTC).minusDays(1),
                "ada@example.com", "monthly invoice attached");
        EmailThread last = newThread(user, "Lunch tomorrow?", LocalDateTime.now(ZoneOffset.UTC),
                "grace@example.com", "see you at noon");

        awaitClockPast(last.getUpdatedAt());
        SavedSearch saved = savedSearches.save(new SavedSearch(user, "From Ada",
                null, "ada@example.com", null, false, false));

        List<SavedSearchView> views = countService.viewsFor(user, List.of(saved));
        assertThat(views).hasSize(1);
        SavedSearchView v = views.get(0);
        // Two threads have ada@example.com as the sender; the third is from grace@.
        assertThat(v.matchCount()).isEqualTo(2);
        // Never visited → newCount falls back to created_at cutoff. Both threads
        // pre-date the saved search, so newCount is 0.
        assertThat(v.newCount()).isEqualTo(0);
        assertThat(v.hasNew()).isFalse();
    }

    @Test
    void newCountCountsOnlyThreadsUpdatedAfterLastViewed() {
        User user = newUser("counts2@example.com");
        grantPersonal(user);
        // One pre-existing thread the user already knew about
        EmailThread old = newThread(user, "Old invoice",
                LocalDateTime.now(ZoneOffset.UTC).minusDays(10), "ada@example.com", "old");

        SavedSearch saved = savedSearches.saveAndFlush(new SavedSearch(user, "From Ada",
                null, "ada@example.com", null, false, false));
        // User opened it once, clearing the badge for everything up to "now",
        // strictly after the old thread's flush-assigned updatedAt.
        awaitClockPast(old.getUpdatedAt());
        saved.setLastViewedAt(LocalDateTime.now(ZoneOffset.UTC));
        savedSearches.saveAndFlush(saved);

        // Now a new thread comes in, strictly after the last-viewed cutoff
        awaitClockPast(saved.getLastViewedAt());
        newThread(user, "New invoice", LocalDateTime.now(ZoneOffset.UTC),
                "ada@example.com", "fresh");

        List<SavedSearchView> views = countService.viewsFor(user, List.of(saved));
        SavedSearchView v = views.get(0);
        assertThat(v.matchCount()).isEqualTo(2);
        assertThat(v.newCount()).isEqualTo(1);
        assertThat(v.hasNew()).isTrue();
    }

    @Test
    void freeUserGetsSubjectAndParticipantSearchCountsOnly() {
        User user = newUser("countsfree@example.com");
        // Free plan — body-content matches are excluded.
        newThread(user, "Subject hit", LocalDateTime.now(ZoneOffset.UTC).minusDays(2),
                "ada@example.com", "no keyword inside");
        newThread(user, "Random subject", LocalDateTime.now(ZoneOffset.UTC).minusDays(1),
                "grace@example.com", "this body has the keyword urgentpayment somewhere");

        SavedSearch saved = savedSearches.save(new SavedSearch(user, "urgentpayment",
                "urgentpayment", null, null, false, false));

        List<SavedSearchView> views = countService.viewsFor(user, List.of(saved));
        // Free should NOT pick up the body-only match → 0 matches.
        assertThat(views.get(0).matchCount()).isEqualTo(0);
    }

    @Test
    void personalPlanIncludesBodyContentSearchInCounts() {
        User user = newUser("countspaid@example.com");
        grantPersonal(user);
        newThread(user, "Random subject", LocalDateTime.now(ZoneOffset.UTC).minusDays(1),
                "grace@example.com", "this body has the keyword urgentpayment somewhere");

        SavedSearch saved = savedSearches.save(new SavedSearch(user, "urgentpayment",
                "urgentpayment", null, null, false, false));

        List<SavedSearchView> views = countService.viewsFor(user, List.of(saved));
        assertThat(views.get(0).matchCount()).isEqualTo(1);
    }

    @Test
    void emptyInputReturnsEmptyListWithoutLoadingPlan() {
        User user = newUser("countsempty@example.com");

        assertThat(countService.viewsFor(user, List.of())).isEmpty();
    }
}
