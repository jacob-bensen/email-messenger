package com.emailmessenger.api;

import com.emailmessenger.service.ConversationListItem;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * One chat row for {@code GET /api/v1/chats}. See {@code docs/API.md}
 * §ChatSummary for the field contract. This is the client-facing shape;
 * it maps 1:1 from {@link ConversationListItem} plus the epoch-millis
 * helper.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
record ChatSummary(
        String key,
        String title,
        String initials,
        boolean group,
        int memberCount,
        String preview,
        LocalDateTime lastActivity,
        Long lastActivityEpochMillis,
        boolean unread,
        long threadCount,
        List<String> accounts,
        String senderEmail
) {
    static ChatSummary from(ConversationListItem item) {
        LocalDateTime last = item.lastActivity();
        Long millis = last == null ? null : last.toInstant(ZoneOffset.UTC).toEpochMilli();
        return new ChatSummary(
                item.key(),
                item.title(),
                item.initials(),
                item.group(),
                item.memberCount(),
                item.preview(),
                last,
                millis,
                item.unread(),
                item.threadCount(),
                item.accounts() == null ? List.of() : item.accounts(),
                item.senderEmail());
    }
}
