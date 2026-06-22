package com.emailmessenger.service;

import java.util.List;
import java.time.LocalDateTime;

/**
 * One row in the chats list: a person or group, the time and a one-line preview
 * of the most recent message, and whether anything in it is unread. Ordered
 * most-recent first by the query that produces it.
 *
 * <p>{@code accounts} are the user's own connected-account addresses this
 * conversation came in on — populated only for the cross-account Dashboard so
 * each row can show which inbox it belongs to; empty in single-account views.
 *
 * <p>{@code senderEmail} is the email of the most recent message's sender (or
 * the primary correspondent when you sent it), shown in grey after the name.
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
    long threadCount,
    List<String> accounts,
    String senderEmail
) {}
