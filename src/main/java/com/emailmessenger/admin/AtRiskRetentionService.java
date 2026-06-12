package com.emailmessenger.admin;

import com.emailmessenger.admin.AtRiskRetentionMetrics.Entry;
import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.CancellationReason;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the "at-risk retention queue" rendered on the operator dashboard —
 * the last N {@code active → canceled} rows in the rolling-30-day window,
 * sorted newest-first so a fresh cancellation is at the top of the page.
 * FREE rows are skipped — there is no paid relationship to win back, and
 * a Free-tier "cancel" is just a row from a deleted Stripe customer.
 */
@Service
public class AtRiskRetentionService {

    static final int WINDOW_DAYS = 30;
    static final int DISPLAY_LIMIT = 20;

    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    AtRiskRetentionService(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AtRiskRetentionMetrics snapshot() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = now.minusDays(WINDOW_DAYS);

        List<Subscription> canceled = new ArrayList<>(
                subscriptions.findCanceledBetween(windowStart, now));
        canceled.removeIf(s -> s.getPlan() == null || s.getPlan() == Plan.FREE);
        canceled.sort(Comparator.comparing(Subscription::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        long total = canceled.size();
        int cap = Math.min(canceled.size(), DISPLAY_LIMIT);
        List<Entry> entries = new ArrayList<>(cap);
        for (int i = 0; i < cap; i++) {
            entries.add(toEntry(canceled.get(i)));
        }
        return new AtRiskRetentionMetrics(WINDOW_DAYS, total, DISPLAY_LIMIT, entries);
    }

    private static Entry toEntry(Subscription sub) {
        BillingPeriod period = sub.getBillingPeriod() == null
                ? BillingPeriod.MONTHLY
                : sub.getBillingPeriod();
        int priceCents = PlanPricing.monthlyCents(sub.getPlan(), period);
        return new Entry(
                sub.getId(),
                sub.getUser().getEmail(),
                planLabel(sub.getPlan()),
                periodLabel(period),
                priceCents,
                RevenueMetricsService.formatCents(priceCents),
                sourceLabel(sub.getUser().getAcquisitionSource()),
                reasonLabel(sub.getCancellationReason()),
                sub.getUpdatedAt(),
                sub.getLastWinBackEmailSentAt());
    }

    private static String planLabel(Plan plan) {
        return switch (plan) {
            case PERSONAL -> "Personal";
            case TEAM -> "Team";
            case ENTERPRISE -> "Enterprise";
            case FREE -> "Free";
        };
    }

    private static String periodLabel(BillingPeriod period) {
        return period == BillingPeriod.ANNUAL ? "Annual" : "Monthly";
    }

    private static String sourceLabel(String source) {
        if (source == null || source.isBlank()) {
            return "Direct / unknown";
        }
        return source;
    }

    private static String reasonLabel(CancellationReason reason) {
        if (reason == null) {
            return "not recorded";
        }
        return switch (reason) {
            case TOO_EXPENSIVE -> "Too expensive";
            case MISSING_FEATURE -> "Missing feature";
            case SWITCHING -> "Switching tools";
            case TEMPORARY -> "Temporary";
            case OTHER -> "Other";
        };
    }
}
