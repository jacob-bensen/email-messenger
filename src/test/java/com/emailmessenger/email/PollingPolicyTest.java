package com.emailmessenger.email;

import com.emailmessenger.domain.Plan;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PollingPolicyTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 5, 26, 12, 0);
    private final PollingPolicy policy = new PollingPolicy();

    @Test
    void freeTierPollsEveryFifteenMinutes() {
        assertThat(policy.intervalFor(Plan.FREE)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void paidTiersPollEveryFiveMinutes() {
        assertThat(policy.intervalFor(Plan.PERSONAL)).isEqualTo(Duration.ofMinutes(5));
        assertThat(policy.intervalFor(Plan.TEAM)).isEqualTo(Duration.ofMinutes(5));
        assertThat(policy.intervalFor(Plan.ENTERPRISE)).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void nextPollAtForFreeFallsWithinFifteenMinuteWindowPlusOrMinusThirtySeconds() {
        LocalDateTime earliest = FROM.plusMinutes(15).minusSeconds(PollingPolicy.JITTER_SECONDS);
        LocalDateTime latest = FROM.plusMinutes(15).plusSeconds(PollingPolicy.JITTER_SECONDS);
        // Sample many times to assert the bounds hold across the random distribution.
        for (int i = 0; i < 200; i++) {
            LocalDateTime next = policy.nextPollAt(Plan.FREE, FROM);
            assertThat(next).isBetween(earliest, latest);
        }
    }

    @Test
    void nextPollAtForPersonalFallsWithinFiveMinuteWindowPlusOrMinusThirtySeconds() {
        LocalDateTime earliest = FROM.plusMinutes(5).minusSeconds(PollingPolicy.JITTER_SECONDS);
        LocalDateTime latest = FROM.plusMinutes(5).plusSeconds(PollingPolicy.JITTER_SECONDS);
        for (int i = 0; i < 200; i++) {
            LocalDateTime next = policy.nextPollAt(Plan.PERSONAL, FROM);
            assertThat(next).isBetween(earliest, latest);
        }
    }

    @Test
    void jitterIsActuallyApplied() {
        // 200 draws over a 61-second window: getting the same value every time
        // would mean the jitter was wired in but always zero. Assert that we
        // see at least two distinct values across the sample.
        LocalDateTime first = policy.nextPollAt(Plan.PERSONAL, FROM);
        boolean sawDifferent = false;
        for (int i = 0; i < 200; i++) {
            if (!policy.nextPollAt(Plan.PERSONAL, FROM).equals(first)) {
                sawDifferent = true;
                break;
            }
        }
        assertThat(sawDifferent).isTrue();
    }

    @Test
    void suspendThresholdIsFiveFailures() {
        assertThat(policy.suspendAtFailures()).isEqualTo(5);
    }
}
