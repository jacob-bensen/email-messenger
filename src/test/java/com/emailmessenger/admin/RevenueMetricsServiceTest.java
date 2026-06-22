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
        assertThat(m.mrrFormatted()).isEqualTo("$0.00");
        assertThat(m.recentEvents()).isEmpty();
        assertThat(m.sourceBreakdown()).isEmpty();
        assertThat(m.planBreakdown()).hasSize(2);
    }

    @Test
    void mrrSumsActivePlanPricingByCadenceAndExcludesTrials() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("a@example.com", Plan.PRO, BillingPeriod.MONTHLY, null),
                active("b@example.com", Plan.PRO, BillingPeriod.ANNUAL, null),
                active("c@example.com", Plan.BUSINESS, BillingPeriod.MONTHLY, null),
                trialing("d@example.com", Plan.PRO, BillingPeriod.MONTHLY, now.plusDays(10)),
                canceled("e@example.com", Plan.PRO, BillingPeriod.MONTHLY)));

        RevenueMetrics m = service.snapshot();
        assertThat(m.activeSubscribers()).isEqualTo(3);
        assertThat(m.trialingSubscribers()).isEqualTo(1);
        assertThat(m.canceledSubscribers()).isEqualTo(1);
        // 699 (Pro monthly) + 599 (Pro annual) + 0 (Business) = 1298 cents
        assertThat(m.mrrCents()).isEqualTo(1298L);
        assertThat(m.arrCents()).isEqualTo(15_576L);
        assertThat(m.mrrFormatted()).isEqualTo("$12.98");
        // Trial pipeline = 699 (Pro monthly)
        assertThat(m.trialPipelineCents()).isEqualTo(699L);
    }

    @Test
    void annualMixPercentReflectsActiveAnnualVsActiveMonthly() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("m1@example.com", Plan.PRO, BillingPeriod.MONTHLY, null),
                active("m2@example.com", Plan.PRO, BillingPeriod.MONTHLY, null),
                active("a1@example.com", Plan.PRO, BillingPeriod.ANNUAL, null)));

        RevenueMetrics m = service.snapshot();
        assertThat(m.monthlyActive()).isEqualTo(2);
        assertThat(m.annualActive()).isEqualTo(1);
        assertThat(m.annualSharePercent()).isEqualTo(33);
    }

    @Test
    void planBreakdownAlwaysListsBothPaidPlansInOrderEvenWhenEmpty() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("b@example.com", Plan.BUSINESS, BillingPeriod.ANNUAL, null)));

        RevenueMetrics m = service.snapshot();
        assertThat(m.planBreakdown()).hasSize(2);
        assertThat(m.planBreakdown().get(0).planLabel()).isEqualTo("Pro");
        assertThat(m.planBreakdown().get(0).monthlyActive()).isZero();
        assertThat(m.planBreakdown().get(0).annualActive()).isZero();
        assertThat(m.planBreakdown().get(1).planLabel()).isEqualTo("Business");
        assertThat(m.planBreakdown().get(1).annualActive()).isEqualTo(1);
        // Business is custom/contact-sales — contributes 0c to MRR.
        assertThat(m.planBreakdown().get(1).mrrCents()).isEqualTo(0L);
    }

    @Test
    void sourceBreakdownGroupsActiveSubsByAcquisitionSourceAndSortsByMrr() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                active("p1@example.com", Plan.PRO, BillingPeriod.MONTHLY, "producthunt"),
                active("p2@example.com", Plan.PRO, BillingPeriod.MONTHLY, "producthunt"),
                active("t1@example.com", Plan.PRO, BillingPeriod.MONTHLY, "twitter"),
                active("direct@example.com", Plan.PRO, BillingPeriod.ANNUAL, null)));

        RevenueMetrics m = service.snapshot();
        assertThat(m.sourceBreakdown()).hasSize(3);
        // Business contributes 0c, so ordering is driven by the count and
        // cadence of Pro subs per source:
        // producthunt (2 × Pro monthly = 1398c) beats twitter (1 × Pro monthly
        // = 699c) beats direct (1 × Pro annual = 599c).
        assertThat(m.sourceBreakdown().get(0).sourceLabel()).isEqualTo("producthunt");
        assertThat(m.sourceBreakdown().get(0).mrrCents()).isEqualTo(1398L);
        assertThat(m.sourceBreakdown().get(0).activeSubscribers()).isEqualTo(2);
        assertThat(m.sourceBreakdown().get(1).sourceLabel()).isEqualTo("twitter");
        assertThat(m.sourceBreakdown().get(1).mrrCents()).isEqualTo(699L);
        assertThat(m.sourceBreakdown().get(2).sourceLabel()).isEqualTo("Direct / unknown");
        assertThat(m.sourceBreakdown().get(2).mrrCents()).isEqualTo(599L);
    }

    @Test
    void trialsEndingSoonCountsTrialingInsideSevenDayWindow() {
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(
                trialing("a@example.com", Plan.PRO, BillingPeriod.MONTHLY, now.plusDays(2)),
                trialing("b@example.com", Plan.PRO, BillingPeriod.MONTHLY, now.plusDays(6)),
                trialing("c@example.com", Plan.PRO, BillingPeriod.MONTHLY, now.plusDays(10))));

        RevenueMetrics m = service.snapshot();
        assertThat(m.trialingSubscribers()).isEqualTo(3);
        assertThat(m.trialsEndingSoon()).isEqualTo(2);
    }

    @Test
    void recentEventsCappedAtTenAndOrderedByRepositoryOrder() {
        Subscription[] subs = new Subscription[12];
        for (int i = 0; i < 12; i++) {
            subs[i] = active("u" + i + "@example.com", Plan.PRO, BillingPeriod.MONTHLY, null);
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
        Subscription[] many = new Subscription[250];
        for (int i = 0; i < 250; i++) {
            many[i] = active("u" + i + "@example.com", Plan.PRO, BillingPeriod.MONTHLY, null);
        }
        when(subscriptions.findAllWithUserNewestFirst()).thenReturn(List.of(many));

        RevenueMetrics m = service.snapshot();
        // 250 × Pro monthly $6.99 = 174,750c = $1,747.50
        assertThat(m.mrrFormatted()).isEqualTo("$1,747.50");
        // ARR = 174,750c × 12 = 2,097,000c = $20,970.00
        assertThat(m.arrFormatted()).isEqualTo("$20,970.00");
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
