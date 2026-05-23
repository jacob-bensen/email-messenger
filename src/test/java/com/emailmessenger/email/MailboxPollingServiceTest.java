package com.emailmessenger.email;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "mailbox.polling.enabled=true",
        // Push the auto schedule far enough out that the manual pollOne calls
        // we make here are not racing the @Scheduled trigger.
        "mailbox.polling.initial-delay-ms=600000",
        "mailbox.polling.interval-ms=600000"
})
@Transactional
class MailboxPollingServiceTest {

    @Autowired MailboxPollingService polling;
    @Autowired MailAccountRepository accountRepository;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired CredentialEncryptor encryptor;

    @MockBean ImapClient imapClient;
    @MockBean EmailImportService importService;

    private MailAccount account;

    @BeforeEach
    void setUp() {
        // Belt-and-braces: ensure no stubbing/invocation state leaks across the
        // shared @SpringBootTest application context.
        Mockito.reset(imapClient, importService);
        userService.register("polling-owner@example.com", "password1", "Polling Owner");
        User owner = userRepository.findByEmail("polling-owner@example.com").orElseThrow();
        account = accountRepository.save(new MailAccount(
                owner, "imap.example.com", 993, true,
                "polling-owner@example.com", encryptor.encrypt("app-pw")));
    }

    @Test
    void firstPollEstablishesBaselineAndImportsNothing() {
        when(imapClient.fetchSinceUid(anyString(), anyInt(), anyBoolean(),
                anyString(), anyString(), eq(null)))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), 1042L));

        polling.pollOne(account.getId());

        MailAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getLastSeenUid()).isEqualTo(1042L);
        assertThat(reloaded.getLastSyncedAt()).isNotNull();
        assertThat(reloaded.getLastSyncError()).isNull();
        verify(importService, never()).importMessage(any(), any());
    }

    @Test
    void incrementalPollImportsNewMessagesAndAdvancesCursor() throws Exception {
        account.setLastSeenUid(100L);
        accountRepository.save(account);

        MimeMessage m1 = plainMessage("<poll-1@test>", "Hello", "a@example.com", "Body 1");
        MimeMessage m2 = plainMessage("<poll-2@test>", "Hi there", "b@example.com", "Body 2");
        when(imapClient.fetchSinceUid(eq("imap.example.com"), eq(993), eq(true),
                eq("polling-owner@example.com"), eq("app-pw"), eq(100L)))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(m1, m2), 102L));
        when(importService.importMessage(any(MimeMessage.class), any(User.class)))
                .thenReturn(Optional.empty());

        polling.pollOne(account.getId());

        verify(importService, times(2)).importMessage(any(MimeMessage.class), any(User.class));
        MailAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getLastSeenUid()).isEqualTo(102L);
        assertThat(reloaded.getLastSyncedAt()).isNotNull();
        assertThat(reloaded.getLastSyncError()).isNull();
    }

    @Test
    void connectionFailureIsRecordedOnAccountWithoutAdvancingCursor() {
        account.setLastSeenUid(50L);
        accountRepository.save(account);

        when(imapClient.fetchSinceUid(anyString(), anyInt(), anyBoolean(),
                anyString(), anyString(), any()))
                .thenThrow(new ImapConnectionException("Login failed", null));

        polling.pollOne(account.getId());

        MailAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getLastSeenUid()).isEqualTo(50L);
        assertThat(reloaded.getLastSyncError()).isEqualTo("Login failed");
        verify(importService, never()).importMessage(any(), any());
    }

    @Test
    void pollAllContinuesPastAFailingMailbox() {
        MailAccount second = accountRepository.save(new MailAccount(
                account.getUser(), "imap.other.com", 993, true,
                "polling-owner@example.com", encryptor.encrypt("app-pw")));

        when(imapClient.fetchSinceUid(eq("imap.example.com"), anyInt(), anyBoolean(),
                anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("boom"));
        when(imapClient.fetchSinceUid(eq("imap.other.com"), anyInt(), anyBoolean(),
                anyString(), anyString(), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), 7L));

        polling.pollAll();

        MailAccount reloadedSecond = accountRepository.findById(second.getId()).orElseThrow();
        assertThat(reloadedSecond.getLastSeenUid()).isEqualTo(7L);
        assertThat(reloadedSecond.getLastSyncedAt()).isNotNull();
    }

    private MimeMessage plainMessage(String messageId, String subject, String from, String body)
            throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session);
        msg.setHeader("Message-ID", messageId);
        msg.setSubject(subject);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(jakarta.mail.Message.RecipientType.TO,
                InternetAddress.parse("polling-owner@example.com"));
        msg.setText(body);
        msg.setSentDate(new Date());
        return msg;
    }
}
