package com.emailmessenger.billing;

import com.emailmessenger.domain.Plan;

/**
 * Raised when a user attempts an operation that would exceed the caps of
 * their current {@link Plan}. The upgrade modal catches this and renders a
 * plan-comparison panel with a "Upgrade to Pro" CTA into Checkout.
 */
public class PlanLimitExceededException extends RuntimeException {

    private final Plan currentPlan;
    private final PlanLimitKind kind;
    private final long limit;
    private final long current;

    public PlanLimitExceededException(Plan currentPlan, PlanLimitKind kind, long limit, long current) {
        super("Plan " + currentPlan + " " + kind + " limit reached (limit=" + limit + ", current=" + current + ")");
        this.currentPlan = currentPlan;
        this.kind = kind;
        this.limit = limit;
        this.current = current;
    }

    public Plan getCurrentPlan() { return currentPlan; }
    public PlanLimitKind getKind() { return kind; }
    public long getLimit() { return limit; }
    public long getCurrent() { return current; }
}
