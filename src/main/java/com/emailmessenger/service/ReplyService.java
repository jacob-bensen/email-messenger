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

import java.util.List;

@Service
public class ReplyService {

    private final MessageRepository messageRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@conexusmail.com}")
    private String fromAddress = "noreply@conexusmail.com";

    ReplyService(MessageRepository messageRepository, JavaMailSender mailSender) {
        this.messageRepository = messageRepository;
        this.mailSender = mailSender;
    }

    public void sendReply(long threadId, String threadSubject, String body) {
        sendReply(threadId, threadSubject, body, List.of());
    }

    @Transactional(readOnly = true)
    public void sendReply(long threadId, String threadSubject, String body,
                          List<OutgoingAttachment> attachments) {
        List<Message> messages = messageRepository.findByThreadIdOrderBySentAtAsc(threadId);
        if (messages.isEmpty()) return;

        Message lastMessage = messages.get(messages.size() - 1);
        String replyToAddress = lastMessage.getSender().getEmail();
        String inReplyToHeader = lastMessage.getMessageIdHeader();

        boolean multipart = attachments != null && !attachments.isEmpty();
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(replyToAddress);
            helper.setSubject("Re: " + threadSubject);
            helper.setText(body, false);
            if (inReplyToHeader != null) {
                mimeMessage.setHeader("In-Reply-To", inReplyToHeader);
                mimeMessage.setHeader("References", inReplyToHeader);
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
            throw new MailPreparationException("Could not compose reply", e);
        }
        mailSender.send(mimeMessage);
    }
}
