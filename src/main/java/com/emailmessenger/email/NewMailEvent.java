package com.emailmessenger.email;

/**
 * Published after a poll (scheduled, IDLE-triggered, or manual) imports at
 * least one message for a user. Listeners fan the signal out to that user's
 * open browser sessions so the chats view can update in place.
 */
public record NewMailEvent(Long userId, int importedCount) {}
