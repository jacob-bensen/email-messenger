package com.emailmessenger.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron trigger for the operator weekly digest. Gated by
 * {@code admin.weekly-digest.enabled=true} so dev/test environments don't
 * email anyone by accident; {@link AdminWeeklyDigestService} stays in the
 * application context either way for direct invocation.
 */
@Component
@ConditionalOnProperty(name = "admin.weekly-digest.enabled", havingValue = "true")
class AdminWeeklyDigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdminWeeklyDigestScheduler.class);

    private final AdminWeeklyDigestService digestService;

    AdminWeeklyDigestScheduler(AdminWeeklyDigestService digestService) {
        this.digestService = digestService;
    }

    @Scheduled(cron = "${admin.weekly-digest.cron:0 0 9 ? * MON}",
               zone = "${admin.weekly-digest.zone:UTC}")
    void runOperatorDigest() {
        int sent = digestService.sendDigest();
        log.info("Operator weekly digest: sent {} email(s)", sent);
    }
}
