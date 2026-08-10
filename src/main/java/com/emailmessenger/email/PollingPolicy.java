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
 *   <li>Free polls every 15 minutes; Pro / Business every 5 minutes.
 *       Paid tiers get a tighter loop so "always-fresh inbox" — the thing
 *       they pay $6.99/mo for — actually delivers within sight. Users can
 *       slow this down (never speed it up past their tier) via the sync
 *       frequency preference on /mailboxes.</li>
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

    /** The plan's floor — the fastest cadence the tier allows. */
    public Duration intervalFor(Plan plan) {
        return plan == Plan.FREE ? FREE_INTERVAL : PAID_INTERVAL;
    }

    /**
     * The cadence actually used: the user's chosen frequency, but never
     * faster than the plan floor. {@code null} (no preference) and a
     * too-fast leftover from a downgraded plan both resolve to the floor.
     */
    public Duration effectiveInterval(Plan plan, Integer userMinutes) {
        Duration floor = intervalFor(plan);
        if (userMinutes == null) {
            return floor;
        }
        Duration requested = Duration.ofMinutes(userMinutes);
        return requested.compareTo(floor) < 0 ? floor : requested;
    }

    public int suspendAtFailures() {
        return SUSPEND_AT_FAILURES;
    }

    /**
     * {@code from + effectiveInterval(plan, userMinutes) +/- jitter}. The jitter
     * is uniformly distributed in {@code [-JITTER_SECONDS, +JITTER_SECONDS]} seconds.
     */
    public LocalDateTime nextPollAt(Plan plan, Integer userMinutes, LocalDateTime from) {
        long jitter = ThreadLocalRandom.current()
                .nextLong(-JITTER_SECONDS, JITTER_SECONDS + 1L);
        return from.plus(effectiveInterval(plan, userMinutes)).plusSeconds(jitter);
    }
}
