package com.emailmessenger.api;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * First endpoint on the mobile-facing REST surface. Public so the
 * client can verify reachability before the auth flow lands. Contract
 * lives in docs/API.md — keep the two in sync.
 */
@RestController
@RequestMapping("/api/v1")
class StatusApiController {

    @GetMapping("/status")
    StatusResponse status() {
        return new StatusResponse("email-messenger", "v1", "ok", Instant.now());
    }
}
