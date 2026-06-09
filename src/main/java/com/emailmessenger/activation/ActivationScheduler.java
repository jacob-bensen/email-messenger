package com.emailmessenger.activation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron trigger for the activation sweep. Gated by
 * {@code activation.enabled=true} so dev/test environments don't email
 * anyone by accident; {@link ActivationService} itself stays in the
 * application context either way for manual or programmatic invocation.
 */
@Component
@ConditionalOnProperty(name = "activation.enabled", havingValue = "true")
class ActivationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ActivationScheduler.class);

    private final ActivationService service;

    ActivationScheduler(ActivationService service) {
        this.service = service;
    }

    @Scheduled(cron = "${activation.cron:0 30 13 * * ?}", zone = "${activation.zone:UTC}")
    void runActivation() {
        int sent = service.runActivationCycle();
        log.info("Activation cycle: sent {} nudge email(s)", sent);
    }
}
