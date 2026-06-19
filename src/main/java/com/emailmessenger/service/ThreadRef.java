package com.emailmessenger.service;

/**
 * Lightweight identifier for the thread a bubble run came from, used to badge
 * messages in the cross-thread sender chat and link back to the full thread.
 */
public record ThreadRef(Long threadId, String subject) {}
