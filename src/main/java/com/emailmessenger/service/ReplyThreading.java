package com.emailmessenger.service;

import com.emailmessenger.domain.Message;
import com.emailmessenger.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Resolves which thread and headers a reply uses (docs/API.md §5.8). */
@Service
public class ReplyThreading {

    private final MessageRepository messageRepository;

    ReplyThreading(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /** Answers the thread's latest message; empty for an unknown or empty thread. */
    @Transactional(readOnly = true)
    public Optional<ReplyService.Threading> latestIn(long threadId) {
        List<Message> messages = messageRepository.findByThreadIdOrderBySentAtAsc(threadId);
        return messages.isEmpty()
                ? Optional.empty()
                : Optional.of(answering(messages.get(messages.size() - 1)));
    }

    /**
     * Answers one message. Callers check it belongs to the user's conversation
     * first; this only resolves its thread and headers.
     */
    @Transactional(readOnly = true)
    public Optional<ReplyService.Threading> answering(long messageId) {
        return messageRepository.findById(messageId).map(ReplyThreading::answering);
    }

    // We don't store a message's References chain, so its In-Reply-To stands in
    // for the rest of it: enough for clients that thread on the last entries.
    static ReplyService.Threading answering(Message message) {
        String id = message.getMessageIdHeader();
        String references = null;
        if (id != null) {
            references = message.getInReplyTo() != null && !message.getInReplyTo().equals(id)
                    ? message.getInReplyTo() + " " + id
                    : id;
        }
        return new ReplyService.Threading(message.getThread().getId(), message.getThread().getSubject(),
                id, references);
    }
}
