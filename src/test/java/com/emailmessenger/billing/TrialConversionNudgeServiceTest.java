package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrialConversionNudgeServiceTest {

    @Mock SubscriptionRepository subscriptions;

    private final User user = new User("user@example.com", "hash", "User");
    private final LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private TrialConversionNudgeService service;

    @BeforeEach
    void setUp() {
        service = new TrialConversionNudgeService(subscriptions, fixedClock);
    }

    @Test
    void returnsEmptyWhenNoSubscription() {
        when(subscriptions.findByUser(user)).thenReturn(Optional.empty());

        assertThat(service.nudgeFor(user)).isEmpty();
    }

    @Test
    void returnsEmptyWhenNotTrialing() {
        Subscription sub = new Subscription(user, "cus_1", "active");
        sub.setPlan(Plan.PRO);
        sub.setTrialEndsAt(now.plusDays(1));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        assertThat(service.nudgeFor(user)).isEmpty();
    }

    @Test
    void returnsEmptyWhenTrialEndsBeyondWindow() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setPlan(Plan.PRO);
        sub.setTrialEndsAt(now.plusDays(7));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        assertThat(service.nudgeFor(user)).isEmpty();
    }

    @Test
    void returnsProNudgeWhenTrialEndsInThreeDays() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setPlan(Plan.PRO);
        sub.setTrialEndsAt(now.plusDays(3));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        TrialConversionNudge nudge = service.nudgeFor(user).orElseThrow();
        assertThat(nudge.planLabel()).isEqualTo("Pro");
        assertThat(nudge.planParam()).isEqualTo("pro");
        assertThat(nudge.monthlyPrice()).isEqualTo("$6.99");
        assertThat(nudge.annualMonthlyEquivalent()).isEqualTo("$5.99");
        assertThat(nudge.annualCashAmount()).isEqualTo("$71.88");
        assertThat(nudge.daysLeft()).isEqualTo(3L);
        assertThat(nudge.dismissKey()).isEqualTo("conexusmail-trial-nudge-2026-06-08-d3");
        assertThat(nudge.inAnnualUpsellWindow()).isTrue();
    }

    @Test
    void returnsProNudgeWhenTrialEndsInOneDay() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setPlan(Plan.PRO);
        sub.setTrialEndsAt(now.plusHours(20));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        TrialConversionNudge nudge = service.nudgeFor(user).orElseThrow();
        assertThat(nudge.planLabel()).isEqualTo("Pro");
        assertThat(nudge.planParam()).isEqualTo("pro");
        assertThat(nudge.monthlyPrice()).isEqualTo("$6.99");
        assertThat(nudge.daysLeft()).isEqualTo(1L);
        assertThat(nudge.dismissKey()).endsWith("-d1");
        assertThat(nudge.inAnnualUpsellWindow()).isTrue();
    }

    @Test
    void returnsEmptyWhenPlanIsBusiness() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setPlan(Plan.BUSINESS);
        sub.setTrialEndsAt(now.plusHours(20));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        assertThat(service.nudgeFor(user)).isEmpty();
    }

    @Test
    void dismissKeyChangesAsTrialApproachesExpiry() {
        Subscription threeDay = new Subscription(user, "cus_1", "trialing");
        threeDay.setPlan(Plan.PRO);
        threeDay.setTrialEndsAt(now.plusDays(3));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(threeDay));
        String day3Key = service.nudgeFor(user).orElseThrow().dismissKey();

        Subscription oneDay = new Subscription(user, "cus_1", "trialing");
        oneDay.setPlan(Plan.PRO);
        oneDay.setTrialEndsAt(now.plusDays(1));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(oneDay));
        String day1Key = service.nudgeFor(user).orElseThrow().dismissKey();

        assertThat(day3Key).isNotEqualTo(day1Key);
    }

    @Test
    void returnsEmptyWhenPlanIsBusinessNearExpiry() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setPlan(Plan.BUSINESS);
        sub.setTrialEndsAt(now.plusDays(2));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        assertThat(service.nudgeFor(user)).isEmpty();
    }

    @Test
    void returnsEmptyWhenPlanIsNull() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setTrialEndsAt(now.plusDays(2));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        assertThat(service.nudgeFor(user)).isEmpty();
    }

    @Test
    void returnsEmptyWhenTrialEndsAtIsNull() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setPlan(Plan.PRO);
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        assertThat(service.nudgeFor(user)).isEmpty();
    }
}
