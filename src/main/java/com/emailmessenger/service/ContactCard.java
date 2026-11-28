package com.emailmessenger.service;

import java.util.List;

/**
 * A member's signature read as a contact card (docs/API.md §5.6
 * {@code members[].signature}). {@code title} and {@code company} are
 * best-effort and may be null; the lists may be empty.
 */
public record ContactCard(
    String text,
    String title,
    String company,
    List<Phone> phones,
    List<String> websites
) {
    /** {@code display} as written; {@code tel} is "+" and digits only. */
    public record Phone(String display, String tel) {}

    public boolean hasFields() {
        return title != null || company != null || !phones.isEmpty() || !websites.isEmpty();
    }
}
