package com.emailmessenger.web.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

/**
 * Liveness probe for the mobile client. Public and distinct from
 * `/actuator/health`, which is reserved for the hosting platform.
 * Response contract lives in `docs/API.md`.
 */
@RestController
@RequestMapping("/api/v1")
class HealthApiController {

    private final Clock clock;

    HealthApiController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("up", Instant.now(clock).toString(), "v1");
    }

    record HealthResponse(String status, String serverTime, String apiVersion) {}
}
