package com.emailmessenger.admin;

import com.emailmessenger.domain.Subscription;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Counts trial-end conversion emails in the rolling 30-day window plus the
 * subset whose underlying subscription is currently {@code active}, so the
 * operator can see whether the new email is actually moving revenue and
 * compare it to the pre-EPIC-14 baseline at a glance.
 */
@Service
public class TrialEndConversionMetricsService {

    static final int WINDOW_DAYS = 30;
    private static final String STATUS_ACTIVE = "active";

    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    TrialEndConversionMetricsService(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TrialEndConversionMetrics snapshot() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(WINDOW_DAYS);
        List<Subscription> emailed = subscriptions.findTrialEndEmailedSince(cutoff);
        int sent = emailed.size();
        int converted = 0;
        for (Subscription sub : emailed) {
            if (STATUS_ACTIVE.equalsIgnoreCase(sub.getStatus())) {
                converted++;
            }
        }
        int rate = sent == 0 ? 0 : (int) Math.round(100.0 * converted / sent);
        return new TrialEndConversionMetrics(WINDOW_DAYS, sent, converted, rate);
    }
}
