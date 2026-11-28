package com.emailmessenger.service;

/**
 * One non-owner participant in a conversation, for the chat header and the
 * signature panel. {@code signatureHtml} is that person's most recent
 * signature (pre-sanitized), or "" if none was detected; {@code card} is
 * that signature parsed into contact fields, or null when there is none.
 */
public record ChatMember(
    String email,
    String displayName,
    String initials,
    String label,
    String signatureHtml,
    ContactCard card
) {
    public boolean hasSignature() {
        return signatureHtml != null && !signatureHtml.isBlank();
    }
}
