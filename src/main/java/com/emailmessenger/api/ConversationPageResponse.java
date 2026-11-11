package com.emailmessenger.api;

import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.web.ConversationListService.ChatCounts;
import org.springframework.data.domain.Page;

import java.util.List;

record ConversationPageResponse(
        List<ConversationSummaryResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext,
        Counts counts) {

    record Counts(long all, long unread, long awaiting, long attachments) {}

    static ConversationPageResponse of(Page<ConversationListItem> page, ChatCounts counts) {
        return new ConversationPageResponse(
                page.getContent().stream().map(ConversationSummaryResponse::of).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.hasNext(),
                new Counts(counts.all(), counts.unread(), counts.awaiting(), counts.attachments()));
    }
}
