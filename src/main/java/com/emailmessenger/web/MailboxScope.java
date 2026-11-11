package com.emailmessenger.web;

import com.emailmessenger.domain.MailAccount;

import java.util.List;
import java.util.Locale;

/**
 * Resolves the {@code mailbox} request parameter into the account scope the
 * chats queries run under. Shared by the Thymeleaf workspace and the REST API
 * so both scope conversations identically.
 */
public final class MailboxScope {

    private MailboxScope() {}

    /**
     * The account a view is scoped to: the one whose id matches
     * {@code mailboxParam}, or {@code null} when no (valid) mailbox is specified —
     * the cross-account Dashboard for the list, or the owner-wide timeline for a
     * single chat opened from it.
     */
    public static MailAccount select(List<MailAccount> accounts, String mailboxParam) {
        Long id = parseId(mailboxParam);
        if (id == null) {
            return null;
        }
        for (MailAccount account : accounts) {
            if (account.getId().equals(id)) {
                return account;
            }
        }
        return null;
    }

    /**
     * The address to scope conversation queries to, or {@code null} for the
     * unscoped (all-conversations) view. Scoping only matters to SEPARATE
     * multiple accounts — with a single connected mailbox every conversation
     * belongs to it, so we don't scope. That also avoids dropping list/alias-
     * delivered mail whose headers don't carry the mailbox address.
     */
    public static String address(MailAccount selected, List<MailAccount> accounts) {
        return (selected != null && accounts.size() > 1) ? normalized(selected) : null;
    }

    /** {@link #select} then {@link #address} in one step. */
    public static String address(List<MailAccount> accounts, String mailboxParam) {
        return address(select(accounts, mailboxParam), accounts);
    }

    private static String normalized(MailAccount account) {
        return account.getUsername() == null ? "" : account.getUsername().trim().toLowerCase(Locale.ROOT);
    }

    private static Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
