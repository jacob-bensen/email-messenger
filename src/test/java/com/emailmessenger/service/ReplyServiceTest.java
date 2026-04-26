package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.Participant;
import com.emailmessenger.repository.MessageRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyServiceTest {

    @Mock MessageRepository messageRepository;
    @Mock JavaMailSender mailSender;

    ReplyService replyService;

    @BeforeEach
    void setUp() {
        replyService = new ReplyService(messageRepository, mailSender);
        ReflectionTestUtils.setField(replyService, "fromAddress", "noreply@mailaim.app");
    }

    @Test
    void sendReplyDoesNothingWhenThreadHasNoMessages() {
        when(messageRepository.findByThreadIdOrderBySentAtAsc(1L)).thenReturn(List.of());

        replyService.sendReply(1L, "Subject", "Hello");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendReplySendsMimeMessageToLastMessageSender() throws Exception {
        EmailThread thread = new EmailThread("Test", "<root@test>");
        Participant alice = new Participant("alice@example.com", "Alice");
        Message msg = new Message(thread, alice, "Test", "body", null, LocalDateTime.now());

        MimeMessage mimeMsg = new MimeMessage((Session) null);
        when(messageRepository.findByThreadIdOrderBySentAtAsc(1L)).thenReturn(List.of(msg));
        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);

        replyService.sendReply(1L, "Test Subject", "My reply body");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Re: Test Subject");
        assertThat(captor.getValue().getAllRecipients()[0].toString())
                .isEqualTo("alice@example.com");
    }

    @Test
    void sendReplyUsesLastMessageSenderWhenMultipleMessages() throws Exception {
        EmailThread thread = new EmailThread("Test", "<root@test>");
        Participant alice = new Participant("alice@example.com", "Alice");
        Participant bob = new Participant("bob@example.com", "Bob");
        Message first = new Message(thread, alice, "Test", "first", null, LocalDateTime.now().minusHours(1));
        Message last = new Message(thread, bob, "Test", "last", null, LocalDateTime.now());

        MimeMessage mimeMsg = new MimeMessage((Session) null);
        when(messageRepository.findByThreadIdOrderBySentAtAsc(1L)).thenReturn(List.of(first, last));
        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);

        replyService.sendReply(1L, "Subject", "Reply");

        verify(mailSender).send(any(MimeMessage.class));
        assertThat(mimeMsg.getAllRecipients()[0].toString()).isEqualTo("bob@example.com");
    }

    @Test
    void sendReplyPropagatesMailSendExceptionOnFailure() {
        EmailThread thread = new EmailThread("Test", "<root@test>");
        Participant alice = new Participant("alice@example.com", "Alice");
        Message msg = new Message(thread, alice, "Test", "body", null, LocalDateTime.now());

        MimeMessage mimeMsg = new MimeMessage((Session) null);
        when(messageRepository.findByThreadIdOrderBySentAtAsc(1L)).thenReturn(List.of(msg));
        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> replyService.sendReply(1L, "Subject", "Hello"))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("SMTP unavailable");
    }
}
