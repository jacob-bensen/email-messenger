package com.emailmessenger.web;

import com.emailmessenger.domain.SavedSearch;

/**
 * Rail-row view model for the "Saved searches" section above the sender
 * drill-down. {@link #matches} lets the template highlight the row that
 * corresponds to the active inbox query — Free users with one saved search
 * use this to navigate back to it instead of re-typing the query.
 */
public record SavedSearchView(
        Long id,
        String name,
        String query,
        String senderEmail,
        String sincePreset,
        boolean requireUnread,
        boolean requireAttachments
) {

    public static SavedSearchView from(SavedSearch s) {
        return new SavedSearchView(
                s.getId(),
                s.getName(),
                s.getQuery(),
                s.getSenderEmail(),
                s.getSincePreset(),
                s.isRequireUnread(),
                s.isRequireAttachments());
    }

    public boolean hasQuery() { return query != null && !query.isBlank(); }
    public boolean hasSender() { return senderEmail != null && !senderEmail.isBlank(); }
    public boolean hasSincePreset() { return sincePreset != null && !sincePreset.isBlank(); }

    /**
     * True when the supplied active params line up byte-for-byte with this
     * saved search. The template uses it to render the row's active-state
     * brand-tint without a duplicate set of URL helpers.
     */
    public boolean matches(String activeQuery,
                           String activeSender,
                           String activeSincePreset,
                           boolean activeUnread,
                           boolean activeAttachments) {
        return equalsIgnoreBlank(query, activeQuery)
                && equalsCaseInsensitive(senderEmail, activeSender)
                && equalsIgnoreBlank(sincePreset, activeSincePreset)
                && requireUnread == activeUnread
                && requireAttachments == activeAttachments;
    }

    private static boolean equalsIgnoreBlank(String a, String b) {
        String aa = (a == null || a.isBlank()) ? null : a;
        String bb = (b == null || b.isBlank()) ? null : b;
        if (aa == null && bb == null) return true;
        if (aa == null || bb == null) return false;
        return aa.equals(bb);
    }

    private static boolean equalsCaseInsensitive(String a, String b) {
        String aa = (a == null || a.isBlank()) ? null : a;
        String bb = (b == null || b.isBlank()) ? null : b;
        if (aa == null && bb == null) return true;
        if (aa == null || bb == null) return false;
        return aa.equalsIgnoreCase(bb);
    }
}
