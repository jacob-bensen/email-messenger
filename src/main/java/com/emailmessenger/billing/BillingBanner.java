package com.emailmessenger.billing;

public record BillingBanner(Kind kind, long daysLeft) {

    public enum Kind { TRIAL_ENDING, SUBSCRIPTION_ENDED }

    public static BillingBanner trialEnding(long daysLeft) {
        return new BillingBanner(Kind.TRIAL_ENDING, Math.max(0, daysLeft));
    }

    public static BillingBanner subscriptionEnded() {
        return new BillingBanner(Kind.SUBSCRIPTION_ENDED, 0);
    }

    public boolean isTrialEnding() {
        return kind == Kind.TRIAL_ENDING;
    }

    public boolean isSubscriptionEnded() {
        return kind == Kind.SUBSCRIPTION_ENDED;
    }
}
