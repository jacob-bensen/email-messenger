package com.emailmessenger.service;

import com.emailmessenger.domain.Participant;
import java.time.LocalDate;
import java.util.List;

record BubbleRun(
    Participant sender,
    List<BubbleMessage> messages
) {
    LocalDate date() {
        if (messages.isEmpty()) return null;
        var sentAt = messages.get(0).sentAt();
        return sentAt != null ? sentAt.toLocalDate() : null;
    }
}
