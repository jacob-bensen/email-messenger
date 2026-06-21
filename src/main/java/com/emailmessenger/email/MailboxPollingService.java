package com.emailmessenger.email;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.MailAccount;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.repository.MailAccountRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
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

    /** Recent Sent-folder window backfilled on a mailbox's first sent sync. */
    static final int SENT_BACKFILL_LIMIT = 30;

    private final MailAccountRepository accounts;
    private final ImapClient imapClient;
    private final ImapCredentialsResolver credentialsResolver;
    private final EmailImportService importService;
    private final PlanLimitService planLimitService;
    private final PollingPolicy pollingPolicy;
    private final Clock clock;

    MailboxPollingService(MailAccountRepository accounts,
                          ImapClient imapClient,
                          ImapCredentialsResolver credentialsResolver,
                          EmailImportService importService,
                          PlanLimitService planLimitService,
                          PollingPolicy pollingPolicy,
                          Clock clock) {
        this.accounts = accounts;
        this.imapClient = imapClient;
        this.credentialsResolver = credentialsResolver;
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

    /**
     * Fire-and-forget refresh of one user's due mailboxes, triggered when they
     * open their inbox so sign-in feels fresh without blocking the page render.
     * Only accounts whose next-poll time has passed are touched, so repeated
     * page loads don't re-poll an account the scheduler just serviced.
     */
    @Async
    public void refreshDueForUserAsync(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (MailAccount account : accounts.findDueForPollingByUserId(userId, now)) {
            try {
                pollOne(account.getId());
            } catch (Exception e) {
                log.warn("On-open refresh failed for mailbox id={} ({}): {}",
                        account.getId(), account.getUsername(), e.getMessage());
            }
        }
    }

    /** Foreground "page is open" cadence — much tighter than the background tier. */
    static final java.time.Duration ACTIVE_REFRESH_INTERVAL = java.time.Duration.ofMinutes(1);

    /**
     * Synchronous poll driven by the open chats page's ~1-minute heartbeat, so
     * connected mailboxes refresh roughly every minute while the app is in
     * active use. Skips accounts synced within {@link #ACTIVE_REFRESH_INTERVAL}
     * (so multiple tabs / rapid pings don't hammer IMAP) and circuit-broken
     * ones. Returns the total messages imported so the caller can decide
     * whether the on-screen list needs refreshing.
     */
    public int refreshActiveUserNow(Long userId) {
        // lastSyncedAt is stamped by MailAccount.markSynced() with
        // LocalDateTime.now() (system zone), so the cutoff must use the same
        // basis — the injected Clock is systemUTC(), and using it here would
        // skew the 1-minute floor by the host's zone offset (e.g. 5h locally),
        // defeating it. Compare like-with-like.
        LocalDateTime cutoff = LocalDateTime.now().minus(ACTIVE_REFRESH_INTERVAL);
        int imported = 0;
        for (MailAccount account : accounts.findActiveRefreshable(userId, cutoff)) {
            try {
                imported += pollOne(account.getId());
            } catch (Exception e) {
                log.warn("Active refresh failed for mailbox id={} ({}): {}",
                        account.getId(), account.getUsername(), e.getMessage());
            }
        }
        return imported;
    }

    @Transactional
    public int pollOne(Long accountId) {
        MailAccount account = accounts.findById(accountId).orElse(null);
        if (account == null) return 0;

        int imported = 0;
        int sentImported = 0;
        try {
            // Resolving creds can fail (undecryptable secret, expired OAuth
            // refresh) — that throws ImapConnectionException, handled below on
            // the same path as a login failure.
            ImapCredentials credentials = credentialsResolver.resolve(account);
            ImapClient.IncrementalFetch fetch = imapClient.fetchSinceUid(
                    credentials, account.getLastSeenUid());

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

            // Pull sent mail too, imported as outbound, so both sides of each
            // conversation stay in sync. The first time (cursor null) we backfill
            // a recent window — this surfaces sent history even for mailboxes
            // connected before sent sync existed — then track only newer sent mail.
            if (account.getLastSeenSentUid() == null) {
                for (MimeMessage mime : imapClient.fetchRecentSent(credentials, SENT_BACKFILL_LIMIT)) {
                    sentImported += importSent(mime, account);
                }
                account.setLastSeenSentUid(
                        imapClient.fetchSentSinceUid(credentials, null).newLastUid());
            } else {
                ImapClient.IncrementalFetch sentFetch = imapClient.fetchSentSinceUid(
                        credentials, account.getLastSeenSentUid());
                for (MimeMessage mime : sentFetch.messages()) {
                    sentImported += importSent(mime, account);
                }
                if (sentFetch.newLastUid() != null) {
                    account.setLastSeenSentUid(sentFetch.newLastUid());
                }
            }

            account.markSynced();
            if (imported > 0 || sentImported > 0) {
                log.info("Poll imported {} received + {} sent message(s) for {}@{}",
                        imported, sentImported, account.getUsername(), account.getHost());
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
        return imported + sentImported;
    }

    private int importSent(MimeMessage mime, MailAccount account) {
        try {
            return importService.importMessage(mime, account.getUser(), true).isPresent() ? 1 : 0;
        } catch (EmailImportException e) {
            log.warn("Skipping unparseable sent message for {}@{}: {}",
                    account.getUsername(), account.getHost(), e.getMessage());
            return 0;
        }
    }

    private void scheduleNextPoll(MailAccount account) {
        Plan plan = planLimitService.currentPlan(account.getUser());
        account.setNextPollAt(pollingPolicy.nextPollAt(plan, LocalDateTime.now(clock)));
    }
}
