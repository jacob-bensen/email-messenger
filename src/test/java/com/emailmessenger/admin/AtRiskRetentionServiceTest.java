package com.emailmessenger.admin;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.CancellationReason;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtRiskRetentionServiceTest {

    @Mock SubscriptionRepository subscriptions;

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 11, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private AtRiskRetentionService service;

    @BeforeEach
    void setUp() {
        service = new AtRiskRetentionService(subscriptions, fixedClock);
        when(subscriptions.findRecoveredByWinBackSince(any())).thenReturn(List.of());
    }

    @Test
    void emptyDataReturnsEmptyQueueAndDisplayLimit() {
        when(subscriptions.findCanceledBetween(any(), any())).thenReturn(List.of());

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.windowDays()).isEqualTo(30);
        assertThat(m.entries()).isEmpty();
        assertThat(m.totalCanceledInWindow()).isZero();
        assertThat(m.totalRecoveredInWindow()).isZero();
        assertThat(m.displayLimit()).isEqualTo(20);
        assertThat(m.isTruncated()).isFalse();
    }

    @Test
    void queryWindowIsLast30Days() {
        when(subscriptions.findCanceledBetween(any(), any())).thenReturn(List.of());

        service.snapshot();

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(subscriptions).findCanceledBetween(from.capture(), to.capture());
        assertThat(from.getValue()).isEqualTo(now.minusDays(30));
        assertThat(to.getValue()).isEqualTo(now);

        ArgumentCaptor<LocalDateTime> recoveredCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(subscriptions).findRecoveredByWinBackSince(recoveredCutoff.capture());
        assertThat(recoveredCutoff.getValue()).isEqualTo(now.minusDays(30));
    }

    @Test
    void rowMapsPlanCadenceMrrSourceReasonAndCanceledAt() {
        Subscription sub = canceled("payer@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY,
                "producthunt", CancellationReason.TOO_EXPENSIVE,
                now.minusHours(2));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now)))
                .thenReturn(List.of(sub));

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.entries()).hasSize(1);
        AtRiskRetentionMetrics.Entry e = m.entries().get(0);
        assertThat(e.customerEmail()).isEqualTo("payer@example.com");
        assertThat(e.planLabel()).isEqualTo("Personal");
        assertThat(e.cadenceLabel()).isEqualTo("Monthly");
        assertThat(e.monthlyEquivalentCents()).isEqualTo(900L);
        assertThat(e.monthlyEquivalentFormatted()).isEqualTo("$9");
        assertThat(e.sourceLabel()).isEqualTo("producthunt");
        assertThat(e.reasonLabel()).isEqualTo("Too expensive");
        assertThat(e.canceledAt()).isEqualTo(now.minusHours(2));
        assertThat(e.recovered()).isFalse();
    }

    @Test
    void nullSourceAndNullReasonFallBackToOperatorReadableLabels() {
        Subscription sub = canceled("nosrc@example.com", Plan.TEAM, BillingPeriod.ANNUAL,
                null, null, now.minusDays(1));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now)))
                .thenReturn(List.of(sub));

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.entries().get(0).sourceLabel()).isEqualTo("Direct / unknown");
        assertThat(m.entries().get(0).reasonLabel()).isEqualTo("not recorded");
        assertThat(m.entries().get(0).cadenceLabel()).isEqualTo("Annual");
        assertThat(m.entries().get(0).monthlyEquivalentCents()).isEqualTo(2400L);
    }

    @Test
    void entriesAreSortedNewestFirst() {
        Subscription oldest = canceled("oldest@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, "google", CancellationReason.OTHER,
                now.minusDays(25));
        Subscription mid = canceled("mid@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, "google", CancellationReason.OTHER,
                now.minusDays(10));
        Subscription newest = canceled("newest@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, "google", CancellationReason.OTHER,
                now.minusHours(1));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now)))
                .thenReturn(List.of(oldest, mid, newest));

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.entries()).extracting(AtRiskRetentionMetrics.Entry::customerEmail)
                .containsExactly("newest@example.com", "mid@example.com", "oldest@example.com");
    }

    @Test
    void freeAndNullPlanRowsAreSkipped() {
        Subscription free = canceled("free@example.com", Plan.FREE, BillingPeriod.MONTHLY,
                null, null, now.minusDays(2));
        Subscription unknown = canceled("unknown@example.com", null, BillingPeriod.MONTHLY,
                null, null, now.minusDays(3));
        Subscription paid = canceled("paid@example.com", Plan.TEAM, BillingPeriod.MONTHLY,
                "twitter", CancellationReason.SWITCHING, now.minusDays(1));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now)))
                .thenReturn(List.of(free, unknown, paid));

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.entries()).hasSize(1);
        assertThat(m.entries().get(0).customerEmail()).isEqualTo("paid@example.com");
        assertThat(m.entries().get(0).reasonLabel()).isEqualTo("Switching tools");
        assertThat(m.totalCanceledInWindow()).isEqualTo(1);
    }

    @Test
    void displayLimitCapsAt20AndTruncationFlagFires() {
        List<Subscription> twentyFive = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            twentyFive.add(canceled("u" + i + "@example.com", Plan.PERSONAL,
                    BillingPeriod.MONTHLY, "ads", CancellationReason.OTHER,
                    now.minusHours(i + 1)));
        }
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now)))
                .thenReturn(twentyFive);

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.entries()).hasSize(20);
        assertThat(m.totalCanceledInWindow()).isEqualTo(25);
        assertThat(m.isTruncated()).isTrue();
        // Cap is applied AFTER newest-first sort — so u0 (1h ago) is in
        // and u24 (25h ago) is the first one dropped.
        assertThat(m.entries().get(0).customerEmail()).isEqualTo("u0@example.com");
        assertThat(m.entries().get(19).customerEmail()).isEqualTo("u19@example.com");
    }

    @Test
    void allFiveReasonEnumsRenderHumanReadableLabels() {
        Subscription tooExp = canceled("a@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, null, CancellationReason.TOO_EXPENSIVE, now.minusMinutes(1));
        Subscription missingFeat = canceled("b@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, null, CancellationReason.MISSING_FEATURE, now.minusMinutes(2));
        Subscription switching = canceled("c@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, null, CancellationReason.SWITCHING, now.minusMinutes(3));
        Subscription temp = canceled("d@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, null, CancellationReason.TEMPORARY, now.minusMinutes(4));
        Subscription other = canceled("e@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, null, CancellationReason.OTHER, now.minusMinutes(5));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now)))
                .thenReturn(List.of(tooExp, missingFeat, switching, temp, other));

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.entries()).extracting(AtRiskRetentionMetrics.Entry::reasonLabel)
                .containsExactly("Too expensive", "Missing feature", "Switching tools",
                        "Temporary", "Other");
    }

    @Test
    void entryCarriesSubscriptionIdAndWinBackTimestampForTheActionColumn() {
        Subscription notSent = canceled("fresh@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, "ads", CancellationReason.OTHER,
                now.minusHours(1));
        setId(notSent, 42L);
        Subscription alreadySent = canceled("alreadyemailed@example.com", Plan.TEAM,
                BillingPeriod.MONTHLY, "twitter", CancellationReason.OTHER,
                now.minusHours(2));
        setId(alreadySent, 99L);
        alreadySent.setLastWinBackEmailSentAt(now.minusMinutes(30));
        when(subscriptions.findCanceledBetween(any(), any()))
                .thenReturn(List.of(notSent, alreadySent));

        AtRiskRetentionMetrics m = service.snapshot();

        AtRiskRetentionMetrics.Entry e0 = m.entries().get(0);
        assertThat(e0.subscriptionId()).isEqualTo(42L);
        assertThat(e0.winBackSentAt()).isNull();
        assertThat(e0.winBackAlreadySent()).isFalse();
        assertThat(e0.recovered()).isFalse();
        AtRiskRetentionMetrics.Entry e1 = m.entries().get(1);
        assertThat(e1.subscriptionId()).isEqualTo(99L);
        assertThat(e1.winBackSentAt()).isEqualTo(now.minusMinutes(30));
        assertThat(e1.winBackAlreadySent()).isTrue();
        assertThat(e1.recovered()).isFalse();
    }

    @Test
    void recoveredRowsAreSurfacedWithRecoveredFlagSetAndCountedSeparately() {
        Subscription stillCanceled = canceled("walked@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, "ads", CancellationReason.TOO_EXPENSIVE,
                now.minusHours(4));
        setId(stillCanceled, 1L);
        Subscription comeback = active("comeback@example.com", Plan.TEAM,
                BillingPeriod.MONTHLY, "google",
                now.minusHours(1));
        setId(comeback, 2L);
        comeback.setLastWinBackEmailSentAt(now.minusHours(6));
        when(subscriptions.findCanceledBetween(any(), any()))
                .thenReturn(List.of(stillCanceled));
        when(subscriptions.findRecoveredByWinBackSince(eq(now.minusDays(30))))
                .thenReturn(List.of(comeback));

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.entries()).hasSize(2);
        assertThat(m.totalCanceledInWindow()).isEqualTo(1);
        assertThat(m.totalRecoveredInWindow()).isEqualTo(1);
        // Newest-first sort puts the just-recovered row at the top.
        AtRiskRetentionMetrics.Entry top = m.entries().get(0);
        assertThat(top.customerEmail()).isEqualTo("comeback@example.com");
        assertThat(top.recovered()).isTrue();
        assertThat(top.winBackAlreadySent()).isTrue();
        AtRiskRetentionMetrics.Entry below = m.entries().get(1);
        assertThat(below.customerEmail()).isEqualTo("walked@example.com");
        assertThat(below.recovered()).isFalse();
    }

    @Test
    void recoveredRowsAreDedupedAgainstCanceledCohortById() {
        // A subscription that's somehow returned by both queries — e.g. a
        // canceled row stamped with a win-back send — must not double up.
        Subscription sub = canceled("dup@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, null, CancellationReason.OTHER,
                now.minusHours(3));
        setId(sub, 777L);
        sub.setLastWinBackEmailSentAt(now.minusHours(1));
        when(subscriptions.findCanceledBetween(any(), any()))
                .thenReturn(List.of(sub));
        when(subscriptions.findRecoveredByWinBackSince(any()))
                .thenReturn(List.of(sub));

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.entries()).hasSize(1);
        assertThat(m.totalCanceledInWindow()).isEqualTo(1);
        assertThat(m.totalRecoveredInWindow()).isZero();
        // Canceled cohort wins the row — it carries the "Sent X ago"
        // timestamp, not the recovered badge.
        assertThat(m.entries().get(0).recovered()).isFalse();
    }

    @Test
    void freeAndNullPlanRecoveredRowsAreSkippedToo() {
        Subscription freeRecovered = active("free@example.com", Plan.FREE,
                BillingPeriod.MONTHLY, null, now.minusHours(1));
        setId(freeRecovered, 50L);
        freeRecovered.setLastWinBackEmailSentAt(now.minusDays(2));
        Subscription nullPlanRecovered = active("null@example.com", null,
                BillingPeriod.MONTHLY, null, now.minusHours(2));
        setId(nullPlanRecovered, 51L);
        nullPlanRecovered.setLastWinBackEmailSentAt(now.minusDays(2));
        Subscription paidRecovered = active("paid@example.com", Plan.PERSONAL,
                BillingPeriod.MONTHLY, "google", now.minusHours(3));
        setId(paidRecovered, 52L);
        paidRecovered.setLastWinBackEmailSentAt(now.minusDays(2));
        when(subscriptions.findCanceledBetween(any(), any())).thenReturn(List.of());
        when(subscriptions.findRecoveredByWinBackSince(any()))
                .thenReturn(List.of(freeRecovered, nullPlanRecovered, paidRecovered));

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.entries()).hasSize(1);
        assertThat(m.entries().get(0).customerEmail()).isEqualTo("paid@example.com");
        assertThat(m.entries().get(0).recovered()).isTrue();
        assertThat(m.totalRecoveredInWindow()).isEqualTo(1);
    }

    @Test
    void truncationFlagFiresWhenCanceledPlusRecoveredExceedsDisplayLimit() {
        List<Subscription> twelveCanceled = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            twelveCanceled.add(canceled("c" + i + "@example.com", Plan.PERSONAL,
                    BillingPeriod.MONTHLY, "ads", CancellationReason.OTHER,
                    now.minusHours(i + 1)));
        }
        List<Subscription> tenRecovered = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Subscription r = active("r" + i + "@example.com", Plan.TEAM,
                    BillingPeriod.MONTHLY, "google", now.minusDays(i + 1));
            setId(r, (long) (1000 + i));
            r.setLastWinBackEmailSentAt(now.minusDays(i + 2));
            tenRecovered.add(r);
        }
        when(subscriptions.findCanceledBetween(any(), any())).thenReturn(twelveCanceled);
        when(subscriptions.findRecoveredByWinBackSince(any())).thenReturn(tenRecovered);

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.entries()).hasSize(20);
        assertThat(m.totalCanceledInWindow()).isEqualTo(12);
        assertThat(m.totalRecoveredInWindow()).isEqualTo(10);
        assertThat(m.totalInWindow()).isEqualTo(22);
        assertThat(m.isTruncated()).isTrue();
    }

    private Subscription canceled(String email, Plan plan, BillingPeriod period,
                                  String source, CancellationReason reason,
                                  LocalDateTime canceledAt) {
        User user = new User(email, "hash", null);
        user.setAcquisitionSource(source);
        Subscription s = new Subscription(user, "cus_" + email, "canceled");
        s.setPlan(plan);
        s.setBillingPeriod(period);
        if (reason != null) {
            s.setCancellationReason(reason);
            s.setCancellationReasonAt(canceledAt);
        }
        setUpdatedAt(s, canceledAt);
        return s;
    }

    private Subscription active(String email, Plan plan, BillingPeriod period,
                                String source, LocalDateTime reactivatedAt) {
        User user = new User(email, "hash", null);
        user.setAcquisitionSource(source);
        Subscription s = new Subscription(user, "cus_" + email, "active");
        s.setPlan(plan);
        s.setBillingPeriod(period);
        setUpdatedAt(s, reactivatedAt);
        return s;
    }

    private static void setId(Subscription sub, Long id) {
        try {
            Field f = Subscription.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(sub, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setUpdatedAt(Subscription sub, LocalDateTime ts) {
        try {
            Field f = Subscription.class.getDeclaredField("updatedAt");
            f.setAccessible(true);
            f.set(sub, ts);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
