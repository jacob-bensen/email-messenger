package com.emailmessenger.web;

import com.emailmessenger.domain.Attachment;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.email.OwnerAddressService;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import com.emailmessenger.service.ConversationKeyService;
import com.emailmessenger.service.OutgoingAttachment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private final ConversationKeyService conversationKeyService;
    private final OwnerAddressService ownerAddressService;
    private final Clock clock;

    OutboundMessageService(EmailThreadRepository threadRepo,
                           MessageRepository messageRepo,
                           ParticipantRepository participantRepo,
                           ConversationKeyService conversationKeyService,
                           OwnerAddressService ownerAddressService,
                           Clock clock) {
        this.threadRepo = threadRepo;
        this.messageRepo = messageRepo;
        this.participantRepo = participantRepo;
        this.conversationKeyService = conversationKeyService;
        this.ownerAddressService = ownerAddressService;
        this.clock = clock;
    }

    @Transactional
    void recordReply(Long threadId, User owner, String body, List<OutgoingAttachment> attachments) {
        EmailThread thread = threadRepo.findByIdAndOwner(threadId, owner).orElse(null);
        if (thread == null) {
            return;
        }
        Participant me = resolveOwnerParticipant(owner);

        Message reply = new Message(thread, me, "Re: " + thread.getSubject(),
                body == null ? "" : body, null, LocalDateTime.now(clock));
        reply.markOutbound();
        addAttachments(reply, attachments);

        Message saved = messageRepo.save(reply);
        thread.addMessage(saved);
        // The user just sent this from an open thread — it isn't "unread".
        thread.markRead();
        // Keep the conversation key in sync (a brand-new thread won't have one yet).
        thread.setConversationKey(
                conversationKeyService.compute(thread, ownerAddressService.addressesFor(owner)));
        threadRepo.save(thread);
    }

    /**
     * Records a brand-new outbound email as its own thread. The recipients are
     * added so the thread keys into the same conversation (same participant set)
     * and the message shows in that chat as a "you" bubble.
     */
    @Transactional
    void recordNewEmail(User owner, String conversationKey, String subject, String body,
                        List<String> recipientEmails, List<OutgoingAttachment> attachments) {
        Participant me = resolveOwnerParticipant(owner);
        String messageId = "<" + UUID.randomUUID() + "@conexusmail.com>";

        EmailThread thread = threadRepo.save(new EmailThread(owner, subject, messageId));
        Message message = new Message(thread, me, subject,
                body == null ? "" : body, null, LocalDateTime.now(clock));
        message.setMessageIdHeader(messageId);
        message.markOutbound();
        for (String email : recipientEmails) {
            message.addRecipient(resolveParticipant(email, null), RecipientType.TO);
        }
        addAttachments(message, attachments);

        Message saved = messageRepo.save(message);
        // Outbound and viewed from an open chat — not unread.
        thread.addMessage(saved, false);
        thread.markRead();
        // Pin it to the conversation the user is in, so it appears in that chat.
        thread.setConversationKey(conversationKey);
        threadRepo.save(thread);
    }

    private Participant resolveOwnerParticipant(User owner) {
        return resolveParticipant(owner.getEmail(), owner.getDisplayName());
    }

    private Participant resolveParticipant(String email, String displayName) {
        return participantRepo.findByEmail(email)
                .orElseGet(() -> participantRepo.save(new Participant(email, displayName)));
    }

    private static void addAttachments(Message message, List<OutgoingAttachment> attachments) {
        for (OutgoingAttachment att : attachments) {
            message.addAttachment(new Attachment(message, att.filename(), att.contentType(),
                    (long) att.content().length, null));
        }
    }
}
