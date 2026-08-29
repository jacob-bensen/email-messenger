package com.emailmessenger.api;

import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

/**
 * First endpoint on the mobile-facing {@code /api} namespace: a cheap
 * connectivity check. The mobile client calls this on startup to prove
 * it can reach the server and to surface the deployed version when the
 * two disagree.
 *
 * <p>Kept anonymous so the mobile splash screen works without a session.
 */
@RestController
@RequestMapping("/api")
class ApiHealthController {

    private final Clock clock;
    private final ObjectProvider<BuildProperties> buildProperties;

    ApiHealthController(Clock clock, ObjectProvider<BuildProperties> buildProperties) {
        this.clock = clock;
        this.buildProperties = buildProperties;
    }

    @GetMapping("/health")
    HealthResponse health() {
        BuildProperties bp = buildProperties.getIfAvailable();
        String version = bp != null ? bp.getVersion() : "dev";
        return new HealthResponse("ok", version, Instant.now(clock).toString());
    }
}
