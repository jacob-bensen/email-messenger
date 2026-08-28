package com.emailmessenger.api;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

record HealthResponse(
        String status,
        String version,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant serverTimeUtc) {
}
