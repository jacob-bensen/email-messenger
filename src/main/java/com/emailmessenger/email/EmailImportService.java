package com.emailmessenger.email;

import com.emailmessenger.domain.Attachment;
import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.ParticipantRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class EmailImportService {

    private final EmailThreadRepository threadRepo;
    private final MessageRepository messageRepo;
    private final ParticipantRepository participantRepo;
    private final MimeMessageParser parser = new MimeMessageParser();

    EmailImportService(EmailThreadRepository threadRepo,
                       MessageRepository messageRepo,
                       ParticipantRepository participantRepo) {
        this.threadRepo = threadRepo;
        this.messageRepo = messageRepo;
        this.participantRepo = participantRepo;
    }

    /**
     * Parses and persists a MimeMessage into the given owner's mailbox. Returns empty
     * if the owner has already imported this message (idempotent per-owner on Message-ID).
     * Thread assignment follows RFC 5322: References newest-first, then In-Reply-To, then
     * rootMessageId lookup — all scoped to the owner so threads never cross tenants.
     */
    @Transactional
    public Optional<Message> importMessage(MimeMessage mimeMessage, User owner) {
        ParsedEmail parsed;
        try {
            parsed = parser.parse(mimeMessage);
        } catch (MessagingException | IOException e) {
            throw new EmailImportException("Failed to parse email message", e);
        }

        if (parsed.messageId() != null
                && messageRepo.findByMessageIdHeaderAndOwner(parsed.messageId(), owner).isPresent()) {
            return Optional.empty();
        }

        Participant sender = resolveParticipant(parsed.fromEmail(), parsed.fromName());
        EmailThread thread = resolveThread(parsed, owner);

        Message message = new Message(thread, sender, parsed.subject(),
                parsed.bodyPlain(), parsed.bodyHtml(), parsed.sentAt());
        message.setMessageIdHeader(parsed.messageId());
        message.setInReplyTo(parsed.inReplyTo());

        for (ParsedEmail.AddressEntry a : parsed.toRecipients()) {
            message.addRecipient(resolveParticipant(a.email(), a.name()), RecipientType.TO);
        }
        for (ParsedEmail.AddressEntry a : parsed.ccRecipients()) {
            message.addRecipient(resolveParticipant(a.email(), a.name()), RecipientType.CC);
        }
        for (ParsedEmail.AddressEntry a : parsed.bccRecipients()) {
            message.addRecipient(resolveParticipant(a.email(), a.name()), RecipientType.BCC);
        }

        for (ParsedEmail.AttachmentEntry att : parsed.attachments()) {
            message.addAttachment(
                    new Attachment(message, att.filename(), att.mimeType(), att.sizeBytes(), null));
        }

        // Persist message first so cascade merge on threadRepo.save is a no-op, not a second INSERT.
        Message saved = messageRepo.save(message);
        thread.addMessage(saved);
        threadRepo.save(thread);
        return Optional.of(saved);
    }

    private Participant resolveParticipant(String email, String displayName) {
        return participantRepo.findByEmail(email)
                .orElseGet(() -> participantRepo.save(new Participant(email, displayName)));
    }

    private EmailThread resolveThread(ParsedEmail parsed, User owner) {
        // Walk References newest-first (RFC 5322 lists oldest first, newest last)
        List<String> refs = parsed.references();
        for (int i = refs.size() - 1; i >= 0; i--) {
            Optional<Message> ref = messageRepo.findByMessageIdHeaderAndOwner(refs.get(i), owner);
            if (ref.isPresent()) return ref.get().getThread();
        }

        if (parsed.inReplyTo() != null) {
            Optional<Message> parent = messageRepo.findByMessageIdHeaderAndOwner(parsed.inReplyTo(), owner);
            if (parent.isPresent()) return parent.get().getThread();
        }

        if (parsed.messageId() != null) {
            Optional<EmailThread> existing = threadRepo.findByRootMessageIdAndOwner(parsed.messageId(), owner);
            if (existing.isPresent()) return existing.get();
        }

        return threadRepo.save(new EmailThread(owner, parsed.subject(), parsed.messageId()));
    }
}
