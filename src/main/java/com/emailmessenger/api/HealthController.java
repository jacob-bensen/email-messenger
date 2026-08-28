package com.emailmessenger.api;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public liveness + version probe for the mobile client.
 *
 * <p>Distinct from {@code /actuator/health}: the shape of this response is
 * ours to keep stable, while Actuator's JSON has changed between Spring Boot
 * versions and is intended for platform probes, not app clients.
 */
@RestController
@RequestMapping("/api/v1")
class HealthController {

    private final String version;
    private final Clock clock;

    HealthController(@Value("${app.version:0.0.0-unknown}") String version, Clock clock) {
        this.version = version;
        this.clock = clock;
    }

    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("ok", version, Instant.now(clock));
    }
}
