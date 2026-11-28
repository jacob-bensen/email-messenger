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

    /**
     * Where a reply goes: the email thread it continues, that thread's subject,
     * and the threading headers that answer one message in it. Headers are
     * null when the answered message has no Message-ID. Built by {@link ReplyThreading}.
     */
    public record Threading(long threadId, String threadSubject, String inReplyTo, String references) {}

    /**
     * Replies on an existing thread to {@code toRecipients} (the conversation's
     * participants). {@code from} is the sender's own address; null falls back
     * to the configured mailbox. {@code messageId} is the Message-ID to send
     * with, so the recorded copy can carry the same one.
     */
    public void sendReply(Threading threading, String body, List<OutgoingAttachment> attachments,
                          List<String> toRecipients, String from, String messageId) {
        if (toRecipients == null || toRecipients.isEmpty()) {
            return;
        }
        compose(from, toRecipients.toArray(String[]::new), "Re: " + threading.threadSubject(), body,
                attachments, messageId, threading.inReplyTo(), threading.references());
    }

    /**
     * Sends a brand-new email (its own subject, no threading headers) to the
     * given recipients. Used when a chat message isn't explicitly a reply.
     */
    public void sendNewEmail(String subject, String body, List<OutgoingAttachment> attachments,
                             List<String> toRecipients, String from, String messageId) {
        if (toRecipients == null || toRecipients.isEmpty()) {
            return;
        }
        compose(from, toRecipients.toArray(String[]::new), subject, body, attachments, messageId, null, null);
    }

    private void compose(String from, String[] recipients, String subject, String body,
                         List<OutgoingAttachment> attachments, String messageId,
                         String inReplyTo, String references) {
        boolean multipart = attachments != null && !attachments.isEmpty();
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, "UTF-8");
            helper.setFrom(fromOrDefault(from));
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(body == null ? "" : body, false);
            // JavaMailSenderImpl keeps an explicitly set Message-ID across saveChanges().
            if (messageId != null) {
                mimeMessage.setHeader("Message-ID", messageId);
            }
            if (inReplyTo != null) {
                mimeMessage.setHeader("In-Reply-To", inReplyTo);
                mimeMessage.setHeader("References", references != null ? references : inReplyTo);
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
