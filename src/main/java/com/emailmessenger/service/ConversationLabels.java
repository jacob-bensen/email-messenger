package com.emailmessenger.service;

import java.util.List;

/**
 * Shared formatting for conversation headers and chat-list rows, so a group is
 * labelled identically everywhere ("Ada, Bob +2") and avatars derive the same
 * initials.
 */
public final class ConversationLabels {

    private ConversationLabels() {}

    /** "You" for a self-thread, the single name for a 1:1, or "A, B, C +N" for a group. */
    public static String title(List<String> memberLabels) {
        if (memberLabels.isEmpty()) {
            return "You";
        }
        if (memberLabels.size() == 1) {
            return memberLabels.get(0);
        }
        int shown = Math.min(3, memberLabels.size());
        String names = String.join(", ", memberLabels.subList(0, shown));
        return memberLabels.size() > shown ? names + " +" + (memberLabels.size() - shown) : names;
    }

    /** 1–2 uppercase initials from a display name, falling back to the email local-part. */
    public static String initials(String displayName, String email) {
        String source = (displayName != null && !displayName.isBlank()) ? displayName : email;
        if (source == null || source.isBlank()) {
            return "?";
        }
        if (!source.contains(" ") && source.contains("@")) {
            source = source.substring(0, source.indexOf('@'));
        }
        String[] parts = source.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return "?";
        }
        if (parts.length >= 2 && !parts[1].isBlank()) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, 1).toUpperCase();
    }
}
