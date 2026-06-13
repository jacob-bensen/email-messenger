package com.emailmessenger.admin;

import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.SavedSearchRepository;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.TeamInviteRepository;
import com.emailmessenger.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingFunnelMetricsServiceTest {

    @Mock UserRepository users;
    @Mock MailAccountRepository mailAccounts;
    @Mock EmailThreadRepository threads;
    @Mock SavedSearchRepository savedSearches;
    @Mock TeamInviteRepository teamInvites;
    @Mock SubscriptionRepository subscriptions;

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private OnboardingFunnelMetricsService service;

    @BeforeEach
    void setUp() {
        service = new OnboardingFunnelMetricsService(
                users, mailAccounts, threads, savedSearches,
                teamInvites, subscriptions, fixedClock);
    }

    @Test
    void emptyCohortShortCircuitsToZerosWithoutHittingDownstreamRepos() {
        when(users.findCreatedAtAfter(any())).thenReturn(List.of());

        OnboardingFunnelMetrics m = service.snapshot();
        assertThat(m.windowDays()).isEqualTo(30);
        assertThat(m.signups()).isZero();
        assertThat(m.mailboxConnected()).isZero();
        assertThat(m.tenThreadsImported()).isZero();
        assertThat(m.savedSearchSaved()).isZero();
        assertThat(m.inviteSent()).isZero();
        assertThat(m.paid()).isZero();
        assertThat(m.mailboxRatePercent()).isZero();
        assertThat(m.tenThreadsRatePercent()).isZero();
        assertThat(m.savedSearchRatePercent()).isZero();
        assertThat(m.inviteRatePercent()).isZero();
        assertThat(m.paidRatePercent()).isZero();
        verify(mailAccounts, never()).countDistinctOwnersIn(any());
        verify(threads, never()).findOwnerIdsWithAtLeastThreadsAmong(any(), anyLong());
        verify(savedSearches, never()).countDistinctOwnersIn(any());
        verify(teamInvites, never()).countDistinctInvitersIn(any());
        verify(subscriptions, never()).countActiveOwnersIn(any());
    }

    @Test
    void cutoffPassedToUserRepoIsThirtyDaysBeforeClockNow() {
        when(users.findCreatedAtAfter(any())).thenReturn(List.of());

        service.snapshot();

        ArgumentCaptor<LocalDateTime> userCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(users).findCreatedAtAfter(userCutoff.capture());
        assertThat(userCutoff.getValue()).isEqualTo(now.minusDays(30));
    }

    @Test
    void everyStepCountsAndRatesAreSignupAnchored() {
        when(users.findCreatedAtAfter(any())).thenReturn(cohort(10));
        when(mailAccounts.countDistinctOwnersIn(any())).thenReturn(8L);
        when(threads.findOwnerIdsWithAtLeastThreadsAmong(any(), eq(10L)))
                .thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
        when(savedSearches.countDistinctOwnersIn(any())).thenReturn(3L);
        when(teamInvites.countDistinctInvitersIn(any())).thenReturn(2L);
        when(subscriptions.countActiveOwnersIn(any())).thenReturn(1L);

        OnboardingFunnelMetrics m = service.snapshot();
        assertThat(m.signups()).isEqualTo(10);
        assertThat(m.mailboxConnected()).isEqualTo(8);
        assertThat(m.tenThreadsImported()).isEqualTo(5);
        assertThat(m.savedSearchSaved()).isEqualTo(3);
        assertThat(m.inviteSent()).isEqualTo(2);
        assertThat(m.paid()).isEqualTo(1);
        // All rates are step / signups, not step / previous-step — the
        // operator reads each percent as "what fraction of cohort
        // reached this step", which makes the largest drop the next leak.
        assertThat(m.mailboxRatePercent()).isEqualTo(80);
        assertThat(m.tenThreadsRatePercent()).isEqualTo(50);
        assertThat(m.savedSearchRatePercent()).isEqualTo(30);
        assertThat(m.inviteRatePercent()).isEqualTo(20);
        assertThat(m.paidRatePercent()).isEqualTo(10);
    }

    @Test
    void cohortIdsPassedToEveryDownstreamRepoMatchTheUserCohort() {
        when(users.findCreatedAtAfter(any())).thenReturn(cohort(3));
        when(mailAccounts.countDistinctOwnersIn(any())).thenReturn(0L);
        when(threads.findOwnerIdsWithAtLeastThreadsAmong(any(), anyLong()))
                .thenReturn(List.of());
        when(savedSearches.countDistinctOwnersIn(any())).thenReturn(0L);
        when(teamInvites.countDistinctInvitersIn(any())).thenReturn(0L);
        when(subscriptions.countActiveOwnersIn(any())).thenReturn(0L);

        service.snapshot();

        ArgumentCaptor<Collection<Long>> ma = idsCaptor();
        verify(mailAccounts).countDistinctOwnersIn(ma.capture());
        assertThat(ma.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);

        ArgumentCaptor<Collection<Long>> th = idsCaptor();
        ArgumentCaptor<Long> threshold = ArgumentCaptor.forClass(Long.class);
        verify(threads).findOwnerIdsWithAtLeastThreadsAmong(th.capture(), threshold.capture());
        assertThat(th.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(threshold.getValue()).isEqualTo(10L);

        ArgumentCaptor<Collection<Long>> ss = idsCaptor();
        verify(savedSearches).countDistinctOwnersIn(ss.capture());
        assertThat(ss.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);

        ArgumentCaptor<Collection<Long>> ti = idsCaptor();
        verify(teamInvites).countDistinctInvitersIn(ti.capture());
        assertThat(ti.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);

        ArgumentCaptor<Collection<Long>> sub = idsCaptor();
        verify(subscriptions).countActiveOwnersIn(sub.capture());
        assertThat(sub.getValue()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void zeroPaidConversionsStillRendersZeroPercentNotDivideByZero() {
        when(users.findCreatedAtAfter(any())).thenReturn(cohort(4));
        when(mailAccounts.countDistinctOwnersIn(any())).thenReturn(0L);
        when(threads.findOwnerIdsWithAtLeastThreadsAmong(any(), anyLong()))
                .thenReturn(List.of());
        when(savedSearches.countDistinctOwnersIn(any())).thenReturn(0L);
        when(teamInvites.countDistinctInvitersIn(any())).thenReturn(0L);
        when(subscriptions.countActiveOwnersIn(any())).thenReturn(0L);

        OnboardingFunnelMetrics m = service.snapshot();
        assertThat(m.signups()).isEqualTo(4);
        assertThat(m.paid()).isZero();
        assertThat(m.paidRatePercent()).isZero();
    }

    @Test
    void ratesRoundHalfUpToNearestPercent() {
        when(users.findCreatedAtAfter(any())).thenReturn(cohort(3));
        // 1/3 = 33.33% → rounds to 33; 2/3 = 66.67% → rounds to 67.
        when(mailAccounts.countDistinctOwnersIn(any())).thenReturn(1L);
        when(threads.findOwnerIdsWithAtLeastThreadsAmong(any(), anyLong()))
                .thenReturn(List.of(1L, 2L));
        when(savedSearches.countDistinctOwnersIn(any())).thenReturn(0L);
        when(teamInvites.countDistinctInvitersIn(any())).thenReturn(0L);
        when(subscriptions.countActiveOwnersIn(any())).thenReturn(0L);

        OnboardingFunnelMetrics m = service.snapshot();
        assertThat(m.mailboxRatePercent()).isEqualTo(33);
        assertThat(m.tenThreadsRatePercent()).isEqualTo(67);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Collection<Long>> idsCaptor() {
        return ArgumentCaptor.forClass((Class) Collection.class);
    }

    private static List<User> cohort(int n) {
        List<User> out = new java.util.ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            out.add(userWithId((long) i));
        }
        return out;
    }

    private static User userWithId(Long id) {
        User u = new User("u" + id + "@example.com", "hash", null);
        try {
            Field f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return u;
    }
}
