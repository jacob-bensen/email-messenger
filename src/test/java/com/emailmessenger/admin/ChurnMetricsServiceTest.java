package com.emailmessenger.admin;

import com.emailmessenger.billing.BillingPeriod;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChurnMetricsServiceTest {

    @Mock SubscriptionRepository subscriptions;

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 11, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private ChurnMetricsService service;

    @BeforeEach
    void setUp() {
        service = new ChurnMetricsService(subscriptions, fixedClock);
    }

    @Test
    void emptyDataYieldsZeroChurnAndBothPaidPlansInBreakdown() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of());
        when(subscriptions.findCanceledBetween(any(), any())).thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        assertThat(m.windowDays()).isEqualTo(30);
        assertThat(m.canceledSubscribers()).isZero();
        assertThat(m.lostMrrCents()).isZero();
        assertThat(m.lostMrrFormatted()).isEqualTo("$0.00");
        assertThat(m.lostArrCents()).isZero();
        assertThat(m.startingMrrCents()).isZero();
        assertThat(m.grossRevenueChurnRatePercent()).isZero();
        assertThat(m.byPlan()).hasSize(2);
        assertThat(m.byPlan()).extracting(ChurnMetrics.PlanChurnBreakdown::planLabel)
                .containsExactly("Pro", "Business");
    }

    @Test
    void cutoffsPassedToRepositoryAreWindowAndPriorBrackets() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of());
        when(subscriptions.findCanceledBetween(any(), any())).thenReturn(List.of());

        service.snapshot();

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(subscriptions, org.mockito.Mockito.times(2))
                .findCanceledBetween(from.capture(), to.capture());
        // First call = current window [now-30, now); second = prior [now-60, now-30).
        assertThat(from.getAllValues().get(0)).isEqualTo(now.minusDays(30));
        assertThat(to.getAllValues().get(0)).isEqualTo(now);
        assertThat(from.getAllValues().get(1)).isEqualTo(now.minusDays(60));
        assertThat(to.getAllValues().get(1)).isEqualTo(now.minusDays(30));
    }

    @Test
    void lostMrrSumsPerPlanMonthlyEquivalentsAndExcludesFree() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of());
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("a@example.com", Plan.PRO,      BillingPeriod.MONTHLY),
                canceled("b@example.com", Plan.PRO,      BillingPeriod.ANNUAL),
                canceled("c@example.com", Plan.BUSINESS, BillingPeriod.MONTHLY),
                canceled("d@example.com", Plan.FREE,     BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        // 699 (Pro monthly) + 599 (Pro annual = monthly-eq) + 0 (Business = custom)
        // = 1298 cents; FREE row excluded but the 3 paid cancels still count.
        assertThat(m.canceledSubscribers()).isEqualTo(3);
        assertThat(m.lostMrrCents()).isEqualTo(1298L);
        assertThat(m.lostMrrFormatted()).isEqualTo("$12.98");
        assertThat(m.lostArrCents()).isEqualTo(15_576L);
        assertThat(m.lostArrFormatted()).isEqualTo("$155.76");
    }

    @Test
    void perPlanBreakdownBucketsCancellationsByPlan() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of());
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("p1@example.com", Plan.PRO,      BillingPeriod.MONTHLY),
                canceled("p2@example.com", Plan.PRO,      BillingPeriod.MONTHLY),
                canceled("b1@example.com", Plan.BUSINESS, BillingPeriod.MONTHLY),
                canceled("b2@example.com", Plan.BUSINESS, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        // Service iterates plans in List.of(PRO, BUSINESS), so get(0)=Pro, get(1)=Business.
        assertThat(m.byPlan()).hasSize(2);
        assertThat(m.byPlan().get(0).planLabel()).isEqualTo("Pro");
        assertThat(m.byPlan().get(0).canceledSubscribers()).isEqualTo(2);
        // 2 × Pro monthly 699 = 1398c
        assertThat(m.byPlan().get(0).lostMrrCents()).isEqualTo(1398L);
        assertThat(m.byPlan().get(1).planLabel()).isEqualTo("Business");
        assertThat(m.byPlan().get(1).canceledSubscribers()).isEqualTo(2);
        // Business is custom/contact-sales — 0c MRR.
        assertThat(m.byPlan().get(1).lostMrrCents()).isEqualTo(0L);
    }

    @Test
    void grossChurnRateIsLostMrrOverCurrentPlusLostMrr() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("keep1@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                active("keep2@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                active("keep3@example.com", Plan.PRO, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("gone@example.com", Plan.PRO, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        // current MRR = 3 × 699 = 2097; lost = 699; starting = 2796;
        // gross churn = 699 / 2796 = 25%
        assertThat(m.startingMrrCents()).isEqualTo(2796L);
        assertThat(m.startingMrrFormatted()).isEqualTo("$27.96");
        assertThat(m.grossRevenueChurnRatePercent()).isEqualTo(25);
    }

    @Test
    void grossChurnRateIsZeroWhenStartingMrrIsZero() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of());
        when(subscriptions.findCanceledBetween(any(), any())).thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        assertThat(m.grossRevenueChurnRatePercent()).isZero();
    }

    @Test
    void priorWindowFiguresAndDeltasReflectChurnMomentum() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("survivor@example.com", Plan.PRO, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("recent@example.com", Plan.PRO, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of(
                        canceled("older@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                        canceled("older2@example.com", Plan.PRO, BillingPeriod.MONTHLY)));

        ChurnMetrics m = service.snapshot();

        assertThat(m.priorCanceledSubscribers()).isEqualTo(2);
        assertThat(m.priorLostMrrCents()).isEqualTo(1398L);
        assertThat(m.priorLostMrrFormatted()).isEqualTo("$13.98");

        // Current 1 vs. prior 2 → -50%; lost MRR 699 vs. 1398 → -50%.
        assertThat(m.canceledDeltaPercent()).isEqualTo(-50);
        assertThat(m.lostMrrDeltaPercent()).isEqualTo(-50);
        assertThat(m.canceledDeltaLabel()).isEqualTo("▼ 50% vs. prior 30 days");
        assertThat(m.lostMrrDeltaLabel()).isEqualTo("▼ 50% vs. prior 30 days");
    }

    @Test
    void churnRateDeltaIsInPercentagePointsNotPercent() {
        // Current churn rate: lost 699, starting = 699 + 0 = 699, rate = 100%
        // Prior churn rate: lost 0, prior starting = 0 + 0 + 0 (no current MRR), rate = 0%
        // Reuse a tighter setup: build current rate ≈ 10%, prior ≈ 17%.
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("a@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                active("b@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                active("c@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                active("d@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                active("e@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                active("f@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                active("g@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                active("h@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                active("i@example.com", Plan.PRO, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("recent@example.com", Plan.PRO, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of(
                        canceled("o1@example.com", Plan.PRO, BillingPeriod.MONTHLY),
                        canceled("o2@example.com", Plan.PRO, BillingPeriod.MONTHLY)));

        ChurnMetrics m = service.snapshot();

        // current rate = 699 / (9*699 + 699) = 699 / 6990 = 10%
        assertThat(m.grossRevenueChurnRatePercent()).isEqualTo(10);
        // prior rate = 1398 / (9*699 + 699 + 1398) = 1398 / 8388 = 17% (rounded)
        assertThat(m.priorGrossRevenueChurnRatePercent()).isEqualTo(17);
        assertThat(m.churnRateDeltaPoints()).isEqualTo(-7);
        assertThat(m.churnRateDeltaLabel()).isEqualTo("▼ 7 pts vs. prior 30 days");
    }

    @Test
    void deltaLabelsHandleNoPriorDataCases() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("a@example.com", Plan.PRO, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("recent@example.com", Plan.PRO, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        assertThat(m.canceledDeltaLabel()).isEqualTo("new vs. prior 30 days");
        assertThat(m.lostMrrDeltaLabel()).isEqualTo("new vs. prior 30 days");
        // Current rate is non-zero (50%), prior is zero — churn-rate label
        // shouldn't read "no prior-window data" because the operator can
        // tell from the current value that there's a signal.
        assertThat(m.churnRateDeltaLabel())
                .isEqualTo("▲ " + m.grossRevenueChurnRatePercent() + " pts vs. prior 30 days");
    }

    @Test
    void rowsWithoutPlanAreSkippedNotCounted() {
        Subscription noPlan = new Subscription(userWith("ghost@example.com"),
                "cus_ghost", "canceled");
        // plan intentionally left null — a pre-checkout or webhook-only row.
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of());
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now)))
                .thenReturn(List.of(noPlan));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        assertThat(m.canceledSubscribers()).isZero();
        assertThat(m.lostMrrCents()).isZero();
    }

    private Subscription active(String email, Plan plan, BillingPeriod period) {
        Subscription s = new Subscription(userWith(email), "cus_" + email, "active");
        s.setPlan(plan);
        s.setBillingPeriod(period);
        return s;
    }

    private Subscription canceled(String email, Plan plan, BillingPeriod period) {
        Subscription s = new Subscription(userWith(email), "cus_" + email, "canceled");
        s.setPlan(plan);
        s.setBillingPeriod(period);
        return s;
    }

    private static User userWith(String email) {
        return new User(email, "hash", null);
    }
}
