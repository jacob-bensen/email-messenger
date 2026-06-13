package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-plan caps on mailboxes, threads, and saved searches. {@link #UNLIMITED}
 * signals no enforced ceiling for paid plans. Free is what creates upgrade
 * pressure; paid tiers exist here mainly so the cap can be wired in once the
 * matching feature ships.
 */
public final class PlanLimits {

    public static final long UNLIMITED = Long.MAX_VALUE;

    private static final Map<Plan, PlanLimits> CAPS = new EnumMap<>(Plan.class);
    static {
        CAPS.put(Plan.FREE,       new PlanLimits(1,  500,       1));
        CAPS.put(Plan.PERSONAL,   new PlanLimits(3,  UNLIMITED, UNLIMITED));
        CAPS.put(Plan.TEAM,       new PlanLimits(10, UNLIMITED, UNLIMITED));
        CAPS.put(Plan.ENTERPRISE, new PlanLimits(UNLIMITED, UNLIMITED, UNLIMITED));
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
