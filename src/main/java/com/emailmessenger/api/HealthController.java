package com.emailmessenger.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

/**
 * Mobile-facing health probe. The Flutter client hits this on launch to confirm
 * it can reach the server. Public (no auth) so the app can distinguish "server
 * down" from "not signed in".
 */
@RestController
@RequestMapping("/api/v1")
class HealthController {

    static final String API_VERSION = "v1";

    private final Clock clock;

    HealthController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/health")
    Health health() {
        return new Health("ok", API_VERSION, Instant.now(clock).toString());
    }

    record Health(String status, String apiVersion, String serverTime) {}
}
