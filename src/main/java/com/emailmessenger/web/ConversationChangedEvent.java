package com.emailmessenger.web;

import java.util.List;

/**
 * A change the user made to a conversation (a send, or a mark-read that
 * turned something read), fanned out to every open stream of that user —
 * including the one that made it — as the SSE event {@link Kind#eventName()}
 * ({@code docs/API.md} §5.24).
 */
public record ConversationChangedEvent(Long userId, Kind kind, List<String> conversationKeys) {

    public enum Kind {
        SENT("message-sent"),
        READ("conversation-read");

        private final String eventName;

        Kind(String eventName) {
            this.eventName = eventName;
        }

        public String eventName() {
            return eventName;
        }
    }

    public ConversationChangedEvent {
        conversationKeys = List.copyOf(conversationKeys);
    }

    static ConversationChangedEvent of(Long userId, Kind kind, String conversationKey) {
        return new ConversationChangedEvent(userId, kind,
                conversationKey == null ? List.of() : List.of(conversationKey));
    }
}
