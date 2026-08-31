package com.emailmessenger.api;

import java.time.Instant;

/**
 * Shape returned by GET /api/v1/status. Mirrored by the mobile
 * client. If you change a field name, update docs/API.md AND the
 * client's DTO in the same commit.
 */
record StatusResponse(String service, String apiVersion, String status, Instant serverTime) {}
