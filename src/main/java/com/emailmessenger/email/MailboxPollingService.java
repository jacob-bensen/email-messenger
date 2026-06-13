package com.emailmessenger.email;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.repository.MailAccountRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * IMAP poll of a connected mailbox: one {@link ImapClient} call fetches
 * messages whose UID is strictly greater than the persisted
 * {@code lastSeenUid} cursor; each new message is fed to
 * {@link EmailImportService} and the cursor is advanced. Per-account
 * failures are recorded on the row and the loop continues so a single
 * bad mailbox cannot block the rest.
 *
 * <p>Cadence is plan-tiered (see {@link PollingPolicy}): Free polls every
 * 15 minutes, paid every 5, both with +/-30s jitter. After
 * {@link PollingPolicy#suspendAtFailures()} consecutive failures the
 * account is circuit-broken — {@link #pollAll()} skips it until a
 * successful manual "Sync now" clears the breaker.
 *
 * <p>Always present so the manual "Sync now" controller path can invoke
 * {@link #pollOne(Long)} regardless of whether the scheduler feature
 * flag is on; the recurring schedule itself lives in
 * {@link MailboxPollingScheduler}, which is gated by
 * {@code mailbox.polling.enabled=true}.
 */
@Component
public class MailboxPollingService {

    private static final Logger log = LoggerFactory.getLogger(MailboxPollingService.class);

    private final MailAccountRepository accounts;
    private final ImapClient imapClient;
    private final CredentialEncryptor encryptor;
    private final EmailImportService importService;
    private final PlanLimitService planLimitService;
    private final PollingPolicy pollingPolicy;
    private final Clock clock;

    MailboxPollingService(MailAccountRepository accounts,
                          ImapClient imapClient,
                          CredentialEncryptor encryptor,
                          EmailImportService importService,
                          PlanLimitService planLimitService,
                          PollingPolicy pollingPolicy,
                          Clock clock) {
        this.accounts = accounts;
        this.imapClient = imapClient;
        this.encryptor = encryptor;
        this.importService = importService;
        this.planLimitService = planLimitService;
        this.pollingPolicy = pollingPolicy;
        this.clock = clock;
    }

    public void pollAll() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<MailAccount> due = accounts.findDueForPolling(now);
        log.debug("Polling {} due mailbox(es)", due.size());
        for (MailAccount account : due) {
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
            account.recordPollFailure(pollingPolicy.suspendAtFailures());
            scheduleNextPoll(account);
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
            account.recordPollFailure(pollingPolicy.suspendAtFailures());
            if (account.isPollingSuspended()) {
                log.warn("Mailbox id={} ({}@{}) suspended after {} consecutive failures",
                        account.getId(), account.getUsername(), account.getHost(),
                        account.getConsecutiveFailureCount());
            }
        }
        scheduleNextPoll(account);
        accounts.save(account);
    }

    private void scheduleNextPoll(MailAccount account) {
        Plan plan = planLimitService.currentPlan(account.getUser());
        account.setNextPollAt(pollingPolicy.nextPollAt(plan, LocalDateTime.now(clock)));
    }
}
