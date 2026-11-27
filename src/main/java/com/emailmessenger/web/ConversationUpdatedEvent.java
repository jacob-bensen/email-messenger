package com.emailmessenger.web;

import java.util.List;

/**
 * Published when the user changes a conversation themselves — a send recorded
 * from any client, or unread threads marked read — so their other open tabs and
 * devices catch up ({@code docs/API.md} §5.24 {@code conversation-updated}).
 * Mail arriving from outside is {@link com.emailmessenger.email.NewMailEvent}.
 */
public record ConversationUpdatedEvent(Long userId, Reason reason, List<String> conversationKeys) {

    public enum Reason {
        SENT("sent"),
        READ("read");

        private final String wire;

        Reason(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }
    }

    public ConversationUpdatedEvent {
        conversationKeys = List.copyOf(conversationKeys);
    }

    static ConversationUpdatedEvent of(Long userId, Reason reason, String conversationKey) {
        return new ConversationUpdatedEvent(userId, reason,
                conversationKey == null || conversationKey.isBlank() ? List.of() : List.of(conversationKey));
    }
}
