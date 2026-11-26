package com.emailmessenger.email;

import java.util.Set;

/**
 * Published after a poll (scheduled, IDLE-triggered, or manual) imports at
 * least one message for a user. Listeners fan the signal out to that user's
 * open browser tabs and API event streams so lists can update in place.
 *
 * @param conversationKeys the conversations the poll touched (docs/API.md §5.24)
 */
public record NewMailEvent(Long userId, int importedCount, Set<String> conversationKeys) {

    public NewMailEvent {
        conversationKeys = Set.copyOf(conversationKeys);
    }
}
