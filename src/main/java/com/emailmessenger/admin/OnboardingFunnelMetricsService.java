package com.emailmessenger.admin;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.SavedSearchRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.TeamInviteRepository;
import com.emailmessenger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rolling 30-day onboarding funnel for {@code /admin/revenue}. Anchors on
 * the signup cohort (users created in the window) and counts, step by step,
 * how many of those signups reached: mailbox connected → 10 threads
 * imported → saved a search → invited a teammate → active paid
 * subscription. The 10-thread threshold mirrors
 * {@link com.emailmessenger.web.OnboardingChecklist#THREADS_TARGET} so the
 * operator funnel and the in-product checkmark agree on what "imported"
 * means.
 */
@Service
public class OnboardingFunnelMetricsService {

    static final int WINDOW_DAYS = 30;
    static final long THREADS_TARGET = 10;

    private final UserRepository users;
    private final MailAccountRepository mailAccounts;
    private final EmailThreadRepository threads;
    private final SavedSearchRepository savedSearches;
    private final TeamInviteRepository teamInvites;
    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    OnboardingFunnelMetricsService(UserRepository users,
                                   MailAccountRepository mailAccounts,
                                   EmailThreadRepository threads,
                                   SavedSearchRepository savedSearches,
                                   TeamInviteRepository teamInvites,
                                   SubscriptionRepository subscriptions,
                                   Clock clock) {
        this.users = users;
        this.mailAccounts = mailAccounts;
        this.threads = threads;
        this.savedSearches = savedSearches;
        this.teamInvites = teamInvites;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OnboardingFunnelMetrics snapshot() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(WINDOW_DAYS);
        List<User> cohort = users.findCreatedAtAfter(cutoff);
        if (cohort.isEmpty()) {
            return OnboardingFunnelMetrics.empty(WINDOW_DAYS);
        }
        List<Long> ids = cohort.stream().map(User::getId).toList();

        int signups = cohort.size();
        int mailboxConnected = (int) mailAccounts.countDistinctOwnersIn(ids);
        int tenThreads = threads.findOwnerIdsWithAtLeastThreadsAmong(ids, THREADS_TARGET).size();
        int savedSearchSaved = (int) savedSearches.countDistinctOwnersIn(ids);
        int inviteSent = (int) teamInvites.countDistinctInvitersIn(ids);
        int paid = (int) subscriptions.countActiveOwnersIn(ids);

        return new OnboardingFunnelMetrics(
                WINDOW_DAYS,
                signups,
                mailboxConnected,
                tenThreads,
                savedSearchSaved,
                inviteSent,
                paid,
                percentOf(mailboxConnected, signups),
                percentOf(tenThreads, signups),
                percentOf(savedSearchSaved, signups),
                percentOf(inviteSent, signups),
                percentOf(paid, signups));
    }

    static int percentOf(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * numerator / denominator);
    }
}
