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
    void emptyDataYieldsZeroChurnAndAllThreePaidPlansInBreakdown() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of());
        when(subscriptions.findCanceledBetween(any(), any())).thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        assertThat(m.windowDays()).isEqualTo(30);
        assertThat(m.canceledSubscribers()).isZero();
        assertThat(m.lostMrrCents()).isZero();
        assertThat(m.lostMrrFormatted()).isEqualTo("$0");
        assertThat(m.lostArrCents()).isZero();
        assertThat(m.startingMrrCents()).isZero();
        assertThat(m.grossRevenueChurnRatePercent()).isZero();
        assertThat(m.byPlan()).hasSize(3);
        assertThat(m.byPlan()).extracting(ChurnMetrics.PlanChurnBreakdown::planLabel)
                .containsExactly("Personal", "Team", "Enterprise");
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
                canceled("a@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                canceled("b@example.com", Plan.PERSONAL, BillingPeriod.ANNUAL),
                canceled("c@example.com", Plan.TEAM,     BillingPeriod.MONTHLY),
                canceled("d@example.com", Plan.FREE,     BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        // 900 (Personal monthly) + 700 (Personal annual = monthly-eq) + 2900 (Team monthly)
        // = 4500 cents; FREE row excluded.
        assertThat(m.canceledSubscribers()).isEqualTo(3);
        assertThat(m.lostMrrCents()).isEqualTo(4500L);
        assertThat(m.lostMrrFormatted()).isEqualTo("$45");
        assertThat(m.lostArrCents()).isEqualTo(54_000L);
        assertThat(m.lostArrFormatted()).isEqualTo("$540");
    }

    @Test
    void perPlanBreakdownBucketsCancellationsByPlan() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of());
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("p1@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                canceled("p2@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                canceled("t1@example.com", Plan.TEAM,     BillingPeriod.MONTHLY),
                canceled("e1@example.com", Plan.ENTERPRISE, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        assertThat(m.byPlan()).hasSize(3);
        assertThat(m.byPlan().get(0).planLabel()).isEqualTo("Personal");
        assertThat(m.byPlan().get(0).canceledSubscribers()).isEqualTo(2);
        assertThat(m.byPlan().get(0).lostMrrCents()).isEqualTo(1800L);
        assertThat(m.byPlan().get(1).planLabel()).isEqualTo("Team");
        assertThat(m.byPlan().get(1).canceledSubscribers()).isEqualTo(1);
        assertThat(m.byPlan().get(1).lostMrrCents()).isEqualTo(2900L);
        assertThat(m.byPlan().get(2).planLabel()).isEqualTo("Enterprise");
        assertThat(m.byPlan().get(2).canceledSubscribers()).isEqualTo(1);
        assertThat(m.byPlan().get(2).lostMrrCents()).isEqualTo(9900L);
    }

    @Test
    void grossChurnRateIsLostMrrOverCurrentPlusLostMrr() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("keep1@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                active("keep2@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                active("keep3@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("gone@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of());

        ChurnMetrics m = service.snapshot();

        // current MRR = 3 × 900 = 2700; lost = 900; starting = 3600;
        // gross churn = 900 / 3600 = 25%
        assertThat(m.startingMrrCents()).isEqualTo(3600L);
        assertThat(m.startingMrrFormatted()).isEqualTo("$36");
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
                active("survivor@example.com", Plan.TEAM, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("recent@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of(
                        canceled("older@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                        canceled("older2@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY)));

        ChurnMetrics m = service.snapshot();

        assertThat(m.priorCanceledSubscribers()).isEqualTo(2);
        assertThat(m.priorLostMrrCents()).isEqualTo(1800L);
        assertThat(m.priorLostMrrFormatted()).isEqualTo("$18");

        // Current 1 vs. prior 2 → -50%; lost MRR 900 vs. 1800 → -50%.
        assertThat(m.canceledDeltaPercent()).isEqualTo(-50);
        assertThat(m.lostMrrDeltaPercent()).isEqualTo(-50);
        assertThat(m.canceledDeltaLabel()).isEqualTo("▼ 50% vs. prior 30 days");
        assertThat(m.lostMrrDeltaLabel()).isEqualTo("▼ 50% vs. prior 30 days");
    }

    @Test
    void churnRateDeltaIsInPercentagePointsNotPercent() {
        // Current churn rate: lost 900, starting = 900 + 0 = 900, rate = 100%
        // Prior churn rate: lost 0, prior starting = 0 + 0 + 0 (no current MRR), rate = 0%
        // Reuse a tighter setup: build current rate ≈ 10%, prior ≈ 20%.
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("a@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                active("b@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                active("c@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                active("d@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                active("e@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                active("f@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                active("g@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                active("h@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                active("i@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("recent@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(60)), eq(now.minusDays(30))))
                .thenReturn(List.of(
                        canceled("o1@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY),
                        canceled("o2@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY)));

        ChurnMetrics m = service.snapshot();

        // current rate = 900 / (9*900 + 900) = 900 / 9000 = 10%
        assertThat(m.grossRevenueChurnRatePercent()).isEqualTo(10);
        // prior rate = 1800 / (9*900 + 900 + 1800) = 1800 / 10800 = 17% (rounded)
        assertThat(m.priorGrossRevenueChurnRatePercent()).isEqualTo(17);
        assertThat(m.churnRateDeltaPoints()).isEqualTo(-7);
        assertThat(m.churnRateDeltaLabel()).isEqualTo("▼ 7 pts vs. prior 30 days");
    }

    @Test
    void deltaLabelsHandleNoPriorDataCases() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("a@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY)));
        when(subscriptions.findCanceledBetween(eq(now.minusDays(30)), eq(now))).thenReturn(List.of(
                canceled("recent@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY)));
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
