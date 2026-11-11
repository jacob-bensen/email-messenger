package com.emailmessenger.api;

import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.web.ConversationListService.ChatCounts;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

record ConversationPageDto(
        List<Item> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages,
        boolean hasNext,
        Counts counts
) {
    record Item(
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
            String senderEmail
    ) {
        static Item of(ConversationListItem item) {
            return new Item(item.key(), item.title(), item.initials(), item.group(),
                    item.memberCount(), item.preview(), ApiTime.toInstant(item.lastActivity()),
                    item.unread(), item.threadCount(), item.accounts(), item.senderEmail());
        }
    }

    record Counts(long all, long unread, long awaiting, long attachments) {}

    static ConversationPageDto of(Page<ConversationListItem> page, ChatCounts counts) {
        return new ConversationPageDto(
                page.getContent().stream().map(Item::of).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.hasNext(),
                new Counts(counts.all(), counts.unread(), counts.awaiting(), counts.attachments()));
    }
}
