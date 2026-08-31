package com.emailmessenger.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
class ApiHealthController {

    static final String API_VERSION = "v1";

    private final Clock clock;

    ApiHealthController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("ok", API_VERSION, Instant.now(clock));
    }
}
