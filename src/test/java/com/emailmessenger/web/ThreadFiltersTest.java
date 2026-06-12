package com.emailmessenger.web;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadFiltersTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-06-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void parseSevenDayPresetSubtractsSevenDaysFromClock() {
        ThreadFilters f = ThreadFilters.parse("7d", false, false, CLOCK);

        assertThat(f.sincePreset()).isEqualTo("7d");
        assertThat(f.since()).isEqualTo(LocalDateTime.of(2026, 5, 30, 12, 0));
        assertThat(f.isActive()).isTrue();
    }

    @Test
    void parseThirtyAndNinetyDayPresetsAreAccepted() {
        assertThat(ThreadFilters.parse("30d", false, false, CLOCK).since())
                .isEqualTo(LocalDateTime.of(2026, 5, 7, 12, 0));
        assertThat(ThreadFilters.parse("90d", false, false, CLOCK).since())
                .isEqualTo(LocalDateTime.of(2026, 3, 8, 12, 0));
    }

    @Test
    void parseUnknownPresetIsIgnoredAndSinceIsNull() {
        ThreadFilters f = ThreadFilters.parse("1y", false, false, CLOCK);

        assertThat(f.sincePreset()).isNull();
        assertThat(f.since()).isNull();
    }

    @Test
    void parseTreatsBlankAndNullSinceAsNoFilter() {
        assertThat(ThreadFilters.parse(null, false, false, CLOCK).since()).isNull();
        assertThat(ThreadFilters.parse("", false, false, CLOCK).since()).isNull();
        assertThat(ThreadFilters.parse("  ", false, false, CLOCK).since()).isNull();
    }

    @Test
    void parseUnreadAndAttachmentsPropagateThrough() {
        ThreadFilters f = ThreadFilters.parse(null, true, true, CLOCK);

        assertThat(f.requireUnread()).isTrue();
        assertThat(f.requireAttachments()).isTrue();
        assertThat(f.isActive()).isTrue();
    }

    @Test
    void noneFiltersIsNotActive() {
        assertThat(ThreadFilters.NONE.isActive()).isFalse();
        assertThat(ThreadFilters.parse(null, false, false, CLOCK).isActive()).isFalse();
    }

    @Test
    void presetIsCaseInsensitive() {
        assertThat(ThreadFilters.parse("7D", false, false, CLOCK).sincePreset()).isEqualTo("7d");
    }
}
