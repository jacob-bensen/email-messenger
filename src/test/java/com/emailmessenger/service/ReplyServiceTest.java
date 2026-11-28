package com.emailmessenger.service;

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

import jakarta.mail.Multipart;
import java.nio.charset.StandardCharsets;
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

    private static final ReplyService.Threading LUNCH =
            new ReplyService.Threading(1L, "Lunch", "<pizza@acme.com>", "<root@acme.com> <pizza@acme.com>");
    private static final List<String> ADA = List.of("ada@example.com");

    @BeforeEach
    void setUp() {
        replyService = new ReplyService(messageRepository, mailSender);
        ReflectionTestUtils.setField(replyService, "fromAddress", "noreply@conexusmail.com");
    }

    @Test
    void sendReplyContinuesTheThreadSubjectAndAnswersTheTargetMessage() throws Exception {
        MimeMessage mimeMsg = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);

        replyService.sendReply(LUNCH, "Pizza it is", List.of(), ADA, "me@mybox.com", "<mine@conexusmail.com>");

        verify(mailSender).send(any(MimeMessage.class));
        assertThat(mimeMsg.getSubject()).isEqualTo("Re: Lunch");
        assertThat(mimeMsg.getAllRecipients()[0].toString()).isEqualTo("ada@example.com");
        assertThat(mimeMsg.getFrom()[0].toString()).isEqualTo("me@mybox.com");
        assertThat(mimeMsg.getHeader("In-Reply-To")).containsExactly("<pizza@acme.com>");
        assertThat(mimeMsg.getHeader("References")).containsExactly("<root@acme.com> <pizza@acme.com>");
        assertThat(mimeMsg.getHeader("Message-ID")).containsExactly("<mine@conexusmail.com>");
    }

    @Test
    void sendReplyOmitsThreadingHeadersWhenTheTargetHasNoMessageId() throws Exception {
        MimeMessage mimeMsg = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);

        replyService.sendReply(new ReplyService.Threading(1L, "Lunch", null, null), "Reply body",
                List.of(), ADA, null, "<mine@conexusmail.com>");

        assertThat(mimeMsg.getSubject()).isEqualTo("Re: Lunch");
        assertThat(mimeMsg.getHeader("In-Reply-To")).isNull();
        assertThat(mimeMsg.getHeader("References")).isNull();
        assertThat(mimeMsg.getFrom()[0].toString()).isEqualTo("noreply@conexusmail.com");
    }

    @Test
    void sendReplySendsToEveryRecipient() throws Exception {
        MimeMessage mimeMsg = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);

        replyService.sendReply(LUNCH, "body", List.of(),
                List.of("ada@example.com", "bob@example.com"), "me@mybox.com", null);

        assertThat(mimeMsg.getAllRecipients()).hasSize(2);
    }

    @Test
    void sendReplyDoesNothingWithoutRecipients() {
        replyService.sendReply(LUNCH, "body", List.of(), List.of(), "me@mybox.com", null);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendReplyAttachesProvidedFilesAsMultipart() throws Exception {
        MimeMessage mimeMsg = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);

        OutgoingAttachment att = new OutgoingAttachment(
                "report.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        replyService.sendReply(LUNCH, "See attached", List.of(att), ADA, null, null);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        Object content = captor.getValue().getContent();
        assertThat(content).isInstanceOf(Multipart.class);
        Multipart mp = (Multipart) content;
        boolean attached = false;
        for (int i = 0; i < mp.getCount(); i++) {
            if ("report.txt".equals(mp.getBodyPart(i).getFileName())) {
                attached = true;
            }
        }
        assertThat(attached).isTrue();
    }

    @Test
    void sendNewEmailSendsWithGivenSubjectRecipientsFromAndNoThreadingHeaders() throws Exception {
        MimeMessage mimeMsg = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);

        replyService.sendNewEmail("Lunch?", "Friday work?", List.of(), ADA, "me@mybox.com",
                "<new@conexusmail.com>");

        verify(mailSender).send(any(MimeMessage.class));
        assertThat(mimeMsg.getSubject()).isEqualTo("Lunch?");
        assertThat(mimeMsg.getAllRecipients()[0].toString()).isEqualTo("ada@example.com");
        assertThat(mimeMsg.getFrom()[0].toString()).isEqualTo("me@mybox.com");
        assertThat(mimeMsg.getHeader("In-Reply-To")).isNull();
        assertThat(mimeMsg.getHeader("Message-ID")).containsExactly("<new@conexusmail.com>");
    }

    @Test
    void sendNewEmailDoesNothingWithoutRecipients() {
        replyService.sendNewEmail("Subject", "Body", List.of(), List.of(), "me@mybox.com", null);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendReplyPropagatesMailSendExceptionOnFailure() {
        MimeMessage mimeMsg = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMsg);
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> replyService.sendReply(LUNCH, "Hello", List.of(), ADA, null, null))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("SMTP unavailable");
    }
}
