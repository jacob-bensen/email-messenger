package com.emailmessenger.email;

/**
 * A mail preference or signature the rules in {@link MailPreferencesRules}
 * reject. The message is end-user-safe; the website shows it as a flash and
 * the API maps {@link #reason()} to its error code.
 */
public class MailSettingsException extends RuntimeException {

    public enum Reason { INVALID_SYNC_FREQUENCY, SYNC_FREQUENCY_BELOW_PLAN, SIGNATURE_TOO_LONG }

    private final Reason reason;

    MailSettingsException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
