package com.emailmessenger.auth;

import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pre-formatted billing slice for the /account page.
 *
 * <p>{@code label} is the headline cadence ("Personal · Annual"). When the
 * subscription is in trial it reads "Personal · Annual trial". {@code
 * renewsOn} is the next-renewal date in ISO format, or null when Stripe
 * hasn't surfaced one yet (e.g. status=incomplete before the first
 * customer.subscription.created event).
 */
public record AccountBillingSummary(String label,
                                    LocalDate renewsOn,
                                    boolean trialing,
                                    boolean canceled) {

    public static AccountBillingSummary from(Subscription sub) {
        if (sub == null) {
            return null;
        }
        Plan plan = sub.getPlan();
        if (plan == null || plan == Plan.FREE) {
            return null;
        }
        String planName = planLabel(plan);
        String periodName = periodLabel(sub.getBillingPeriod());
        boolean trialing = "trialing".equalsIgnoreCase(sub.getStatus());
        boolean canceled = "canceled".equalsIgnoreCase(sub.getStatus());
        String label;
        if (canceled) {
            label = planName + " · Canceled";
        } else if (trialing) {
            label = planName + " · " + periodName + " trial";
        } else {
            label = planName + " · " + periodName;
        }
        LocalDateTime renew = trialing && sub.getTrialEndsAt() != null
                ? sub.getTrialEndsAt()
                : sub.getCurrentPeriodEnd();
        LocalDate renewsOn = renew == null ? null : renew.toLocalDate();
        return new AccountBillingSummary(label, renewsOn, trialing, canceled);
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
        if (period == BillingPeriod.ANNUAL) {
            return "Annual";
        }
        return "Monthly";
    }
}
