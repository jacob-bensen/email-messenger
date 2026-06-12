package com.emailmessenger.web;

import com.emailmessenger.domain.MailAccount;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Render-ready view of a {@link MailAccount}: the raw fields the
 * template needs plus a friendly relative timestamp for
 * {@code lastSyncedAt} and a remediation hint derived from
 * {@code lastSyncError}.
 */
public record MailboxView(
        Long id,
        String host,
        String username,
        boolean synced,
        String lastSyncedRelative,
        String lastSyncError,
        String errorHint,
        boolean pollingSuspended,
        int consecutiveFailureCount) {

    private static final DateTimeFormatter ABSOLUTE_DATE =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    public static MailboxView from(MailAccount account, LocalDateTime now) {
        return new MailboxView(
                account.getId(),
                account.getHost(),
                account.getUsername(),
                account.getLastSyncedAt() != null,
                formatRelative(account.getLastSyncedAt(), now),
                account.getLastSyncError(),
                errorHint(account.getLastSyncError()),
                account.isPollingSuspended(),
                account.getConsecutiveFailureCount());
    }

    static String formatRelative(LocalDateTime ts, LocalDateTime now) {
        if (ts == null) return null;
        Duration delta = Duration.between(ts, now);
        long seconds = delta.getSeconds();
        // Clock skew or freshly-saved row: treat anything within the same minute as "Just now".
        if (seconds < 60) return "Just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        long hours = minutes / 60;
        if (hours < 24) return hours == 1 ? "1 hour ago" : hours + " hours ago";
        long days = hours / 24;
        if (days == 1) return "Yesterday";
        if (days < 7) return days + " days ago";
        return ts.format(ABSOLUTE_DATE);
    }

    static String errorHint(String error) {
        if (error == null) return null;
        String lower = error.toLowerCase(Locale.ROOT);
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
