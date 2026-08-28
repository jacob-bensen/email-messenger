package com.emailmessenger.web.api;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public bootstrap endpoint for the {@code conexus-mail-mobile} client.
 * Lets the mobile app prove connectivity to the API and later negotiate
 * capability flags. Kept dependency-free on purpose. See
 * {@code docs/API.md#public-endpoints-implemented}.
 */
@RestController
@RequestMapping("/api/v1")
class VersionController {

    static final String APP = "conexus-mail";
    static final String API_VERSION = "v1";
    static final String BUILD_VERSION = "0.1.0";

    @GetMapping(path = "/version", produces = "application/json")
    VersionResponse version() {
        return new VersionResponse(APP, API_VERSION, BUILD_VERSION, Instant.now());
    }

    record VersionResponse(String app, String apiVersion, String buildVersion, Instant serverTime) {}
}
