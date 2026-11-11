package com.emailmessenger.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Timestamps are persisted as UTC {@code LocalDateTime}; the API always emits
 * them as ISO-8601 instants with a {@code Z} suffix, at millisecond precision.
 */
final class ApiTime {

    private ApiTime() {}

    static Instant toInstant(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
    }
}
