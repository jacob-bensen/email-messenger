package com.emailmessenger.email;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Published after a poll (scheduled, IDLE-triggered, or manual) imports at
 * least one message for a user. Listeners fan the signal out to that user's
 * open browser sessions and app streams so lists can update in place.
 *
 * @param conversationKeys the conversations the imported messages landed in,
 *                         as keyed after the poll; may be empty when unknown
 */
public record NewMailEvent(Long userId, int importedCount, Set<String> conversationKeys) {

    public NewMailEvent {
        conversationKeys = conversationKeys == null ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(conversationKeys));
    }
}
