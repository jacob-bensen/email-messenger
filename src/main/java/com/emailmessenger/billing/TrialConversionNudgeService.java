package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

/**
 * Produces a last-mile trial-conversion nudge for trialing users whose trial
 * expires in ≤3 days. Sits beside {@link BillingBannerService}: that one
 * powers the always-on strip banner; this one drives a more prominent
 * dismissable modal with a one-click checkout for the plan the user is
 * already trialing.
 */
@Service
public class TrialConversionNudgeService {

    private static final long NUDGE_WINDOW_DAYS = 3;

    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    TrialConversionNudgeService(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<TrialConversionNudge> nudgeFor(User user) {
        Subscription sub = subscriptions.findByUser(user).orElse(null);
        if (sub == null) {
            return Optional.empty();
        }
        if (!"trialing".equals(sub.getStatus())) {
            return Optional.empty();
        }
        if (sub.getTrialEndsAt() == null) {
            return Optional.empty();
        }
        Plan plan = sub.getPlan();
        if (plan == null || plan == Plan.FREE || plan == Plan.ENTERPRISE) {
            return Optional.empty();
        }

        long hours = ChronoUnit.HOURS.between(LocalDateTime.now(clock), sub.getTrialEndsAt());
        long days = hours <= 0 ? 0 : (hours + 23) / 24;
        if (days > NUDGE_WINDOW_DAYS) {
            return Optional.empty();
        }

        // Key embeds both the trial-end date and the current days-left bucket
        // so a Day-3 dismissal doesn't also silence the more-urgent Day-1
        // nudge the same user sees 48h later.
        String dismissKey = "mailim-trial-nudge-"
                + sub.getTrialEndsAt().toLocalDate()
                + "-d" + days;

        return Optional.of(new TrialConversionNudge(
                planLabel(plan),
                plan.name().toLowerCase(Locale.ROOT),
                days,
                monthlyPrice(plan),
                dismissKey));
    }

    private static String planLabel(Plan plan) {
        return switch (plan) {
            case PERSONAL -> "Personal";
            case TEAM -> "Team";
            case ENTERPRISE, FREE -> "";
        };
    }

    private static String monthlyPrice(Plan plan) {
        return switch (plan) {
            case PERSONAL -> "$9";
            case TEAM -> "$29";
            case ENTERPRISE, FREE -> "";
        };
    }
}
