package com.emailmessenger.web;

public record OnboardingChecklist(
        boolean mailboxConnected,
        long threadCount,
        boolean savedSearchSaved,
        boolean teammateInvited) {

    public static final long THREADS_TARGET = 10;
    public static final int TOTAL_STEPS = 4;

    public boolean threadsImported() {
        return threadCount >= THREADS_TARGET;
    }

    public int completedSteps() {
        int n = 0;
        if (mailboxConnected) n++;
        if (threadsImported()) n++;
        if (savedSearchSaved) n++;
        if (teammateInvited) n++;
        return n;
    }

    public int totalSteps() {
        return TOTAL_STEPS;
    }

    public boolean isComplete() {
        return completedSteps() >= TOTAL_STEPS;
    }

    /**
     * The two essential steps — connect a mailbox and import threads — are what
     * gate a usable inbox. Saving a search and inviting a teammate are optional
     * polish, so they don't keep the onboarding panel pinned open; once the core
     * steps are done the panel collapses.
     */
    public boolean coreStepsComplete() {
        return mailboxConnected && threadsImported();
    }

    public int percentComplete() {
        return (int) Math.round(100.0 * completedSteps() / TOTAL_STEPS);
    }

    public long threadsRemaining() {
        long remaining = THREADS_TARGET - threadCount;
        return remaining < 0 ? 0 : remaining;
    }

    public String nextStepCtaUrl() {
        if (!mailboxConnected) {
            return "/mailboxes/new";
        }
        if (!threadsImported()) {
            return "/mailboxes";
        }
        if (!savedSearchSaved) {
            return "/threads";
        }
        if (!teammateInvited) {
            return "/team/invite";
        }
        return "/threads";
    }

    public String nextStepCtaLabel() {
        if (!mailboxConnected) {
            return "Connect your inbox";
        }
        if (!threadsImported()) {
            return "Sync now";
        }
        if (!savedSearchSaved) {
            return "Save your first search";
        }
        if (!teammateInvited) {
            return "Invite a teammate";
        }
        return "Open inbox";
    }
}
