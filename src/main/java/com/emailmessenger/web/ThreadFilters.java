package com.emailmessenger.web;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Active state for the filter-chip row above `/threads`. Each field maps
 * to one query parameter so a bookmarked URL renders the same result set
 * on reload, and so the controller can compose chips with the active
 * search/sender filter.
 *
 * <p>`since` accepts presets {@code 7d}, {@code 30d}, {@code 90d}; any
 * other value is ignored so a stray crawler-fuzzed param can't blow up
 * the inbox.
 */
public record ThreadFilters(LocalDateTime since,
                            String sincePreset,
                            boolean requireUnread,
                            boolean requireAttachments) {

    public static final ThreadFilters NONE = new ThreadFilters(null, null, false, false);

    public boolean isActive() {
        return since != null || requireUnread || requireAttachments;
    }

    public static ThreadFilters parse(String sinceParam, boolean unread, boolean attachments, Clock clock) {
        String preset = normalizePreset(sinceParam);
        LocalDateTime since = (preset == null) ? null : LocalDateTime.now(clock).minusDays(days(preset));
        return new ThreadFilters(since, preset, unread, attachments);
    }

    private static String normalizePreset(String sinceParam) {
        if (sinceParam == null) return null;
        String s = sinceParam.trim().toLowerCase();
        return switch (s) {
            case "7d", "30d", "90d" -> s;
            default -> null;
        };
    }

    private static int days(String preset) {
        return switch (Objects.requireNonNull(preset)) {
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            default -> throw new IllegalArgumentException(preset);
        };
    }
}
