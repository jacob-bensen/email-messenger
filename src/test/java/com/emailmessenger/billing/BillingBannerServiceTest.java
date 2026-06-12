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
class BillingBannerServiceTest {

    @Mock SubscriptionRepository subscriptions;

    private final User user = new User("user@example.com", "hash", "User");
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 19, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private BillingBannerService service;

    @BeforeEach
    void setUp() {
        service = new BillingBannerService(subscriptions, fixedClock);
    }

    @Test
    void returnsEmptyWhenNoSubscription() {
        when(subscriptions.findByUser(user)).thenReturn(Optional.empty());

        assertThat(service.bannerFor(user)).isEmpty();
    }

    @Test
    void returnsEmptyForActiveSubscription() {
        Subscription sub = new Subscription(user, "cus_1", "active");
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        assertThat(service.bannerFor(user)).isEmpty();
    }

    @Test
    void returnsTrialBannerWithDaysRemainingWhenTrialing() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setTrialEndsAt(now.plusDays(7));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        BillingBanner banner = service.bannerFor(user).orElseThrow();
        assertThat(banner.isTrialEnding()).isTrue();
        assertThat(banner.daysLeft()).isEqualTo(7L);
    }

    @Test
    void roundsTrialDaysUpFromPartialDay() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setTrialEndsAt(now.plusHours(36));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        BillingBanner banner = service.bannerFor(user).orElseThrow();
        assertThat(banner.daysLeft()).isEqualTo(2L);
    }

    @Test
    void clampsTrialDaysToZeroWhenTrialAlreadyEnded() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setTrialEndsAt(now.minusHours(3));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        BillingBanner banner = service.bannerFor(user).orElseThrow();
        assertThat(banner.isTrialEnding()).isTrue();
        assertThat(banner.daysLeft()).isZero();
    }

    @Test
    void returnsEmptyWhenTrialingButNoTrialEndDate() {
        // Defensive: applyStripeEvent should always set trial_ends_at on trialing,
        // but we don't want a stray banner with bogus days if it didn't.
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        assertThat(service.bannerFor(user)).isEmpty();
    }

    @Test
    void trialBannerSurfacesChosenPlanLabel() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setPlan(Plan.PERSONAL);
        sub.setTrialEndsAt(now.plusDays(5));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        BillingBanner banner = service.bannerFor(user).orElseThrow();
        assertThat(banner.planLabel()).isEqualTo("Personal");
        assertThat(banner.daysLeft()).isEqualTo(5L);
    }

    @Test
    void trialBannerHasNullPlanLabelWhenPlanUnset() {
        Subscription sub = new Subscription(user, "cus_1", "trialing");
        sub.setTrialEndsAt(now.plusDays(3));
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        BillingBanner banner = service.bannerFor(user).orElseThrow();
        assertThat(banner.planLabel()).isNull();
    }

    @Test
    void returnsLockoutBannerWhenCanceled() {
        Subscription sub = new Subscription(user, "cus_1", "canceled");
        when(subscriptions.findByUser(user)).thenReturn(Optional.of(sub));

        BillingBanner banner = service.bannerFor(user).orElseThrow();
        assertThat(banner.isSubscriptionEnded()).isTrue();
        assertThat(banner.isTrialEnding()).isFalse();
    }
}
