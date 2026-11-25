package com.emailmessenger.email;

import java.util.List;

/**
 * Published after a poll (scheduled, IDLE-triggered, or manual) imports at
 * least one message for a user. Listeners fan the signal out to that user's
 * open browser tabs and API event streams (docs/API.md §3, §5.24).
 *
 * @param conversationKeys sorted, distinct keys of the threads the messages
 *                         landed in
 */
public record NewMailEvent(Long userId, int importedCount, List<String> conversationKeys) {

    public NewMailEvent {
        conversationKeys = List.copyOf(conversationKeys);
    }
}
