package com.emailmessenger.domain;

import java.util.Locale;

/**
 * Stable, operator-readable enum captured at the point of cancel — the
 * one-question reason picker on {@code /billing/cancel-subscription}
 * writes one of these onto the Subscription row before redirecting the
 * user to the Stripe Billing Portal. Direct Stripe-Portal cancels (no
 * in-app pre-step) leave the field {@code null}; the operator dashboard
 * treats that as "unrecorded" rather than guessing.
 */
public enum CancellationReason {
    TOO_EXPENSIVE,
    MISSING_FEATURE,
    SWITCHING,
    TEMPORARY,
    OTHER;

    public static CancellationReason parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("reason is required");
        }
        try {
            return CancellationReason.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown reason: " + raw);
        }
    }
}
