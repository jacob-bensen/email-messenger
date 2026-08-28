package com.emailmessenger.api.v1;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON API for the mobile client. Contract lives in docs/API.md; keep this
 * controller and that file in lockstep.
 */
@RestController
@RequestMapping("/api/v1")
class PublicApiController {

    private final String appVersion;

    PublicApiController(@Value("${spring.application.version:0.1.0-SNAPSHOT}") String appVersion) {
        this.appVersion = appVersion;
    }

    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("ok", appVersion, Instant.now().toString());
    }

    record HealthResponse(String status, String version, String serverTime) {}
}
