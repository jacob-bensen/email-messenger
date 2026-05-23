package com.emailmessenger.email;

import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.repository.MailAccountRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Periodic IMAP poll of every connected mailbox. One {@link ImapClient}
 * call per account fetches messages whose UID is strictly greater than
 * the persisted {@code lastSeenUid} cursor; each new message is fed to
 * {@link EmailImportService} and the cursor is advanced. Per-account
 * failures are recorded on the row and the loop continues so a single
 * bad mailbox cannot block the rest.
 *
 * <p>Behind {@code mailbox.polling.enabled=true} so production can roll
 * the feature on without redeploying app code, and so tests don't have
 * a scheduler racing them by default.
 */
@Component
@ConditionalOnProperty(name = "mailbox.polling.enabled", havingValue = "true")
public class MailboxPollingService {

    private static final Logger log = LoggerFactory.getLogger(MailboxPollingService.class);

    private final MailAccountRepository accounts;
    private final ImapClient imapClient;
    private final CredentialEncryptor encryptor;
    private final EmailImportService importService;

    MailboxPollingService(MailAccountRepository accounts,
                          ImapClient imapClient,
                          CredentialEncryptor encryptor,
                          EmailImportService importService) {
        this.accounts = accounts;
        this.imapClient = imapClient;
        this.encryptor = encryptor;
        this.importService = importService;
    }

    @Scheduled(fixedDelayString = "${mailbox.polling.interval-ms:300000}",
               initialDelayString = "${mailbox.polling.initial-delay-ms:30000}")
    public void pollAll() {
        List<MailAccount> all = accounts.findAll();
        log.debug("Polling {} mailbox(es)", all.size());
        for (MailAccount account : all) {
            try {
                pollOne(account.getId());
            } catch (Exception e) {
                log.warn("Polling failed for mailbox id={} ({}): {}",
                        account.getId(), account.getUsername(), e.getMessage());
            }
        }
    }

    @Transactional
    public void pollOne(Long accountId) {
        MailAccount account = accounts.findById(accountId).orElse(null);
        if (account == null) return;

        String password;
        try {
            password = encryptor.decrypt(account.getPasswordCiphertext());
        } catch (Exception e) {
            log.warn("Could not decrypt stored credentials for mailbox id={}: {}",
                    account.getId(), e.getMessage());
            account.markSyncError("Could not decrypt stored credentials");
            accounts.save(account);
            return;
        }

        try {
            ImapClient.IncrementalFetch fetch = imapClient.fetchSinceUid(
                    account.getHost(), account.getPort(), account.isSsl(),
                    account.getUsername(), password, account.getLastSeenUid());

            int imported = 0;
            for (MimeMessage mime : fetch.messages()) {
                try {
                    if (importService.importMessage(mime, account.getUser()).isPresent()) {
                        imported++;
                    }
                } catch (EmailImportException e) {
                    log.warn("Skipping unparseable polled message for {}@{}: {}",
                            account.getUsername(), account.getHost(), e.getMessage());
                }
            }
            if (fetch.newLastUid() != null) {
                account.setLastSeenUid(fetch.newLastUid());
            }
            account.markSynced();
            if (imported > 0) {
                log.info("Poll imported {} new message(s) for {}@{} (cursor now uid={})",
                        imported, account.getUsername(), account.getHost(), account.getLastSeenUid());
            }
        } catch (ImapConnectionException e) {
            log.warn("Poll IMAP error for {}@{}: {}", account.getUsername(),
                    account.getHost(), e.getMessage());
            account.markSyncError(e.getMessage());
        }
        accounts.save(account);
    }
}
