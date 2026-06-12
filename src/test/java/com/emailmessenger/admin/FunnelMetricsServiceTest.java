package com.emailmessenger.admin;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FunnelMetricsServiceTest {

    @Mock UserRepository users;
    @Mock SubscriptionRepository subscriptions;

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private FunnelMetricsService service;

    @BeforeEach
    void setUp() {
        service = new FunnelMetricsService(users, subscriptions, fixedClock);
    }

    @Test
    void emptyRepoYieldsZeroFunnelAndEmptySources() {
        when(users.findCreatedAtAfter(any())).thenReturn(List.of());
        when(subscriptions.findTrialCohortSince(any())).thenReturn(List.of());

        FunnelMetrics m = service.snapshot();
        assertThat(m.windowDays()).isEqualTo(30);
        assertThat(m.signupsLast30d()).isZero();
        assertThat(m.trialStartsLast30d()).isZero();
        assertThat(m.paidConversionsLast30d()).isZero();
        assertThat(m.trialRatePercent()).isZero();
        assertThat(m.paidRatePercent()).isZero();
        assertThat(m.bySource()).isEmpty();
    }

    @Test
    void cutoffPassedToRepositoriesIsThirtyDaysBeforeClockNow() {
        when(users.findCreatedAtAfter(any())).thenReturn(List.of());
        when(subscriptions.findTrialCohortSince(any())).thenReturn(List.of());

        service.snapshot();

        ArgumentCaptor<LocalDateTime> userCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> subCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(users).findCreatedAtAfter(userCutoff.capture());
        verify(subscriptions).findTrialCohortSince(subCutoff.capture());
        assertThat(userCutoff.getValue()).isEqualTo(now.minusDays(30));
        assertThat(subCutoff.getValue()).isEqualTo(now.minusDays(30));
    }

    @Test
    void trialRatePercentIsTrialStartsOverSignups() {
        when(users.findCreatedAtAfter(any())).thenReturn(signups(10, "twitter"));
        when(subscriptions.findTrialCohortSince(any())).thenReturn(List.of(
                trialing("a@example.com", "twitter"),
                trialing("b@example.com", "twitter"),
                trialing("c@example.com", "twitter")));

        FunnelMetrics m = service.snapshot();
        assertThat(m.signupsLast30d()).isEqualTo(10);
        assertThat(m.trialStartsLast30d()).isEqualTo(3);
        assertThat(m.trialRatePercent()).isEqualTo(30);
    }

    @Test
    void paidRatePercentIsActiveSubsOverTrialStartsIgnoringCanceled() {
        when(users.findCreatedAtAfter(any())).thenReturn(signups(10, "twitter"));
        when(subscriptions.findTrialCohortSince(any())).thenReturn(List.of(
                trialing("a@example.com", "twitter"),
                active("b@example.com", "twitter"),
                active("c@example.com", "twitter"),
                canceled("d@example.com", "twitter")));

        FunnelMetrics m = service.snapshot();
        assertThat(m.trialStartsLast30d()).isEqualTo(4);
        assertThat(m.paidConversionsLast30d()).isEqualTo(2);
        // 2/4 = 50%
        assertThat(m.paidRatePercent()).isEqualTo(50);
    }

    @Test
    void zeroDenominatorReturnsZeroPercentNotDivideByZero() {
        when(users.findCreatedAtAfter(any())).thenReturn(List.of());
        when(subscriptions.findTrialCohortSince(any())).thenReturn(List.of(
                active("orphan@example.com", "twitter")));

        FunnelMetrics m = service.snapshot();
        assertThat(m.signupsLast30d()).isZero();
        assertThat(m.trialRatePercent()).isZero();
        assertThat(m.paidRatePercent()).isEqualTo(100);
    }

    @Test
    void bySourceBreaksDownAndSortsBySignupCountDesc() {
        when(users.findCreatedAtAfter(any())).thenReturn(List.of(
                user("a@example.com", "producthunt"),
                user("b@example.com", "producthunt"),
                user("c@example.com", "producthunt"),
                user("d@example.com", "twitter"),
                user("e@example.com", "twitter"),
                user("f@example.com", null)));
        when(subscriptions.findTrialCohortSince(any())).thenReturn(List.of(
                active("a@example.com", "producthunt"),
                trialing("d@example.com", "twitter"),
                active("e@example.com", "twitter")));

        FunnelMetrics m = service.snapshot();
        assertThat(m.bySource()).hasSize(3);
        // producthunt (3 signups) ahead of twitter (2) ahead of Direct/unknown (1)
        assertThat(m.bySource().get(0).sourceLabel()).isEqualTo("producthunt");
        assertThat(m.bySource().get(0).signups()).isEqualTo(3);
        assertThat(m.bySource().get(0).trialStarts()).isEqualTo(1);
        assertThat(m.bySource().get(0).paidConversions()).isEqualTo(1);
        assertThat(m.bySource().get(0).trialRatePercent()).isEqualTo(33);
        assertThat(m.bySource().get(0).paidRatePercent()).isEqualTo(100);

        assertThat(m.bySource().get(1).sourceLabel()).isEqualTo("twitter");
        assertThat(m.bySource().get(1).signups()).isEqualTo(2);
        assertThat(m.bySource().get(1).trialStarts()).isEqualTo(2);
        assertThat(m.bySource().get(1).paidConversions()).isEqualTo(1);
        assertThat(m.bySource().get(1).paidRatePercent()).isEqualTo(50);

        assertThat(m.bySource().get(2).sourceLabel()).isEqualTo("Direct / unknown");
        assertThat(m.bySource().get(2).signups()).isEqualTo(1);
        assertThat(m.bySource().get(2).trialStarts()).isZero();
    }

    @Test
    void trialStartsForOlderSignupsStillAppearUnderTheirOwnSource() {
        // Older signup (before the window) but trial started inside it — the
        // per-source row still credits the trial to its acquisition source so
        // an "active sub created by an older user" doesn't get silently lost.
        when(users.findCreatedAtAfter(any())).thenReturn(List.of(
                user("new@example.com", "producthunt")));
        when(subscriptions.findTrialCohortSince(any())).thenReturn(List.of(
                active("old@example.com", "twitter")));

        FunnelMetrics m = service.snapshot();
        assertThat(m.bySource()).hasSize(2);
        SourceFunnelByLabel found = findBySource(m, "twitter");
        assertThat(found.signups).isZero();
        assertThat(found.trialStarts).isEqualTo(1);
        assertThat(found.paidConversions).isEqualTo(1);
    }

    @Test
    void blankAcquisitionSourceFoldsIntoDirectUnknownBucket() {
        when(users.findCreatedAtAfter(any())).thenReturn(List.of(
                user("a@example.com", null),
                user("b@example.com", "   "),
                user("c@example.com", "")));
        when(subscriptions.findTrialCohortSince(any())).thenReturn(List.of());

        FunnelMetrics m = service.snapshot();
        assertThat(m.bySource()).hasSize(1);
        assertThat(m.bySource().get(0).sourceLabel()).isEqualTo("Direct / unknown");
        assertThat(m.bySource().get(0).signups()).isEqualTo(3);
    }

    private record SourceFunnelByLabel(int signups, int trialStarts, int paidConversions) {}

    private static SourceFunnelByLabel findBySource(FunnelMetrics m, String label) {
        return m.bySource().stream()
                .filter(r -> r.sourceLabel().equals(label))
                .map(r -> new SourceFunnelByLabel(r.signups(), r.trialStarts(), r.paidConversions()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no source row for " + label));
    }

    private static List<User> signups(int n, String source) {
        List<User> out = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(user("s" + i + "@example.com", source));
        }
        return out;
    }

    private static User user(String email, String source) {
        User u = new User(email, "hash", null);
        u.setAcquisitionSource(source);
        return u;
    }

    private static Subscription trialing(String email, String source) {
        return sub(email, source, "trialing");
    }

    private static Subscription active(String email, String source) {
        return sub(email, source, "active");
    }

    private static Subscription canceled(String email, String source) {
        return sub(email, source, "canceled");
    }

    private static Subscription sub(String email, String source, String status) {
        User u = user(email, source);
        Subscription s = new Subscription(u, "cus_" + email, status);
        s.setPlan(Plan.PERSONAL);
        s.setBillingPeriod(BillingPeriod.MONTHLY);
        return s;
    }
}
