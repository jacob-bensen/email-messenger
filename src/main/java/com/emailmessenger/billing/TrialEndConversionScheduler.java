package com.emailmessenger.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron trigger for the trial-end conversion sweep. Gated by
 * {@code trial-end.enabled=true} so dev/test environments don't email
 * anyone by accident; {@link TrialEndConversionService} itself stays in
 * the application context either way for manual or programmatic
 * invocation.
 */
@Component
@ConditionalOnProperty(name = "trial-end.enabled", havingValue = "true")
class TrialEndConversionScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrialEndConversionScheduler.class);

    private final TrialEndConversionService service;

    TrialEndConversionScheduler(TrialEndConversionService service) {
        this.service = service;
    }

    @Scheduled(cron = "${trial-end.cron:0 30 14 * * ?}", zone = "${trial-end.zone:UTC}")
    void runTrialEndConversion() {
        int sent = service.runTrialEndCycle();
        log.info("Trial-end conversion cycle: sent {} email(s)", sent);
    }
}
