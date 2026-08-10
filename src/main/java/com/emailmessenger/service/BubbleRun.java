package com.emailmessenger.service;

import java.time.LocalDate;
import java.util.List;

public record BubbleRun(
    SenderView sender,
    List<BubbleMessage> messages,
    boolean outbound
) {
    public LocalDate date() {
        if (messages.isEmpty()) return null;
        var sentAt = messages.get(0).sentAt();
        return sentAt != null ? sentAt.toLocalDate() : null;
    }

    /** UTC epoch millis of the run's first message; drives the local-time day separators. */
    public Long startEpochMilli() {
        return messages.isEmpty() ? null : messages.get(0).sentAtEpochMilli();
    }
}
