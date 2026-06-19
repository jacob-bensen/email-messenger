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
}
