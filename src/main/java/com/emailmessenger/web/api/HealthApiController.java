package com.emailmessenger.web.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated liveness endpoint the mobile client (see the
 * conexus-mail-mobile repo) pings on launch to gate on backend reach.
 *
 * <p>Contract lives in {@code docs/API.md} — do not change the payload
 * shape without updating that file and the mirrored Dart class in the
 * mobile repo in the same commit.
 */
@RestController
@RequestMapping("/api/v1")
class HealthApiController {

    static final String API_VERSION = "0.1.0";

    @GetMapping(path = "/health", produces = "application/json")
    HealthResponse health() {
        return new HealthResponse("UP", API_VERSION);
    }

    record HealthResponse(String status, String version) {}
}
