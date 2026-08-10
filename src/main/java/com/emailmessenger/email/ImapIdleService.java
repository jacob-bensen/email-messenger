package com.emailmessenger.email;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds one IMAP IDLE connection per eligible mailbox (paid tier, not
 * circuit-broken) so new mail lands within seconds instead of the poll
 * cadence. When the server signals new messages the account is run through
 * the normal {@link MailboxPollingService#pollOne} path — same import, same
 * cursor bookkeeping, same {@link NewMailEvent} out the other side.
 *
 * <p>A one-minute reconcile loop starts workers for newly eligible accounts
 * and stops them for disconnected / downgraded / suspended ones. Workers
 * reconnect with fresh credentials after each quiet idle window (which also
 * re-exchanges OAuth access tokens) and back off on failures; hard polling
 * suspension still comes from {@code pollOne}'s circuit breaker.
 *
 * <p>Gated by {@code mailbox.idle.enabled} the same way the polling
 * scheduler is gated, so tests and dev setups aren't holding sockets open.
 */
@Component
@ConditionalOnProperty(name = "mailbox.idle.enabled", havingValue = "true")
class ImapIdleService {

    private static final Logger log = LoggerFactory.getLogger(ImapIdleService.class);

    private static final long BACKOFF_BASE_MS = 30_000L;
    private static final long BACKOFF_MAX_MS = 10 * 60_000L;

    private final ImapIdleSupport support;
    private final ImapIdleClient idleClient;
    private final MailboxPollingService pollingService;
    private final Map<Long, Worker> workers = new ConcurrentHashMap<>();

    ImapIdleService(ImapIdleSupport support,
                    ImapIdleClient idleClient,
                    MailboxPollingService pollingService) {
        this.support = support;
        this.idleClient = idleClient;
        this.pollingService = pollingService;
    }

    @Scheduled(fixedDelayString = "${mailbox.idle.reconcile-interval-ms:60000}",
               initialDelayString = "${mailbox.idle.initial-delay-ms:15000}")
    void reconcile() {
        Set<Long> eligible = support.eligibleAccountIds();
        workers.forEach((accountId, worker) -> {
            if (!eligible.contains(accountId)) {
                log.info("Stopping IMAP IDLE for mailbox id={}", accountId);
                worker.stop();
                workers.remove(accountId, worker);
            }
        });
        for (Long accountId : eligible) {
            workers.computeIfAbsent(accountId, id -> {
                log.info("Starting IMAP IDLE for mailbox id={}", id);
                Worker worker = new Worker(id);
                worker.start();
                return worker;
            });
        }
    }

    @PreDestroy
    void shutdown() {
        workers.forEach((id, worker) -> worker.stop());
        workers.clear();
    }

    int activeWorkerCount() {
        return workers.size();
    }

    private final class Worker implements Runnable {

        private final Long accountId;
        private final Thread thread;
        private volatile boolean running = true;
        private volatile ImapIdleClient.IdleConnection connection;

        private Worker(Long accountId) {
            this.accountId = accountId;
            this.thread = new Thread(this, "imap-idle-" + accountId);
            this.thread.setDaemon(true);
        }

        void start() {
            thread.start();
        }

        void stop() {
            running = false;
            // Closing the connection makes a blocked idle() return; the
            // interrupt covers a worker sleeping in backoff.
            ImapIdleClient.IdleConnection conn = connection;
            if (conn != null) {
                conn.close();
            }
            thread.interrupt();
        }

        @Override
        public void run() {
            int failures = 0;
            while (running) {
                try {
                    ImapCredentials credentials = support.credentialsFor(accountId);
                    try (ImapIdleClient.IdleConnection conn = idleClient.open(credentials)) {
                        connection = conn;
                        failures = 0;
                        // Catch anything that arrived while we weren't connected.
                        pollingService.pollOne(accountId);
                        while (running && conn.awaitNewMail()) {
                            pollingService.pollOne(accountId);
                        }
                        // Quiet window elapsed — fall through and reconnect
                        // with fresh credentials.
                    } finally {
                        connection = null;
                    }
                } catch (Exception e) {
                    if (!running) {
                        break;
                    }
                    failures++;
                    long backoff = Math.min(BACKOFF_MAX_MS, BACKOFF_BASE_MS * failures);
                    log.warn("IMAP IDLE failed for mailbox id={} (attempt {}): {} — retrying in {}s",
                            accountId, failures, e.getMessage(), backoff / 1000);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
}
