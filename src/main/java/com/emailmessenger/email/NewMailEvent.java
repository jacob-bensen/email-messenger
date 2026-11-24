package com.emailmessenger.email;

import java.util.List;

/**
 * Published after a poll (scheduled, IDLE-triggered, or manual) imports at
 * least one message for a user. Listeners fan the signal out to that user's
 * open browser tabs and app connections (API.md §5.24) so they can update in
 * place. {@code conversationKeys} are the distinct keys of the conversations
 * that gained messages, as keyed after the import.
 */
public record NewMailEvent(Long userId, int importedCount, List<String> conversationKeys) {

    public NewMailEvent {
        conversationKeys = List.copyOf(conversationKeys);
    }
}
