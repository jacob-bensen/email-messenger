package com.emailmessenger.api;

import java.time.Instant;

record HealthResponse(String status, String apiVersion, Instant serverTime) {
}
