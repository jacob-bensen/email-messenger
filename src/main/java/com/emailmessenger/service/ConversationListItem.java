package com.emailmessenger.service;

import java.time.LocalDateTime;

/**
 * One row in the chats list: a person or group, the time and a one-line preview
 * of the most recent message, and whether anything in it is unread. Ordered
 * most-recent first by the query that produces it.
 */
public record ConversationListItem(
    String key,
    String title,
    String initials,
    boolean group,
    int memberCount,
    String preview,
    LocalDateTime lastActivity,
    boolean unread,
    long threadCount
) {}
