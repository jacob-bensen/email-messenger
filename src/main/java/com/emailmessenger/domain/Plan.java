package com.emailmessenger.domain;

import java.util.Locale;

public enum Plan {
    FREE,
    PRO,
    BUSINESS;

    public static Plan parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("plan is required");
        }
        try {
            return Plan.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown plan: " + raw);
        }
    }
}
