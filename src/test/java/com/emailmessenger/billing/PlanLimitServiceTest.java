package com.emailmessenger.billing;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.domain.SavedSearch;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class PlanLimitServiceTest {

    @Autowired PlanLimitService planLimitService;
    @Autowired UserService userService;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired EmailThreadRepository threads;
    @Autowired MailAccountRepository mailAccounts;
    @Autowired SavedSearchRepository savedSearches;

    @MockBean StripeCheckoutGateway gateway;
    @MockBean StripePortalGateway portalGateway;
    @MockBean ReplyService replyService;

    private User newUser(String email) {
        userService.register(email, "password1", "Test");
        return users.findByEmail(email).orElseThrow();
    }

    @Test
    void userWithoutSubscriptionIsFree() {
        User user = newUser("nosub@example.com");
        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.FREE);
        assertThat(planLimitService.limitsFor(user).threads()).isEqualTo(500);
    }

    @Test
    void incompleteSubscriptionStillCountsAsFree() {
        User user = newUser("incomplete@example.com");
        Subscription sub = new Subscription(user, "cus_incomplete", "incomplete");
        sub.setPlan(Plan.PERSONAL);
        subscriptions.save(sub);

        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.FREE);
    }

    @Test
    void trialingSubscriptionGrantsPaidEntitlements() {
        User user = newUser("trial@example.com");
        Subscription sub = new Subscription(user, "cus_trial", "trialing");
        sub.setPlan(Plan.PERSONAL);
        subscriptions.save(sub);

        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.PERSONAL);
        assertThat(planLimitService.limitsFor(user).threads()).isEqualTo(PlanLimits.UNLIMITED);
    }

    @Test
    void canceledSubscriptionRevertsUserToFree() {
        User user = newUser("canceled@example.com");
        Subscription sub = new Subscription(user, "cus_canceled", "canceled");
        sub.setPlan(Plan.PERSONAL);
        subscriptions.save(sub);

        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.FREE);
    }

    @Test
    void pastDueKeepsAccessSoStripeCanRetry() {
        User user = newUser("pastdue@example.com");
        Subscription sub = new Subscription(user, "cus_past", "past_due");
        sub.setPlan(Plan.TEAM);
        subscriptions.save(sub);

        assertThat(planLimitService.currentPlan(user)).isEqualTo(Plan.TEAM);
    }

    @Test
    void enforceCanCreateThreadPassesUnderLimitForFreeUser() {
        User user = newUser("under@example.com");
        seedThreads(user, 5);

        planLimitService.enforceCanCreateThread(user); // no throw
    }

    @Test
    void enforceCanCreateThreadThrowsAtFreeCap() {
        User user = newUser("atcap@example.com");
        seedThreads(user, 500);

        PlanLimitExceededException ex = catchThrowableOfType(
                () -> planLimitService.enforceCanCreateThread(user),
                PlanLimitExceededException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getCurrentPlan()).isEqualTo(Plan.FREE);
        assertThat(ex.getKind()).isEqualTo(PlanLimitKind.THREAD_COUNT);
        assertThat(ex.getLimit()).isEqualTo(500);
        assertThat(ex.getCurrent()).isEqualTo(500);
    }

    @Test
    void enforceCanCreateThreadNoOpsForPaidPlan() {
        User user = newUser("paid@example.com");
        Subscription sub = new Subscription(user, "cus_paid", "active");
        sub.setPlan(Plan.PERSONAL);
        subscriptions.save(sub);
        // Drop in a few threads just to be sure we don't compare to a stale cap.
        seedThreads(user, 10);

        planLimitService.enforceCanCreateThread(user); // no throw even though 10 > free cap of 500 only — here we're paid
    }

    @Test
    void freePlanLimitsAreExposedThroughLimitsFor() {
        User user = newUser("limits@example.com");
        PlanLimits caps = planLimitService.limitsFor(user);
        assertThat(caps.mailboxes()).isEqualTo(1);
        assertThat(caps.threads()).isEqualTo(500);
    }

    @Test
    void enforceCanCreateMailboxPassesUnderFreeCap() {
        User user = newUser("mbox-under@example.com");

        planLimitService.enforceCanCreateMailbox(user); // 0 mailboxes < 1 cap, no throw
    }

    @Test
    void enforceCanCreateMailboxThrowsAtFreeCap() {
        User user = newUser("mbox-cap@example.com");
        mailAccounts.save(new MailAccount(user, "imap.example.com", 993, true,
                "first@example.com", "ciphertext"));

        PlanLimitExceededException ex = catchThrowableOfType(
                () -> planLimitService.enforceCanCreateMailbox(user),
                PlanLimitExceededException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getCurrentPlan()).isEqualTo(Plan.FREE);
        assertThat(ex.getKind()).isEqualTo(PlanLimitKind.MAILBOX_COUNT);
        assertThat(ex.getLimit()).isEqualTo(1);
        assertThat(ex.getCurrent()).isEqualTo(1);
    }

    @Test
    void enforceCanCreateMailboxAllowsThreeOnPersonal() {
        User user = newUser("mbox-personal@example.com");
        Subscription sub = new Subscription(user, "cus_mbox", "active");
        sub.setPlan(Plan.PERSONAL);
        subscriptions.save(sub);
        for (int i = 0; i < 2; i++) {
            mailAccounts.save(new MailAccount(user, "host" + i, 993, true,
                    "u" + i + "@example.com", "ct"));
        }

        planLimitService.enforceCanCreateMailbox(user); // 2 < 3, no throw
    }

    @Test
    void enforceCanCreateSavedSearchPassesUnderFreeCap() {
        User user = newUser("ss-under@example.com");

        planLimitService.enforceCanCreateSavedSearch(user); // 0 saved < 1 cap, no throw
    }

    @Test
    void enforceCanCreateSavedSearchThrowsAtFreeCap() {
        User user = newUser("ss-cap@example.com");
        savedSearches.save(new SavedSearch(user, "First", null,
                "ada@example.com", null, false, false));

        PlanLimitExceededException ex = catchThrowableOfType(
                () -> planLimitService.enforceCanCreateSavedSearch(user),
                PlanLimitExceededException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getCurrentPlan()).isEqualTo(Plan.FREE);
        assertThat(ex.getKind()).isEqualTo(PlanLimitKind.SAVED_SEARCH_COUNT);
        assertThat(ex.getLimit()).isEqualTo(1);
        assertThat(ex.getCurrent()).isEqualTo(1);
    }

    @Test
    void enforceCanCreateSavedSearchNoOpsForPaidPlan() {
        User user = newUser("ss-paid@example.com");
        Subscription sub = new Subscription(user, "cus_ss_paid", "active");
        sub.setPlan(Plan.PERSONAL);
        subscriptions.save(sub);
        for (int i = 0; i < 3; i++) {
            savedSearches.save(new SavedSearch(user, "s" + i, null,
                    "x" + i + "@example.com", null, false, false));
        }

        planLimitService.enforceCanCreateSavedSearch(user); // no throw despite >Free cap
    }

    @Test
    void freePlanSavedSearchCapIsOne() {
        User user = newUser("ss-limit@example.com");
        PlanLimits caps = planLimitService.limitsFor(user);
        assertThat(caps.savedSearches()).isEqualTo(1);
    }

    private void seedThreads(User owner, int count) {
        for (int i = 0; i < count; i++) {
            threads.save(new EmailThread(owner, "subject " + i, "<m" + i + "@t>"));
        }
    }
}
