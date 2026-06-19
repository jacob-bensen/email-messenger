package com.emailmessenger.service;

import com.emailmessenger.domain.Participant;

/**
 * Proxy-free snapshot of a message sender for the conversation view. Built
 * inside the read transaction so the rendered template never touches a lazy
 * Participant proxy after the session has closed.
 */
public record SenderView(String email, String displayName, String initials) {

    static SenderView from(Participant participant) {
        return new SenderView(
                participant.getEmail(),
                participant.getDisplayName(),
                participant.initials());
    }
}
