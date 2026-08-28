package com.emailmessenger.api;

/**
 * {@code GET /api/version} response body.
 *
 * <p>Wire format documented in {@code docs/API.md#versionresponse}.
 * The {@code serverTime} field is an ISO-8601 UTC string so client
 * platforms with different JSON date encodings all agree.
 */
public record VersionResponse(String app, String version, int api, String serverTime) {}
