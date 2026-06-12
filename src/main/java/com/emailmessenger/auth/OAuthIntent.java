package com.emailmessenger.auth;

/**
 * The plan/billing/utm_source the visitor expressed before bouncing out
 * to Google's consent screen. Carried through the OAuth round-trip in
 * the HTTP session so the callback's success handler can resume the
 * Stripe Checkout redirect, and so first-time provisioning credits the
 * inbound channel instead of the literal string {@code "google"}.
 *
 * <p>Any field may be null when the visitor didn't supply it.
 */
record OAuthIntent(String plan, String billing, String utmSource) {

    static final OAuthIntent EMPTY = new OAuthIntent(null, null, null);

    boolean isEmpty() {
        return plan == null && billing == null && utmSource == null;
    }
}
