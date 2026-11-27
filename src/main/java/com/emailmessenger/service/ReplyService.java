package com.emailmessenger.service;

import com.emailmessenger.domain.Message;
import com.emailmessenger.repository.MessageRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ReplyService {

    private final MessageRepository messageRepository;
    private final JavaMailSender mailSender;

    // Fallback From only — the sender's own mailbox address is used when
    // available (passed by the caller). spring.mail.username is often blank in
    // local/dev, so this can resolve to "", which is why fromOrDefault guards it.
    @Value("${spring.mail.username:noreply@conexusmail.com}")
    private String fromAddress = "noreply@conexusmail.com";

    ReplyService(MessageRepository messageRepository, JavaMailSender mailSender) {
        this.messageRepository = messageRepository;
        this.mailSender = mailSender;
    }

    public void sendReply(long threadId, String threadSubject, String body) {
        sendReply(threadId, threadSubject, body, List.of());
    }

    public void sendReply(long threadId, String threadSubject, String body,
                          List<OutgoingAttachment> attachments) {
        sendReply(threadId, threadSubject, body, attachments, List.of(), null);
    }

    /**
     * Replies on an existing thread, continuing its subject and threading
     * headers. {@code toRecipients} are the conversation's participants (reply-all
     * for a group); when empty, defaults to the last message's sender.
     * {@code from} is the sender's own address; null falls back to the configured
     * mailbox.
     */
    @Transactional(readOnly = true)
    public void sendReply(long threadId, String threadSubject, String body,
                          List<OutgoingAttachment> attachments, List<String> toRecipients,
                          String from) {
        List<Message> messages = messageRepository.findByThreadIdOrderBySentAtAsc(threadId);
        if (messages.isEmpty()) return;

        Message lastMessage = messages.get(messages.size() - 1);
        String[] recipients = (toRecipients == null || toRecipients.isEmpty())
                ? new String[]{ lastMessage.getSender().getEmail() }
                : toRecipients.toArray(String[]::new);
        String inReplyTo = lastMessage.getMessageIdHeader();
        compose(from, recipients, "Re: " + threadSubject, body, attachments, inReplyTo, inReplyTo, null);
    }

    /** The thread a targeted reply joined and the Message-ID it answered (null when the target had none). */
    public record ReplyTarget(long threadId, String inReplyTo) {}

    /**
     * Replies to one specific message rather than the thread's latest: its
     * thread's subject, {@code In-Reply-To} its Message-ID, {@code References}
     * its parent then itself. {@code messageId} becomes the outgoing
     * Message-ID so the recorded copy can carry the same one.
     */
    @Transactional(readOnly = true)
    public ReplyTarget sendReplyTo(long targetMessageId, String body, List<OutgoingAttachment> attachments,
                                   List<String> toRecipients, String from, String messageId) {
        Message target = messageRepository.findById(targetMessageId)
                .orElseThrow(() -> new IllegalArgumentException("No message " + targetMessageId));
        String header = target.getMessageIdHeader();
        String references = header == null ? null
                : target.getInReplyTo() == null ? header : target.getInReplyTo() + " " + header;
        compose(from, toRecipients.toArray(String[]::new), "Re: " + target.getThread().getSubject(), body,
                attachments, header, references, messageId);
        return new ReplyTarget(target.getThread().getId(), header);
    }

    /**
     * Sends a brand-new email (its own subject, no threading headers) to the
     * given recipients. Used when a chat message isn't explicitly a reply.
     */
    public void sendNewEmail(String subject, String body, List<OutgoingAttachment> attachments,
                             List<String> toRecipients, String from) {
        if (toRecipients == null || toRecipients.isEmpty()) {
            return;
        }
        compose(from, toRecipients.toArray(String[]::new), subject, body, attachments, null, null, null);
    }

    private void compose(String from, String[] recipients, String subject, String body,
                         List<OutgoingAttachment> attachments, String inReplyTo, String references,
                         String messageId) {
        boolean multipart = attachments != null && !attachments.isEmpty();
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, "UTF-8");
            helper.setFrom(fromOrDefault(from));
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(body == null ? "" : body, false);
            if (inReplyTo != null) {
                mimeMessage.setHeader("In-Reply-To", inReplyTo);
                mimeMessage.setHeader("References", references);
            }
            if (messageId != null) {
                // JavaMailSenderImpl keeps an explicit Message-ID across saveChanges().
                mimeMessage.setHeader("Message-ID", messageId);
            }
            if (multipart) {
                for (OutgoingAttachment att : attachments) {
                    String contentType = att.contentType() != null
                            ? att.contentType() : "application/octet-stream";
                    helper.addAttachment(att.filename(),
                            new ByteArrayResource(att.content()), contentType);
                }
            }
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose email", e);
        }
        mailSender.send(mimeMessage);
    }

    private String fromOrDefault(String from) {
        if (StringUtils.hasText(from)) {
            return from.trim();
        }
        if (StringUtils.hasText(fromAddress)) {
            return fromAddress.trim();
        }
        return "noreply@conexusmail.com";
    }
}
