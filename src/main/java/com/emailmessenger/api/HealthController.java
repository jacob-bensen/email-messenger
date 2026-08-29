package com.emailmessenger.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
class HealthController {

    private final Clock clock;
    private final String version;

    HealthController(Clock clock, @Value("${info.build.version:0.0.0}") String version) {
        this.clock = clock;
        this.version = version;
    }

    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("ok", version, Instant.now(clock));
    }

    record HealthResponse(String status, String version, Instant time) {
    }
}
