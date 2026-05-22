package com.emailmessenger.email;

import com.emailmessenger.billing.PlanLimitService;
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
        // Throws ImapConnectionException on bad creds / unreachable host —
        // controller catches it and re-renders the form with the message.
        imapClient.verifyConnection(host, port, ssl, username, rawPassword);

        MailAccount account = new MailAccount(user, host, port, ssl, username,
                encryptor.encrypt(rawPassword));
        MailAccount saved = repository.save(account);

        runInitialSync(saved, rawPassword);
        return repository.save(saved);
    }

    private void runInitialSync(MailAccount account, String rawPassword) {
        try {
            List<MimeMessage> messages = imapClient.fetchRecentInbox(
                    account.getHost(), account.getPort(), account.isSsl(),
                    account.getUsername(), rawPassword, INITIAL_SYNC_LIMIT);
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
