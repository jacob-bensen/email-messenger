package com.emailmessenger.web;

import com.emailmessenger.domain.AuthType;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.email.SyncErrorHint;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Render-ready view of a {@link MailAccount}: the raw fields the
 * template needs plus a friendly relative timestamp for
 * {@code lastSyncedAt} and the {@link SyncErrorHint} for
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
        int consecutiveFailureCount,
        boolean oauth,
        String signature) {

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
                SyncErrorHint.forError(account.getLastSyncError()),
                account.isPollingSuspended(),
                account.getConsecutiveFailureCount(),
                account.getAuthType() == AuthType.XOAUTH2,
                account.getSignature());
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
}
