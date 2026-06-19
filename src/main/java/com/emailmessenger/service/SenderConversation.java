package com.emailmessenger.service;

import java.util.List;

/**
 * Every message from one email address, across all of the owner's threads,
 * collapsed into a single IM-style timeline. Each {@link SenderBubbleRun}
 * carries the thread it belongs to so messages stay traceable to their source.
 */
public record SenderConversation(
    SenderView sender,
    long threadCount,
    List<SenderBubbleRun> runs
) {
    public int messageCount() {
        return runs.stream().mapToInt(r -> r.messages().size()).sum();
    }
}
