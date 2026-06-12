package com.emailmessenger.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Operator-only allowlist that gates {@code /admin/**}. Sourced from
 * {@code ADMIN_EMAILS} as a comma-separated list. Empty means no one is
 * admin — every {@code /admin/**} request returns 404, hiding the
 * surface from non-operators.
 */
@ConfigurationProperties("admin")
public class AdminProperties {

    private List<String> emails = List.of();

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> raw) {
        if (raw == null) {
            this.emails = List.of();
            return;
        }
        List<String> normalised = new ArrayList<>(raw.size());
        for (String e : raw) {
            if (e == null) {
                continue;
            }
            String t = e.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) {
                normalised.add(t);
            }
        }
        this.emails = List.copyOf(normalised);
    }
}
