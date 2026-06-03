package com.emailmessenger.billing;

public record BillingBanner(Kind kind, long daysLeft, String planLabel) {

    public enum Kind { TRIAL_ENDING, SUBSCRIPTION_ENDED }

    public static BillingBanner trialEnding(long daysLeft) {
        return trialEnding(daysLeft, null);
    }

    public static BillingBanner trialEnding(long daysLeft, String planLabel) {
        return new BillingBanner(Kind.TRIAL_ENDING, Math.max(0, daysLeft), planLabel);
    }

    public static BillingBanner subscriptionEnded() {
        return new BillingBanner(Kind.SUBSCRIPTION_ENDED, 0, null);
    }

    public boolean isTrialEnding() {
        return kind == Kind.TRIAL_ENDING;
    }

    public boolean isSubscriptionEnded() {
        return kind == Kind.SUBSCRIPTION_ENDED;
    }
}
