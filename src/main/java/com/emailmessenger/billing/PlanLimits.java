package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-plan caps on mailboxes, threads, and saved searches. {@link #UNLIMITED}
 * signals no enforced ceiling. Free is what creates upgrade pressure: it has
 * every feature but caps mailboxes and thread history. Pro lifts those two
 * caps; Business is unbounded.
 */
public final class PlanLimits {

    public static final long UNLIMITED = Long.MAX_VALUE;

    private static final Map<Plan, PlanLimits> CAPS = new EnumMap<>(Plan.class);
    static {
        // Free: full feature access, but capped at 3 mailboxes and 500 threads
        // of history (saved searches are unlimited — the only paid levers are
        // mailbox count and history). Pro lifts those to 5 mailboxes + full
        // history; Business is unbounded and sales-assisted.
        CAPS.put(Plan.FREE,     new PlanLimits(3, 500,       UNLIMITED));
        CAPS.put(Plan.PRO,      new PlanLimits(5, UNLIMITED, UNLIMITED));
        CAPS.put(Plan.BUSINESS, new PlanLimits(UNLIMITED, UNLIMITED, UNLIMITED));
    }

    private final long mailboxes;
    private final long threads;
    private final long savedSearches;

    private PlanLimits(long mailboxes, long threads, long savedSearches) {
        this.mailboxes = mailboxes;
        this.threads = threads;
        this.savedSearches = savedSearches;
    }

    public long mailboxes() { return mailboxes; }
    public long threads() { return threads; }
    public long savedSearches() { return savedSearches; }

    public static PlanLimits forPlan(Plan plan) {
        PlanLimits caps = CAPS.get(plan);
        if (caps == null) {
            throw new IllegalArgumentException("No limits configured for plan " + plan);
        }
        return caps;
    }
}
