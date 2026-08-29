package com.emailmessenger.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

/**
 * First mobile-facing REST endpoint. Called by the ConexusMail client at
 * startup to confirm connectivity and read the server's API version + time.
 * Documented in docs/API.md.
 */
@RestController
@RequestMapping("/api/v1")
class AppInfoController {

    private final Clock clock;

    AppInfoController(Clock clock) {
        this.clock = clock;
    }

    @GetMapping("/app-info")
    AppInfo appInfo() {
        return new AppInfo("ConexusMail", "v1", Instant.now(clock), "ok");
    }

    record AppInfo(String product, String apiVersion, Instant serverTime, String status) {}
}
