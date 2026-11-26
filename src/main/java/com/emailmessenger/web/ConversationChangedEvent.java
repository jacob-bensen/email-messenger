package com.emailmessenger.web;

import java.util.List;
import java.util.TreeSet;

/**
 * Published when the user's own action changes a conversation — a send or a
 * read-state flip — so their other tabs and devices refresh
 * ({@code docs/API.md} §5.24 {@code conversation-changed}).
 *
 * @param conversationKeys sorted, distinct; nulls dropped
 */
public record ConversationChangedEvent(Long userId, Change change, List<String> conversationKeys) {

    public enum Change {
        SENT("sent"),
        READ("read");

        private final String wire;

        Change(String wire) {
            this.wire = wire;
        }

        public String wire() {
            return wire;
        }
    }

    public ConversationChangedEvent {
        TreeSet<String> keys = new TreeSet<>();
        for (String key : conversationKeys) {
            if (key != null) {
                keys.add(key);
            }
        }
        conversationKeys = List.copyOf(keys);
    }
}
