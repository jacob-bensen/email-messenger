package com.emailmessenger.email;

import java.util.List;

/**
 * Published after a poll (scheduled, IDLE-triggered, or manual) imports at
 * least one message for a user. Listeners fan the signal out to that user's
 * open browser sessions and API event streams so lists can update in place.
 * {@code conversationKeys} are the distinct keys that received the messages.
 */
public record NewMailEvent(Long userId, int importedCount, List<String> conversationKeys) {

    public NewMailEvent {
        conversationKeys = List.copyOf(conversationKeys);
    }
}
