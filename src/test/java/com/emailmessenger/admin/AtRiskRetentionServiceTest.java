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
    }

    @Test
    void emptyDataReturnsEmptyQueueAndDisplayLimit() {
        when(subscriptions.findCanceledBetween(any(), any())).thenReturn(List.of());

        AtRiskRetentionMetrics m = service.snapshot();

        assertThat(m.windowDays()).isEqualTo(30);
        assertThat(m.entries()).isEmpty();
        assertThat(m.totalCanceledInWindow()).isZero();
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
        AtRiskRetentionMetrics.Entry e1 = m.entries().get(1);
        assertThat(e1.subscriptionId()).isEqualTo(99L);
        assertThat(e1.winBackSentAt()).isEqualTo(now.minusMinutes(30));
        assertThat(e1.winBackAlreadySent()).isTrue();
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
