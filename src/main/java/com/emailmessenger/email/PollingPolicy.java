package com.emailmessenger.email;

import com.emailmessenger.domain.Plan;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cadence + circuit-breaker policy for the recurring IMAP poll loop.
 *
 * <ul>
 *   <li>Free polls every 15 minutes; Personal / Team / Enterprise every
 *       5 minutes. Paid tiers get a tighter loop so "always-fresh inbox"
 *       — the thing they pay $9/mo for — actually delivers within sight.</li>
 *   <li>Each scheduled next-poll time gets {@value #JITTER_SECONDS}s of
 *       symmetric jitter so a herd of mailboxes that connected at the same
 *       instant doesn't slam IMAP servers in lockstep five minutes later.</li>
 *   <li>After {@value #SUSPEND_AT_FAILURES} consecutive failures the
 *       account is marked {@code polling_suspended=true} and is skipped by
 *       {@code pollAll()} until a successful poll clears it (the user can
 *       force one via the manual "Sync now" button).</li>
 * </ul>
 */
@Component
public class PollingPolicy {

    static final Duration FREE_INTERVAL = Duration.ofMinutes(15);
    static final Duration PAID_INTERVAL = Duration.ofMinutes(5);
    static final int JITTER_SECONDS = 30;
    static final int SUSPEND_AT_FAILURES = 5;

    public Duration intervalFor(Plan plan) {
        return plan == Plan.FREE ? FREE_INTERVAL : PAID_INTERVAL;
    }

    public int suspendAtFailures() {
        return SUSPEND_AT_FAILURES;
    }

    /**
     * {@code from + intervalFor(plan) +/- jitter}. The jitter is uniformly
     * distributed in {@code [-JITTER_SECONDS, +JITTER_SECONDS]} seconds.
     */
    public LocalDateTime nextPollAt(Plan plan, LocalDateTime from) {
        long jitter = ThreadLocalRandom.current()
                .nextLong(-JITTER_SECONDS, JITTER_SECONDS + 1L);
        return from.plus(intervalFor(plan)).plusSeconds(jitter);
    }
}
