package com.emailmessenger.service;

import java.time.LocalDate;
import java.util.List;

/**
 * A run of consecutive messages from the same side (the correspondent, or the
 * owner's outbound replies) within one thread, surfaced in the per-sender chat.
 * {@link #thread()} badges the run; {@link #outbound()} flags the owner's own
 * messages so they render as "you".
 */
public record SenderBubbleRun(
    SenderView sender,
    ThreadRef thread,
    List<BubbleMessage> messages,
    boolean outbound
) {
    public LocalDate date() {
        if (messages.isEmpty()) return null;
        var sentAt = messages.get(0).sentAt();
        return sentAt != null ? sentAt.toLocalDate() : null;
    }
}
