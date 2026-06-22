package com.emailmessenger.admin;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.repository.PlanChangeEventRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProAdoptionMetricsServiceTest {

    @Mock PlanChangeEventRepository planChanges;
    @Mock SubscriptionRepository subscriptions;

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 11, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private ProAdoptionMetricsService service;

    @BeforeEach
    void setUp() {
        service = new ProAdoptionMetricsService(planChanges, subscriptions, fixedClock);
    }

    @Test
    void currentWindowCutoffIsThirtyDaysBeforeClockNow() {
        zeroEverything();

        service.snapshot();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(planChanges).countDistinctUsersByTransitionSince(
                eq(Plan.FREE), eq(Plan.PRO), cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(now.minusDays(30));
    }

    @Test
    void priorWindowUsesTheTwoToOneTimesThirtyDayBracket() {
        zeroEverything();

        service.snapshot();

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(planChanges).countDistinctUsersByTransitionBetween(
                eq(Plan.FREE), eq(Plan.PRO), start.capture(), end.capture());
        assertThat(start.getValue()).isEqualTo(now.minusDays(60));
        assertThat(end.getValue()).isEqualTo(now.minusDays(30));
    }

    @Test
    void priorWindowCountIsPropagatedAndDeltaPercentReflectsLift() {
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.PRO), any()))
                .thenReturn(8L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.FREE), eq(Plan.PRO), any(), any())).thenReturn(4L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        ProAdoptionMetrics m = service.snapshot();

        assertThat(m.freeToProConversions()).isEqualTo(8);
        assertThat(m.priorFreeToProConversions()).isEqualTo(4);
        // 8 vs prior 4 → +100%
        assertThat(m.freeToProDeltaPercent()).isEqualTo(100);
        assertThat(m.freeToProDeltaLabel()).isEqualTo("▲ 100% vs. prior 30 days");
    }

    @Test
    void deltaLabelReadsNewWhenPriorWindowWasEmpty() {
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.PRO), any()))
                .thenReturn(5L);
        when(planChanges.countDistinctUsersByTransitionBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        ProAdoptionMetrics m = service.snapshot();

        assertThat(m.freeToProDeltaLabel()).isEqualTo("new vs. prior 30 days");
        assertThat(m.freeToProDeltaPercent()).isZero();
    }

    @Test
    void deltaLabelReadsFlatWhenCurrentMatchesPrior() {
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.PRO), any()))
                .thenReturn(6L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.FREE), eq(Plan.PRO), any(), any())).thenReturn(6L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        ProAdoptionMetrics m = service.snapshot();

        assertThat(m.freeToProDeltaPercent()).isZero();
        assertThat(m.freeToProDeltaLabel()).isEqualTo("flat vs. prior 30 days");
    }

    @Test
    void deltaLabelReadsNoPriorDataWhenBothWindowsAreZero() {
        zeroEverything();

        ProAdoptionMetrics m = service.snapshot();

        assertThat(m.freeToProDeltaLabel()).isEqualTo("no prior-window data");
    }

    @Test
    void deltaLabelReadsDownWhenCurrentWindowIsBelowPrior() {
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.PRO), any()))
                .thenReturn(3L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.FREE), eq(Plan.PRO), any(), any())).thenReturn(10L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        ProAdoptionMetrics m = service.snapshot();

        // 3 vs prior 10 → -70%
        assertThat(m.freeToProDeltaPercent()).isEqualTo(-70);
        assertThat(m.freeToProDeltaLabel()).isEqualTo("▼ 70% vs. prior 30 days");
    }

    @Test
    void entitledProAndBusinessSubscriberCountsAreLookedUpPerPlan() {
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        when(subscriptions.countEntitledOn(Plan.PRO)).thenReturn(12L);
        when(subscriptions.countEntitledOn(Plan.BUSINESS)).thenReturn(2L);

        ProAdoptionMetrics m = service.snapshot();

        assertThat(m.entitledProSubscribers()).isEqualTo(12);
        assertThat(m.entitledBusinessSubscribers()).isEqualTo(2);
    }

    @Test
    void windowDaysIsThirty() {
        zeroEverything();

        assertThat(service.snapshot().windowDays()).isEqualTo(30);
    }

    private void zeroEverything() {
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);
    }
}
