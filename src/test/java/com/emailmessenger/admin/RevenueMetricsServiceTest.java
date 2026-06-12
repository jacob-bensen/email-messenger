package com.emailmessenger.admin;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueMetricsServiceTest {

    @Mock SubscriptionRepository subscriptions;

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private RevenueMetricsService service;

    @BeforeEach
    void setUp() {
        service = new RevenueMetricsService(subscriptions, fixedClock);
    }

    @Test
    void emptyRepoYieldsZeroMetrics() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of());

        RevenueMetrics m = service.snapshot();
        assertThat(m.mrrCents()).isZero();
        assertThat(m.arrCents()).isZero();
        assertThat(m.activeSubscribers()).isZero();
        assertThat(m.trialingSubscribers()).isZero();
        assertThat(m.canceledSubscribers()).isZero();
        assertThat(m.annualSharePercent()).isZero();
        assertThat(m.mrrFormatted()).isEqualTo("$0");
        assertThat(m.recentEvents()).isEmpty();
        assertThat(m.sourceBreakdown()).isEmpty();
        assertThat(m.planBreakdown()).hasSize(3);
    }

    @Test
    void mrrSumsActivePlanPricingByCadenceAndExcludesTrials() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("a@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, null),
                active("b@example.com", Plan.PERSONAL, BillingPeriod.ANNUAL, null),
                active("c@example.com", Plan.TEAM, BillingPeriod.MONTHLY, null),
                trialing("d@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, now.plusDays(10)),
                canceled("e@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY)));

        RevenueMetrics m = service.snapshot();
        assertThat(m.activeSubscribers()).isEqualTo(3);
        assertThat(m.trialingSubscribers()).isEqualTo(1);
        assertThat(m.canceledSubscribers()).isEqualTo(1);
        // 900 + 700 + 2900 = 4500 cents
        assertThat(m.mrrCents()).isEqualTo(4500L);
        assertThat(m.arrCents()).isEqualTo(54_000L);
        assertThat(m.mrrFormatted()).isEqualTo("$45");
        // Trial pipeline = 900 (Personal monthly)
        assertThat(m.trialPipelineCents()).isEqualTo(900L);
    }

    @Test
    void annualMixPercentReflectsActiveAnnualVsActiveMonthly() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("m1@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, null),
                active("m2@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, null),
                active("a1@example.com", Plan.PERSONAL, BillingPeriod.ANNUAL, null)));

        RevenueMetrics m = service.snapshot();
        assertThat(m.monthlyActive()).isEqualTo(2);
        assertThat(m.annualActive()).isEqualTo(1);
        assertThat(m.annualSharePercent()).isEqualTo(33);
    }

    @Test
    void planBreakdownAlwaysListsThreePaidPlansInOrderEvenWhenEmpty() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("t@example.com", Plan.TEAM, BillingPeriod.ANNUAL, null)));

        RevenueMetrics m = service.snapshot();
        assertThat(m.planBreakdown()).hasSize(3);
        assertThat(m.planBreakdown().get(0).planLabel()).isEqualTo("Personal");
        assertThat(m.planBreakdown().get(0).monthlyActive()).isZero();
        assertThat(m.planBreakdown().get(0).annualActive()).isZero();
        assertThat(m.planBreakdown().get(1).planLabel()).isEqualTo("Team");
        assertThat(m.planBreakdown().get(1).annualActive()).isEqualTo(1);
        assertThat(m.planBreakdown().get(1).mrrCents()).isEqualTo(2400L);
        assertThat(m.planBreakdown().get(2).planLabel()).isEqualTo("Enterprise");
    }

    @Test
    void sourceBreakdownGroupsActiveSubsByAcquisitionSourceAndSortsByMrr() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("p1@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, "producthunt"),
                active("p2@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, "producthunt"),
                active("t1@example.com", Plan.TEAM, BillingPeriod.MONTHLY, "twitter"),
                active("direct@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, null)));

        RevenueMetrics m = service.snapshot();
        assertThat(m.sourceBreakdown()).hasSize(3);
        // twitter (Team monthly = $29) beats producthunt (2 × Personal = $18) beats direct ($9)
        assertThat(m.sourceBreakdown().get(0).sourceLabel()).isEqualTo("twitter");
        assertThat(m.sourceBreakdown().get(0).mrrCents()).isEqualTo(2900L);
        assertThat(m.sourceBreakdown().get(1).sourceLabel()).isEqualTo("producthunt");
        assertThat(m.sourceBreakdown().get(1).activeSubscribers()).isEqualTo(2);
        assertThat(m.sourceBreakdown().get(2).sourceLabel()).isEqualTo("Direct / unknown");
    }

    @Test
    void trialsEndingSoonCountsTrialingInsideSevenDayWindow() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                trialing("a@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, now.plusDays(2)),
                trialing("b@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, now.plusDays(6)),
                trialing("c@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, now.plusDays(10))));

        RevenueMetrics m = service.snapshot();
        assertThat(m.trialingSubscribers()).isEqualTo(3);
        assertThat(m.trialsEndingSoon()).isEqualTo(2);
    }

    @Test
    void recentEventsCappedAtTenAndOrderedByRepositoryOrder() {
        Subscription[] subs = new Subscription[12];
        for (int i = 0; i < 12; i++) {
            subs[i] = active("u" + i + "@example.com", Plan.PERSONAL, BillingPeriod.MONTHLY, null);
            setUpdatedAt(subs[i], now.minusHours(i));
        }
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(subs));

        RevenueMetrics m = service.snapshot();
        assertThat(m.recentEvents()).hasSize(10);
        assertThat(m.recentEvents().get(0).userEmail()).isEqualTo("u0@example.com");
        assertThat(m.recentEvents().get(9).userEmail()).isEqualTo("u9@example.com");
    }

    @Test
    void formatsLargeMrrWithThousandsSeparators() {
        Subscription[] many = new Subscription[150];
        for (int i = 0; i < 150; i++) {
            many[i] = active("u" + i + "@example.com", Plan.TEAM, BillingPeriod.MONTHLY, null);
        }
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(many));

        RevenueMetrics m = service.snapshot();
        // 150 × $29 = $4,350
        assertThat(m.mrrFormatted()).isEqualTo("$4,350");
        // ARR = $52,200
        assertThat(m.arrFormatted()).isEqualTo("$52,200");
    }

    private Subscription active(String email, Plan plan, BillingPeriod period, String source) {
        User u = userWith(email, source);
        Subscription s = new Subscription(u, "cus_" + email, "active");
        s.setPlan(plan);
        s.setBillingPeriod(period);
        return s;
    }

    private Subscription trialing(String email, Plan plan, BillingPeriod period, LocalDateTime trialEnd) {
        User u = userWith(email, null);
        Subscription s = new Subscription(u, "cus_" + email, "trialing");
        s.setPlan(plan);
        s.setBillingPeriod(period);
        s.setTrialEndsAt(trialEnd);
        return s;
    }

    private Subscription canceled(String email, Plan plan, BillingPeriod period) {
        User u = userWith(email, null);
        Subscription s = new Subscription(u, "cus_" + email, "canceled");
        s.setPlan(plan);
        s.setBillingPeriod(period);
        return s;
    }

    private static User userWith(String email, String source) {
        User u = new User(email, "hash", null);
        u.setAcquisitionSource(source);
        return u;
    }

    /**
     * {@code Subscription.updatedAt} is JPA-managed via {@code @PrePersist}
     * and has no public setter — tests need to force a value to assert
     * ordering by recency.
     */
    private static void setUpdatedAt(Subscription sub, LocalDateTime when) {
        try {
            Field f = Subscription.class.getDeclaredField("updatedAt");
            f.setAccessible(true);
            f.set(sub, when);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
