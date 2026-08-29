package com.emailmessenger.api;

/**
 * JSON shape for one plan in {@code GET /api/v1/plans}. Cap fields are
 * nullable — a null value means "no ceiling" (matches PlanLimits.UNLIMITED),
 * so clients render "Unlimited" rather than the sentinel Long.MAX_VALUE.
 * BUSINESS returns zero cents because pricing is contact-sales.
 */
record PlanResource(
        String id,
        int monthlyCents,
        int annualMonthlyCents,
        Long mailboxLimit,
        Long threadLimit,
        Long savedSearchLimit) {}
