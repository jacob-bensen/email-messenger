package com.emailmessenger.email;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.repository.MailAccountRepository;
import com.emailmessenger.repository.MessageRepository;
import com.emailmessenger.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class MailAccountServiceTest {

    @Autowired MailAccountService mailAccountService;
    @Autowired MailAccountRepository mailAccountRepository;
    @Autowired EmailThreadRepository threadRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired CredentialEncryptor encryptor;

    @MockBean ImapClient imapClient;

    private User owner;

    @BeforeEach
    void setUp() {
        userService.register("mailbox-owner@example.com", "password1", "Owner");
        owner = userRepository.findByEmail("mailbox-owner@example.com").orElseThrow();
    }

    @Test
    void deleteRemovesOwnedMailbox() {
        when(imapClient.fetchRecentInbox(any(ImapCredentials.class), anyInt())).thenReturn(List.of());
        MailAccount saved = mailAccountService.connect(owner, "imap.example.com", 993, true,
                "first@example.com", "pw1");

        boolean removed = mailAccountService.delete(owner, saved.getId());

        assertThat(removed).isTrue();
        assertThat(mailAccountRepository.findById(saved.getId())).isEmpty();
        assertThat(mailAccountRepository.countByUser(owner)).isZero();
    }

    @Test
    void deleteForeignOrUnknownMailboxReturnsFalse() {
        assertThat(mailAccountService.delete(owner, 999_999L)).isFalse();
    }

    @Test
    void connectPersistsAccountAndImportsRecentMessages() throws Exception {
        MimeMessage m1 = plainMessage("<m1@test>", "Welcome", "alice@example.com", "First message.");
        MimeMessage m2 = plainMessage("<m2@test>", "Project kickoff", "bob@example.com", "Let's start.");
        ImapCredentials creds = ImapCredentials.password("imap.example.com", 993, true,
                "user@example.com", "app-pw");
        when(imapClient.fetchRecentInbox(eq(creds), eq(MailAccountService.INITIAL_SYNC_LIMIT)))
                .thenReturn(List.of(m1, m2));

        MailAccount saved = mailAccountService.connect(owner, "imap.example.com", 993, true,
                "user@example.com", "app-pw");

        verify(imapClient).verifyConnection(creds);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLastSyncedAt()).isNotNull();
        assertThat(saved.getLastSyncError()).isNull();
        assertThat(threadRepository.findByOwnerOrderByUpdatedAtDesc(owner,
                org.springframework.data.domain.PageRequest.of(0, 20)).getTotalElements()).isEqualTo(2L);
        assertThat(messageRepository.findByMessageIdHeaderAndOwner("<m1@test>", owner)).isPresent();
        assertThat(messageRepository.findByMessageIdHeaderAndOwner("<m2@test>", owner)).isPresent();
    }

    @Test
    void connectStoresEncryptedPasswordNotPlaintext() {
        when(imapClient.fetchRecentInbox(any(ImapCredentials.class), anyInt())).thenReturn(List.of());

        MailAccount saved = mailAccountService.connect(owner, "imap.example.com", 993, true,
                "user@example.com", "secret-app-password");

        assertThat(saved.getPasswordCiphertext()).isNotEqualTo("secret-app-password");
        assertThat(saved.getPasswordCiphertext()).doesNotContain("secret-app-password");
        assertThat(encryptor.decrypt(saved.getPasswordCiphertext())).isEqualTo("secret-app-password");
    }

    @Test
    void invalidCredentialsThrowAndPersistNothing() {
        doThrow(new ImapConnectionException("AUTHENTICATE failed", null))
                .when(imapClient).verifyConnection(any(ImapCredentials.class));

        ImapConnectionException ex = catchThrowableOfType(
                () -> mailAccountService.connect(owner, "imap.example.com", 993, true,
                        "user@example.com", "wrong"),
                ImapConnectionException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getMessage()).contains("AUTHENTICATE failed");
        assertThat(mailAccountRepository.findByUserOrderByCreatedAtAsc(owner)).isEmpty();
        verify(imapClient, never()).fetchRecentInbox(any(ImapCredentials.class), anyInt());
    }

    @Test
    void secondMailboxConnectsSincePaidFeaturesAreUnlocked() {
        // Mailbox caps are lifted for everyone, so a second mailbox connects fine.
        when(imapClient.fetchRecentInbox(any(ImapCredentials.class), anyInt())).thenReturn(List.of());
        mailAccountService.connect(owner, "imap.example.com", 993, true,
                "first@example.com", "pw1");
        mailAccountService.connect(owner, "imap.other.com", 993, true,
                "second@example.com", "pw2");

        assertThat(mailAccountRepository.countByUser(owner)).isEqualTo(2L);
    }

    @Test
    void fetchFailureAfterSuccessfulVerifyStillPersistsAccountWithError() {
        when(imapClient.fetchRecentInbox(any(ImapCredentials.class), anyInt()))
                .thenThrow(new ImapConnectionException("INBOX unavailable", null));

        MailAccount saved = mailAccountService.connect(owner, "imap.example.com", 993, true,
                "user@example.com", "app-pw");

        // Credentials are good; the row exists so the user can manage it.
        // Sync error is recorded so the UI can surface it on /mailboxes.
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLastSyncedAt()).isNull();
        assertThat(saved.getLastSyncError()).isEqualTo("INBOX unavailable");
    }

    @Test
    void connectGmailOAuthPersistsXoauth2AccountWithEncryptedRefreshToken() throws Exception {
        MimeMessage m1 = plainMessage("<g1@test>", "Hi", "alice@example.com", "Hello there.");
        ImapCredentials creds = ImapCredentials.xoauth2("imap.gmail.com", 993, true,
                "mailbox-owner@example.com", "access-token-123");
        when(imapClient.fetchRecentInbox(eq(creds), eq(MailAccountService.INITIAL_SYNC_LIMIT)))
                .thenReturn(List.of(m1));

        MailAccount saved = mailAccountService.connectGmailOAuth(owner,
                "mailbox-owner@example.com", "refresh-token-xyz", "access-token-123");

        verify(imapClient).verifyConnection(creds);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAuthType()).isEqualTo(com.emailmessenger.domain.AuthType.XOAUTH2);
        assertThat(saved.getHost()).isEqualTo("imap.gmail.com");
        // The encrypted secret is the refresh token, not the access token.
        assertThat(encryptor.decrypt(saved.getPasswordCiphertext())).isEqualTo("refresh-token-xyz");
        assertThat(saved.getLastSyncError()).isNull();
        assertThat(messageRepository.findByMessageIdHeaderAndOwner("<g1@test>", owner)).isPresent();
    }

    private MimeMessage plainMessage(String messageId, String subject, String from, String body)
            throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session);
        msg.setHeader("Message-ID", messageId);
        msg.setSubject(subject);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(jakarta.mail.Message.RecipientType.TO,
                InternetAddress.parse("mailbox-owner@example.com"));
        msg.setText(body);
        msg.setSentDate(new Date());
        return msg;
    }
}
