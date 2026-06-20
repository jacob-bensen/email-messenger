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
class TeamAdoptionMetricsServiceTest {

    @Mock PlanChangeEventRepository planChanges;
    @Mock SubscriptionRepository subscriptions;

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 11, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private TeamAdoptionMetricsService service;

    @BeforeEach
    void setUp() {
        service = new TeamAdoptionMetricsService(planChanges, subscriptions, fixedClock);
    }

    @Test
    void currentWindowCutoffIsThirtyDaysBeforeClockNow() {
        zeroEverything();

        service.snapshot();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(planChanges).countDistinctUsersByTransitionSince(
                eq(Plan.FREE), eq(Plan.TEAM), cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(now.minusDays(30));
    }

    @Test
    void priorWindowUsesTheTwoToOneTimesThirtyDayBracket() {
        zeroEverything();

        service.snapshot();

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(planChanges).countDistinctUsersByTransitionBetween(
                eq(Plan.FREE), eq(Plan.TEAM), start.capture(), end.capture());
        assertThat(start.getValue()).isEqualTo(now.minusDays(60));
        assertThat(end.getValue()).isEqualTo(now.minusDays(30));
    }

    @Test
    void priorWindowCountsArePropagatedAndDeltaPercentReflectsLift() {
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.TEAM), any()))
                .thenReturn(8L);
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.PERSONAL), eq(Plan.TEAM), any()))
                .thenReturn(2L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.FREE), eq(Plan.TEAM), any(), any())).thenReturn(4L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.PERSONAL), eq(Plan.TEAM), any(), any())).thenReturn(2L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.priorFreeToTeamConversions()).isEqualTo(4);
        assertThat(m.priorPersonalToTeamConversions()).isEqualTo(2);
        assertThat(m.priorTotalTeamConversions()).isEqualTo(6);

        assertThat(m.freeToTeamDeltaPercent()).isEqualTo(100);
        assertThat(m.personalToTeamDeltaPercent()).isEqualTo(0);
        assertThat(m.totalTeamConversionsDeltaPercent()).isEqualTo(67);

        assertThat(m.freeToTeamDeltaLabel()).isEqualTo("▲ 100% vs. prior 30 days");
        assertThat(m.personalToTeamDeltaLabel()).isEqualTo("flat vs. prior 30 days");
        assertThat(m.totalTeamConversionsDeltaLabel()).isEqualTo("▲ 67% vs. prior 30 days");
    }

    @Test
    void deltaLabelReadsNewWhenPriorWindowWasEmpty() {
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.TEAM), any()))
                .thenReturn(5L);
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.PERSONAL), eq(Plan.TEAM), any()))
                .thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.freeToTeamDeltaLabel()).isEqualTo("new vs. prior 30 days");
        assertThat(m.freeToTeamDeltaPercent()).isZero();
    }

    @Test
    void deltaLabelReadsNoPriorDataWhenBothWindowsAreZero() {
        zeroEverything();

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.totalTeamConversionsDeltaLabel()).isEqualTo("no prior-window data");
    }

    @Test
    void deltaLabelReadsDownWhenCurrentWindowIsBelowPrior() {
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.TEAM), any()))
                .thenReturn(3L);
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.PERSONAL), eq(Plan.TEAM), any()))
                .thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.FREE), eq(Plan.TEAM), any(), any())).thenReturn(10L);
        when(planChanges.countDistinctUsersByTransitionBetween(
                eq(Plan.PERSONAL), eq(Plan.TEAM), any(), any())).thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.freeToTeamDeltaPercent()).isEqualTo(-70);
        assertThat(m.freeToTeamDeltaLabel()).isEqualTo("▼ 70% vs. prior 30 days");
    }

    @Test
    void freeToTeamAndPersonalToTeamAreBucketedSeparately() {
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.FREE), eq(Plan.TEAM), any()))
                .thenReturn(7L);
        when(planChanges.countDistinctUsersByTransitionSince(eq(Plan.PERSONAL), eq(Plan.TEAM), any()))
                .thenReturn(3L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.freeToTeamConversions()).isEqualTo(7);
        assertThat(m.personalToTeamConversions()).isEqualTo(3);
        assertThat(m.totalTeamConversions()).isEqualTo(10);
        assertThat(m.freeToTeamSharePercent()).isEqualTo(70);
        assertThat(m.personalToTeamSharePercent()).isEqualTo(30);
    }

    @Test
    void zeroTeamConversionsRendersZeroShareNotDivideByZero() {
        zeroEverything();

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.totalTeamConversions()).isZero();
        assertThat(m.freeToTeamSharePercent()).isZero();
        assertThat(m.personalToTeamSharePercent()).isZero();
    }

    @Test
    void entitledTeamAndEnterpriseSubscriberCountsAreLookedUpPerPlan() {
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(subscriptions.countEntitledOn(Plan.TEAM)).thenReturn(12L);
        when(subscriptions.countEntitledOn(Plan.ENTERPRISE)).thenReturn(2L);

        TeamAdoptionMetrics m = service.snapshot();

        assertThat(m.entitledTeamSubscribers()).isEqualTo(12);
        assertThat(m.entitledEnterpriseSubscribers()).isEqualTo(2);
    }

    private void zeroEverything() {
        when(planChanges.countDistinctUsersByTransitionSince(any(), any(), any())).thenReturn(0L);
        when(planChanges.countDistinctUsersByTransitionBetween(any(), any(), any(), any()))
                .thenReturn(0L);
        when(subscriptions.countEntitledOn(any())).thenReturn(0L);
    }
}
