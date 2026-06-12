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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrialEndConversionMetricsServiceTest {

    @Mock SubscriptionRepository subscriptions;

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0);
    private final Clock fixedClock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private TrialEndConversionMetricsService service;

    @BeforeEach
    void setUp() {
        service = new TrialEndConversionMetricsService(subscriptions, fixedClock);
    }

    @Test
    void emptyRepoYieldsAllZeros() {
        when(subscriptions.findTrialEndEmailedSince(now.minusDays(30))).thenReturn(List.of());

        TrialEndConversionMetrics m = service.snapshot();
        assertThat(m.windowDays()).isEqualTo(30);
        assertThat(m.emailsSent()).isZero();
        assertThat(m.converted()).isZero();
        assertThat(m.conversionRatePercent()).isZero();
    }

    @Test
    void countsEmailedAndConvertedSubsAndComputesRate() {
        when(subscriptions.findTrialEndEmailedSince(now.minusDays(30))).thenReturn(List.of(
                emailed("a@example.com", "active"),
                emailed("b@example.com", "active"),
                emailed("c@example.com", "trialing"),
                emailed("d@example.com", "canceled")));

        TrialEndConversionMetrics m = service.snapshot();
        assertThat(m.emailsSent()).isEqualTo(4);
        assertThat(m.converted()).isEqualTo(2);
        assertThat(m.conversionRatePercent()).isEqualTo(50);
    }

    @Test
    void conversionRateRoundsToNearestPercent() {
        when(subscriptions.findTrialEndEmailedSince(now.minusDays(30))).thenReturn(List.of(
                emailed("a@example.com", "active"),
                emailed("b@example.com", "canceled"),
                emailed("c@example.com", "canceled")));

        TrialEndConversionMetrics m = service.snapshot();
        // 1/3 = 33.33% -> 33
        assertThat(m.conversionRatePercent()).isEqualTo(33);
    }

    @Test
    void zeroSentMeansZeroRateNotDivideByZero() {
        when(subscriptions.findTrialEndEmailedSince(now.minusDays(30))).thenReturn(List.of());

        TrialEndConversionMetrics m = service.snapshot();
        assertThat(m.conversionRatePercent()).isZero();
    }

    private Subscription emailed(String email, String status) {
        User u = new User(email, "hash", null);
        Subscription s = new Subscription(u, "cus_" + email, status);
        s.setPlan(Plan.PERSONAL);
        s.setBillingPeriod(BillingPeriod.MONTHLY);
        s.setLastTrialEndEmailSentAt(now.minusHours(6));
        return s;
    }
}
