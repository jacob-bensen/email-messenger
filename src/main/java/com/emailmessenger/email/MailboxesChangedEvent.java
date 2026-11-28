package com.emailmessenger.email;

import java.util.List;

/**
 * Published when the user's set of mailboxes or their settings change
 * (connect, reconnect, remove, signature, mail preferences). Fanned out as the
 * SSE event {@code mailboxes-changed} ({@code docs/API.md} §5.24).
 *
 * @param mailboxIds sorted ids of the mailboxes affected; empty for a change to
 *                   the user's mail preferences rather than to one mailbox
 */
public record MailboxesChangedEvent(Long userId, List<Long> mailboxIds) {

    public MailboxesChangedEvent {
        mailboxIds = mailboxIds.stream().distinct().sorted().toList();
    }

    static MailboxesChangedEvent of(Long userId, Long mailboxId) {
        return new MailboxesChangedEvent(userId, List.of(mailboxId));
    }
}
