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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        // Default: no Sent folder activity. Individual tests exercising the
        // INBOX path don't care about the sent leg; lenient so it's optional.
        Mockito.lenient().when(imapClient.fetchSentSinceUid(any(ImapCredentials.class), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), null));
    }

    @Test
    void firstPollEstablishesBaselineAndImportsNothing() {
        when(imapClient.fetchSinceUid(any(ImapCredentials.class), eq(null)))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), 1042L));

        polling.pollOne(account.getId());

        MailAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getLastSeenUid()).isEqualTo(1042L);
        assertThat(reloaded.getLastSyncedAt()).isNotNull();
        assertThat(reloaded.getLastSyncError()).isNull();
        verify(importService, never()).importMessage(any(), any());
    }

    @Test
    void firstSentSyncBackfillsRecentSentAsOutbound() throws Exception {
        when(imapClient.fetchSinceUid(any(ImapCredentials.class), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), 10L));
        MimeMessage sent = plainMessage("<sent-1@test>", "Re: Hello",
                "polling-owner@example.com", "My reply");
        when(imapClient.fetchRecentSent(any(ImapCredentials.class),
                eq(MailboxPollingService.SENT_BACKFILL_LIMIT)))
                .thenReturn(List.of(sent));
        when(imapClient.fetchSentSinceUid(any(ImapCredentials.class), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), 500L));

        polling.pollOne(account.getId());

        // The sent message is imported as outbound, and the Sent cursor is baselined.
        verify(importService).importMessage(eq(sent), any(User.class), eq(true));
        assertThat(accountRepository.findById(account.getId()).orElseThrow()
                .getLastSeenSentUid()).isEqualTo(500L);
    }

    @Test
    void incrementalPollImportsNewMessagesAndAdvancesCursor() throws Exception {
        account.setLastSeenUid(100L);
        accountRepository.save(account);

        MimeMessage m1 = plainMessage("<poll-1@test>", "Hello", "a@example.com", "Body 1");
        MimeMessage m2 = plainMessage("<poll-2@test>", "Hi there", "b@example.com", "Body 2");
        ImapCredentials creds = ImapCredentials.password("imap.example.com", 993, true,
                "polling-owner@example.com", "app-pw");
        when(imapClient.fetchSinceUid(eq(creds), eq(100L)))
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

        when(imapClient.fetchSinceUid(any(ImapCredentials.class), any()))
                .thenThrow(new ImapConnectionException("Login failed", null));

        polling.pollOne(account.getId());

        MailAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getLastSeenUid()).isEqualTo(50L);
        assertThat(reloaded.getLastSyncError()).isEqualTo("Login failed");
        assertThat(reloaded.getConsecutiveFailureCount()).isEqualTo(1);
        assertThat(reloaded.isPollingSuspended()).isFalse();
        verify(importService, never()).importMessage(any(), any());
    }

    @Test
    void successResetsFailureCounterAndUnsuspends() {
        account.setLastSeenUid(50L);
        accountRepository.save(account);
        // Drive the breaker open via repeated failures, then a success closes it.
        when(imapClient.fetchSinceUid(any(ImapCredentials.class), any()))
                .thenThrow(new ImapConnectionException("Login failed", null));
        for (int i = 0; i < PollingPolicy.SUSPEND_AT_FAILURES; i++) {
            polling.pollOne(account.getId());
        }
        MailAccount tripped = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(tripped.isPollingSuspended()).isTrue();
        assertThat(tripped.getConsecutiveFailureCount())
                .isEqualTo(PollingPolicy.SUSPEND_AT_FAILURES);

        Mockito.reset(imapClient, importService);
        when(imapClient.fetchSinceUid(any(ImapCredentials.class), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), 60L));
        when(imapClient.fetchSentSinceUid(any(ImapCredentials.class), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), null));

        polling.pollOne(account.getId());

        MailAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.isPollingSuspended()).isFalse();
        assertThat(reloaded.getConsecutiveFailureCount()).isZero();
        assertThat(reloaded.getLastSyncError()).isNull();
    }

    @Test
    void suspendedMailboxIsSkippedByPollAll() {
        when(imapClient.fetchSinceUid(any(ImapCredentials.class), any()))
                .thenThrow(new ImapConnectionException("Login failed", null));
        // Trip the circuit breaker.
        for (int i = 0; i < PollingPolicy.SUSPEND_AT_FAILURES; i++) {
            polling.pollOne(account.getId());
        }
        assertThat(accountRepository.findById(account.getId()).orElseThrow().isPollingSuspended())
                .isTrue();

        Mockito.reset(imapClient, importService);

        polling.pollAll();

        // findDueForPolling must filter out the suspended row so the IMAP
        // client is never touched on a tick.
        verifyNoInteractions(imapClient);
    }

    @Test
    void notYetDueMailboxIsSkippedByPollAll() {
        // Stamp next_poll_at well into the future; both Clock-zone variants
        // (UTC vs default) treat +1d as "not due now".
        account.setNextPollAt(java.time.LocalDateTime.now().plusDays(1));
        accountRepository.save(account);

        polling.pollAll();

        verifyNoInteractions(imapClient);
    }

    @Test
    void successfulPollSchedulesNextPollAt() {
        // Fresh account has next_poll_at = null → due immediately.
        when(imapClient.fetchSinceUid(any(ImapCredentials.class), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), 1L));

        polling.pollOne(account.getId());

        MailAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getNextPollAt()).isNotNull();
        // Free tier (no subscription row) → next poll between 14:30 and 15:30 from now.
        java.time.LocalDateTime earliest = java.time.LocalDateTime.now()
                .plusMinutes(14).plusSeconds(20);
        java.time.LocalDateTime latest = java.time.LocalDateTime.now()
                .plusMinutes(15).plusSeconds(40);
        assertThat(reloaded.getNextPollAt()).isAfter(earliest).isBefore(latest);
    }

    @Test
    void pollAllContinuesPastAFailingMailbox() {
        MailAccount second = accountRepository.save(new MailAccount(
                account.getUser(), "imap.other.com", 993, true,
                "polling-owner@example.com", encryptor.encrypt("app-pw")));

        ImapCredentials firstCreds = ImapCredentials.password("imap.example.com", 993, true,
                "polling-owner@example.com", "app-pw");
        ImapCredentials secondCreds = ImapCredentials.password("imap.other.com", 993, true,
                "polling-owner@example.com", "app-pw");
        when(imapClient.fetchSinceUid(eq(firstCreds), any()))
                .thenThrow(new RuntimeException("boom"));
        when(imapClient.fetchSinceUid(eq(secondCreds), any()))
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
