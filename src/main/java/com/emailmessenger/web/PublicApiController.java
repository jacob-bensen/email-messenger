package com.emailmessenger.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;

/**
 * Anonymous JSON endpoints under {@code /api/v1} for the ConexusMail
 * mobile client. See {@code docs/API.md} for the contract.
 *
 * <p>Kept intentionally small; auth-bearing endpoints will land in a
 * separate controller (or a package-per-feature layout) as the mobile
 * app grows.
 */
@RestController
@RequestMapping("/api/v1")
class PublicApiController {

    private static final String API_NAMESPACE = "v1";

    private final Clock clock;
    private final String applicationName;
    private final BuildProperties buildProperties;

    PublicApiController(Clock clock,
                        @Value("${spring.application.name:email-messenger}") String applicationName,
                        ObjectProvider<BuildProperties> buildProperties) {
        this.clock = clock;
        this.applicationName = applicationName;
        this.buildProperties = buildProperties.getIfAvailable();
    }

    @GetMapping("/health")
    HealthResponse health() {
        return new HealthResponse("ok", Instant.now(clock));
    }

    @GetMapping("/version")
    VersionResponse version() {
        String name = applicationName;
        String version = "dev";
        if (buildProperties != null) {
            if (buildProperties.getName() != null && !buildProperties.getName().isBlank()) {
                name = buildProperties.getName();
            }
            if (buildProperties.getVersion() != null && !buildProperties.getVersion().isBlank()) {
                version = buildProperties.getVersion();
            }
        }
        return new VersionResponse(name, version, API_NAMESPACE);
    }

    record HealthResponse(String status, Instant timestamp) {}

    record VersionResponse(String name, String version, String api) {}
}
