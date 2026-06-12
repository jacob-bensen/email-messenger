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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the "at-risk retention queue" rendered on the operator dashboard —
 * the last N {@code active → canceled} rows in the rolling-30-day window,
 * sorted newest-first so a fresh cancellation is at the top of the page.
 * FREE rows are skipped — there is no paid relationship to win back, and
 * a Free-tier "cancel" is just a row from a deleted Stripe customer.
 *
 * <p>EPIC-18 M3 widens the queue to also include <em>recovered</em> rows
 * (a paid sub that received a win-back email and has since flipped back
 * to {@code active}), so the operator can read the loop's close on the
 * same page they fire it from.
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
        canceled.removeIf(AtRiskRetentionService::isUnpaid);

        List<Subscription> recovered = new ArrayList<>(
                subscriptions.findRecoveredByWinBackSince(windowStart));
        recovered.removeIf(AtRiskRetentionService::isUnpaid);

        Set<Long> canceledIds = new HashSet<>();
        for (Subscription s : canceled) {
            canceledIds.add(s.getId());
        }
        recovered.removeIf(s -> canceledIds.contains(s.getId()));

        long totalCanceled = canceled.size();
        long totalRecovered = recovered.size();

        List<Entry> merged = new ArrayList<>(canceled.size() + recovered.size());
        for (Subscription s : canceled) {
            merged.add(toEntry(s, false));
        }
        for (Subscription s : recovered) {
            merged.add(toEntry(s, true));
        }
        merged.sort(Comparator.comparing(Entry::canceledAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int cap = Math.min(merged.size(), DISPLAY_LIMIT);
        List<Entry> entries = new ArrayList<>(merged.subList(0, cap));
        return new AtRiskRetentionMetrics(WINDOW_DAYS, totalCanceled, totalRecovered,
                DISPLAY_LIMIT, entries);
    }

    private static boolean isUnpaid(Subscription s) {
        return s.getPlan() == null || s.getPlan() == Plan.FREE;
    }

    private static Entry toEntry(Subscription sub, boolean recovered) {
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
                sub.getLastWinBackEmailSentAt(),
                recovered);
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
