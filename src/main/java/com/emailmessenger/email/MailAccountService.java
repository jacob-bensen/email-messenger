package com.emailmessenger.email;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.AuthType;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.MailAccountRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Connect-a-mailbox flow: validate the IMAP credentials by opening a
 * real connection, persist an encrypted record, then pull the most
 * recent INBOX messages and feed them to {@link EmailImportService}
 * so the user lands on /threads with content instead of an empty inbox.
 */
@Service
public class MailAccountService {

    private static final Logger log = LoggerFactory.getLogger(MailAccountService.class);

    /** Initial sync window. Capped low so the first connect is snappy. */
    static final int INITIAL_SYNC_LIMIT = 30;

    /** Gmail's IMAP endpoint — fixed for every OAuth-connected Google mailbox. */
    static final String GMAIL_IMAP_HOST = "imap.gmail.com";
    static final int GMAIL_IMAP_PORT = 993;

    private final MailAccountRepository repository;
    private final ImapClient imapClient;
    private final CredentialEncryptor encryptor;
    private final PlanLimitService planLimitService;
    private final EmailImportService importService;

    MailAccountService(MailAccountRepository repository,
                       ImapClient imapClient,
                       CredentialEncryptor encryptor,
                       PlanLimitService planLimitService,
                       EmailImportService importService) {
        this.repository = repository;
        this.imapClient = imapClient;
        this.encryptor = encryptor;
        this.planLimitService = planLimitService;
        this.importService = importService;
    }

    @Transactional(readOnly = true)
    public List<MailAccount> list(User user) {
        return repository.findByUserOrderByCreatedAtAsc(user);
    }

    /**
     * Disconnects a mailbox the user owns: deletes the account row, including
     * its encrypted credentials and sync cursor, so polling stops. Conversations
     * already imported stay in the inbox (they aren't tied to a single mailbox).
     * Returns {@code true} if a matching mailbox was found and removed.
     */
    @Transactional
    public boolean delete(User user, Long id) {
        return repository.findByIdAndUser(id, user)
                .map(account -> {
                    repository.delete(account);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Validates credentials, enforces the plan-level mailbox cap, persists
     * the account, then pulls the most recent INBOX messages and imports
     * each one. Returns the persisted {@link MailAccount}.
     *
     * @throws ImapConnectionException        if IMAP login fails
     * @throws com.emailmessenger.billing.PlanLimitExceededException
     *         if the user is already at their plan's mailbox cap
     */
    @Transactional
    public MailAccount connect(User user, String host, int port, boolean ssl,
                               String username, String rawPassword) {
        planLimitService.enforceCanCreateMailbox(user);
        ImapCredentials credentials = ImapCredentials.password(host, port, ssl, username, rawPassword);
        // Throws ImapConnectionException on bad creds / unreachable host —
        // controller catches it and re-renders the form with the message.
        imapClient.verifyConnection(credentials);

        MailAccount account = new MailAccount(user, host, port, ssl, username,
                encryptor.encrypt(rawPassword));
        MailAccount saved = repository.save(account);

        runInitialSync(saved, credentials);
        return repository.save(saved);
    }

    /**
     * Connects a Gmail mailbox via OAuth: the IMAP session authenticates with
     * XOAUTH2 using {@code accessToken}, and we persist the (longer-lived)
     * {@code refreshToken} encrypted so the poller can mint fresh access
     * tokens later. Host/port are fixed to Gmail's IMAP endpoint.
     */
    @Transactional
    public MailAccount connectGmailOAuth(User user, String email,
                                         String refreshToken, String accessToken) {
        planLimitService.enforceCanCreateMailbox(user);
        ImapCredentials credentials = ImapCredentials.xoauth2(
                GMAIL_IMAP_HOST, GMAIL_IMAP_PORT, true, email, accessToken);
        imapClient.verifyConnection(credentials);

        MailAccount account = new MailAccount(user, GMAIL_IMAP_HOST, GMAIL_IMAP_PORT, true,
                email, encryptor.encrypt(refreshToken), AuthType.XOAUTH2);
        MailAccount saved = repository.save(account);

        runInitialSync(saved, credentials);
        return repository.save(saved);
    }

    private void runInitialSync(MailAccount account, ImapCredentials credentials) {
        try {
            List<MimeMessage> messages = imapClient.fetchRecentInbox(
                    credentials, INITIAL_SYNC_LIMIT);
            int imported = 0;
            for (MimeMessage mime : messages) {
                try {
                    if (importService.importMessage(mime, account.getUser()).isPresent()) {
                        imported++;
                    }
                } catch (EmailImportException e) {
                    log.warn("Skipping unparseable message during initial sync for {}@{}: {}",
                            account.getUsername(), account.getHost(), e.getMessage());
                }
            }
            log.info("Initial sync imported {} of {} messages for {}@{}",
                    imported, messages.size(), account.getUsername(), account.getHost());
            // Sent mail (imported as outbound) is pulled by the poller: the first
            // poll backfills a recent Sent window, so this also surfaces sent
            // history for mailboxes connected before sent sync existed.
            account.markSynced();
        } catch (ImapConnectionException e) {
            // The verify step already succeeded, so the user's credentials are
            // good. A failure here is a transient fetch issue — record it on
            // the account row so the UI can show "last sync failed" instead of
            // failing the whole connect flow.
            log.warn("Initial sync failed for {}@{}: {}", account.getUsername(),
                    account.getHost(), e.getMessage());
            account.markSyncError(e.getMessage());
        }
    }
}
