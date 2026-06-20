package com.emailmessenger.service;

import java.util.List;

/**
 * A texting-style conversation: every message exchanged with one person or one
 * group (the same participant set), merged across email threads into a single
 * timeline. {@code members} are the non-owner participants — one for a 1:1
 * chat, several for a group — each carrying their latest signature for the
 * side panel. {@code runs} are the chat bubbles, grouped by thread + side.
 */
public record ChatConversation(
    String key,
    String title,
    String initials,
    List<ChatMember> members,
    long threadCount,
    List<SenderBubbleRun> runs
) {
    public int messageCount() {
        return runs.stream().mapToInt(r -> r.messages().size()).sum();
    }

    public boolean isGroup() {
        return members.size() > 1;
    }

    public boolean hasSignatures() {
        return members.stream().anyMatch(ChatMember::hasSignature);
    }
}
