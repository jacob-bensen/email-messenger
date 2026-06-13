package com.emailmessenger.digest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron trigger for the weekly digest. Gated by {@code digest.enabled=true}
 * so dev/test environments don't email anyone by accident; the
 * {@link WeeklyDigestService} itself stays in the application context
 * either way for manual or programmatic invocation.
 */
@Component
@ConditionalOnProperty(name = "digest.enabled", havingValue = "true")
class WeeklyDigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyDigestScheduler.class);

    private final WeeklyDigestService digestService;

    WeeklyDigestScheduler(WeeklyDigestService digestService) {
        this.digestService = digestService;
    }

    @Scheduled(cron = "${digest.cron:0 0 14 ? * MON}", zone = "${digest.zone:UTC}")
    void runWeeklyDigest() {
        int sent = digestService.runDigestCycle();
        log.info("Weekly digest cycle: sent {} email(s)", sent);
    }
}
