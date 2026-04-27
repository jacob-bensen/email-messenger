package com.emailmessenger.email;

import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImapPollingJobTest {

    @Mock EmailImportService emailImportService;

    private ImapPollingJob job;
    private ImapPollingProperties props;

    @BeforeEach
    void setUp() {
        props = new ImapPollingProperties();
        props.setHost("imap.example.com");
        props.setPort(993);
        props.setUsername("user@example.com");
        props.setPassword("secret");
        job = new ImapPollingJob(emailImportService, props);
    }

    @Test
    void importedMessageIsMarkedSeen() throws Exception {
        MimeMessage mime = mock(MimeMessage.class);
        when(emailImportService.importMessage(mime)).thenReturn(Optional.of(mock(com.emailmessenger.domain.Message.class)));

        job.processMessages(new Message[]{mime});

        verify(mime).setFlag(Flags.Flag.SEEN, true);
        verify(emailImportService).importMessage(mime);
    }

    @Test
    void alreadyImportedMessageIsNotMarkedSeen() throws Exception {
        MimeMessage mime = mock(MimeMessage.class);
        when(emailImportService.importMessage(mime)).thenReturn(Optional.empty());

        job.processMessages(new Message[]{mime});

        verify(mime, never()).setFlag(any(), anyBoolean());
    }

    @Test
    void exceptionOnOneMessageDoesNotAbortOthers() throws Exception {
        MimeMessage first = mock(MimeMessage.class);
        MimeMessage second = mock(MimeMessage.class);
        when(emailImportService.importMessage(first)).thenThrow(new RuntimeException("parse error"));
        when(emailImportService.importMessage(second)).thenReturn(Optional.of(mock(com.emailmessenger.domain.Message.class)));

        job.processMessages(new Message[]{first, second});

        verify(second).setFlag(Flags.Flag.SEEN, true);
    }

    @Test
    void nonMimeMessageIsSkipped() throws Exception {
        Message notMime = mock(Message.class);

        job.processMessages(new Message[]{notMime});

        verifyNoInteractions(emailImportService);
        verify(notMime, never()).setFlag(any(), anyBoolean());
    }

    @Test
    void emptyMessageArrayIsHandledGracefully() {
        job.processMessages(new Message[0]);
        verifyNoInteractions(emailImportService);
    }

    @Test
    void multipleMessagesAllImported() throws Exception {
        MimeMessage m1 = mock(MimeMessage.class);
        MimeMessage m2 = mock(MimeMessage.class);
        MimeMessage m3 = mock(MimeMessage.class);
        com.emailmessenger.domain.Message saved = mock(com.emailmessenger.domain.Message.class);
        when(emailImportService.importMessage(any())).thenReturn(Optional.of(saved));

        job.processMessages(new Message[]{m1, m2, m3});

        verify(m1).setFlag(Flags.Flag.SEEN, true);
        verify(m2).setFlag(Flags.Flag.SEEN, true);
        verify(m3).setFlag(Flags.Flag.SEEN, true);
    }
}
