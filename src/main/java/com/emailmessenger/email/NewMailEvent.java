package com.emailmessenger.email;

import java.util.Set;

/**
 * Published after a poll (scheduled, IDLE-triggered, or manual) imports at
 * least one message for a user. Listeners fan the signal out to that user's
 * open browser sessions and API event streams so views can update in place.
 * {@code conversationKeys} are the conversations the imported messages landed
 * in, so a client can tell whether the conversation it shows changed.
 */
public record NewMailEvent(Long userId, int importedCount, Set<String> conversationKeys) {

    public NewMailEvent {
        conversationKeys = conversationKeys == null ? Set.of() : Set.copyOf(conversationKeys);
    }
}
