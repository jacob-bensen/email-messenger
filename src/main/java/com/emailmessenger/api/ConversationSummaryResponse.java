package com.emailmessenger.api;

import com.emailmessenger.service.ConversationListItem;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

record ConversationSummaryResponse(
        String key,
        String title,
        String initials,
        boolean group,
        int memberCount,
        String preview,
        Instant lastActivityAt,
        boolean unread,
        long threadCount,
        List<String> accounts,
        String senderEmail) {

    static ConversationSummaryResponse of(ConversationListItem item) {
        return new ConversationSummaryResponse(
                item.key(), item.title(), item.initials(), item.group(), item.memberCount(),
                item.preview(),
                // Stored timestamps are UTC wall-clock (see the app Clock bean).
                item.lastActivity() == null ? null : item.lastActivity().toInstant(ZoneOffset.UTC),
                item.unread(), item.threadCount(), item.accounts(), item.senderEmail());
    }
}
