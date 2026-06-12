package com.emailmessenger.service;

import com.emailmessenger.domain.Participant;
import java.time.LocalDate;
import java.util.List;

public record BubbleRun(
    Participant sender,
    List<BubbleMessage> messages
) {
    public LocalDate date() {
        if (messages.isEmpty()) return null;
        var sentAt = messages.get(0).sentAt();
        return sentAt != null ? sentAt.toLocalDate() : null;
    }
}
