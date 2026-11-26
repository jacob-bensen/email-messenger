package com.emailmessenger.web;

import java.util.List;
import java.util.Locale;

/**
 * Published when the owner changes a conversation through the shared send
 * and mark-read paths (website or {@code /api/v1}), so the user's other tabs
 * and devices catch up ({@code docs/API.md} §5.24 {@code conversation-updated}).
 *
 * @param conversationKeys sorted, distinct keys of the changed conversations
 */
public record ConversationUpdatedEvent(Long userId, Change change, List<String> conversationKeys) {

    public enum Change {
        SENT, READ;

        String wireName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public ConversationUpdatedEvent {
        conversationKeys = conversationKeys.stream().distinct().sorted().toList();
    }

    static ConversationUpdatedEvent of(Long userId, Change change, String conversationKey) {
        return new ConversationUpdatedEvent(userId, change,
                conversationKey == null ? List.of() : List.of(conversationKey));
    }
}
