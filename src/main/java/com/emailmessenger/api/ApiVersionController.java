package com.emailmessenger.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated smoke-test endpoint for the ConexusMail mobile client. The
 * client hits this at startup to verify it can reach the configured server
 * and to inspect the wire-contract version — {@code apiVersion} bumps on
 * every incompatible change to any endpoint under {@code /api/v*}. Keep this
 * cheap: no DB, no auth, no side effects.
 */
@RestController
@RequestMapping("/api/v1")
class ApiVersionController {

    private final String appVersion;

    ApiVersionController(@Value("${info.app.version:0.1.0-SNAPSHOT}") String appVersion) {
        this.appVersion = appVersion;
    }

    @GetMapping("/version")
    VersionResponse version() {
        return new VersionResponse("conexusmail-backend", appVersion, 1);
    }

    record VersionResponse(String service, String version, int apiVersion) {}
}
