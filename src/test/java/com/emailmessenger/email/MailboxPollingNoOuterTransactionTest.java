package com.emailmessenger.email;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Deliberately NOT {@code @Transactional}: the scheduler and the /chats/refresh
 * heartbeat call the polling loops with no surrounding transaction, and a
 * test-managed one would hide a poll that fails to open its own.
 */
@SpringBootTest
class MailboxPollingNoOuterTransactionTest {

    @Autowired MailboxPollingService polling;
    @Autowired MailAccountRepository accountRepository;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired CredentialEncryptor encryptor;

    @MockitoBean ImapClient imapClient;

    private User owner;
    private MailAccount account;

    @BeforeEach
    void setUp() {
        owner = userService.register("no-tx-poll-owner@example.com", "password1", "Owner");
        account = accountRepository.save(new MailAccount(
                owner, "imap.example.com", 993, true, "no-tx-poll-owner@example.com",
                encryptor.encrypt("pw")));
        when(imapClient.fetchSinceUid(any(), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), 5L));
        when(imapClient.fetchSentSinceUid(any(), any()))
                .thenReturn(new ImapClient.IncrementalFetch(List.of(), null));
        when(imapClient.fetchRecentSent(any(), anyInt())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        accountRepository.deleteById(account.getId());
        userRepository.deleteById(owner.getId());
    }

    @Test
    void pollAllPersistsCursorAndNextPollWithoutCallerTransaction() {
        polling.pollAll();

        MailAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getLastSeenUid()).isEqualTo(5L);
        assertThat(reloaded.getLastSyncedAt()).isNotNull();
        assertThat(reloaded.getNextPollAt()).isNotNull();
    }

    @Test
    void activeRefreshPersistsCursorWithoutCallerTransaction() {
        polling.refreshActiveUserNow(owner.getId());

        MailAccount reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getLastSeenUid()).isEqualTo(5L);
        assertThat(reloaded.getLastSyncedAt()).isNotNull();
    }
}
