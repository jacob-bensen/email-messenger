package com.emailmessenger.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Thin {@code @Scheduled} trigger that delegates to
 * {@link MailboxPollingService#pollAll()}. Gated by
 * {@code mailbox.polling.enabled=true} so prod can flip the flag on
 * without redeploying and tests don't have a scheduler racing them by
 * default. The sync logic itself lives in {@link MailboxPollingService}
 * and is always available, so the manual "Sync now" button works
 * regardless of this flag.
 */
@Component
@ConditionalOnProperty(name = "mailbox.polling.enabled", havingValue = "true")
class MailboxPollingScheduler {

    private final MailboxPollingService pollingService;

    MailboxPollingScheduler(MailboxPollingService pollingService) {
        this.pollingService = pollingService;
    }

    @Scheduled(fixedDelayString = "${mailbox.polling.interval-ms:300000}",
               initialDelayString = "${mailbox.polling.initial-delay-ms:30000}")
    void runScheduledPoll() {
        pollingService.pollAll();
    }
}
