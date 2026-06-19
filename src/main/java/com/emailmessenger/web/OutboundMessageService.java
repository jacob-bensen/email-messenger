package com.emailmessenger.web;

import com.emailmessenger.domain.Attachment;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.service.OutgoingAttachment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Records a reply the user just sent as a message in the thread, so it shows up
 * in the conversation as a "you" chat bubble. Attachment bytes aren't stored
 * (we don't keep a blob store) — only their metadata, which is enough to render
 * the filename chips.
 */
@Service
class OutboundMessageService {

    private final EmailThreadRepository threadRepo;
    private final MessageRepository messageRepo;
    private final ParticipantRepository participantRepo;
    private final Clock clock;

    OutboundMessageService(EmailThreadRepository threadRepo,
                           MessageRepository messageRepo,
                           ParticipantRepository participantRepo,
                           Clock clock) {
        this.threadRepo = threadRepo;
        this.messageRepo = messageRepo;
        this.participantRepo = participantRepo;
        this.clock = clock;
    }

    @Transactional
    void recordReply(Long threadId, User owner, String body, List<OutgoingAttachment> attachments) {
        EmailThread thread = threadRepo.findByIdAndOwner(threadId, owner).orElse(null);
        if (thread == null) {
            return;
        }
        Participant me = participantRepo.findByEmail(owner.getEmail())
                .orElseGet(() -> participantRepo.save(
                        new Participant(owner.getEmail(), owner.getDisplayName())));

        Message reply = new Message(thread, me, "Re: " + thread.getSubject(),
                body == null ? "" : body, null, LocalDateTime.now(clock));
        reply.markOutbound();
        for (OutgoingAttachment att : attachments) {
            reply.addAttachment(new Attachment(reply, att.filename(), att.contentType(),
                    (long) att.content().length, null));
        }

        Message saved = messageRepo.save(reply);
        thread.addMessage(saved);
        // The user just sent this from an open thread — it isn't "unread".
        thread.markRead();
        threadRepo.save(thread);
    }
}
