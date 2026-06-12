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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WinBackConversionMetricsServiceTest {

    @Mock SubscriptionRepository subscriptions;

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 12, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private WinBackConversionMetricsService service;

    @BeforeEach
    void setUp() {
        service = new WinBackConversionMetricsService(subscriptions, fixedClock);
    }

    @Test
    void emptyRepoYieldsAllZeros() {
        when(subscriptions.findWinBackEmailedSince(any())).thenReturn(List.of());

        WinBackConversionMetrics m = service.snapshot();

        assertThat(m.windowDays()).isEqualTo(30);
        assertThat(m.emailsSent()).isZero();
        assertThat(m.reactivated()).isZero();
        assertThat(m.mrrRecoveredCents()).isZero();
        assertThat(m.mrrRecoveredFormatted()).isEqualTo("$0");
        assertThat(m.conversionRatePercent()).isZero();
        assertThat(m.priorEmailsSent()).isZero();
        assertThat(m.priorReactivated()).isZero();
        assertThat(m.priorMrrRecoveredCents()).isZero();
        assertThat(m.priorConversionRatePercent()).isZero();
    }

    @Test
    void repoCutoffSpansBothWindows() {
        when(subscriptions.findWinBackEmailedSince(any())).thenReturn(List.of());

        service.snapshot();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(subscriptions).findWinBackEmailedSince(cutoff.capture());
        // 60 days back so the service can partition into current [now-30, now)
        // and prior [now-60, now-30).
        assertThat(cutoff.getValue()).isEqualTo(now.minusDays(60));
    }

    @Test
    void countsCurrentWindowSentAndReactivatedAndMrr() {
        when(subscriptions.findWinBackEmailedSince(any())).thenReturn(List.of(
                emailed("a@example.com", "active", Plan.PERSONAL, BillingPeriod.MONTHLY, now.minusDays(1)),
                emailed("b@example.com", "active", Plan.TEAM, BillingPeriod.MONTHLY, now.minusDays(5)),
                emailed("c@example.com", "canceled", Plan.PERSONAL, BillingPeriod.MONTHLY, now.minusDays(10)),
                emailed("d@example.com", "canceled", Plan.ENTERPRISE, BillingPeriod.ANNUAL, now.minusDays(15))));

        WinBackConversionMetrics m = service.snapshot();

        assertThat(m.emailsSent()).isEqualTo(4);
        assertThat(m.reactivated()).isEqualTo(2);
        // Personal monthly 900 + Team monthly 2900 = 3800c
        assertThat(m.mrrRecoveredCents()).isEqualTo(3800L);
        assertThat(m.mrrRecoveredFormatted()).isEqualTo("$38");
        // 2 / 4 = 50%
        assertThat(m.conversionRatePercent()).isEqualTo(50);
    }

    @Test
    void priorWindowFiguresMirrorCurrentAndDeltasShowMomentum() {
        when(subscriptions.findWinBackEmailedSince(any())).thenReturn(List.of(
                // Current window: 2 sent, 1 reactivated, 900c recovered
                emailed("now1@example.com", "active", Plan.PERSONAL, BillingPeriod.MONTHLY, now.minusDays(5)),
                emailed("now2@example.com", "canceled", Plan.PERSONAL, BillingPeriod.MONTHLY, now.minusDays(20)),
                // Prior window: 4 sent, 2 reactivated, 1800c recovered
                emailed("p1@example.com", "active", Plan.PERSONAL, BillingPeriod.MONTHLY, now.minusDays(40)),
                emailed("p2@example.com", "active", Plan.PERSONAL, BillingPeriod.MONTHLY, now.minusDays(45)),
                emailed("p3@example.com", "canceled", Plan.PERSONAL, BillingPeriod.MONTHLY, now.minusDays(50)),
                emailed("p4@example.com", "canceled", Plan.PERSONAL, BillingPeriod.MONTHLY, now.minusDays(55))));

        WinBackConversionMetrics m = service.snapshot();

        assertThat(m.emailsSent()).isEqualTo(2);
        assertThat(m.reactivated()).isEqualTo(1);
        assertThat(m.mrrRecoveredCents()).isEqualTo(900L);
        assertThat(m.conversionRatePercent()).isEqualTo(50);

        assertThat(m.priorEmailsSent()).isEqualTo(4);
        assertThat(m.priorReactivated()).isEqualTo(2);
        assertThat(m.priorMrrRecoveredCents()).isEqualTo(1800L);
        assertThat(m.priorMrrRecoveredFormatted()).isEqualTo("$18");
        assertThat(m.priorConversionRatePercent()).isEqualTo(50);

        // Current 2 vs prior 4 → -50%; recovered 900 vs 1800 → -50%.
        assertThat(m.emailsSentDeltaPercent()).isEqualTo(-50);
        assertThat(m.mrrRecoveredDeltaPercent()).isEqualTo(-50);
        assertThat(m.emailsSentDeltaLabel()).isEqualTo("▼ 50% vs. prior 30 days");
        assertThat(m.mrrRecoveredDeltaLabel()).isEqualTo("▼ 50% vs. prior 30 days");
        // Rate held flat at 50%.
        assertThat(m.conversionRateDeltaPoints()).isZero();
        assertThat(m.conversionRateDeltaLabel()).isEqualTo("flat vs. prior 30 days");
    }

    @Test
    void boundaryAtWindowStartCountsAsCurrentWindow() {
        when(subscriptions.findWinBackEmailedSince(any())).thenReturn(List.of(
                emailed("edge@example.com", "active", Plan.PERSONAL,
                        BillingPeriod.MONTHLY, now.minusDays(30))));

        WinBackConversionMetrics m = service.snapshot();

        assertThat(m.emailsSent()).isEqualTo(1);
        assertThat(m.priorEmailsSent()).isZero();
    }

    @Test
    void freeOrNullPlanRowsAreSkipped() {
        when(subscriptions.findWinBackEmailedSince(any())).thenReturn(List.of(
                emailed("free@example.com", "active", Plan.FREE,
                        BillingPeriod.MONTHLY, now.minusDays(5)),
                emailedWithNullPlan("null@example.com", "active", now.minusDays(5))));

        WinBackConversionMetrics m = service.snapshot();

        assertThat(m.emailsSent()).isZero();
        assertThat(m.reactivated()).isZero();
        assertThat(m.mrrRecoveredCents()).isZero();
    }

    @Test
    void conversionRateDeltaLabelReadsNoPriorDataWhenBothZero() {
        when(subscriptions.findWinBackEmailedSince(any())).thenReturn(List.of());

        WinBackConversionMetrics m = service.snapshot();

        assertThat(m.conversionRateDeltaLabel()).isEqualTo("no prior-window data");
    }

    @Test
    void newDeltaLabelWhenCurrentNonZeroAndPriorZero() {
        when(subscriptions.findWinBackEmailedSince(any())).thenReturn(List.of(
                emailed("a@example.com", "active", Plan.PERSONAL,
                        BillingPeriod.MONTHLY, now.minusDays(2))));

        WinBackConversionMetrics m = service.snapshot();

        assertThat(m.emailsSentDeltaLabel()).isEqualTo("new vs. prior 30 days");
        assertThat(m.reactivatedDeltaLabel()).isEqualTo("new vs. prior 30 days");
        assertThat(m.mrrRecoveredDeltaLabel()).isEqualTo("new vs. prior 30 days");
    }

    private Subscription emailed(String email, String status, Plan plan,
                                 BillingPeriod period, LocalDateTime sentAt) {
        Subscription s = new Subscription(new User(email, "hash", null),
                "cus_" + email, status);
        s.setPlan(plan);
        s.setBillingPeriod(period);
        s.setLastWinBackEmailSentAt(sentAt);
        return s;
    }

    private Subscription emailedWithNullPlan(String email, String status, LocalDateTime sentAt) {
        Subscription s = new Subscription(new User(email, "hash", null),
                "cus_" + email, status);
        s.setLastWinBackEmailSentAt(sentAt);
        return s;
    }
}
