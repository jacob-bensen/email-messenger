package com.emailmessenger.web;

import java.util.List;
import java.util.TreeSet;

/**
 * The user's own change to one or more conversations, fanned out by
 * {@link ChatEventStream} as {@code conversation-changed} so their other
 * tabs and devices refetch ({@code docs/API.md} §5.24).
 *
 * @param conversationKeys sorted, distinct; never empty
 */
public record ConversationChangedEvent(Long userId, Change change, List<String> conversationKeys) {

    public enum Change {
        SENT, READ;

        String wireName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public ConversationChangedEvent {
        conversationKeys = List.copyOf(new TreeSet<>(conversationKeys));
        if (conversationKeys.isEmpty()) {
            throw new IllegalArgumentException("conversationKeys must not be empty");
        }
    }

    static ConversationChangedEvent of(Long userId, Change change, String conversationKey) {
        return new ConversationChangedEvent(userId, change, List.of(conversationKey));
    }
}
