package com.emailmessenger.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
class ApiHealthController {

    private final Clock clock;
    private final String version;

    ApiHealthController(Clock clock,
                        @Value("${spring.application.version:0.1.0-SNAPSHOT}") String version) {
        this.clock = clock;
        this.version = version;
    }

    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("ok", "conexusmail-backend", version, Instant.now(clock));
    }

    record HealthResponse(String status, String service, String version, Instant time) {
    }
}
