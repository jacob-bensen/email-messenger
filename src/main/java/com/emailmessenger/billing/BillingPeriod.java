package com.emailmessenger.billing;

import java.util.Locale;

public enum BillingPeriod {
    MONTHLY,
    ANNUAL;

    /**
     * Lenient parse: blank / null / unknown values fall back to MONTHLY so a
     * tampered or missing query string can never strand a user mid-checkout
     * and an unrecognised value (e.g. a future "QUARTERLY") quietly degrades
     * to the safer, default billing cadence instead of throwing.
     */
    public static BillingPeriod parse(String raw) {
        if (raw == null) {
            return MONTHLY;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return MONTHLY;
        }
        try {
            return BillingPeriod.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MONTHLY;
        }
    }

    public String paramValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
