package com.emailmessenger.admin;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * Allowlist check for {@code /admin/**} controllers. Backed by
 * {@link AdminProperties#getEmails()}. {@link #requireAdmin(String)}
 * throws {@link NoSuchElementException} on a non-admin so the global
 * 404 handler kicks in — non-operators never learn the admin URL
 * exists.
 */
@Component
public class AdminAuthorizer {

    private final AdminProperties properties;

    public AdminAuthorizer(AdminProperties properties) {
        this.properties = properties;
    }

    public boolean isAdmin(String email) {
        if (email == null) {
            return false;
        }
        String normalised = email.trim().toLowerCase(Locale.ROOT);
        if (normalised.isEmpty()) {
            return false;
        }
        return properties.getEmails().contains(normalised);
    }

    public void requireAdmin(String email) {
        if (!isAdmin(email)) {
            throw new NoSuchElementException("admin not found");
        }
    }
}
