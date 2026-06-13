package com.emailmessenger.admin;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Computes the rolling-30-day win-back conversion snapshot rendered on the
 * operator dashboard. Counts emails fired by
 * {@link WinBackOutreachService}, the subset whose underlying subscription
 * is currently {@code active} again, and the monthly-equivalent MRR
 * recovered — paired with the prior-30-day window so the operator can read
 * whether the copy/offer is improving the close rate or just adding sends.
 */
@Service
public class WinBackConversionMetricsService {

    static final int WINDOW_DAYS = 30;
    private static final String STATUS_ACTIVE = "active";

    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    WinBackConversionMetricsService(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WinBackConversionMetrics snapshot() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = now.minusDays(WINDOW_DAYS);
        LocalDateTime priorStart = now.minusDays(2L * WINDOW_DAYS);

        List<Subscription> emailed = subscriptions.findWinBackEmailedSince(priorStart);

        int sent = 0;
        int reactivated = 0;
        long mrrCents = 0;
        int priorSent = 0;
        int priorReactivated = 0;
        long priorMrrCents = 0;

        for (Subscription sub : emailed) {
            LocalDateTime sentAt = sub.getLastWinBackEmailSentAt();
            if (sentAt == null) {
                continue;
            }
            Plan plan = sub.getPlan();
            if (plan == null || plan == Plan.FREE) {
                continue;
            }
            boolean inCurrent = !sentAt.isBefore(windowStart);
            boolean isActive = STATUS_ACTIVE.equalsIgnoreCase(sub.getStatus());
            int priceCents = PlanPricing.monthlyCents(plan, periodOf(sub));
            if (inCurrent) {
                sent++;
                if (isActive) {
                    reactivated++;
                    mrrCents += priceCents;
                }
            } else {
                priorSent++;
                if (isActive) {
                    priorReactivated++;
                    priorMrrCents += priceCents;
                }
            }
        }

        int rate = ratePercent(reactivated, sent);
        int priorRate = ratePercent(priorReactivated, priorSent);

        return new WinBackConversionMetrics(
                WINDOW_DAYS,
                sent,
                reactivated,
                mrrCents, RevenueMetricsService.formatCents(mrrCents),
                rate,
                priorSent,
                priorReactivated,
                priorMrrCents, RevenueMetricsService.formatCents(priorMrrCents),
                priorRate);
    }

    private static BillingPeriod periodOf(Subscription sub) {
        return sub.getBillingPeriod() == null ? BillingPeriod.MONTHLY : sub.getBillingPeriod();
    }

    private static int ratePercent(int converted, int sent) {
        if (sent <= 0) {
            return 0;
        }
        return (int) Math.round(100.0 * converted / sent);
    }
}
