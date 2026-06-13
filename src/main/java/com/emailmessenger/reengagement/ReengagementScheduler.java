package com.emailmessenger.reengagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron trigger for the re-engagement sweep. Gated by
 * {@code reengagement.enabled=true} so dev/test environments don't email
 * anyone by accident; {@link ReengagementService} itself stays in the
 * application context either way for manual or programmatic invocation.
 */
@Component
@ConditionalOnProperty(name = "reengagement.enabled", havingValue = "true")
class ReengagementScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReengagementScheduler.class);

    private final ReengagementService service;

    ReengagementScheduler(ReengagementService service) {
        this.service = service;
    }

    @Scheduled(cron = "${reengagement.cron:0 0 13 * * ?}", zone = "${reengagement.zone:UTC}")
    void runReengagement() {
        int sent = service.runReengagementCycle();
        log.info("Re-engagement cycle: sent {} email(s)", sent);
    }
}
