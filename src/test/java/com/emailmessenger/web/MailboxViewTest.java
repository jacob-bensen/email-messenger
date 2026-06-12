package com.emailmessenger.web;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MailboxViewTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 25, 12, 0);

    @Test
    void formatRelativeReturnsNullWhenTimestampMissing() {
        assertThat(MailboxView.formatRelative(null, NOW)).isNull();
    }

    @Test
    void formatRelativeReturnsJustNowForVeryRecent() {
        assertThat(MailboxView.formatRelative(NOW.minusSeconds(30), NOW)).isEqualTo("Just now");
    }

    @Test
    void formatRelativeRendersMinutesAndHours() {
        assertThat(MailboxView.formatRelative(NOW.minusMinutes(1), NOW)).isEqualTo("1 minute ago");
        assertThat(MailboxView.formatRelative(NOW.minusMinutes(5), NOW)).isEqualTo("5 minutes ago");
        assertThat(MailboxView.formatRelative(NOW.minusHours(1), NOW)).isEqualTo("1 hour ago");
        assertThat(MailboxView.formatRelative(NOW.minusHours(3), NOW)).isEqualTo("3 hours ago");
    }

    @Test
    void formatRelativeRendersYesterdayAndDays() {
        assertThat(MailboxView.formatRelative(NOW.minusDays(1), NOW)).isEqualTo("Yesterday");
        assertThat(MailboxView.formatRelative(NOW.minusDays(3), NOW)).isEqualTo("3 days ago");
    }

    @Test
    void formatRelativeFallsBackToAbsoluteDateBeyondAWeek() {
        // Beyond 7 days the relative phrasing stops being useful — show the date.
        assertThat(MailboxView.formatRelative(LocalDateTime.of(2026, 4, 1, 9, 0), NOW))
                .isEqualTo("Apr 1, 2026");
    }

    @Test
    void errorHintIsNullWhenNoError() {
        assertThat(MailboxView.errorHint(null)).isNull();
    }

    @Test
    void errorHintCallsOutAuthenticationFailures() {
        assertThat(MailboxView.errorHint("AUTHENTICATE failed"))
                .contains("app password");
        assertThat(MailboxView.errorHint("Invalid login or password"))
                .contains("app password");
    }

    @Test
    void errorHintCallsOutConnectivityFailures() {
        assertThat(MailboxView.errorHint("Connection refused: imap.example.com"))
                .contains("mail server");
        assertThat(MailboxView.errorHint("Read timed out"))
                .contains("didn't respond");
    }

    @Test
    void errorHintFallsBackToGenericRetryAdvice() {
        assertThat(MailboxView.errorHint("Quota exceeded"))
                .contains("Sync now");
    }
}
