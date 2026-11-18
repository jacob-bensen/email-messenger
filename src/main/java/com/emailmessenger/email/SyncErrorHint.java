package com.emailmessenger.email;

import java.util.Locale;

/**
 * User-facing remediation for a mailbox's {@code lastSyncError}. One rule
 * for the website's /mailboxes page and the API (docs/API.md §5.10).
 */
public final class SyncErrorHint {

    private SyncErrorHint() {}

    public static String forError(String error) {
        if (error == null) return null;
        String lower = error.toLowerCase(Locale.ROOT);
        if (lower.contains("google") || lower.contains("reconnect")) {
            return "Your Google authorization expired. Use Reconnect to sign in with Google again.";
        }
        // Decryption failures are NOT a rejected sign-in — the stored secret
        // couldn't be unlocked (e.g. the encryption key changed). Re-saving it
        // by reconnecting fixes it; a new app password is not required.
        if (lower.contains("decrypt")) {
            return "We couldn't unlock this mailbox's saved password. Reconnect the mailbox to re-save it.";
        }
        if (lower.contains("auth") || lower.contains("login")
                || lower.contains("password") || lower.contains("credential")) {
            return "Sign-in was rejected. Generate a new app password and reconnect the mailbox.";
        }
        if (lower.contains("timed out") || lower.contains("timeout")) {
            return "The mail server didn't respond in time. Try again in a moment.";
        }
        if (lower.contains("ssl") || lower.contains("tls") || lower.contains("certificate")) {
            return "TLS handshake failed. Double-check the host and port for your provider.";
        }
        if (lower.contains("unreachable") || lower.contains("connect")
                || lower.contains("host") || lower.contains("network")) {
            return "We couldn't reach your mail server. Check the host/port and your network.";
        }
        return "Try Sync now again. If the error persists, reconnect the mailbox.";
    }
}
