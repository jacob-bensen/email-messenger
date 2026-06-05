package com.emailmessenger.billing;

public record TrialConversionNudge(
        String planLabel,
        String planParam,
        long daysLeft,
        String monthlyPrice,
        String dismissKey) {
}
