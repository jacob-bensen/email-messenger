package com.emailmessenger.email;

import java.util.Set;

/**
 * Published after a poll (scheduled, IDLE-triggered, or manual) imports at
 * least one message for a user. Listeners fan the signal out to that user's
 * open browser sessions so the chats view can update in place.
 * {@code conversationKeys} names the conversations the imported messages
 * landed in, so an API client can refresh just those rows.
 */
public record NewMailEvent(Long userId, int importedCount, Set<String> conversationKeys) {

    public NewMailEvent {
        conversationKeys = Set.copyOf(conversationKeys);
    }
}
