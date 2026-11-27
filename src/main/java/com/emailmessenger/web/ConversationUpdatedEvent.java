package com.emailmessenger.web;

import java.util.List;
import java.util.TreeSet;

/**
 * Published when the user's own action changes a conversation, whichever
 * client it came from, so their other open clients can catch up
 * ({@code docs/API.md} §5.24 {@code conversation-updated}).
 *
 * @param conversationKeys sorted, distinct; never empty
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
        conversationKeys = List.copyOf(new TreeSet<>(conversationKeys));
        if (conversationKeys.isEmpty()) {
            throw new IllegalArgumentException("conversationKeys must not be empty");
        }
    }

    static ConversationUpdatedEvent of(Long userId, Reason reason, String key) {
        return new ConversationUpdatedEvent(userId, reason, List.of(key));
    }
}
