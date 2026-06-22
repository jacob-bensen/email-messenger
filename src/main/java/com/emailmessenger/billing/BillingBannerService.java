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
import java.util.Optional;

@Service
public class BillingBannerService {

    private final SubscriptionRepository subscriptions;
    private final Clock clock;

    BillingBannerService(SubscriptionRepository subscriptions, Clock clock) {
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<BillingBanner> bannerFor(User user) {
        Subscription sub = subscriptions.findByUser(user).orElse(null);
        if (sub == null) {
            return Optional.empty();
        }
        String status = sub.getStatus();
        if ("canceled".equals(status)) {
            return Optional.of(BillingBanner.subscriptionEnded());
        }
        if ("trialing".equals(status) && sub.getTrialEndsAt() != null) {
            // Ceiling-divide hours-until-trial-end so 36h left reads as "2 days",
            // matching the user's intuition that a trial ends "tomorrow" rather
            // than "today" when there's still more than 24h on the clock.
            long hours = ChronoUnit.HOURS.between(LocalDateTime.now(clock), sub.getTrialEndsAt());
            long days = hours <= 0 ? 0 : (hours + 23) / 24;
            return Optional.of(BillingBanner.trialEnding(days, planLabel(sub.getPlan())));
        }
        return Optional.empty();
    }

    private static String planLabel(Plan plan) {
        if (plan == null) {
            return null;
        }
        return switch (plan) {
            case PRO -> "Pro";
            case BUSINESS -> "Business";
            case FREE -> null;
        };
    }
}
